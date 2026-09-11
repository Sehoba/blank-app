import Phaser from 'phaser';
import { GameInput } from './Input';
import { freshSave, loadSave, writeSave, maxHp, meleeDamage, eggDamage, moveMultiplier, xpToNext, type SaveData, type PlayerStats } from './types';

const evt=(name:string,detail:unknown={})=>window.dispatchEvent(new CustomEvent(name,{detail}));
type EKind='snake'|'monkey'|'idol'|'toucan'|'boar'|'beetle';
type Enemy={sprite:Phaser.GameObjects.Sprite;shadow:Phaser.GameObjects.Ellipse;kind:EKind;wx:number;wy:number;homeX:number;homeY:number;hp:number;xp:number;alive:boolean;hitCd:number};
type Egg={sprite:Phaser.GameObjects.Sprite;wx:number;wy:number;z:number;vx:number;vy:number;life:number};
type Feather={sprite:Phaser.GameObjects.Sprite;id:number;wx:number;wy:number;baseZ:number;phase:number};

const WORLD_W=5200, WORLD_D=760;
const SPAWNS:[EKind,number,number,number][]=[
['snake',760,560,3],['beetle',980,430,3],['monkey',1180,250,5],['boar',1580,610,6],['toucan',1780,220,4],
['idol',2050,470,7],['snake',2260,310,3],['monkey',2440,590,5],['beetle',2680,390,4],['boar',2850,620,6],
['toucan',3070,210,4],['idol',3310,480,7],['snake',3540,330,4],['monkey',3750,610,5],['boar',3970,520,6],
['toucan',4190,250,4],['idol',4380,470,7],['snake',4620,610,4],['beetle',4750,380,4],['monkey',4880,560,6]
];
const FEATHERS:[number,number][]=[[480,520],[1250,250],[2500,590],[3420,300],[4260,520]];

export class Level2Scene extends Phaser.Scene{
  private inputMap!:GameInput; private player!:Phaser.GameObjects.Sprite; private shadow!:Phaser.GameObjects.Ellipse; private save!:SaveData;
  private wx=150; private wy=550; private z=0; private vz=0; private faceX=1; private faceY=0;
  private enemies:Enemy[]=[]; private eggs:Egg[]=[]; private feathers:Feather[]=[];
  private boss?:Enemy; private bossHp=36; private bossClock=0; private phase=1; private inv=0; private attackCd=0; private powerCd=0; private shootCd=0;
  private jumpHeld=false; private attackHeld=false; private powerHeld=false; private shootHeld=false; private done=false; private started=0;
  private skillHandler=(e:Event)=>this.upgrade((e as CustomEvent).detail.key as keyof PlayerStats);
  constructor(){super('level2')}

  create(){
    this.save=loadSave()||freshSave(); this.inputMap=new GameInput(); this.started=this.time.now; this.makeTextures();
    const cp=this.save.level2Checkpoint||{x:150,y:550}; this.wx=cp.x; this.wy=Phaser.Math.Clamp(cp.y,80,WORLD_D-60);
    this.cameras.main.setBounds(0,0,5700,900).setBackgroundColor('#10261b'); this.drawWorld();
    this.shadow=this.add.ellipse(0,0,48,20,0x07110a,.42).setDepth(1);
    this.player=this.add.sprite(0,0,'mokuhero').setOrigin(.5,.84).setDepth(10); this.projectPlayer(); this.cameras.main.startFollow(this.player,true,.075,.075,0,30);
    this.enemies=SPAWNS.map(([kind,x,y,hp],id)=>this.makeEnemy(kind,x,y,hp,18+hp*3,id));
    if(!this.save.level2BossDefeated)this.boss=this.makeEnemy('idol',4930,420,36,260,99,true); else this.bossHp=0;
    FEATHERS.forEach(([x,y],id)=>{if(this.save.level2Feathers.includes(id))return;const s=this.add.sprite(0,0,'sunfeather').setOrigin(.5,.85);this.feathers.push({sprite:s,id,wx:x,wy:y,baseZ:34,phase:id*1.1})});
    this.save.hp=Math.max(1,Math.min(this.save.hp,maxHp(this.save.stats))); this.pushHud();
    window.addEventListener('skill-upgrade',this.skillHandler); this.events.once(Phaser.Scenes.Events.SHUTDOWN,()=>window.removeEventListener('skill-upgrade',this.skillHandler));
    evt('region',{name:'RUINEN VON MOKU · 2,5D',objective:'Erkunde die Ruinen frei, sammle 5 Sonnenfedern und besiege den Mooswächter'});
    this.toast('Level 2 · 2,5D-Ruinenmodus aktiv');
  }

  update(t:number,ms:number){
    if(this.done)return; const dt=Math.min(.04,ms/1000),i=this.inputMap.poll();
    this.inv=Math.max(0,this.inv-dt);this.attackCd=Math.max(0,this.attackCd-dt);this.powerCd=Math.max(0,this.powerCd-dt);this.shootCd=Math.max(0,this.shootCd-dt);
    const len=Math.hypot(i.x,i.y),nx=len>.12?i.x/Math.max(1,len):0,ny=len>.12?i.y/Math.max(1,len):0,speed=245*moveMultiplier(this.save.stats);
    if(len>.12){this.wx=Phaser.Math.Clamp(this.wx+nx*speed*dt,80,WORLD_W-80);this.wy=Phaser.Math.Clamp(this.wy+ny*speed*dt,90,WORLD_D-70);this.faceX=nx;this.faceY=ny;this.player.setFlipX(nx<-.15)}
    if(i.jump&&!this.jumpHeld&&this.z<=.1){this.vz=480;this.jumpHeld=true} if(!i.jump)this.jumpHeld=false;
    this.vz-=1080*dt;this.z+=this.vz*dt;if(this.z<0){this.z=0;this.vz=0}
    if(i.attack&&!this.attackHeld&&this.attackCd<=0){this.melee();this.attackHeld=true}if(!i.attack)this.attackHeld=false;
    if(i.power&&!this.powerHeld&&this.z<=1&&this.powerCd<=0){this.stomp();this.powerHeld=true}if(!i.power)this.powerHeld=false;
    if(i.shoot&&!this.shootHeld&&this.shootCd<=0){this.shoot();this.shootHeld=true}if(!i.shoot)this.shootHeld=false;
    this.player.setAlpha(this.inv>0&&Math.floor(this.time.now/75)%2?.35:1);this.projectPlayer();this.updateEnemies(dt);this.updateEggs(dt);this.updateFeathers(t);this.updateBoss(dt);this.checkContacts();
    if(this.wx>1500&&this.save.level2Checkpoint.x<1500)this.checkpoint(1580,this.wy);if(this.wx>3200&&this.save.level2Checkpoint.x<3200)this.checkpoint(3300,this.wy);this.save.playTime+=dt;
  }

  private proj(wx:number,wy:number,z=0){return{x:wx+(wy-380)*.42+180,y:255+wy*.56-z}}
  private projectPlayer(){const p=this.proj(this.wx,this.wy,this.z),g=this.proj(this.wx,this.wy,0);this.player.setPosition(p.x,p.y).setDepth(1000+g.y);this.shadow.setPosition(g.x,g.y+5).setScale(1-Math.min(.45,this.z/420)).setAlpha(.42-Math.min(.2,this.z/900)).setDepth(999+g.y)}
  private projectEnemy(e:Enemy,z=0){const p=this.proj(e.wx,e.wy,z);e.sprite.setPosition(p.x,p.y).setDepth(1000+p.y);const g=this.proj(e.wx,e.wy,0);e.shadow.setPosition(g.x,g.y+4).setDepth(999+g.y)}

  private makeEnemy(kind:EKind,x:number,y:number,hp:number,xp:number,id:number,boss=false):Enemy{
    const shadow=this.add.ellipse(0,0,boss?92:44,boss?30:16,0x07110a,.38);const sprite=this.add.sprite(0,0,boss?'mossboss':kind).setOrigin(.5,.82).setData('id',id);const e={sprite,shadow,kind,wx:x,wy:y,homeX:x,homeY:y,hp,xp,alive:true,hitCd:0};this.projectEnemy(e);return e
  }

  private updateEnemies(dt:number){for(const e of this.enemies){if(!e.alive)continue;e.hitCd=Math.max(0,e.hitCd-dt);const dx=this.wx-e.wx,dy=this.wy-e.wy,d=Math.hypot(dx,dy);let sx=0,sy=0;if(d<390){const sp=e.kind==='snake'?165:e.kind==='boar'?140:e.kind==='monkey'?125:e.kind==='toucan'?145:95;sx=dx/Math.max(1,d)*sp;sy=dy/Math.max(1,d)*sp}else{const hx=e.homeX-e.wx,hy=e.homeY-e.wy,hd=Math.hypot(hx,hy);if(hd>35){sx=hx/hd*42;sy=hy/hd*42}}
      e.wx=Phaser.Math.Clamp(e.wx+sx*dt,70,WORLD_W-70);e.wy=Phaser.Math.Clamp(e.wy+sy*dt,80,WORLD_D-60);e.sprite.setFlipX(dx<0);this.projectEnemy(e)} }

  private updateBoss(dt:number){const b=this.boss;if(!b||!b.alive||this.save.level2BossDefeated)return;if(this.wx<4450)return;this.phase=this.bossHp>24?1:this.bossHp>12?2:3;evt('boss',{show:true,hp:this.bossHp/36,name:`MOOSWÄCHTER · PHASE ${this.phase}`});this.bossClock-=dt;const dx=this.wx-b.wx,dy=this.wy-b.wy,d=Math.hypot(dx,dy);if(d>85){const sp=this.phase===3?210:this.phase===2?165:120;b.wx+=dx/Math.max(1,d)*sp*dt;b.wy+=dy/Math.max(1,d)*sp*dt}if(this.bossClock<=0){this.bossClock=this.phase===1?1.35:this.phase===2?.9:.55;this.cameras.main.shake(100,.004+this.phase*.002);if(d<240){b.wx-=dx/Math.max(1,d)*(30+this.phase*12);b.wy-=dy/Math.max(1,d)*(30+this.phase*12)}}this.projectEnemy(b)}

  private updateEggs(dt:number){for(let n=this.eggs.length-1;n>=0;n--){const e=this.eggs[n];e.life-=dt;e.wx+=e.vx*dt;e.wy+=e.vy*dt;const p=this.proj(e.wx,e.wy,e.z);e.sprite.setPosition(p.x,p.y).setDepth(1000+p.y);let hit=false;for(const mob of this.enemies){if(mob.alive&&Math.hypot(mob.wx-e.wx,mob.wy-e.wy)<42){this.hitEnemy(mob,eggDamage(this.save.stats));hit=true;break}}if(!hit&&this.boss?.alive&&Math.hypot(this.boss.wx-e.wx,this.boss.wy-e.wy)<75){this.hitBoss(eggDamage(this.save.stats));hit=true}if(hit||e.life<=0||e.wx<0||e.wx>WORLD_W||e.wy<0||e.wy>WORLD_D){e.sprite.destroy();this.eggs.splice(n,1)}}}
  private updateFeathers(t:number){for(const f of this.feathers){if(!f.sprite.active)continue;const z=f.baseZ+Math.sin(t*.003+f.phase)*10,p=this.proj(f.wx,f.wy,z);f.sprite.setPosition(p.x,p.y).setDepth(1000+p.y);if(Math.hypot(this.wx-f.wx,this.wy-f.wy)<48&&this.z<85)this.collect(f)}}
  private checkContacts(){if(this.inv>0||this.z>55)return;for(const e of this.enemies){if(e.alive&&Math.hypot(this.wx-e.wx,this.wy-e.wy)<45){this.hurt(e.wx,e.wy,2);return}}if(this.boss?.alive&&Math.hypot(this.wx-this.boss.wx,this.wy-this.boss.wy)<75)this.hurt(this.boss.wx,this.boss.wy,3)}

  private melee(){this.attackCd=.27;const dmg=meleeDamage(this.save.stats);for(const e of this.enemies){if(!e.alive)continue;const dx=e.wx-this.wx,dy=e.wy-this.wy,d=Math.hypot(dx,dy),dot=(dx/Math.max(1,d))*this.faceX+(dy/Math.max(1,d))*this.faceY;if(d<105&&dot>.05)this.hitEnemy(e,dmg)}if(this.boss?.alive){const d=Math.hypot(this.boss.wx-this.wx,this.boss.wy-this.wy);if(d<135)this.hitBoss(dmg)}}
  private stomp(){this.powerCd=1.0;this.cameras.main.shake(180,.013);const dmg=1+meleeDamage(this.save.stats);for(const e of this.enemies)if(e.alive&&Math.hypot(e.wx-this.wx,e.wy-this.wy)<185)this.hitEnemy(e,dmg);if(this.boss?.alive&&Math.hypot(this.boss.wx-this.wx,this.boss.wy-this.wy)<210)this.hitBoss(dmg)}
  private shoot(){this.shootCd=Math.max(.21,.56-(this.save.stats.agility-1)*.024);let dx=this.faceX,dy=this.faceY;if(Math.hypot(dx,dy)<.2){dx=1;dy=0}const l=Math.hypot(dx,dy),sp=500+this.save.stats.eggPower*10,s=this.add.sprite(0,0,'egg').setOrigin(.5);this.eggs.push({sprite:s,wx:this.wx+dx/l*42,wy:this.wy+dy/l*42,z:42,vx:dx/l*sp,vy:dy/l*sp,life:1.7})}
  private hitEnemy(e:Enemy,dmg:number){if(!e.alive)return;e.hp-=dmg;e.sprite.setTintFill(0xffffff);this.time.delayedCall(70,()=>e.alive&&e.sprite.clearTint());if(e.hp<=0){e.alive=false;e.sprite.destroy();e.shadow.destroy();this.addXp(e.xp)}}
  private hitBoss(dmg:number){const b=this.boss;if(!b||!b.alive||this.wx<4380)return;if(this.save.level2Feathers.length<3){this.toast('Der Wächter ist versiegelt · sammle mindestens 3 Sonnenfedern');return}this.bossHp-=dmg;b.sprite.setTintFill(0xffffff);this.time.delayedCall(80,()=>b.alive&&b.sprite.clearTint());if(this.bossHp<=0){b.alive=false;b.sprite.destroy();b.shadow.destroy();this.save.level2BossDefeated=true;this.addXp(260);writeSave(this.save);evt('boss',{show:false});this.done=true;evt('level2-victory',{level:this.save.stats.level})}}
  private hurt(ex:number,ey:number,raw:number){if(this.inv>0||this.done)return;this.inv=1.1;const dmg=Math.max(1,raw-Math.floor((this.save.stats.defense-1)/3));this.save.hp-=dmg;const dx=this.wx-ex,dy=this.wy-ey,d=Math.max(1,Math.hypot(dx,dy));this.wx=Phaser.Math.Clamp(this.wx+dx/d*55,60,WORLD_W-60);this.wy=Phaser.Math.Clamp(this.wy+dy/d*55,70,WORLD_D-50);this.vz=210;evt('health',{hp:this.save.hp,maxHp:maxHp(this.save.stats)});if(this.save.hp<=0)this.time.delayedCall(400,()=>this.respawn())}
  private respawn(){this.save.hp=maxHp(this.save.stats);this.wx=this.save.level2Checkpoint.x;this.wy=this.save.level2Checkpoint.y;this.z=0;this.vz=0;this.inv=1.5;writeSave(this.save);this.pushHud();this.toast('Moku-Speicherstein aktiviert')}
  private checkpoint(x:number,y:number){this.save.level2Checkpoint={x,y:Phaser.Math.Clamp(y,100,WORLD_D-80)};writeSave(this.save);this.toast('Ruinen-Speicherstein gesichert')}
  private collect(f:Feather){if(this.save.level2Feathers.includes(f.id))return;this.save.level2Feathers.push(f.id);f.sprite.destroy();this.addXp(18);writeSave(this.save);evt('feathers',{count:this.save.level2Feathers.length,total:5});this.toast(`Sonnenfeder ${this.save.level2Feathers.length}/5`)}

  private makeTextures(){const g=this.add.graphics().setVisible(false),mk=(k:string,w:number,h:number,f:()=>void)=>{if(this.textures.exists(k))return;g.clear();f();g.generateTexture(k,w,h)};mk('mokuhero',72,82,()=>{g.fillStyle(0x4e352d).fillCircle(32,34,25).fillRoundedRect(12,32,42,43,15);g.fillStyle(0xb98a68).fillEllipse(31,32,26,19);g.fillStyle(0x151313).fillCircle(25,27,3).fillCircle(38,27,3);g.fillStyle(0x1aa6a2).fillRoundedRect(50,27,17,39,6);g.fillStyle(0xf15c83).fillRect(52,23,12,7).fillRect(58,10,7,16)});mk('sunfeather',30,40,()=>{g.fillStyle(0xffb72f).fillEllipse(12,15,18,29);g.fillStyle(0xffff9a).fillTriangle(12,1,24,16,12,30);g.lineStyle(3,0x8f5520).lineBetween(12,13,21,38)});const mob=(k:string,c:number)=>mk(k,58,48,()=>{g.fillStyle(c).fillRoundedRect(6,10,46,32,13);g.fillStyle(0xe9f6dd).fillCircle(16,17,5);g.fillStyle(0x101510).fillCircle(15,17,2);g.fillStyle(0x2a2018).fillRect(12,39,10,7).fillRect(38,39,10,7)});mob('snake',0x598b3d);mob('monkey',0x8b5b36);mob('idol',0x7b714e);mob('toucan',0x36302c);mob('boar',0x995143);mob('beetle',0x496ea5);mk('egg',24,18,()=>g.fillStyle(0xffffff).fillEllipse(12,9,22,16).fillStyle(0xffe1a0).fillEllipse(8,6,6,4));mk('mossboss',146,150,()=>{g.fillStyle(0x31543b).fillRoundedRect(12,34,122,108,34);g.fillStyle(0x517459).fillCircle(73,44,49);g.fillStyle(0xa9cf61).fillCircle(34,32,18).fillCircle(112,38,22).fillCircle(76,12,21);g.fillStyle(0xff7a3f).fillTriangle(35,48,62,53,40,62).fillTriangle(111,48,84,53,106,62);g.fillStyle(0x182318).fillEllipse(73,87,48,24)})}
  private drawWorld(){const g=this.add.graphics().setDepth(-20);g.fillGradientStyle(0x173b2e,0x10281e,0x07130d,0x07130d).fillRect(0,0,5700,900);for(let x=0;x<5400;x+=180){const a=this.proj(x,100),b=this.proj(x+120,100),c=this.proj(x+120,690),d=this.proj(x,690);g.fillStyle((Math.floor(x/180)%2)?0x2b5138:0x315b3d,.55).fillPoints([a,b,c,d],true)}for(let x=360;x<5200;x+=520){for(const y of [150,650]){const p=this.proj(x,y);g.fillStyle(0x596e56,.85).fillRect(p.x-19,p.y-125,38,125);g.fillStyle(0x829275,.72).fillRect(p.x-32,p.y-132,64,18);g.fillStyle(0x5f9c55,.3).fillCircle(p.x,p.y-115,34)}}for(let x=720;x<5000;x+=760){const p=this.proj(x,380);g.lineStyle(5,0xa3cb65,.24).lineBetween(p.x-70,p.y-130,p.x+80,p.y+30)}}
  private addXp(n:number){this.save.stats.xp+=n;while(this.save.stats.xp>=xpToNext(this.save.stats.level)){this.save.stats.xp-=xpToNext(this.save.stats.level);this.save.stats.level++;this.save.stats.skillPoints+=2;this.save.hp=maxHp(this.save.stats);this.toast(`LEVEL ${this.save.stats.level}! +2 Skillpunkte`)}writeSave(this.save);evt('rpg',{stats:this.save.stats,next:xpToNext(this.save.stats.level)})}
  private upgrade(k:keyof PlayerStats){if(k==='level'||k==='xp'||k==='skillPoints'||this.save.stats.skillPoints<1)return;const v=this.save.stats[k];if(typeof v!=='number')return;(this.save.stats[k] as number)=v+1;this.save.stats.skillPoints--;this.save.hp=Math.min(maxHp(this.save.stats),this.save.hp+1);writeSave(this.save);this.pushHud();this.toast(`${String(k)} verbessert`)}
  private pushHud(){evt('hud',{hp:this.save.hp,maxHp:maxHp(this.save.stats),feathers:this.save.level2Feathers.length,total:5});evt('rpg',{stats:this.save.stats,next:xpToNext(this.save.stats.level)})}
  private toast(message:string){evt('toast',{message})}
}
