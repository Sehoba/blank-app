export type Vec = { x:number; y:number };
export type InputState = { x:number; y:number; jump:boolean; attack:boolean; power:boolean; shoot:boolean };
export type PlayerStats = {
  level:number; xp:number; skillPoints:number;
  strength:number; defense:number; vitality:number; agility:number; eggPower:number;
};
export type SaveData = {
  version:2;
  feathers:number[];
  checkpoint:Vec;
  hp:number;
  bossDefeated:boolean;
  opened:number[];
  playTime:number;
  stats:PlayerStats;
  level2Unlocked:boolean;
  level2Feathers:number[];
  level2Checkpoint:Vec;
  level2BossDefeated:boolean;
};
export type Region = { name:string; x:number; width:number; sky:number; ground:number; accent:number; objective:string };
export type MobKind = 'beetle'|'boar'|'toucan'|'idol'|'snake'|'monkey';
export type MobState = { id:number; kind:MobKind; x:number; y:number; hp:number; maxHp?:number; dir:number; homeX:number; alive:boolean; cooldown:number; xp?:number };

export const WORLD_W = 9200;
export const GROUND_Y = 690;
export const REGIONS:Region[] = [
  {name:'MOOSPFAD',x:0,width:1700,sky:0x6ac8b4,ground:0x27994c,accent:0xffd24a,objective:'Finde 5 Kronenfedern und erreiche den Baumstamm'},
  {name:'PAPAGEIEN-SCHLUCHT',x:1700,width:1850,sky:0x55aac7,ground:0x398847,accent:0xff6c5c,objective:'Überquere die Schlucht mit Flamingos Gleitflug'},
  {name:'SONNENTEMPEL',x:3550,width:1900,sky:0xe6a94d,ground:0x9b7435,accent:0xffe161,objective:'Aktiviere die drei Sonnengongs'},
  {name:'NEBELSUMPF',x:5450,width:1800,sky:0x4d7c72,ground:0x466d41,accent:0xdb88ff,objective:'Folge den leuchtenden Pilzen'},
  {name:'KRONENFESTE',x:7250,width:1950,sky:0x6d526f,ground:0x424044,accent:0xff4d58,objective:'Öffne das Tor mit 24 Federn und besiege den Hohlkönig'},
];

export const PLATFORMS = [
  [0,690,1150,130],[1210,635,260,185],[1515,570,190,250],
  [1750,670,360,150],[2190,590,260,230],[2540,500,240,320],[2870,600,300,220],[3260,525,240,295],
  [3550,690,520,130],[4150,610,310,210],[4550,535,260,285],[4920,630,470,190],
  [5460,690,450,130],[6000,620,250,200],[6350,550,290,270],[6730,625,470,195],
  [7260,690,430,130],[7790,600,260,220],[8150,525,230,295],[8490,610,280,210],[8850,690,350,130],
] as const;

export const FEATHER_POSITIONS:Vec[] = [
  {x:330,y:570},{x:750,y:535},{x:1320,y:520},{x:1580,y:450},{x:1100,y:545},{x:1880,y:550},
  {x:2310,y:470},{x:2630,y:380},{x:3000,y:490},{x:3370,y:405},{x:2050,y:520},{x:3110,y:470},
  {x:3720,y:560},{x:4260,y:480},{x:4680,y:410},{x:5100,y:510},{x:3980,y:540},{x:5190,y:490},
  {x:5630,y:560},{x:6110,y:500},{x:6490,y:430},{x:6900,y:505},{x:5800,y:540},{x:7070,y:480},
  {x:7400,y:560},{x:7890,y:480},{x:8260,y:405},{x:8600,y:490},{x:8960,y:560},{x:8730,y:480},
];

export const MOB_SPAWNS:Omit<MobState,'alive'|'cooldown'>[] = [
  {id:0,kind:'beetle',x:650,y:650,hp:2,dir:1,homeX:650,xp:12},{id:1,kind:'boar',x:1360,y:590,hp:4,dir:-1,homeX:1360,xp:20},
  {id:2,kind:'toucan',x:1950,y:420,hp:3,dir:1,homeX:1950,xp:16},{id:3,kind:'beetle',x:2320,y:550,hp:2,dir:1,homeX:2320,xp:12},{id:4,kind:'boar',x:3010,y:560,hp:4,dir:-1,homeX:3010,xp:20},
  {id:5,kind:'idol',x:3860,y:630,hp:5,dir:1,homeX:3860,xp:28},{id:6,kind:'toucan',x:4380,y:390,hp:3,dir:-1,homeX:4380,xp:16},{id:7,kind:'idol',x:5020,y:570,hp:5,dir:-1,homeX:5020,xp:28},
  {id:8,kind:'snake',x:5480,y:650,hp:2,dir:1,homeX:5480,xp:15},{id:9,kind:'beetle',x:5660,y:650,hp:2,dir:1,homeX:5660,xp:12},{id:10,kind:'boar',x:6140,y:580,hp:4,dir:-1,homeX:6140,xp:20},
  {id:11,kind:'monkey',x:6410,y:500,hp:4,dir:1,homeX:6410,xp:24},{id:12,kind:'toucan',x:6700,y:400,hp:3,dir:1,homeX:6700,xp:16},{id:13,kind:'idol',x:7040,y:565,hp:5,dir:-1,homeX:7040,xp:28},
  {id:14,kind:'boar',x:7550,y:650,hp:4,dir:1,homeX:7550,xp:20},{id:15,kind:'snake',x:7770,y:565,hp:2,dir:-1,homeX:7770,xp:15},{id:16,kind:'idol',x:7990,y:540,hp:5,dir:-1,homeX:7990,xp:28},
  {id:17,kind:'monkey',x:8250,y:470,hp:4,dir:1,homeX:8250,xp:24},{id:18,kind:'toucan',x:8500,y:380,hp:3,dir:1,homeX:8500,xp:16},{id:19,kind:'boar',x:8760,y:640,hp:4,dir:-1,homeX:8760,xp:20},
];

const SAVE_KEY='gorilla_flamingo_save_v2';
const OLD_SAVE_KEY='gorilla_flamingo_save_v1';
export const defaultStats=():PlayerStats=>({level:1,xp:0,skillPoints:0,strength:1,defense:1,vitality:1,agility:1,eggPower:1});
export function xpToNext(level:number){return 70+(level-1)*45}
export function maxHp(stats:PlayerStats){return 5+Math.floor((stats.vitality-1)/2)}
export function meleeDamage(stats:PlayerStats){return 1+Math.floor((stats.strength-1)/2)}
export function eggDamage(stats:PlayerStats){return 1+Math.floor((stats.eggPower-1)/2)}
export function moveMultiplier(stats:PlayerStats){return 1+Math.min(.35,(stats.agility-1)*.035)}
export function freshSave():SaveData{return{version:2,feathers:[],checkpoint:{x:160,y:550},hp:5,bossDefeated:false,opened:[],playTime:0,stats:defaultStats(),level2Unlocked:false,level2Feathers:[],level2Checkpoint:{x:150,y:550},level2BossDefeated:false}}
export function loadSave():SaveData|null{try{
  const raw=localStorage.getItem(SAVE_KEY);if(raw){const v=JSON.parse(raw);return v?.version===2?v:null}
  const oldRaw=localStorage.getItem(OLD_SAVE_KEY);if(oldRaw){const old=JSON.parse(oldRaw);if(old?.version===1){const v=freshSave();Object.assign(v,{feathers:old.feathers||[],checkpoint:old.checkpoint||v.checkpoint,hp:old.hp||5,bossDefeated:!!old.bossDefeated,opened:old.opened||[],playTime:old.playTime||0,level2Unlocked:!!old.bossDefeated});writeSave(v);return v}}
  return null
}catch{return null}}
export function writeSave(v:SaveData){localStorage.setItem(SAVE_KEY,JSON.stringify(v))}
export function clearSave(){localStorage.removeItem(SAVE_KEY);localStorage.removeItem(OLD_SAVE_KEY)}
