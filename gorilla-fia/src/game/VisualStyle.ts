import Phaser from 'phaser';

type Draw=(c:CanvasRenderingContext2D)=>void;

const COLORS={
  gorilla:'#5b3828',gorillaDark:'#3d281f',gorillaDeep:'#2a1b16',face:'#6d4834',skin:'#f4d6b7',
  bag:'#1e6f6a',flamingo:'#ff76a8',flamingoLight:'#ff8db7',ink:'#111111',egg:'#fff7df',eggShade:'#ffd98a',
  feather:'#ffd63d',featherHi:'#ffea73',moss:'#344b2a',mossHi:'#617c43',bossEye:'#ffd75a'
};

function canvas(w:number,h:number,draw:Draw){
  const el=document.createElement('canvas');el.width=w;el.height=h;
  const c=el.getContext('2d')!;c.imageSmoothingEnabled=false;draw(c);return el;
}
function rect(c:CanvasRenderingContext2D,x:number,y:number,w:number,h:number,color:string){c.fillStyle=color;c.fillRect(Math.round(x),Math.round(y),Math.round(w),Math.round(h))}
function add(game:Phaser.Game,key:string,w:number,h:number,draw:Draw){if(game.textures.exists(key))return;game.textures.addCanvas(key,canvas(w,h,draw))}

function hero(c:CanvasRenderingContext2D){
  // bewusst blockig wie der Moku-Prototyp: Gorilla + türkisfarbene Tasche + Flamingo
  rect(c,20,20,28,34,COLORS.gorilla);rect(c,14,27,8,20,COLORS.gorillaDark);rect(c,46,27,8,20,COLORS.gorillaDark);
  rect(c,22,8,24,18,COLORS.face);rect(c,18,13,7,11,COLORS.gorillaDark);rect(c,43,13,7,11,COLORS.gorillaDark);
  rect(c,26,15,4,4,COLORS.skin);rect(c,38,15,4,4,COLORS.skin);rect(c,29,23,10,5,COLORS.skin);
  rect(c,21,49,10,10,COLORS.gorillaDeep);rect(c,38,49,10,10,COLORS.gorillaDeep);
  rect(c,47,22,13,26,COLORS.bag);rect(c,50,25,7,3,'#43a69f');
  rect(c,55,5,5,23,COLORS.flamingo);rect(c,59,4,11,5,COLORS.flamingoLight);rect(c,68,5,3,3,COLORS.ink);rect(c,53,28,4,13,COLORS.flamingo);
  rect(c,56,38,3,10,COLORS.flamingo);rect(c,60,38,3,10,COLORS.flamingo);
}
function feather(c:CanvasRenderingContext2D){rect(c,12,3,5,28,COLORS.feather);rect(c,6,7,17,6,COLORS.featherHi);rect(c,4,14,17,6,COLORS.feather);rect(c,8,21,12,6,COLORS.featherHi);rect(c,14,29,3,8,'#8a5a26')}
function egg(c:CanvasRenderingContext2D){rect(c,5,4,14,10,COLORS.egg);rect(c,8,2,8,2,COLORS.egg);rect(c,7,14,10,2,COLORS.eggShade);rect(c,6,6,3,3,'#ffffff')}
function mob(c:CanvasRenderingContext2D,body:string,accent:string){rect(c,11,12,34,25,body);rect(c,7,17,8,14,body);rect(c,40,17,8,14,body);rect(c,14,7,27,10,accent);rect(c,16,16,5,5,'#eeeecc');rect(c,35,16,5,5,'#eeeecc');rect(c,18,18,2,2,COLORS.ink);rect(c,37,18,2,2,COLORS.ink);rect(c,14,37,9,6,COLORS.gorillaDeep);rect(c,34,37,9,6,COLORS.gorillaDeep)}
function boss(c:CanvasRenderingContext2D,moku=false){const b=moku?COLORS.moss:'#4f3b51',h=moku?COLORS.mossHi:'#77617c',eye=moku?COLORS.bossEye:'#ff665e';rect(c,20,28,78,76,b);rect(c,28,14,62,28,h);rect(c,10,48,20,36,b);rect(c,90,48,20,36,b);rect(c,31,43,16,8,eye);rect(c,71,43,16,8,eye);rect(c,39,74,40,13,COLORS.gorillaDeep);if(moku){rect(c,18,9,23,13,'#6e8f48');rect(c,46,3,27,14,'#799d4d');rect(c,77,8,24,14,'#6e8f48')}else{rect(c,29,7,14,15,'#d8b64f');rect(c,51,2,15,18,'#e4c75f');rect(c,74,7,14,15,'#d8b64f')}}

export function installVisualStyle(game:Phaser.Game){
  add(game,'hero',76,64,hero);add(game,'mokuhero',76,64,hero);
  add(game,'feather',30,40,feather);add(game,'sunfeather',30,40,feather);add(game,'egg',24,18,egg);
  add(game,'beetle',58,48,c=>mob(c,'#496ea5','#799bd0'));
  add(game,'boar',58,48,c=>mob(c,'#a35b2a','#cc7b42'));
  add(game,'toucan',58,48,c=>mob(c,'#36302c','#ffd85c'));
  add(game,'idol',58,48,c=>mob(c,'#725e79','#b38db7'));
  add(game,'snake',58,48,c=>mob(c,'#5c8f42','#8ec45f'));
  add(game,'monkey',58,48,c=>mob(c,'#6d4834','#a06b46'));
  add(game,'boss',120,116,c=>boss(c,false));add(game,'mossboss',120,116,c=>boss(c,true));
  add(game,'pixelTree',36,46,c=>{rect(c,15,28,7,16,'#6f4427');rect(c,5,12,27,22,'#2e7d42');rect(c,10,5,18,16,'#3e9b4f');rect(c,15,3,8,6,'#69b85f')});
  add(game,'pixelRock',28,20,c=>{rect(c,4,8,20,10,'#84907b');rect(c,8,4,14,7,'#9ca98f');rect(c,12,5,5,3,'#c0c9b4')});
  add(game,'pixelFlower',12,12,c=>{rect(c,5,5,2,7,'#397943');rect(c,2,2,4,4,'#ffd85c');rect(c,6,1,4,4,'#f27cab')});
}
