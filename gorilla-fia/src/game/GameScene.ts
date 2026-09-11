import Phaser from 'phaser';
import { GameInput } from './Input';
import { FEATHER_POSITIONS, MOB_SPAWNS, REGIONS, WORLD_W, freshSave, loadSave, writeSave, maxHp, meleeDamage, eggDamage, moveMultiplier, xpToNext, type SaveData, type PlayerStats } from './types';

const evt=(name:string,detail:unknown={})=>window.dispatchEvent(new CustomEvent(name,{detail}));
type MobKind='beetle'|'boar'|'toucan'|'idol'|'snake'|'monkey';
type Mob={sprite:Phaser.GameObjects.Sprite;shadow:Phaser.GameObjects.Ellipse;wx:number;wy:number;hp:number;xp:number;kind:MobKind;homeX:number;homeY:number;active:boolean};
type Egg={sprite:Phaser.GameObjects.Sprite;wx:number;wy:number;vx:number;vy:number;born:number;active:boolean};
type Feather={sprite:Phaser.GameObjects.Sprite;wx:number;wy:number;id:number;active:boolean};

export class GameScene extends Phaser.Scene{
  private controls!:GameInput; private player!:Phaser.GameObjects.Sprite; private shadow!:Phaser.GameObjects.Ellipse; private save!:SaveData;
  private wx=180; private wy=390; private z=0; private vz=0; private faceX=1; private faceY=0;
  private mobs:Mob[]=[]; private eggs:Egg[]=[]; private feathers:Feather[]=[];
  private jumpHeld=false; private attackHeld=false; private powerHeld=false; private shootHeld=false; private inv=0; private attackCd=0; private powerCd=0; private shootCd=0; private region=-1; private started=0; private done=false;
  private boss?:Mob; private bossHp=30; private skillHandler=(e:Event)=>this.upgrade((e as CustomEvent).detail.key as keyof PlayerStats);
  constructor(){super('game')}

  create(data:{continue?:boolean}){
    this.save=data?.continue&&loadSave()?loadSave()!:freshSave(); this.started=this.time.now; this.controls=new GameInput(); this.makeTextures();
    this.cameras.main.setBackgroundColor('#0f2d2c'); this.cameras.main.setBounds(0,0,WORLD_W+900,980);
    this.decorate25D();
    this.wx=Phaser.Math.Clamp(this.save.checkpoint.x,120,WORLD_W-120); this.wy=390;
    this.shadow=this.add.ellipse(0,0,58,22,0x06140f,.42).setDepth(4);
    this.player=this.add.sprite(0,0,'hero').setDepth(8).setScale(1.05); this.place(this.player,this.wx,this.wy,this.z); this.placeShadow(this.shadow,this.wx,this.wy);
    this.cameras.main.startFollow(this.player,true,.07,.07,0,80); this.cameras.main.setZoom(1.04);
    this.spawnMobs(); this.spawnFeathers(); this.spawnBoss();
    window.addEventListener('skill-upgrade',this.skillHandler); this.events.once(Phaser.Scenes.Events.SHUTDOWN,()=>window.removeEventListener('skill-upgrade',this.skillHandler));
    this.save.hp=Math.max(1,Math.min(this.save.hp,maxHp(this.save.stats))); this.pushHud(); this.updateRegion(true);
    this.toast('Level 1 · 2,5D-Modus aktiv · freie Bewegung + Sprung + Eischuss');
  }

  update(_t:number,ms:number){
    if(this.done)return; const dt=Math.min(.04,ms/1000),i=this.controls.poll();
    this.inv=Math.max(0,this.inv-dt); this.attackCd=Math.max(0,this.attackCd-dt); this.powerCd=Math.max(0,this.powerCd-dt); this.shootCd=Math.max(0,this.shootCd-dt);
    const mul=moveMultiplier(this.save.stats),len=Math.hypot(i.x,i.y)||1,dx=Math.abs(i.x)>.08?i.x/len:0,dy=Math.abs(i.y)>.08?i.y/len:0,speed=265*mul;
    this.wx=Phaser.Math.Clamp(this.wx+dx*speed*dt,70,WORLD_W-70); this.wy=Phaser.Math.Clamp(this.wy+dy*speed*.82*dt,90,720);
    if(Math.abs(dx)+Math.abs(dy)>.08){this.faceX=dx;this.faceY=dy;this.player.setFlipX(dx<-.12)}
    const grounded=this.z<=.01;
    if(i.jump&&!this.jumpHeld&&grounded){this.vz=520;this.jumpHeld=true} if(!i.jump)this.jumpHeld=false;
    this.vz-=1180*dt; this.z+=this.vz*dt; if(this.z<0){this.z=0;this.vz=0}
    if(i.attack&&!this.attackHeld&&this.attackCd<=0){this.melee();this.attackHeld=true} if(!i.attack)this.attackHeld=false;
    if(i.power&&!this.powerHeld&&grounded&&this.powerCd<=0){this.stomp();this.powerHeld=true} if(!i.power)this.powerHeld=false;
    if(i.shoot&&!this.shootHeld&&this.shootCd<=0){this.shoot();this.shootHeld=true} if(!i.shoot)this.shootHeld=false;
    this.place(this.player,this.wx,this.wy,this.z); this.placeShadow(this.shadow,this.wx,this.wy); this.shadow.setScale(Phaser.Math.Clamp(1-this.z/850,.55,1));
    this.player.setAlpha(this.inv>0&&Math.floor(this.time.now/80)%2?.35:1);
    this.updateMobs(dt); this.updateEggs(dt); this.updateBoss(dt); this.updateRegion(false); this.save.playTime+=dt;
  }

  private project(wx:number,wy:number,z=0){return{x:wx+(wy-390)*.38,y:520+(wy-390)*.54-z}}
  private place(o:Phaser.GameObjects.Sprite,wx:number,wy:number,z=0){const p=this.project(wx,wy,z);o.setPosition(p.x,p.y).setDepth(10+p.y*.01+z*.001)}
  private placeShadow(o:Phaser.GameObjects.Ellipse,wx:number,wy:number){const p=this.project(wx,wy,0);o.setPosition(p.x,p.y+30).setDepth(5+p.y*.01)}

  private spawnMobs(){
    const kinds:MobKind[]=['beetle','boar','toucan','idol','snake','monkey'];
    this.mobs=MOB_SPAWNS.map((m,idx)=>{const wy=150+((idx*173+m.x*.17)%500),kind=(m.kind||kinds[idx%kinds.length]) as MobKind;const shadow=this.add.ellipse(0,0,46,16,0x06140f,.34);const sprite=this.add.sprite(0,0,kind);const mob:Mob={sprite,shadow,wx:m.x,wy,hp:m.hp,xp:m.xp||15,kind,homeX:m.x,homeY:wy,active:true};this.place(sprite,mob.wx,mob.wy);this.placeShadow(shadow,mob.wx,mob.wy);return mob});
  }
  private spawnFeathers(){this.feathers=[];FEATHER_POSITIONS.forEach((f,id)=>{if(this.save.feathers.includes(id))return;const wy=130+((id*211+f.x*.11)%520),sprite=this.add.sprite(0,0,'feather');const it:Feather={sprite,wx:f.x,wy,id,active:true};this.place(sprite,it.wx,it.wy,18);this.feathers.push(it)})}
  private spawnBoss(){if(this.save.bossDefeated)return;const shadow=this.add.ellipse(0,0,120,34,0x000000,.45),sprite=this.add.sprite(0,0,'boss').setScale(1.08);this.boss={sprite,shadow,wx:8910,wy:390,hp:this.bossHp,xp:180,kind:'idol',homeX:8910,homeY:390,active:true};this.place(sprite,8910,390);this.placeShadow(shadow,8910,390)}

  private updateMobs(dt:number){
    for(const m of this.mobs){if(!m.active)continue;const dx=this.wx-m.wx,dy=this.wy-m.wy,d=Math.hypot(dx,dy);let speed=m.kind==='snake'?170:m.kind==='boar'?145:m.kind==='monkey'?125:95;
      if(d<330){m.wx+=dx/(d||1)*speed*dt;m.wy+=dy/(d||1)*speed*.82*dt}else{const hx=m.homeX-m.wx,hy=m.homeY-m.wy,hd=Math.hypot(hx,hy);if(hd>18){m.wx+=hx/hd*40*dt;m.wy+=hy/hd*32*dt}}
      if(d<52&&this.z<55)this.hurt(m.wx,m.wy);m.sprite.setFlipX(dx<0);this.place(m.sprite,m.wx,m.wy);this.placeShadow(m.shadow,m.wx,m.wy);
    }
    for(const f of this.feathers){if(!f.active)continue;if(Math.hypot(this.wx-f.wx,this.wy-f.wy)<48&&this.z<85)this.collect(f);this.place(f.sprite,f.wx,f.wy,18+Math.sin(this.time.now/230+f.id)*7)}
  }
  private updateBoss(dt:number){const b=this.boss;if(!b?.active||this.save.bossDefeated)return;const dx=this.wx-b.wx,dy=this.wy-b.wy,d=Math.hypot(dx,dy);if(this.wx>8400){evt('boss',{show:true,hp:this.bossHp/30,name:'DER HOHLKÖNIG'});const speed=this.bossHp<10?185:110;b.wx+=dx/(d||1)*speed*dt;b.wy+=dy/(d||1)*speed*.76*dt;if(d<78&&this.z<70)this.hurt(b.wx,b.wy);this.place(b.sprite,b.wx,b.wy);this.placeShadow(b.shadow,b.wx,b.wy)}}
  private updateEggs(dt:number){for(const e of this.eggs){if(!e.active)continue;e.wx+=e.vx*dt;e.wy+=e.vy*dt;this.place(e.sprite,e.wx,e.wy,34);for(const m of this.mobs){if(m.active&&Math.hypot(e.wx-m.wx,e.wy-m.wy)<40){e.active=false;e.sprite.destroy();this.hitEnemy(m,eggDamage(this.save.stats));break}}if(e.active&&this.boss?.active&&Math.hypot(e.wx-this.boss.wx,e.wy-this.boss.wy)<72){e.active=false;e.sprite.destroy();this.hitBoss(eggDamage(this.save.stats))}if(this.time.now-e.born>1700||e.wx<0||e.wx>WORLD_W){e.active=false;e.sprite.destroy()}}this.eggs=this.eggs.filter(e=>e.active)}

  private melee(){this.attackCd=.28;const dmg=meleeDamage(this.save.stats);for(const m of this.mobs){if(!m.active)continue;const dx=m.wx-this.wx,dy=m.wy-this.wy,d=Math.hypot(dx,dy),dot=(dx/(d||1))*this.faceX+(dy/(d||1))*this.faceY;if(d<105&&dot>.05)this.hitEnemy(m,dmg)}if(this.boss?.active&&Math.hypot(this.boss.wx-this.wx,this.boss.wy-this.wy)<125)this.hitBoss(dmg)}
  private stomp(){this.powerCd=1.05;this.cameras.main.shake(170,.009);for(const m of this.mobs)if(m.active&&Math.hypot(m.wx-this.wx,m.wy-this.wy)<180)this.hitEnemy(m,1+meleeDamage(this.save.stats));if(this.boss?.active&&Math.hypot(this.boss.wx-this.wx,this.boss.wy-this.wy)<205)this.hitBoss(1+meleeDamage(this.save.stats))}
  private shoot(){this.shootCd=Math.max(.22,.60-(this.save.stats.agility-1)*.025);let fx=this.faceX,fy=this.faceY;if(Math.abs(fx)+Math.abs(fy)<.05){fx=1;fy=0}const l=Math.hypot(fx,fy)||1,s=500+this.save.stats.eggPower*10,sprite=this.add.sprite(0,0,'egg');const e:Egg={sprite,wx:this.wx+fx/l*38,wy:this.wy+fy/l*38,vx:fx/l*s,vy:fy/l*s*.82,born:this.time.now,active:true};this.eggs.push(e)}
  private hitEnemy(m:Mob,dmg:number){m.hp-=dmg;m.sprite.setTintFill(0xffffff);this.time.delayedCall(70,()=>m.active&&m.sprite.clearTint());if(m.hp<=0){m.active=false;m.sprite.setVisible(false);m.shadow.setVisible(false);this.addXp(m.xp)}}
  private hurt(ex:number,ey:number){if(this.inv>0||this.done)return;this.inv=1.1;const dmg=Math.max(1,2-Math.floor((this.save.stats.defense-1)/3));this.save.hp-=dmg;const dx=this.wx-ex,dy=this.wy-ey,l=Math.hypot(dx,dy)||1;this.wx+=dx/l*65;this.wy+=dy/l*50;this.vz=220;evt('health',{hp:this.save.hp,maxHp:maxHp(this.save.stats)});if(this.save.hp<=0)this.time.delayedCall(350,()=>this.respawn())}
  private respawn(){this.save.hp=maxHp(this.save.stats);this.wx=this.save.checkpoint.x;this.wy=390;this.z=0;this.vz=0;this.inv=1.4;this.pushHud();this.toast('Zur letzten gespeicherten Position zurück')}
  private collect(f:Feather){if(!f.active||this.save.feathers.includes(f.id))return;f.active=false;f.sprite.destroy();this.save.feathers.push(f.id);this.addXp(8);writeSave(this.save);evt('feathers',{count:this.save.feathers.length,total:30});this.toast(`Kronenfeder ${this.save.feathers.length}/30`)}
  private hitBoss(dmg:number){if(!this.boss?.active||this.wx<8300)return;this.bossHp-=dmg;evt('boss',{show:true,hp:Math.max(0,this.bossHp)/30,name:'DER HOHLKÖNIG'});this.cameras.main.shake(90,.005);if(this.bossHp<=0){this.boss.active=false;this.boss.sprite.setVisible(false);this.boss.shadow.setVisible(false);this.save.bossDefeated=true;this.save.level2Unlocked=true;this.addXp(180);writeSave(this.save);evt('boss',{show:false});this.done=true;evt('victory',{time:Math.floor((this.time.now-this.started)/1000),feathers:this.save.feathers.length})}}

  private makeTextures(){const g=this.add.graphics().setVisible(false),mk=(k:string,w:number,h:number,fn:()=>void)=>{if(this.textures.exists(k))return;g.clear();fn();g.generateTexture(k,w,h)};mk('hero',72,82,()=>{g.fillStyle(0x4d352d).fillCircle(32,34,25).fillRoundedRect(12,32,42,43,15);g.fillStyle(0xb98a68).fillEllipse(31,32,26,19);g.fillStyle(0x111111).fillCircle(25,27,3).fillCircle(38,27,3);g.fillStyle(0x1aa6a2).fillRoundedRect(50,27,17,39,6);g.fillStyle(0xf15c83).fillRect(52,23,12,7).fillRect(58,10,7,16)});mk('feather',28,38,()=>{g.fillStyle(0xffdc4c).fillEllipse(11,14,17,27);g.fillStyle(0xfff2a1).fillTriangle(11,1,22,15,11,28);g.lineStyle(3,0x85531f).lineBetween(11,13,20,36)});const mob=(k:string,c:number)=>mk(k,58,48,()=>{g.fillStyle(c).fillRoundedRect(6,10,46,32,13);g.fillStyle(0xffffff).fillCircle(16,17,5);g.fillStyle(0x111111).fillCircle(15,17,2);g.fillStyle(0x241b18).fillRect(12,39,10,7).fillRect(38,39,10,7)});mob('beetle',0x496ea5);mob('boar',0x9b5143);mob('toucan',0x332f2d);mob('idol',0x7b714e);mob('snake',0x5d9146);mob('monkey',0x8b5b36);mk('egg',24,18,()=>g.fillStyle(0xffffff).fillEllipse(12,9,22,16).fillStyle(0xffe4a0).fillEllipse(8,6,6,4));mk('boss',132,138,()=>{g.fillStyle(0x4d3a51).fillRoundedRect(12,28,108,103,32);g.fillStyle(0x302333).fillCircle(66,40,44);g.fillStyle(0xff4657).fillTriangle(31,41,56,47,36,55).fillTriangle(101,41,76,47,96,55);g.fillStyle(0xe2c769).fillTriangle(26,24,42,2,55,28).fillTriangle(55,22,70,0,84,23).fillTriangle(82,28,105,4,113,36)})}
  private decorate25D(){const g=this.add.graphics().setDepth(-20);for(const r of REGIONS){const a=this.project(r.x,90),b=this.project(r.x+r.width,90),c=this.project(r.x+r.width,720),d=this.project(r.x,720);g.fillStyle(r.sky,.72).fillPoints([a,b,c,d],true);g.lineStyle(2,0xffffff,.05).strokePoints([a,b,c,d],true)}for(let x=100;x<WORLD_W;x+=220){const wy=120+((x*1.73)%560),p=this.project(x,wy);g.fillStyle(0x183f2a,.8).fillEllipse(p.x,p.y,92,38);g.fillStyle(0x2c6b3e,.65).fillEllipse(p.x+14,p.y-8,62,28)}for(let x=180;x<WORLD_W;x+=460){const wy=180+((x*.91)%430),p=this.project(x,wy);g.fillStyle(0x5b4b37,.8).fillRect(p.x-7,p.y-72,14,76);g.fillStyle(0x255f33,.9).fillCircle(p.x,p.y-82,38)}}
  private addXp(n:number){this.save.stats.xp+=n;while(this.save.stats.xp>=xpToNext(this.save.stats.level)){this.save.stats.xp-=xpToNext(this.save.stats.level);this.save.stats.level++;this.save.stats.skillPoints+=2;this.save.hp=maxHp(this.save.stats);this.toast(`LEVEL ${this.save.stats.level}! +2 Skillpunkte`)}writeSave(this.save);evt('rpg',{stats:this.save.stats,next:xpToNext(this.save.stats.level)})}
  private upgrade(k:keyof PlayerStats){if(k==='level'||k==='xp'||k==='skillPoints'||this.save.stats.skillPoints<1)return;const v=this.save.stats[k];if(typeof v!=='number')return;(this.save.stats[k] as number)=v+1;this.save.stats.skillPoints--;this.save.hp=Math.min(maxHp(this.save.stats),this.save.hp+1);writeSave(this.save);this.pushHud();this.toast(`${String(k)} verbessert`)}
  private updateRegion(force:boolean){const idx=REGIONS.findIndex(r=>this.wx>=r.x&&this.wx<r.x+r.width);if(idx>=0&&(force||idx!==this.region)){this.region=idx;const r=REGIONS[idx];this.save.checkpoint={x:Math.max(150,r.x+120),y:520};writeSave(this.save);evt('region',{name:r.name,objective:r.objective})}}
  private pushHud(){evt('hud',{hp:this.save.hp,maxHp:maxHp(this.save.stats),feathers:this.save.feathers.length,total:30});evt('rpg',{stats:this.save.stats,next:xpToNext(this.save.stats.level)})}
  private toast(message:string){evt('toast',{message})}
}
