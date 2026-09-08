import Phaser from 'phaser';
import './style.css';
import { GameScene } from './game/GameScene';
import { Level2Scene } from './game/Level2Scene';
import { installVisualStyle } from './game/VisualStyle';
import { clearSave, loadSave, maxHp, xpToNext, type PlayerStats } from './game/types';

const $=<T extends HTMLElement>(q:string)=>document.querySelector<T>(q)!;
const menu=$('#menu'),hud=$('#hud'),modal=$('#modal'),skillpanel=$('#skillpanel'),start=$<HTMLButtonElement>('#start'),cont=$<HTMLButtonElement>('#continue'),level2btn=$<HTMLButtonElement>('#level2');
const hearts=$('#hearts'),feathers=$('#feathers'),region=$('#region'),objective=$('#objective'),toast=$('#toast'),bossbar=$('#bossbar'),bossfill=$('#bossfill'),bossname=$('#bossname');
const levelEl=$('#level'),xpfill=$('#xpfill'),xptext=$('#xptext'),skillsummary=$('#skillsummary');
let game:Phaser.Game|null=null;let toastTimer=0;let activeScene='game';let modalAction:()=>void=()=>game?.scene.resume(activeScene);

function boot(scene:'game'|'level2',continuing:boolean){
  activeScene=scene;menu.classList.add('hidden');modal.classList.add('hidden');skillpanel.classList.add('hidden');hud.classList.remove('hidden');bossbar.classList.add('hidden');
  if(game){game.destroy(true);document.querySelector('#game')!.innerHTML=''}
  game=new Phaser.Game({type:Phaser.AUTO,parent:'game',backgroundColor:'#101714',pixelArt:true,antialias:false,scale:{mode:Phaser.Scale.RESIZE,autoCenter:Phaser.Scale.CENTER_BOTH,width:1280,height:720},physics:{default:'arcade',arcade:{gravity:{x:0,y:1450},debug:false}},scene:[GameScene,Level2Scene],render:{roundPixels:true,powerPreference:'high-performance'},input:{activePointers:6}});
  game.events.once(Phaser.Core.Events.READY,()=>{if(!game)return;installVisualStyle(game);game.scene.start(scene,scene==='game'?{continue:continuing}:{continue:continuing})});
}
function backToMenu(){game?.destroy(true);game=null;document.querySelector('#game')!.innerHTML='';hud.classList.add('hidden');modal.classList.add('hidden');skillpanel.classList.add('hidden');menu.classList.remove('hidden');refreshMenu()}
function hp(n:number,max=5){hearts.innerHTML='';for(let i=0;i<max;i++){const s=document.createElement('span');s.textContent=i<n?'♥':'♡';hearts.append(s)}}
function showToast(message:string){toast.textContent=message;toast.classList.add('show');clearTimeout(toastTimer);toastTimer=window.setTimeout(()=>toast.classList.remove('show'),1600)}
function showModal(title:string,text:string,resumeLabel='WEITER',action?:()=>void){game?.scene.pause(activeScene);$('#modal-title').textContent=title;$('#modal-text').textContent=text;$<HTMLButtonElement>('#resume').textContent=resumeLabel;modalAction=action||(()=>game?.scene.resume(activeScene));modal.classList.remove('hidden')}
function updateRpg(stats:PlayerStats,next:number){levelEl.textContent=String(stats.level);xptext.textContent=`XP ${stats.xp}/${next}`;xpfill.style.width=`${Math.min(100,stats.xp/next*100)}%`;skillsummary.textContent=`Level ${stats.level} · ${stats.skillPoints} Skillpunkte`;
  (['strength','defense','vitality','agility','eggPower'] as const).forEach(k=>{$(`#stat-${k}`).textContent=String(stats[k])});document.querySelectorAll<HTMLButtonElement>('[data-skill]').forEach(b=>b.disabled=stats.skillPoints<=0)}
function refreshMenu(){const s=loadSave();cont.classList.toggle('hidden',!s);if(s){start.textContent='NEUES ABENTEUER';level2btn.textContent=s.level2Unlocked?'LEVEL 2 · RUINEN VON MOKU':'LEVEL 2 · TESTMODUS'}else start.textContent='NEUES ABENTEUER'}

window.addEventListener('hud',(e:any)=>{hp(e.detail.hp,e.detail.maxHp||5);const total=e.detail.total||30;feathers.textContent=`${e.detail.feathers}/${total}`});
window.addEventListener('health',(e:any)=>hp(e.detail.hp,e.detail.maxHp||5));
window.addEventListener('feathers',(e:any)=>feathers.textContent=`${e.detail.count}/${e.detail.total||30}`);
window.addEventListener('region',(e:any)=>{region.textContent=e.detail.name;objective.textContent=e.detail.objective});
window.addEventListener('toast',(e:any)=>showToast(e.detail.message));
window.addEventListener('rpg',(e:any)=>updateRpg(e.detail.stats,e.detail.next));
window.addEventListener('boss',(e:any)=>{bossbar.classList.toggle('hidden',!e.detail.show);if(e.detail.show){bossfill.style.width=`${Math.max(0,e.detail.hp)*100}%`;bossname.textContent=e.detail.name}});
window.addEventListener('victory',(e:any)=>{const m=Math.floor(e.detail.time/60),s=e.detail.time%60;showModal('LEVEL 1 GESCHAFFT',`Der Hohlkönig ist besiegt.\n${e.detail.feathers}/30 Kronenfedern · ${m}:${String(s).padStart(2,'0')} Spielzeit\n\nDie Ruinen von Moku sind jetzt geöffnet.`, 'LEVEL 2 STARTEN',()=>boot('level2',true))});
window.addEventListener('level2-victory',(e:any)=>{showModal('MOKU BEZWUNGEN',`Der Mooswächter fällt.\nKoba & Fia erreichen Level ${e.detail.level}.\n\nLevel 2 abgeschlossen.`, 'HAUPTMENÜ',backToMenu)});

start.onclick=()=>{clearSave();boot('game',false)};cont.onclick=()=>boot('game',true);level2btn.onclick=()=>boot('level2',true);
$<HTMLButtonElement>('#pause').onclick=()=>showModal('PAUSE','Expedition angehalten. Fortschritt wird an Flaggen, Federn und Levelaufstiegen gespeichert.');
$<HTMLButtonElement>('#resume').onclick=()=>{modal.classList.add('hidden');modalAction()};
$<HTMLButtonElement>('#restart').onclick=()=>{clearSave();boot('game',false)};
$<HTMLButtonElement>('#stats').onclick=()=>{game?.scene.pause(activeScene);modal.classList.add('hidden');skillpanel.classList.remove('hidden');const s=loadSave();if(s)updateRpg(s.stats,xpToNext(s.stats.level))};
$<HTMLButtonElement>('#stats-close').onclick=()=>{skillpanel.classList.add('hidden');game?.scene.resume(activeScene)};
document.querySelectorAll<HTMLButtonElement>('[data-skill]').forEach(b=>b.onclick=()=>window.dispatchEvent(new CustomEvent('skill-upgrade',{detail:{key:b.dataset.skill}})));
document.addEventListener('visibilitychange',()=>{if(document.hidden&&game?.scene.isActive(activeScene))showModal('PAUSE','Das Abenteuer wartet auf dich.')});window.addEventListener('contextmenu',e=>e.preventDefault());

const saved=loadSave();if(saved){updateRpg(saved.stats,xpToNext(saved.stats.level));hp(saved.hp,maxHp(saved.stats))}else hp(5,5);refreshMenu();

const directScene=new URLSearchParams(location.search).get('scene');if(directScene==='level2')setTimeout(()=>boot('level2',true),0);
