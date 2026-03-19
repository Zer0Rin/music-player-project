<template>
  <div class="login-page">

    <div id="wall-bg" ref="wallBgRef" />

    <div class="ambient-orb orb-1" />
    <div class="ambient-orb orb-2" />
    <div class="ambient-orb orb-3" />

    <div class="login-container liquid-panel">
      <div class="login-logo">
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
          <circle cx="20" cy="20" r="20" fill="rgba(250, 45, 72, 0.15)"/>
          <path d="M14 12v16l14-8-14-8z" fill="#fa2d48"/>
        </svg>
        <span>My Music</span>
      </div>

      <div class="tab-switch">
        <button :class="['tab-btn', { active: mode === 'login' }]" @click="mode = 'login'">登录</button>
        <button :class="['tab-btn', { active: mode === 'register' }]" @click="mode = 'register'">注册</button>
      </div>

      <div class="form-body">
        <div class="input-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" @keyup.enter="submit" />
        </div>

        <div v-if="mode === 'register'" class="input-group">
          <label>昵称（可选）</label>
          <input v-model="form.nickname" type="text" placeholder="显示名称，默认同用户名" />
        </div>

        <div class="input-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="submit" />
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button class="submit-btn liquid-btn" :disabled="loading" @click="submit">
          {{ loading ? '请稍候...' : (mode === 'login' ? '登录' : '注册') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

definePageMeta({ layout: false })

const authStore = useAuthStore()
const router = useRouter()

if (authStore.isLoggedIn) {
  router.push('/')
}

const mode = ref('login')
const loading = ref(false)
const errorMsg = ref('')
const form = reactive({ username: '', password: '', nickname: '' })

async function submit() {
  errorMsg.value = ''
  if (!form.username || !form.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await authStore.login(form.username, form.password)
    } else {
      await authStore.register(form.username, form.password, form.nickname)
    }
    router.push('/')
  } catch (e) {
    errorMsg.value = e?.data?.message || '操作失败，请重试'
  } finally {
    loading.value = false
  }
}

// ==========================================
// Three.js 克莱因蓝波浪背景逻辑
// ==========================================
const wallBgRef = ref(null)

const SEPARATION = 100;
const AMOUNTX = 50;
const AMOUNTY = 50;

let camera, scene, renderer, particles, animationId;
let count = 0;
let mouseX = 0, mouseY = 0;
let windowHalfX = typeof window !== 'undefined' ? window.innerWidth / 2 : 0;
let windowHalfY = typeof window !== 'undefined' ? window.innerHeight / 2 : 0;

// 💡 修复 1：提亮基础的克莱因蓝，让它在黑底上更耀眼
const KLEIN_BLUE_HEX = 0x0066ff;

onMounted(() => {
  if (process.client) {
    initThree();
    animateThree();
    window.addEventListener('resize', onWindowResize);
    document.addEventListener('mousemove', onDocumentMouseMove);
  }
});

onUnmounted(() => {
  if (process.client) {
    cancelAnimationFrame(animationId);
    window.removeEventListener('resize', onWindowResize);
    document.removeEventListener('mousemove', onDocumentMouseMove);
    if (renderer) {
      renderer.dispose();
      wallBgRef.value?.removeChild(renderer.domElement);
    }
  }
});

function onWindowResize() {
  if (!camera || !renderer) return;
  windowHalfX = window.innerWidth / 2;
  windowHalfY = window.innerHeight / 2;
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
}

function onDocumentMouseMove(event) {
  mouseX = event.clientX - windowHalfX;
  mouseY = event.clientY - windowHalfY;
}

function initThree() {
  const container = wallBgRef.value;

  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 1, 10000);
  camera.position.z = 1200;
  camera.position.y = 150;

  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x000000);

  const numParticles = AMOUNTX * AMOUNTY;
  const positions = new Float32Array(numParticles * 3);
  const scales = new Float32Array(numParticles);

  let i = 0;
  for (let ix = 0; ix < AMOUNTX; ix++) {
    for (let iy = 0; iy < AMOUNTY; iy++) {
      positions[i * 3] = ix * SEPARATION - ((AMOUNTX * SEPARATION) / 2);
      positions[i * 3 + 1] = 0;
      positions[i * 3 + 2] = iy * SEPARATION - ((AMOUNTY * SEPARATION) / 2);
      scales[i] = 1;
      i++;
    }
  }

  particles = new THREE.BufferGeometry();
  particles.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  particles.setAttribute('scale', new THREE.BufferAttribute(scales, 1));

  const material = new THREE.ShaderMaterial({
    uniforms: {
      color: { value: new THREE.Color(KLEIN_BLUE_HEX) },
    },
    vertexShader: `
      attribute float scale;
      void main() {
        vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );
        // 确保粒子足够大 (1000.0)
        gl_PointSize = scale * ( 1000.0 / - mvPosition.z );
        gl_Position = projectionMatrix * mvPosition;
      }
    `,
    fragmentShader: `
      uniform vec3 color;
      void main() {
        float distanceToCenter = distance(gl_PointCoord, vec2(0.5, 0.5));
        if (distanceToCenter > 0.5) {
          discard;
        }
        float alpha = smoothstep(0.5, 0.48, distanceToCenter);

        // 💡 修复 2：乘以 1.5 强行拉高曝光，产生自发光的霓虹感！
        gl_FragColor = vec4( color * 1.5, alpha );
      }
    `,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });

  const points = new THREE.Points(particles, material);
  scene.add(points);

  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(window.innerWidth, window.innerHeight);
  container.appendChild(renderer.domElement);
}

function animateThree() {
  animationId = requestAnimationFrame(animateThree);
  renderThree();
}

function renderThree() {
  camera.position.x += (mouseX - camera.position.x) * 0.05;
  camera.position.y += (-mouseY - camera.position.y) * 0.05;
  camera.lookAt(scene.position);

  const positions = particles.attributes.position.array;
  const scales = particles.attributes.scale.array;

  let i = 0;
  for (let ix = 0; ix < AMOUNTX; ix++) {
    for (let iy = 0; iy < AMOUNTY; iy++) {
      const idxY = i * 3 + 1;
      positions[idxY] = (Math.sin((ix + count) * 0.3) * 80) + (Math.sin((iy + count) * 0.5) * 80);
      scales[i] = (Math.sin((ix + count) * 0.3) + 1.2) * 1.0 + (Math.sin((iy + count) * 0.5) + 1.2) * 1.0;
      i++;
    }
  }

  particles.attributes.position.needsUpdate = true;
  particles.attributes.scale.needsUpdate = true;

  renderer.render(scene, camera);
  count += 0.08;
}
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000000;
  position: relative;
  overflow: hidden;
}

#wall-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
}

/* 💡 恢复：把光球的 z-index 放在波浪(1)和面板(10)之间，产生纵深感 */
.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  animation: breathe 12s ease-in-out infinite;
  pointer-events: none;
  z-index: 2;
}

.orb-1 { width: 450px; height: 450px; background: rgba(0, 47, 167, 0.4); top: -100px; left: -100px; }
.orb-2 { width: 350px; height: 350px; background: rgba(0, 85, 255, 0.3); bottom: -80px; right: -80px; animation-duration: 18s; }
.orb-3 { width: 300px; height: 300px; background: rgba(0, 30, 120, 0.3); top: 50%; left: 50%; transform: translate(-50%,-50%); animation-duration: 15s; }

@keyframes breathe {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 1; }
}

.login-container {
  position: relative;
  z-index: 10;
  width: 380px;
  padding: 48px 40px;
  border-radius: 28px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  background: rgba(30, 30, 30, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.login-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  font-size: 24px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.tab-switch {
  display: flex;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 4px;
  gap: 4px;
}
.tab-btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.tab-btn:hover:not(.active) {
  color: rgba(255, 255, 255, 0.8);
}
.tab-btn.active {
  background: rgba(250, 45, 72, 0.15);
  color: #fa2d48;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.form-body { display: flex; flex-direction: column; gap: 20px; }

.input-group { display: flex; flex-direction: column; gap: 8px; }
.input-group label { font-size: 13px; font-weight: 500; color: rgba(255, 255, 255, 0.6); padding-left: 2px; }
.input-group input {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(0, 0, 0, 0.2);
  color: white;
  font-size: 15px;
  outline: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.input-group input:focus {
  border-color: rgba(250, 45, 72, 0.5);
  background: rgba(0, 0, 0, 0.4);
  box-shadow: 0 0 0 4px rgba(250, 45, 72, 0.1);
}
.input-group input::placeholder { color: rgba(255, 255, 255, 0.25); font-weight: 400; }

.error-msg {
  color: #ff453a;
  font-size: 13px;
  margin: 0;
  text-align: center;
  font-weight: 500;
}

.submit-btn {
  padding: 14px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #fa2d48, #ff7e5f);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  box-shadow: 0 8px 20px rgba(250, 45, 72, 0.3);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(250, 45, 72, 0.4);
}
.submit-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
</style>