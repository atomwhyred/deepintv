// 3D layer of the phone settings page (res/raw/config_page.html): an animated Deepin-blue wave
// field behind the page and a live 3D preview of the TV's Home screen. Bundled with only the
// three.js parts it uses into res/raw/config_scene.js (`npm run build` in this folder).
//
// The page's own script publishes the settings being edited in `window.__deepintv` and fires a
// `deepintv:state` event on every change; this module only reads them, so the form works the same
// with or without WebGL (without it, the preview falls back to the flat 2D drawing of the screen).

import {
  AdditiveBlending,
  AmbientLight,
  BoxGeometry,
  CanvasTexture,
  Color,
  DirectionalLight,
  Group,
  Mesh,
  MeshBasicMaterial,
  MeshStandardMaterial,
  PerspectiveCamera,
  PlaneGeometry,
  Points,
  PointLight,
  Scene,
  ShaderMaterial,
  SRGBColorSpace,
  WebGLRenderer,
} from "three";
import { RoundedBoxGeometry } from "three/addons/geometries/RoundedBoxGeometry.js";

const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
const darkPage = matchMedia("(prefers-color-scheme: dark)");

// ---- The TV's Home screen, drawn in 2D (same tokens as ui/theme/Theme.kt) ---------------------

const SCREEN_W = 1280;
const SCREEN_H = 720;
const TOKENS = {
  dark: { bg: ["#1d2b4a", "#0b0e14"], surface: "rgba(25,31,43,0.94)", text: "#f5f6f8", sub: "#9aa3b2", wash: "rgba(245,246,248,0.12)", dock: [25, 31, 43] },
  light: { bg: ["#f3dccd", "#b9d2ee"], surface: "rgba(255,255,255,0.96)", text: "#10141c", sub: "#5b6577", wash: "rgba(16,20,28,0.12)", dock: [255, 255, 255] },
};
const APP_COLORS = ["#ff2d2d", "#1d4fa8", "#6b4cf6", "#111827", "#e50914", "#0a84ff", "#ff8a00"];

function isLight(state) {
  if (state.theme === "LIGHT") return true;
  if (state.theme !== "AUTO") return false;
  const d = new Date();
  const now = d.getHours() * 60 + d.getMinutes();
  const from = state.lightFrom, to = state.lightTo;
  return from <= to ? now >= from && now < to : now >= from || now < to;
}

function roundRect(g, x, y, w, h, r) {
  g.beginPath();
  g.moveTo(x + r, y);
  g.arcTo(x + w, y, x + w, y + h, r);
  g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r);
  g.arcTo(x, y, x + w, y, r);
  g.closePath();
}

function line(g, x, y, w, color, h = 8) {
  g.fillStyle = color;
  roundRect(g, x, y, w, h, h / 2);
  g.fill();
}

/** A small glyph per widget kind, so the order on the phone reads at a glance on the preview. */
function widgetBody(g, kind, x, y, w, t) {
  switch (kind) {
    case "WEATHER": {
      g.fillStyle = "#ffb020";
      g.beginPath(); g.arc(x + 30, y + 34, 18, 0, Math.PI * 2); g.fill();
      g.fillStyle = t.text; g.font = "300 44px system-ui, sans-serif"; g.fillText("10°", x + 64, y + 50);
      for (let i = 0; i < 4; i++) line(g, x + i * (w / 4), y + 92, w / 6, t.wash, 18);
      break;
    }
    case "SYSTEM": {
      [[0.62, "#0a84ff"], [0.45, "#ffb020"]].forEach(([v, c], i) => {
        line(g, x, y + 10 + i * 46, w * 0.4, t.wash, 10);
        line(g, x, y + 30 + i * 46, w, t.wash, 8);
        line(g, x, y + 30 + i * 46, w * v, c, 8);
      });
      break;
    }
    case "CRYPTO": {
      for (let r = 0; r < 3; r++) {
        line(g, x, y + 8 + r * 40, w * 0.25, t.text, 10);
        g.strokeStyle = "#30d158"; g.lineWidth = 3; g.beginPath();
        for (let i = 0; i <= 20; i++) {
          const px = x + w * 0.45 + (i / 20) * w * 0.4;
          const py = y + 18 + r * 40 + Math.sin(i * 1.3 + r) * 6 - i * 0.4;
          i ? g.lineTo(px, py) : g.moveTo(px, py);
        }
        g.stroke();
      }
      break;
    }
    case "WATCH_NEXT": {
      for (let r = 0; r < 2; r++) {
        line(g, x, y + 4 + r * 52, w * 0.7, t.text, 10);
        line(g, x, y + 22 + r * 52, w * 0.85, t.sub, 8);
        line(g, x, y + 38 + r * 52, w, t.wash, 5);
        line(g, x, y + 38 + r * 52, w * (0.25 + r * 0.3), t.text, 5);
      }
      break;
    }
    default: { // CALENDAR, NOTIFICATIONS and anything new
      for (let r = 0; r < 3; r++) {
        line(g, x, y + 6 + r * 40, w * (0.8 - r * 0.15), t.text, 10);
        line(g, x, y + 24 + r * 40, w * 0.55, t.sub, 8);
      }
    }
  }
}

function drawScreen(canvas, state) {
  const g = canvas.getContext("2d");
  const t = isLight(state) ? TOKENS.light : TOKENS.dark;

  // Wallpaper: a soft Deepin-style gradient with two blurred "dunes".
  const bg = g.createLinearGradient(0, 0, SCREEN_W, SCREEN_H);
  bg.addColorStop(0, t.bg[0]); bg.addColorStop(1, t.bg[1]);
  g.fillStyle = bg; g.fillRect(0, 0, SCREEN_W, SCREEN_H);
  [[260, 640, 520, "rgba(10,132,255,0.22)"], [1040, 560, 460, "rgba(24,144,255,0.18)"]].forEach(([cx, cy, r, c]) => {
    const glow = g.createRadialGradient(cx, cy, 0, cx, cy, r);
    glow.addColorStop(0, c); glow.addColorStop(1, "rgba(0,0,0,0)");
    g.fillStyle = glow; g.fillRect(0, 0, SCREEN_W, SCREEN_H);
  });

  // Clock and date.
  const now = new Date();
  g.fillStyle = t.text; g.font = "200 92px system-ui, sans-serif";
  g.fillText(`${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`, 46, 120);
  g.fillStyle = t.sub; g.font = "400 24px system-ui, sans-serif";
  g.fillText(now.toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" }), 48, 160);

  // Pills: inputs, control center, widgets, power.
  for (let i = 0; i < 4; i++) {
    g.fillStyle = t.surface;
    g.beginPath(); g.arc(SCREEN_W - 70 - i * 84, 78, 34, 0, Math.PI * 2); g.fill();
    line(g, SCREEN_W - 84 - i * 84, 74, 28, t.text, 8);
  }

  // Widget cards, in the chosen order (four fit; the TV scrolls the rest).
  const cardW = 270, cardH = 228, gap = 22, top = 190;
  state.widgets.slice(0, 4).forEach((kind, i) => {
    const x = 46 + i * (cardW + gap);
    g.fillStyle = t.surface; roundRect(g, x, top, cardW, cardH, 22); g.fill();
    g.fillStyle = t.text; g.font = "500 24px system-ui, sans-serif";
    g.fillText((state.labels && state.labels[kind]) || kind, x + 22, top + 42);
    widgetBody(g, kind, x + 22, top + 70, cardW - 44, t);
  });
  if (!state.widgets.length) {
    g.fillStyle = t.sub; g.font = "400 24px system-ui, sans-serif";
    g.fillText("—", 46, top + 40);
  }

  // Dock: tint alpha = transparency% × 0.6 and icons 120 dp × size%, as in Dock/GlassSurface.
  const scale = SCREEN_H / 1080 * 1.5; // dp → this 720 px drawing of a 1080p screen
  const icon = Math.round(120 * state.dockSize / 100 * scale * 0.62);
  const dockH = icon + 40, dockY = SCREEN_H - dockH - 26, dockX = 46, dockW = SCREEN_W - 200;
  const [r, gr, b] = t.dock;
  g.fillStyle = `rgba(${r},${gr},${b},${(state.dockTransparency / 100) * 0.6 + 0.08})`;
  roundRect(g, dockX, dockY, dockW, dockH, 26); g.fill();
  g.strokeStyle = t.wash; g.lineWidth = 2; g.stroke();
  const n = Math.max(1, Math.min(APP_COLORS.length, Math.floor((dockW - 40) / (icon + 18))));
  for (let i = 0; i < n; i++) {
    const x = dockX + 24 + i * (icon + 18);
    g.fillStyle = "#1b2030"; roundRect(g, x, dockY + 20, icon, icon, icon * 0.18); g.fill();
    g.fillStyle = APP_COLORS[i]; roundRect(g, x + icon * 0.2, dockY + 20 + icon * 0.2, icon * 0.6, icon * 0.6, icon * 0.14); g.fill();
  }
  g.fillStyle = t.surface;
  g.beginPath(); g.arc(SCREEN_W - 92, dockY + dockH / 2, 40, 0, Math.PI * 2); g.fill();
  for (let i = 0; i < 9; i++) {
    g.fillStyle = t.text;
    g.fillRect(SCREEN_W - 108 + (i % 3) * 12, dockY + dockH / 2 - 16 + Math.floor(i / 3) * 12, 8, 8);
  }
}

// ---- WebGL --------------------------------------------------------------------------------------

function makeRenderer(canvas, maxRatio) {
  try {
    const renderer = new WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: "low-power" });
    renderer.setPixelRatio(Math.min(devicePixelRatio || 1, maxRatio));
    renderer.outputColorSpace = SRGBColorSpace;
    return renderer;
  } catch (e) {
    return null;
  }
}

const WAVE_VERTEX = `
  uniform float uTime;
  varying float vHeight;
  varying float vDepth;
  void main() {
    vec3 p = position;
    float h = sin(p.x * 0.18 + uTime * 0.6) * 0.9
            + sin(p.y * 0.23 + uTime * 0.45) * 0.7
            + sin((p.x + p.y) * 0.11 + uTime * 0.3) * 1.1;
    p.z += h;
    vHeight = h;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    vDepth = -mv.z;
    gl_PointSize = clamp(42.0 / vDepth, 1.0, 3.5);
    gl_Position = projectionMatrix * mv;
  }`;

const WAVE_FRAGMENT = `
  uniform vec3 uLow;
  uniform vec3 uHigh;
  uniform float uAlpha;
  varying float vHeight;
  varying float vDepth;
  void main() {
    float k = smoothstep(-2.4, 2.4, vHeight);
    float fade = 1.0 - smoothstep(18.0, 60.0, vDepth);
    gl_FragColor = vec4(mix(uLow, uHigh, k), uAlpha * fade);
  }`;

function startBackground(canvas) {
  const renderer = makeRenderer(canvas, 1.5);
  if (!renderer) return;
  const scene = new Scene();
  const camera = new PerspectiveCamera(55, 1, 0.1, 100);
  camera.position.set(0, 7, 18);
  camera.lookAt(0, 0, -6);

  const uniforms = {
    uTime: { value: 0 },
    uLow: { value: new Color() },
    uHigh: { value: new Color() },
    uAlpha: { value: 0.5 },
  };
  const pointUniforms = { ...uniforms, uAlpha: { value: 0.9 } };
  const geometry = new PlaneGeometry(90, 70, 140, 110);
  const surface = new Mesh(geometry, new ShaderMaterial({
    uniforms, vertexShader: WAVE_VERTEX, fragmentShader: WAVE_FRAGMENT, transparent: true, wireframe: true, depthWrite: false,
  }));
  const dots = new Points(geometry, new ShaderMaterial({
    uniforms: pointUniforms, vertexShader: WAVE_VERTEX, fragmentShader: WAVE_FRAGMENT, transparent: true, depthWrite: false, blending: AdditiveBlending,
  }));
  [surface, dots].forEach((m) => { m.rotation.x = -Math.PI / 2.3; m.position.y = -3; scene.add(m); });

  function palette() {
    const dark = darkPage.matches;
    uniforms.uLow.value.set(dark ? "#0b3a7a" : "#8ec5ff");
    uniforms.uHigh.value.set(dark ? "#35c3ff" : "#0081ff");
    uniforms.uAlpha.value = dark ? 0.32 : 0.3;
    pointUniforms.uLow.value.copy(uniforms.uLow.value);
    pointUniforms.uHigh.value.copy(uniforms.uHigh.value);
    pointUniforms.uAlpha.value = dark ? 0.85 : 0.75;
  }
  palette();
  darkPage.addEventListener("change", () => { palette(); if (reducedMotion) draw(0); });

  function resize() {
    renderer.setSize(innerWidth, innerHeight, false);
    camera.aspect = innerWidth / innerHeight;
    camera.updateProjectionMatrix();
  }
  addEventListener("resize", () => { resize(); if (reducedMotion) draw(0); });
  resize();

  function draw(t) {
    uniforms.uTime.value = pointUniforms.uTime.value = t;
    renderer.render(scene, camera);
  }
  if (reducedMotion) { draw(0); return; }
  loop((t) => draw(t * 0.001));
}

const BASE_Z = 5.3;

function startPreview(holder) {
  const screen = document.createElement("canvas");
  screen.width = SCREEN_W; screen.height = SCREEN_H;
  const canvas = document.createElement("canvas");
  holder.appendChild(canvas);
  const renderer = makeRenderer(canvas, 2);
  if (!renderer) {
    // No WebGL: show the flat drawing instead, still live.
    holder.removeChild(canvas);
    screen.className = "flat";
    holder.appendChild(screen);
    const redraw = () => drawScreen(screen, window.__deepintv);
    if (window.__deepintv) redraw();
    addEventListener("deepintv:state", redraw);
    return;
  }

  const scene = new Scene();
  const camera = new PerspectiveCamera(32, 16 / 10, 0.1, 50);
  camera.position.set(0, 0.15, BASE_Z);

  scene.add(new AmbientLight(0xffffff, 0.55));
  const key = new DirectionalLight(0xffffff, 1.4);
  key.position.set(3, 4, 5);
  scene.add(key);
  const rim = new PointLight(0x0081ff, 18, 12);
  rim.position.set(-2.5, 1.5, -1.5);
  scene.add(rim);

  const tv = new Group();
  const frame = new Mesh(
    new RoundedBoxGeometry(3.3, 1.92, 0.12, 4, 0.06),
    new MeshStandardMaterial({ color: 0x14171f, metalness: 0.65, roughness: 0.32 }),
  );
  tv.add(frame);
  const texture = new CanvasTexture(screen);
  texture.colorSpace = SRGBColorSpace;
  texture.anisotropy = renderer.capabilities.getMaxAnisotropy();
  const panel = new Mesh(new PlaneGeometry(3.2, 1.8), new MeshBasicMaterial({ map: texture, toneMapped: false }));
  panel.position.z = 0.062;
  tv.add(panel);
  const metal = new MeshStandardMaterial({ color: 0x20242d, metalness: 0.8, roughness: 0.3 });
  const neck = new Mesh(new BoxGeometry(0.16, 0.42, 0.06), metal);
  neck.position.set(0, -1.17, -0.04);
  tv.add(neck);
  const base = new Mesh(new RoundedBoxGeometry(1.25, 0.05, 0.42, 2, 0.02), metal);
  base.position.set(0, -1.38, 0);
  tv.add(base);
  scene.add(tv);

  // Soft blue glow on the "floor" under the TV.
  const glowCanvas = document.createElement("canvas");
  glowCanvas.width = glowCanvas.height = 128;
  const gg = glowCanvas.getContext("2d");
  const grad = gg.createRadialGradient(64, 64, 0, 64, 64, 64);
  grad.addColorStop(0, "rgba(0,129,255,0.9)"); grad.addColorStop(1, "rgba(0,129,255,0)");
  gg.fillStyle = grad; gg.fillRect(0, 0, 128, 128);
  const glow = new Mesh(
    new PlaneGeometry(5, 1.6),
    new MeshBasicMaterial({ map: new CanvasTexture(glowCanvas), transparent: true, blending: AdditiveBlending, depthWrite: false }),
  );
  glow.rotation.x = -Math.PI / 2;
  glow.position.y = -1.42;
  scene.add(glow);

  // Drag to turn the TV; it eases back to a gentle sway when let go.
  let targetY = 0, targetX = 0, dragging = false, lastX = 0, lastY = 0;
  canvas.addEventListener("pointerdown", (e) => { dragging = true; lastX = e.clientX; lastY = e.clientY; canvas.setPointerCapture(e.pointerId); });
  canvas.addEventListener("pointermove", (e) => {
    if (!dragging) return;
    targetY = Math.max(-0.7, Math.min(0.7, targetY + (e.clientX - lastX) * 0.008));
    targetX = Math.max(-0.3, Math.min(0.3, targetX + (e.clientY - lastY) * 0.004));
    lastX = e.clientX; lastY = e.clientY;
  });
  const release = () => { dragging = false; };
  canvas.addEventListener("pointerup", release);
  canvas.addEventListener("pointercancel", release);

  function resize() {
    const w = holder.clientWidth, h = holder.clientHeight;
    if (!w || !h) return;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    // Keep the whole TV in view on narrow (portrait) phones.
    camera.position.z = camera.aspect < 1.25 ? BASE_Z * (1.25 / camera.aspect) * 0.85 : BASE_Z;
    camera.updateProjectionMatrix();
  }
  new ResizeObserver(resize).observe(holder);
  resize();

  function refresh() {
    if (!window.__deepintv) return;
    drawScreen(screen, window.__deepintv);
    texture.needsUpdate = true;
  }
  refresh();
  addEventListener("deepintv:state", () => { refresh(); if (reducedMotion) render(0); });
  // The clock on the preview follows the real time.
  setInterval(refresh, 30_000);

  function render(t) {
    if (!dragging) {
      targetY *= 0.96; targetX *= 0.96;
    }
    const sway = reducedMotion ? 0 : Math.sin(t * 0.4) * 0.12;
    tv.rotation.y += (targetY + sway - tv.rotation.y) * 0.08;
    tv.rotation.x += (targetX - tv.rotation.x) * 0.08;
    tv.position.y = reducedMotion ? 0 : Math.sin(t * 0.8) * 0.03;
    renderer.render(scene, camera);
  }
  if (reducedMotion) {
    render(0);
    addEventListener("resize", () => render(0));
    return;
  }
  loop((t) => render(t * 0.001));
}

/** requestAnimationFrame that stops while the page is hidden (screen off, other tab). */
function loop(frame) {
  let id = 0;
  const tick = (t) => { frame(t); id = requestAnimationFrame(tick); };
  const start = () => { if (!id) id = requestAnimationFrame(tick); };
  const stop = () => { cancelAnimationFrame(id); id = 0; };
  document.addEventListener("visibilitychange", () => (document.hidden ? stop() : start()));
  start();
}

const bg = document.getElementById("bg");
if (bg) startBackground(bg);
const preview = document.getElementById("tv");
if (preview) startPreview(preview);
document.documentElement.classList.add("has-3d");
