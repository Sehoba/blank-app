(function(){
  const e=React.createElement;
  const N=window.DyonNative||null;
  function App(){
    const [frame,setFrame]=React.useState('');
    const [capture,setCapture]=React.useState(false);
    const [captureMsg,setCaptureMsg]=React.useState('Bereit');
    const [usb,setUsb]=React.useState({connected:false,configured:false,hostDevices:0});
    const [bridge,setBridge]=React.useState(false);
    const [bridgeMsg,setBridgeMsg]=React.useState('nicht gesetzt');
    const [fps,setFps]=React.useState(0);
    const [kbps,setKbps]=React.useState(0);
    const [bridgeUrl,setBridgeUrl]=React.useState(()=>{try{return N?.getBridgeUrl?.()||''}catch(_){return''}});
    const [log,setLog]=React.useState('Android-App bereit.');

    React.useEffect(()=>{
      window.DYON={
        onFrame:(url,f,k)=>{setFrame(url);setFps(f||0);setKbps(k||0)},
        onCaptureState:(running,msg)=>{setCapture(!!running);setCaptureMsg(msg||'');setLog(msg||'')},
        onUsbState:(connected,configured,hostDevices)=>setUsb({connected:!!connected,configured:!!configured,hostDevices:Number(hostDevices||0)}),
        onBridgeState:(ok,msg)=>{setBridge(!!ok);setBridgeMsg(msg||'');}
      };
      try{N?.refreshUsb?.()}catch(_){}
      return()=>{delete window.DYON};
    },[]);

    function start(){
      if(!N){setLog('Native Android-Bridge fehlt. Diese Seite muss in der APK laufen.');return}
      setLog('Android fragt jetzt nach Bildschirmfreigabe…');N.startScreenMirror();
    }
    function stop(){try{N?.stopScreenMirror?.()}catch(_){}}
    function saveBridge(){try{N?.setBridgeUrl?.(bridgeUrl);setLog(bridgeUrl?'Bridge gespeichert: '+bridgeUrl:'Bridge deaktiviert. Nur Sandbox läuft.')}catch(err){setLog(String(err))}}

    const Stat=({label,value,state})=>e('div',{className:'stat '+state},e('small',null,label),e('div',null,e('span',{className:'dot'}),value));
    return e('div',{className:'app'},
      e('header',null,e('div',null,e('div',{className:'eyebrow'},'ANDROID SDK · 480×234'),e('div',{className:'brand'},'DYON ',e('span',null,'SANDBOX'))),e('div',{className:'badge'},'LOW-Q MIRROR')),
      e('section',{className:'panel'},
        e('h2',null,'Screen-Mirroring Sandbox'),
        e('p',{className:'hint'},'Starten, Android-Freigabe bestätigen und danach YouTube, Navigation oder eine andere App öffnen. Die Erfassung läuft im Hintergrund weiter.'),
        e('div',{className:'screenWrap'},e('div',{className:'screen'},frame?e('img',{src:frame,alt:'Live-Sandbox'}):e('div',{className:'placeholder'},e('b',null,'DYON 480×234'),e('span',null,'Noch kein Bild')),e('div',{className:'scan'}),e('div',{className:'hud'},e('span',null,(fps||0)+' FPS'),e('span',null,(kbps||0)+' kbit/s')))),
        e('div',{className:'statusGrid'},
          e(Stat,{label:'Capture',value:capture?captureMsg:'aus',state:capture?'good':'warn'}),
          e(Stat,{label:'USB-Kabel',value:usb.connected?(usb.configured?'verbunden':'erkannt'):'nicht erkannt',state:usb.connected?'good':'warn'}),
          e(Stat,{label:'Pi-Bridge',value:bridge?bridgeMsg:(bridgeUrl?'warte':'optional'),state:bridge?'good':'warn'})
        ),
        e('div',{className:'controls'},capture?e('button',{className:'stop',onClick:stop},'■ Screen-Mirroring stoppen'):e('button',{className:'primary',onClick:start},'▶ Screen-Mirroring starten'))
      ),
      e('section',{className:'panel'},
        e('h3',null,'USB / DYON-Ausgabe'),
        e('div',{className:'notice'},'Der DYON Convey behandelt seinen USB-Port als Massenspeicher-Host. Ein normales, nicht gerootetes Android-Handy kann per App nicht plötzlich selbst zu einem USB-Stick mit Live-Video werden. Die APK erkennt das Kabel und macht die 480×234-Sandbox; für Bild auf dem echten DYON braucht der Stream zusätzlich die DYON Auto Bridge (z. B. Pi Zero 2 W als USB-Gadget).'),
        e('div',{className:'field'},e('label',null,'Optionale DYON Auto Bridge'),e('input',{value:bridgeUrl,onChange:x=>setBridgeUrl(x.target.value),placeholder:'http://192.168.4.1:8080'})),
        e('div',{className:'row',style:{marginTop:'9px'}},e('button',{onClick:saveBridge},'Bridge speichern'),e('button',{onClick:()=>{try{N?.refreshUsb?.();setLog('USB-Status aktualisiert.')}catch(_){}}},'USB prüfen')),
        e('p',{className:'hint',style:{marginTop:'11px'}},'Mit Bridge sendet die App JPEG-Frames mit 480×234 und maximal ca. 6 FPS an /api/screen/frame. Der Pi kann daraus DYON-kompatibles AVI/MPEG-4 erzeugen bzw. Segment-Experimente fahren.')
      ),
      e('section',{className:'panel'},e('h3',null,'Status'),e('div',{className:'logs'},log)),
      e('div',{className:'footer'},'DYON Sandbox Mirror 1.0 · React WebApp im nativen Android-WebView')
    );
  }
  ReactDOM.createRoot(document.getElementById('root')).render(e(App));
})();
