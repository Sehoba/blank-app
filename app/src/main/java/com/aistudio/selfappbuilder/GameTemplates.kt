package com.aistudio.selfappbuilder

data class GameProject(
    val title: String,
    val description: String,
    val icon: String,
    val html: String,
    val css: String,
    val js: String
)

object GameTemplates {

    fun getGorillaFiaGame(
        speedMultiplier: Float = 1.0f,
        jumpPowerMultiplier: Float = 1.0f,
        soundEnabled: Boolean = true
    ): GameProject {
        val html = """<div id="game-container">
  <div id="ui-header">
    <div class="stat-badge">🏆 <span id="score">0</span></div>
    <div class="stat-badge">🍌 <span id="bananas">0</span></div>
    <div class="stat-badge" id="lives-display">❤️❤️❤️</div>
    <div class="stat-badge">🚩 <span id="level-name">Level 1: Dschungel</span></div>
  </div>

  <canvas id="gameCanvas" width="480" height="600"></canvas>

  <div id="touch-controls">
    <div class="ctrl-group dpad">
      <button id="btn-left" class="game-btn">◀</button>
      <button id="btn-right" class="game-btn">▶</button>
    </div>
    <div class="ctrl-group actions">
      <button id="btn-stomp" class="game-btn action-stomp">💥 STAMPF</button>
      <button id="btn-jump" class="game-btn action-jump">⬆ SPRUNG</button>
    </div>
  </div>

  <div id="overlay" class="hidden">
    <div class="modal-card">
      <h2 id="modal-title">Gorilla & Fia</h2>
      <p id="modal-msg">Erkunde den Dschungel, sammle alle Bananen und meistere die Ruinen!</p>
      <button id="modal-btn" onclick="restartGame()">▶ Jetzt Spielen</button>
    </div>
  </div>
</div>"""

        val css = """* { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-user-select: none; }
body {
  background: #061109;
  color: #f1f5f9;
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
}
#game-container {
  position: relative;
  width: 100%;
  max-width: 500px;
  height: 100vh;
  max-height: 800px;
  background: #0a1f11;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0,0,0,0.8);
}
#ui-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  z-index: 10;
  background: linear-gradient(180deg, rgba(6,17,9,0.85) 0%, transparent 100%);
}
.stat-badge {
  background: rgba(16, 44, 25, 0.85);
  border: 1px solid #16a34a;
  border-radius: 14px;
  padding: 4px 10px;
  font-weight: 800;
  font-size: 13px;
  color: #fef08a;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}
canvas {
  width: 100%;
  flex: 1;
  display: block;
  background: linear-gradient(180deg, #134e4a 0%, #14532d 55%, #052e16 100%);
}
#touch-controls {
  position: absolute;
  bottom: 12px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
  z-index: 15;
  pointer-events: none;
}
.ctrl-group {
  display: flex;
  gap: 12px;
  pointer-events: auto;
}
.game-btn {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.3);
  background: rgba(15, 23, 42, 0.75);
  color: white;
  font-size: 18px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: manipulation;
  box-shadow: 0 4px 12px rgba(0,0,0,0.5);
  active: scale(0.92);
  transition: transform 0.08s, background 0.08s;
}
.game-btn:active {
  background: #22c55e;
  transform: scale(0.92);
}
.action-jump {
  width: 76px;
  height: 76px;
  background: rgba(234, 88, 12, 0.85);
  border-color: #fb923c;
  font-size: 13px;
}
.action-stomp {
  width: 66px;
  height: 66px;
  background: rgba(225, 29, 72, 0.85);
  border-color: #f43f5e;
  font-size: 11px;
}
#overlay {
  position: absolute;
  inset: 0;
  background: rgba(5, 20, 10, 0.92);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 25;
  transition: opacity 0.2s;
}
.hidden { display: none !important; }
.modal-card {
  background: #0f291e;
  border: 2px solid #22c55e;
  border-radius: 20px;
  padding: 24px;
  text-align: center;
  max-width: 320px;
  box-shadow: 0 12px 36px rgba(0,0,0,0.6);
}
.modal-card h2 { color: #facc15; font-size: 22px; margin-bottom: 8px; }
.modal-card p { font-size: 14px; color: #cbd5e1; margin-bottom: 20px; line-height: 1.4; }
.modal-card button {
  background: linear-gradient(135deg, #22c55e, #15803d);
  color: white;
  border: 0;
  padding: 12px 28px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(34,197,94,0.4);
}"""

        val js = """// Gorilla & Fia: Dschungelabenteuer Engine
const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const scoreEl = document.getElementById('score');
const bananasEl = document.getElementById('bananas');
const livesEl = document.getElementById('lives-display');
const overlay = document.getElementById('overlay');
const modalTitle = document.getElementById('modal-title');
const modalMsg = document.getElementById('modal-msg');

let audioCtx = null;
function playSfx(type) {
  if (!${soundEnabled}) return;
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    const now = audioCtx.currentTime;

    if (type === 'jump') {
      osc.type = 'sine';
      osc.frequency.setValueAtTime(200, now);
      osc.frequency.exponentialRampToValueAtTime(600, now + 0.15);
      gain.gain.setValueAtTime(0.2, now);
      gain.gain.linearRampToValueAtTime(0.01, now + 0.15);
      osc.start(now);
      osc.stop(now + 0.15);
    } else if (type === 'coin') {
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(700, now);
      osc.frequency.setValueAtTime(1050, now + 0.08);
      gain.gain.setValueAtTime(0.25, now);
      gain.gain.linearRampToValueAtTime(0.01, now + 0.2);
      osc.start(now);
      osc.stop(now + 0.2);
    } else if (type === 'stomp') {
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(160, now);
      osc.frequency.exponentialRampToValueAtTime(40, now + 0.25);
      gain.gain.setValueAtTime(0.3, now);
      gain.gain.linearRampToValueAtTime(0.01, now + 0.25);
      osc.start(now);
      osc.stop(now + 0.25);
    } else if (type === 'hurt') {
      osc.type = 'square';
      osc.frequency.setValueAtTime(220, now);
      osc.frequency.setValueAtTime(110, now + 0.1);
      gain.gain.setValueAtTime(0.25, now);
      gain.gain.linearRampToValueAtTime(0.01, now + 0.25);
      osc.start(now);
      osc.stop(now + 0.25);
    }
  } catch(e) {}
}

let score = 0;
let bananas = 0;
let lives = 3;
let gameOver = false;
let cameraY = 0;

const speedMod = ${speedMultiplier};
const jumpMod = ${jumpPowerMultiplier};

const player = {
  x: 210,
  y: 480,
  w: 48,
  h: 48,
  vx: 0,
  vy: 0,
  speed: 4.8 * speedMod,
  jumpStrength: -11.5 * jumpMod,
  grounded: false,
  doubleJump: true,
  facingRight: true,
  isStomping: false
};

const keys = { left: false, right: false };

let platforms = [];
let items = [];
let enemies = [];
let particles = [];

function initWorld() {
  platforms = [
    { x: 0, y: 550, w: 480, h: 50, type: 'ground' },
    { x: 80, y: 440, w: 120, h: 16, type: 'wood' },
    { x: 260, y: 360, w: 140, h: 16, type: 'wood' },
    { x: 100, y: 260, w: 130, h: 16, type: 'wood' },
    { x: 280, y: 170, w: 120, h: 16, type: 'wood' },
    { x: 60, y: 80, w: 150, h: 16, type: 'wood' },
    { x: 240, y: -20, w: 140, h: 16, type: 'wood' },
    { x: 120, y: -120, w: 180, h: 20, type: 'goal' }
  ];

  items = [
    { x: 130, y: 405, collected: false, kind: 'banana' },
    { x: 320, y: 325, collected: false, kind: 'banana' },
    { x: 160, y: 225, collected: false, kind: 'feather' },
    { x: 330, y: 135, collected: false, kind: 'banana' },
    { x: 120, y: 45, collected: false, kind: 'banana' },
    { x: 200, y: -160, collected: false, kind: 'crown' }
  ];

  enemies = [
    { x: 280, y: 340, w: 32, h: 20, vx: 1.2, minX: 260, maxX: 370 },
    { x: 110, y: 240, w: 32, h: 20, vx: -1.0, minX: 100, maxX: 200 }
  ];
}

function spawnParticles(x, y, color, count = 8) {
  for (let i = 0; i < count; i++) {
    particles.push({
      x, y,
      vx: (Math.random() - 0.5) * 6,
      vy: (Math.random() - 0.5) * 6 - 2,
      size: Math.random() * 4 + 2,
      color: color,
      life: 1.0
    });
  }
}

function restartGame() {
  score = 0;
  bananas = 0;
  lives = 3;
  gameOver = false;
  player.x = 210;
  player.y = 480;
  player.vx = 0;
  player.vy = 0;
  player.isStomping = false;
  overlay.classList.add('hidden');
  initWorld();
  updateUI();
}

function updateUI() {
  scoreEl.textContent = score;
  bananasEl.textContent = bananas;
  livesEl.textContent = '❤️'.repeat(Math.max(0, lives));
}

// Controls
function bindTouch(id, onStart, onEnd) {
  const btn = document.getElementById(id);
  if (!btn) return;
  const start = (e) => { e.preventDefault(); onStart(); };
  const end = (e) => { e.preventDefault(); if (onEnd) onEnd(); };
  btn.addEventListener('touchstart', start, { passive: false });
  btn.addEventListener('touchend', end, { passive: false });
  btn.addEventListener('mousedown', start);
  btn.addEventListener('mouseup', end);
}

bindTouch('btn-left', () => { keys.left = true; player.facingRight = false; }, () => { keys.left = false; });
bindTouch('btn-right', () => { keys.right = true; player.facingRight = true; }, () => { keys.right = false; });
bindTouch('btn-jump', () => {
  if (player.grounded) {
    player.vy = player.jumpStrength;
    player.grounded = false;
    player.doubleJump = true;
    playSfx('jump');
    spawnParticles(player.x + 24, player.y + 44, '#22c55e', 6);
  } else if (player.doubleJump) {
    player.vy = player.jumpStrength * 0.9;
    player.doubleJump = false;
    playSfx('jump');
    spawnParticles(player.x + 24, player.y + 44, '#f472b6', 10);
  }
});
bindTouch('btn-stomp', () => {
  if (!player.grounded) {
    player.vy = 14;
    player.isStomping = true;
    playSfx('stomp');
  }
});

window.addEventListener('keydown', (e) => {
  if (e.key === 'ArrowLeft' || e.key === 'a') { keys.left = true; player.facingRight = false; }
  if (e.key === 'ArrowRight' || e.key === 'd') { keys.right = true; player.facingRight = true; }
  if (e.key === 'ArrowUp' || e.key === ' ' || e.key === 'w') {
    if (player.grounded) {
      player.vy = player.jumpStrength;
      player.grounded = false;
      player.doubleJump = true;
      playSfx('jump');
    } else if (player.doubleJump) {
      player.vy = player.jumpStrength * 0.9;
      player.doubleJump = false;
      playSfx('jump');
    }
  }
  if (e.key === 'ArrowDown' || e.key === 's') {
    if (!player.grounded) { player.vy = 14; player.isStomping = true; playSfx('stomp'); }
  }
});

window.addEventListener('keyup', (e) => {
  if (e.key === 'ArrowLeft' || e.key === 'a') keys.left = false;
  if (e.key === 'ArrowRight' || e.key === 'd') keys.right = false;
});

function loop() {
  update();
  draw();
  requestAnimationFrame(loop);
}

function update() {
  if (gameOver) return;

  // Horizontal Movement
  if (keys.left) player.vx = -player.speed;
  else if (keys.right) player.vx = player.speed;
  else player.vx *= 0.75;

  player.x += player.vx;
  if (player.x < 0) player.x = 0;
  if (player.x + player.w > canvas.width) player.x = canvas.width - player.w;

  // Gravity
  player.vy += 0.52;
  player.y += player.vy;
  player.grounded = false;

  // Platform collision
  for (const plat of platforms) {
    if (
      player.x + player.w > plat.x &&
      player.x < plat.x + plat.w &&
      player.y + player.h >= plat.y &&
      player.y + player.h <= plat.y + 18 &&
      player.vy > 0
    ) {
      player.y = plat.y - player.h;
      player.vy = 0;
      player.grounded = true;
      if (player.isStomping) {
        player.isStomping = false;
        spawnParticles(player.x + 24, player.y + 44, '#eab308', 12);
      }
    }
  }

  // Camera tracking
  const targetCamY = player.y - 320;
  cameraY += (targetCamY - cameraY) * 0.08;

  // Items collision
  items.forEach(item => {
    if (!item.collected) {
      const dx = (player.x + 24) - (item.x + 12);
      const dy = (player.y + 24) - (item.y + 12);
      if (Math.hypot(dx, dy) < 32) {
        item.collected = true;
        playSfx('coin');
        if (item.kind === 'banana') {
          bananas++;
          score += 100;
          spawnParticles(item.x + 12, item.y + 12, '#facc15', 10);
        } else if (item.kind === 'feather') {
          score += 250;
          player.speed *= 1.25;
          setTimeout(() => player.speed /= 1.25, 4000);
          spawnParticles(item.x + 12, item.y + 12, '#ec4899', 14);
        } else if (item.kind === 'crown') {
          score += 1000;
          winGame();
        }
        updateUI();
      }
    }
  });

  // Enemies update
  enemies.forEach(en => {
    en.x += en.vx;
    if (en.x < en.minX || en.x > en.maxX) en.vx *= -1;

    // Hit test
    if (
      player.x + player.w > en.x &&
      player.x < en.x + en.w &&
      player.y + player.h > en.y &&
      player.y < en.y + en.h
    ) {
      if (player.vy > 0 || player.isStomping) {
        // Jump/stomp on enemy
        en.y = 9999;
        score += 200;
        player.vy = -7;
        playSfx('stomp');
        spawnParticles(en.x + 16, en.y + 10, '#f97316', 12);
        updateUI();
      } else {
        // Take damage
        lives--;
        playSfx('hurt');
        player.vy = -6;
        player.vx = (player.x < en.x) ? -5 : 5;
        updateUI();
        if (lives <= 0) die();
      }
    }
  });

  // Fall off bottom
  if (player.y > 650) {
    lives--;
    updateUI();
    if (lives <= 0) die();
    else {
      player.x = 210;
      player.y = 480;
      player.vy = 0;
    }
  }

  // Update particles
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    p.x += p.vx;
    p.y += p.vy;
    p.life -= 0.03;
    if (p.life <= 0) particles.splice(i, 1);
  }
}

function die() {
  gameOver = true;
  modalTitle.textContent = 'Game Over';
  modalMsg.textContent = 'Gorilla & Fia wurden besiegt. Dein Score: ' + score + ' | Bananen: ' + bananas;
  overlay.classList.remove('hidden');
}

function winGame() {
  gameOver = true;
  modalTitle.textContent = '🏆 Dschungelkrone Errungen!';
  modalMsg.textContent = 'Glückwunsch! Du hast Level 1 & 2 gemeistert! Endscore: ' + score;
  overlay.classList.remove('hidden');
}

function draw() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  ctx.save();
  ctx.translate(0, -cameraY);

  // Parallax background trees/ruins
  drawBackdrop();

  // Platforms
  for (const plat of platforms) {
    if (plat.type === 'ground') {
      ctx.fillStyle = '#1e3a1e';
      ctx.fillRect(plat.x, plat.y, plat.w, plat.h);
      ctx.fillStyle = '#15803d';
      ctx.fillRect(plat.x, plat.y, plat.w, 10);
    } else if (plat.type === 'goal') {
      ctx.fillStyle = '#854d0e';
      ctx.fillRect(plat.x, plat.y, plat.w, plat.h);
      ctx.fillStyle = '#eab308';
      ctx.fillRect(plat.x, plat.y, plat.w, 5);
    } else {
      ctx.fillStyle = '#3f2e1c';
      ctx.fillRect(plat.x, plat.y, plat.w, plat.h);
      ctx.fillStyle = '#16a34a';
      ctx.fillRect(plat.x, plat.y, plat.w, 4);
    }
  }

  // Items
  items.forEach(item => {
    if (!item.collected) {
      if (item.kind === 'banana') {
        ctx.font = '24px sans-serif';
        ctx.fillText('🍌', item.x, item.y + 20);
      } else if (item.kind === 'feather') {
        ctx.font = '22px sans-serif';
        ctx.fillText('🪶', item.x, item.y + 20);
      } else if (item.kind === 'crown') {
        ctx.font = '30px sans-serif';
        ctx.fillText('👑', item.x, item.y + 24);
      }
    }
  });

  // Enemies
  enemies.forEach(en => {
    if (en.y < 9000) {
      ctx.font = '24px sans-serif';
      ctx.fillText(en.vx > 0 ? '🐍' : '🦂', en.x, en.y + 20);
    }
  });

  // Player: Gorilla with Fia (Flamingo) on back!
  drawPlayer(player.x, player.y, player.facingRight);

  // Particles
  particles.forEach(p => {
    ctx.fillStyle = p.color;
    ctx.globalAlpha = p.life;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1.0;
  });

  ctx.restore();
}

function drawBackdrop() {
  // Jungle vines and mist
  ctx.fillStyle = 'rgba(21, 128, 61, 0.15)';
  for (let i = -200; i < 700; i += 120) {
    ctx.fillRect(40 + (i % 80), i, 8, 90);
    ctx.fillRect(380 - (i % 60), i + 30, 10, 110);
  }
}

function drawPlayer(x, y, facingRight) {
  ctx.save();
  ctx.translate(x + 24, y + 24);
  if (!facingRight) ctx.scale(-1, 1);

  // Gorilla Body
  ctx.fillStyle = '#1e1b18';
  ctx.beginPath();
  ctx.ellipse(0, 4, 18, 16, 0, 0, Math.PI * 2);
  ctx.fill();

  // Face
  ctx.fillStyle = '#44403c';
  ctx.beginPath();
  ctx.arc(6, -2, 10, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#fef08a';
  ctx.fillRect(8, -4, 3, 3);

  // Arms
  ctx.strokeStyle = '#1e1b18';
  ctx.lineWidth = 6;
  ctx.beginPath();
  ctx.moveTo(-4, 0);
  ctx.lineTo(8, 14);
  ctx.stroke();

  // Fia (Flamingo) riding on Gorilla's shoulder!
  ctx.fillStyle = '#f472b6';
  ctx.beginPath();
  ctx.arc(-8, -14, 8, 0, Math.PI * 2); // Body
  ctx.fill();
  ctx.fillStyle = '#f9a8d4';
  ctx.beginPath();
  ctx.arc(-4, -20, 5, 0, Math.PI * 2); // Head
  ctx.fill();
  // Beak
  ctx.fillStyle = '#f97316';
  ctx.beginPath();
  ctx.moveTo(-1, -20);
  ctx.lineTo(4, -18);
  ctx.lineTo(-1, -16);
  ctx.fill();

  ctx.restore();
}

initWorld();
updateUI();
loop();
"""
        return GameProject(
            title = "Gorilla & Fia: Dschungel-Run",
            description = "Jump'n'Run Plattform-Spiel mit Gorilla & Flamingo Fia! Sammle Bananen, Federn & Krone.",
            icon = "🦍",
            html = html,
            css = css,
            js = js
        )
    }

    fun getFlappyFiaGame(): GameProject {
        val html = """<div id="game-wrap">
  <div id="hud">
    <div>🏆 PUNKTE: <span id="score">0</span></div>
    <div>🥇 BEST: <span id="best">0</span></div>
  </div>
  <canvas id="c" width="400" height="600"></canvas>
  <div id="msg">👆 TIPPEN ZUM FLIEGEN</div>
</div>"""

        val css = """* { margin:0; padding:0; box-sizing:border-box; user-select:none; }
body { background:#030712; color:white; font-family:system-ui; display:flex; justify-content:center; align-items:center; height:100vh; overflow:hidden; }
#game-wrap { position:relative; width:100%; max-width:420px; height:100vh; max-height:750px; background:#0f172a; display:flex; flex-direction:column; }
#hud { position:absolute; top:12px; left:0; right:0; display:flex; justify-content:space-between; padding:0 20px; font-weight:800; font-size:16px; color:#f472b6; z-index:10; }
canvas { width:100%; height:100%; display:block; background:linear-gradient(180deg, #1e1b4b, #0f172a); }
#msg { position:absolute; bottom:20px; left:0; right:0; text-align:center; font-weight:700; color:#cbd5e1; font-size:14px; pointer-events:none; }"""

        val js = """const canvas = document.getElementById('c');
const ctx = canvas.getContext('2d');
const scoreEl = document.getElementById('score');
const bestEl = document.getElementById('best');
const msgEl = document.getElementById('msg');

let score = 0;
let best = localStorage.getItem('fia_flappy_best') || 0;
bestEl.textContent = best;
let running = false;
let pipes = [];

const bird = { x: 80, y: 300, vy: 0, gravity: 0.38, jump: -6.8, r: 16 };

function flap() {
  bird.vy = bird.jump;
  if (!running) {
    running = true;
    score = 0;
    pipes = [];
    bird.y = 300;
    bird.vy = 0;
    msgEl.textContent = 'Flamingo Fia fliegt!';
  }
}

window.addEventListener('pointerdown', flap);
window.addEventListener('keydown', (e) => { if (e.code === 'Space') flap(); });

function addPipe() {
  const gap = 140;
  const top = Math.random() * (canvas.height - gap - 160) + 60;
  pipes.push({ x: canvas.width, top, bottom: top + gap, scored: false });
}

let pipeTimer = 0;

function loop() {
  update();
  draw();
  requestAnimationFrame(loop);
}

function update() {
  if (!running) return;

  bird.vy += bird.gravity;
  bird.y += bird.vy;

  pipeTimer++;
  if (pipeTimer > 100) {
    addPipe();
    pipeTimer = 0;
  }

  for (let i = pipes.length - 1; i >= 0; i--) {
    const p = pipes[i];
    p.x -= 2.6;

    if (!p.scored && p.x + 50 < bird.x) {
      score++;
      p.scored = true;
      scoreEl.textContent = score;
      if (score > best) {
        best = score;
        bestEl.textContent = best;
        localStorage.setItem('fia_flappy_best', best);
      }
    }

    // Collision
    if (bird.x + bird.r > p.x && bird.x - bird.r < p.x + 50) {
      if (bird.y - bird.r < p.top || bird.y + bird.r > p.bottom) {
        gameOver();
      }
    }

    if (p.x < -60) pipes.splice(i, 1);
  }

  if (bird.y + bird.r > canvas.height || bird.y - bird.r < 0) {
    gameOver();
  }
}

function gameOver() {
  running = false;
  msgEl.textContent = '💥 Game Over! Tippen zum Neustarten';
}

function draw() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Pipes (Dschungelranken)
  ctx.fillStyle = '#15803d';
  pipes.forEach(p => {
    ctx.fillRect(p.x, 0, 50, p.top);
    ctx.fillRect(p.x, p.bottom, 50, canvas.height - p.bottom);
    ctx.fillStyle = '#22c55e';
    ctx.fillRect(p.x - 3, p.top - 12, 56, 12);
    ctx.fillRect(p.x - 3, p.bottom, 56, 12);
    ctx.fillStyle = '#15803d';
  });

  // Fia (Flamingo)
  ctx.save();
  ctx.translate(bird.x, bird.y);
  ctx.fillStyle = '#f472b6';
  ctx.beginPath();
  ctx.arc(0, 0, bird.r, 0, Math.PI * 2);
  ctx.fill();
  // Auge & Schnabel
  ctx.fillStyle = '#fb923c';
  ctx.beginPath();
  ctx.moveTo(bird.r, -4);
  ctx.lineTo(bird.r + 14, 2);
  ctx.lineTo(bird.r, 6);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(4, -8, 5, 5);
  ctx.fillStyle = '#000000';
  ctx.fillRect(6, -6, 2, 2);
  ctx.restore();
}

loop();"""

        return GameProject(
            title = "Flappy Fia (Flamingo Run)",
            description = "Fliege als Flamingodame Fia durch exotische Ranken! Tippen zum Flattern.",
            icon = "🦩",
            html = html,
            css = css,
            js = js
        )
    }

    fun getSnakeGame(): GameProject {
        val html = """<div class="wrap">
  <div class="header">
    <div>🐍 SCORE: <span id="s">0</span></div>
    <div>🍎 FRÜCHTE: <span id="f">0</span></div>
  </div>
  <canvas id="cv" width="360" height="360"></canvas>
  <div class="dpad">
    <div class="row"><button id="u">▲</button></div>
    <div class="row">
      <button id="l">◀</button>
      <button id="d">▼</button>
      <button id="r">▶</button>
    </div>
  </div>
</div>"""

        val css = """* { margin:0; padding:0; box-sizing:border-box; user-select:none; }
body { background:#0a0a0c; color:#e2e8f0; font-family:monospace; display:flex; justify-content:center; align-items:center; height:100vh; }
.wrap { display:flex; flex-direction:column; align-items:center; gap:12px; }
.header { display:flex; justify-content:space-between; width:360px; font-weight:bold; font-size:16px; color:#4ade80; }
canvas { border:2px solid #22c55e; border-radius:12px; background:#022c22; box-shadow:0 0 20px rgba(34,197,94,0.3); }
.dpad { display:flex; flex-direction:column; gap:8px; align-items:center; }
.row { display:flex; gap:12px; }
button { width:58px; height:58px; font-size:22px; border-radius:14px; border:0; background:#1e293b; color:#38bdf8; font-weight:bold; box-shadow:0 4px 10px rgba(0,0,0,0.5); }
button:active { background:#22c55e; color:black; }"""

        val js = """const c = document.getElementById('cv');
const ctx = c.getContext('2d');
const sEl = document.getElementById('s');
const fEl = document.getElementById('f');

const GRID = 18;
const TILE = c.width / GRID;

let snake = [{x: 9, y: 9}];
let dir = {x: 1, y: 0};
let nextDir = {x: 1, y: 0};
let food = {x: 4, y: 4};
let score = 0;
let fruits = 0;
let alive = true;

function placeFood() {
  food = {
    x: Math.floor(Math.random() * GRID),
    y: Math.floor(Math.random() * GRID)
  };
}

function update() {
  if (!alive) return;
  dir = nextDir;
  const head = { x: snake[0].x + dir.x, y: snake[0].y + dir.y };

  if (head.x < 0 || head.x >= GRID || head.y < 0 || head.y >= GRID) {
    alive = false;
    alert('💥 Schlange gestoßen! Score: ' + score);
    reset();
    return;
  }

  for (const seg of snake) {
    if (seg.x === head.x && seg.y === head.y) {
      alive = false;
      alert('💥 Selbst gebissen! Score: ' + score);
      reset();
      return;
    }
  }

  snake.unshift(head);
  if (head.x === food.x && head.y === food.y) {
    score += 10;
    fruits++;
    sEl.textContent = score;
    fEl.textContent = fruits;
    placeFood();
  } else {
    snake.pop();
  }
}

function reset() {
  snake = [{x: 9, y: 9}];
  dir = {x: 1, y: 0};
  nextDir = {x: 1, y: 0};
  score = 0;
  fruits = 0;
  sEl.textContent = 0;
  fEl.textContent = 0;
  alive = true;
  placeFood();
}

function draw() {
  ctx.fillStyle = '#022c22';
  ctx.fillRect(0, 0, c.width, c.height);

  // Fruit
  ctx.fillStyle = '#ef4444';
  ctx.beginPath();
  ctx.arc((food.x + 0.5) * TILE, (food.y + 0.5) * TILE, TILE * 0.42, 0, Math.PI * 2);
  ctx.fill();

  // Snake
  snake.forEach((seg, i) => {
    ctx.fillStyle = i === 0 ? '#4ade80' : '#16a34a';
    ctx.fillRect(seg.x * TILE + 1, seg.y * TILE + 1, TILE - 2, TILE - 2);
  });
}

function setD(x, y) {
  if (dir.x !== -x && dir.y !== -y) nextDir = {x, y};
}

document.getElementById('u').onclick = () => setD(0, -1);
document.getElementById('d').onclick = () => setD(0, 1);
document.getElementById('l').onclick = () => setD(-1, 0);
document.getElementById('r').onclick = () => setD(1, 0);

window.addEventListener('keydown', e => {
  if (e.key === 'ArrowUp') setD(0, -1);
  if (e.key === 'ArrowDown') setD(0, 1);
  if (e.key === 'ArrowLeft') setD(-1, 0);
  if (e.key === 'ArrowRight') setD(1, 0);
});

setInterval(() => { update(); draw(); }, 120);"""

        return GameProject(
            title = "Dschungel-Snake (Retro)",
            description = "Klassisches Neon-Schlangenspiel mit Touch-Steuerung und Dschungelfrüchten!",
            icon = "🐍",
            html = html,
            css = css,
            js = js
        )
    }

    fun getSpaceShooterGame(): GameProject {
        val html = """<div id="c-wrap">
  <div id="bar">🚀 GALAXY DEFENDER | PUNKTE: <span id="sc">0</span></div>
  <canvas id="sc-canvas" width="360" height="520"></canvas>
  <div id="fire-bar">
    <button id="f-left">◀</button>
    <button id="f-shoot">🔥 FEUER</button>
    <button id="f-right">▶</button>
  </div>
</div>"""

        val css = """* { margin:0; padding:0; box-sizing:border-box; user-select:none; }
body { background:#030712; color:white; font-family:system-ui; display:flex; justify-content:center; align-items:center; height:100vh; }
#c-wrap { display:flex; flex-direction:column; align-items:center; width:100%; max-width:400px; }
#bar { font-size:14px; font-weight:800; color:#38bdf8; margin-bottom:8px; }
canvas { background:#030712; border:1px solid #1e293b; border-radius:14px; box-shadow:0 0 25px rgba(56,189,248,0.2); }
#fire-bar { display:flex; gap:16px; margin-top:12px; width:100%; justify-content:center; }
button { padding:14px 22px; font-size:16px; font-weight:800; border-radius:12px; border:0; background:#1e293b; color:white; }
#f-shoot { background:#ef4444; }"""

        val js = """const cv = document.getElementById('sc-canvas');
const cx = cv.getContext('2d');
const scEl = document.getElementById('sc');

let ship = { x: 160, y: 460, vx: 0 };
let bullets = [];
let meteors = [];
let score = 0;
let over = false;

function shoot() {
  bullets.push({ x: ship.x + 18, y: ship.y - 6 });
}

document.getElementById('f-left').onpointerdown = () => ship.vx = -4.5;
document.getElementById('f-left').onpointerup = () => ship.vx = 0;
document.getElementById('f-right').onpointerdown = () => ship.vx = 4.5;
document.getElementById('f-right').onpointerup = () => ship.vx = 0;
document.getElementById('f-shoot').onclick = shoot;

window.addEventListener('keydown', e => {
  if (e.key === 'ArrowLeft') ship.vx = -5;
  if (e.key === 'ArrowRight') ship.vx = 5;
  if (e.key === ' ') shoot();
});
window.addEventListener('keyup', e => {
  if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') ship.vx = 0;
});

function spawnMeteor() {
  meteors.push({ x: Math.random() * (cv.width - 30), y: -30, r: Math.random() * 12 + 10, speed: Math.random() * 2 + 1.8 });
}

setInterval(spawnMeteor, 900);

function frame() {
  if (!over) {
    ship.x += ship.vx;
    if (ship.x < 0) ship.x = 0;
    if (ship.x > cv.width - 36) ship.x = cv.width - 36;

    bullets.forEach((b, bi) => {
      b.y -= 7;
      if (b.y < -10) bullets.splice(bi, 1);
    });

    meteors.forEach((m, mi) => {
      m.y += m.speed;
      // Hit bullet
      bullets.forEach((b, bi) => {
        if (Math.hypot(b.x - m.x, b.y - m.y) < m.r + 6) {
          meteors.splice(mi, 1);
          bullets.splice(bi, 1);
          score += 50;
          scEl.textContent = score;
        }
      });
      // Hit ship
      if (Math.hypot(ship.x + 18 - m.x, ship.y + 18 - m.y) < m.r + 14) {
        over = true;
        alert('💥 Raumschiff zerstört! Score: ' + score);
        location.reload();
      }
    });
  }

  // Draw
  cx.fillStyle = '#030712';
  cx.fillRect(0, 0, cv.width, cv.height);

  // Bullets
  cx.fillStyle = '#facc15';
  bullets.forEach(b => cx.fillRect(b.x - 2, b.y, 4, 10));

  // Meteors
  cx.fillStyle = '#a8a29e';
  meteors.forEach(m => {
    cx.beginPath();
    cx.arc(m.x, m.y, m.r, 0, Math.PI * 2);
    cx.fill();
  });

  // Ship
  cx.fillStyle = '#38bdf8';
  cx.beginPath();
  cx.moveTo(ship.x + 18, ship.y);
  cx.lineTo(ship.x + 36, ship.y + 36);
  cx.lineTo(ship.x, ship.y + 36);
  cx.fill();

  requestAnimationFrame(frame);
}

frame();"""

        return GameProject(
            title = "Space Gorilla (Galaxy Defense)",
            description = "Retro Weltraum-Shooter: Zerstöre Meteore und verteidige die Galaxie!",
            icon = "🚀",
            html = html,
            css = css,
            js = js
        )
    }
}
