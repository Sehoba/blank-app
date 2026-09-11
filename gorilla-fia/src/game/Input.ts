import type { InputState } from './types';

export class GameInput {
  state:InputState={x:0,y:0,jump:false,attack:false,power:false,shoot:false};
  private keys=new Set<string>(); private stickId:number|null=null; private cx=0; private cy=0;
  constructor(){
    addEventListener('keydown',e=>{this.keys.add(e.code);if(['Space','ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(e.code))e.preventDefault()});
    addEventListener('keyup',e=>this.keys.delete(e.code));
    const stick=document.querySelector<HTMLElement>('#stick')!,nub=stick.querySelector<HTMLElement>('i')!;
    const move=(e:PointerEvent)=>{if(e.pointerId!==this.stickId)return;const dx=e.clientX-this.cx,dy=e.clientY-this.cy,l=Math.hypot(dx,dy),m=Math.min(42,l),nx=l?dx/l:0,ny=l?dy/l:0;nub.style.transform=`translate(${nx*m}px,${ny*m}px)`;this.state.x=Math.abs(nx)>.2?nx:0;this.state.y=Math.abs(ny)>.2?ny:0};
    stick.onpointerdown=e=>{this.stickId=e.pointerId;const r=stick.getBoundingClientRect();this.cx=r.left+r.width/2;this.cy=r.top+r.height/2;stick.setPointerCapture(e.pointerId);move(e)};
    stick.onpointermove=move; const release=(e:PointerEvent)=>{if(e.pointerId===this.stickId){this.stickId=null;this.state.x=this.state.y=0;nub.style.transform=''}};stick.onpointerup=release;stick.onpointercancel=release;
    this.bindButton('#jump','jump');this.bindButton('#attack','attack');this.bindButton('#power','power');this.bindButton('#shoot','shoot');
  }
  private bindButton(sel:string,key:'jump'|'attack'|'power'|'shoot'){const el=document.querySelector<HTMLElement>(sel)!;el.onpointerdown=e=>{e.preventDefault();this.state[key]=true;el.setPointerCapture(e.pointerId);el.style.transform='scale(.9)'};const up=()=>{this.state[key]=false;el.style.transform=''};el.onpointerup=up;el.onpointercancel=up;}
  poll(){
    const kx=(this.keys.has('KeyD')||this.keys.has('ArrowRight')?1:0)-(this.keys.has('KeyA')||this.keys.has('ArrowLeft')?1:0);
    const ky=(this.keys.has('KeyS')||this.keys.has('ArrowDown')?1:0)-(this.keys.has('KeyW')||this.keys.has('ArrowUp')?1:0);
    return{x:kx||this.state.x,y:ky||this.state.y,jump:this.state.jump||this.keys.has('Space'),attack:this.state.attack||this.keys.has('KeyJ'),power:this.state.power||this.keys.has('KeyK'),shoot:this.state.shoot||this.keys.has('KeyL')||this.keys.has('KeyF')};
  }
}
