<template>
  <Transition name="lyric-view">
    <div v-if="store.showLyricView" class="lyric-overlay">
      <FluidBackground />

      <button class="close-btn" @click="close" title="关闭 (Esc)">
        <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
          <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/>
        </svg>
      </button>

      <div class="lyric-content">
        <div class="left-panel">

          <div class="interactive-area" @click="toggleFlip">
            <Transition name="fade-flip">
              <div v-show="!isMobile || !isFlipped" class="cover-container">
                <img
                    v-if="store.currentSong"
                    :src="coverUrl(store.currentSong.id)"
                    class="cover-img"
                    @error="e => e.target.style.visibility = 'hidden'"
                />
              </div>
            </Transition>

            <Transition name="fade-flip">
              <div v-if="isMobile && isFlipped" class="mobile-lyrics-container">
                <LyricsDisplay @seek="onSeek" />
              </div>
            </Transition>
          </div>

          <AudioVisualizer :width="420" :height="56" class="visualizer" />

          <div class="bottom-controls-wrap">
            <div class="track-info" v-if="store.currentSong">
              <div class="track-title">{{ store.currentSong.title }}</div>
              <div class="track-artist">{{ store.currentSong.artist }}</div>
              <button
                  class="track-fav-btn"
                  :class="{ 'fav-active': plStore.isFavorite(store.currentSong.id) }"
                  @click="plStore.toggleFavorite(store.currentSong.id)"
              >
                <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
              </button>
            </div>

            <div class="progress-area">
              <div class="progress-bar" ref="progRef" @mousedown="onProgDown" @touchstart.prevent="onProgTouchStart">
                <div class="progress-bg" />
                <div class="progress-fill" :style="{ width: displayProgress + '%' }" />
                <div class="progress-dot" :style="{ left: displayProgress + '%' }" />
              </div>
              <div class="progress-times">
                <span>{{ dragging ? formatTime(dragTime) : store.formattedCurrentTime }}</span>
                <span>{{ store.formattedDuration }}</span>
              </div>
            </div>

            <div class="controls-row">
              <div class="ctrl-side">
                <button class="ctrl mode-btn" @click="store.togglePlayMode()" :title="playModeLabel">
                  <svg v-if="store.playMode === 'sequence'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><path d="M4 12h14" /><path d="M12 6l6 6-6 6" /></svg>
                  <svg v-else-if="store.playMode === 'loop-all'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><path d="M17 1l4 4-4 4" /><path d="M3 11V9a4 4 0 0 1 4-4h14" /><path d="M7 23l-4-4 4-4" /><path d="M21 13v2a4 4 0 0 1-4 4H3" /></svg>
                  <svg v-else-if="store.playMode === 'loop-one'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><path d="M17 1l4 4-4 4" /><path d="M3 11V9a4 4 0 0 1 4-4h14" /><path d="M11 15h2v-4h-2" /><path d="M7 23l-4-4 4-4" /><path d="M21 13v2a4 4 0 0 1-4 4H3" /></svg>
                  <svg v-else-if="store.playMode === 'shuffle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><path d="M16 3h5v5" /><path d="M4 20L21 3" /><path d="M21 16v5h-5" /><path d="M15 15l6 6" /><path d="M4 4l5 5" /></svg>
                </button>
              </div>

              <div class="ctrl-main">
                <button class="ctrl-btn" :disabled="!store.hasPrev" @click="store.prevSong()">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32"><path d="M6 6h2v12H6zm3.5 6 8.5 6V6z"/></svg>
                </button>

                <button class="ctrl-btn play-btn" @click="store.togglePlay()">
                  <svg v-if="store.isPlaying" viewBox="0 0 24 24" fill="currentColor" width="36" height="36"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="currentColor" width="36" height="36"><path d="M8 5v14l11-7z"/></svg>
                </button>

                <button class="ctrl-btn" :disabled="!store.hasNext" @click="store.nextSong()">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
                </button>
              </div>

              <div class="ctrl-side">
                <button class="fn-btn" title="更多">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
                </button>
              </div>
            </div>

            <div class="volume-row">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="vol-icon"><path d="M3 9v6h4l5 5V4L7 9H3z"/></svg>
              <div class="vol-track" ref="volRef" @mousedown="onVolDown" @touchstart.prevent="onVolTouchStart">
                <div class="vol-fill" :style="{ width: (volDisplay * 100) + '%' }" />
                <div class="vol-dot" :style="{ left: (volDisplay * 100) + '%' }" />
              </div>
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="vol-icon"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 8.5v7a4.5 4.5 0 0 0 2.5-3.5zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>
            </div>
          </div>
        </div>

        <div v-if="!isMobile" class="right-panel">
          <LyricsDisplay @seek="onSeek" />
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import FluidBackground from './FluidBackground.vue'
import AudioVisualizer from './AudioVisualizer.vue'
import LyricsDisplay from '~/components/lyrics/LyricsDisplay.vue'

// 假设这些是你原本定义的 composables
const store = usePlayerStore()
const plStore = usePlaylistStore()
const { coverUrl } = useCoverUrl()
const emit = defineEmits(['seek', 'volume'])

// 💡 新增：屏幕尺寸检测与翻转状态
const isMobile = ref(false)
const isFlipped = ref(false) // 移动端控制封面和歌词的切换

function toggleFlip() {
  if (isMobile.value) {
    isFlipped.value = !isFlipped.value
  }
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    isFlipped.value = false // 切回 PC 时重置状态
  }
}

const eschandler = (e) => { if (e.key === 'Escape') close() }
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  window.addEventListener('keydown', eschandler)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  window.removeEventListener('keydown', eschandler)
  clearTimeout(modeTipTimer)
})

// === 以下是你原有的拖动逻辑，保持不变 ===
const progRef = ref(null)
const volRef = ref(null)
const showModeTip = ref(false)
let modeTipTimer = null

const dragging = ref(false)
const dragTime = ref(0)
const displayProgress = computed(() => dragging.value ? (dragTime.value / store.duration) * 100 : store.progress * 100)

function onProgDown(e) { dragging.value = true; updateDragFromEvent(e); window.addEventListener('mousemove', onProgMove); window.addEventListener('mouseup', onProgUp) }
function onProgMove(e) { if (dragging.value) updateDragFromEvent(e) }
function onProgUp() { window.removeEventListener('mousemove', onProgMove); window.removeEventListener('mouseup', onProgUp); if (dragging.value) { emit('seek', dragTime.value); dragging.value = false } }
function onProgTouchStart(e) { dragging.value = true; updateDragFromTouch(e); window.addEventListener('touchmove', onProgTouchMove, { passive: false }); window.addEventListener('touchend', onProgTouchEnd) }
function onProgTouchMove(e) { e.preventDefault(); if (dragging.value) updateDragFromTouch(e) }
function onProgTouchEnd() { window.removeEventListener('touchmove', onProgTouchMove); window.removeEventListener('touchend', onProgTouchEnd); if (dragging.value) { emit('seek', dragTime.value); dragging.value = false } }
function updateDragFromEvent(e) { const rect = progRef.value.getBoundingClientRect(); const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)); dragTime.value = ratio * store.duration }
function updateDragFromTouch(e) { const touch = e.touches[0]; const rect = progRef.value.getBoundingClientRect(); const ratio = Math.max(0, Math.min(1, (touch.clientX - rect.left) / rect.width)); dragTime.value = ratio * store.duration }

const volDragging = ref(false)
const volDragVal = ref(0)
const volDisplay = computed(() => volDragging.value ? volDragVal.value : store.volume)

function onVolDown(e) { volDragging.value = true; updateVolFromEvent(e); window.addEventListener('mousemove', onVolMove); window.addEventListener('mouseup', onVolUp) }
function onVolMove(e) { if (volDragging.value) updateVolFromEvent(e) }
function onVolUp() { window.removeEventListener('mousemove', onVolMove); window.removeEventListener('mouseup', onVolUp); if (volDragging.value) { emit('volume', volDragVal.value); volDragging.value = false } }
function onVolTouchStart(e) { volDragging.value = true; updateVolFromTouch(e); window.addEventListener('touchmove', onVolTouchMove, { passive: false }); window.addEventListener('touchend', onVolTouchEnd) }
function onVolTouchMove(e) { e.preventDefault(); if (volDragging.value) updateVolFromTouch(e) }
function onVolTouchEnd() { window.removeEventListener('touchmove', onVolTouchMove); window.removeEventListener('touchend', onVolTouchEnd); if (volDragging.value) { emit('volume', volDragVal.value); volDragging.value = false } }
function updateVolFromEvent(e) { const rect = volRef.value.getBoundingClientRect(); volDragVal.value = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)); emit('volume', volDragVal.value) }
function updateVolFromTouch(e) { const touch = e.touches[0]; const rect = volRef.value.getBoundingClientRect(); volDragVal.value = Math.max(0, Math.min(1, (touch.clientX - rect.left) / rect.width)); emit('volume', volDragVal.value) }

const playModeLabel = computed(() => ({ 'sequence': '顺序播放', 'loop-all': '列表循环', 'loop-one': '单曲循环', 'shuffle': '随机播放' })[store.playMode])
watch(() => store.playMode, () => { showModeTip.value = true; clearTimeout(modeTipTimer); modeTipTimer = setTimeout(() => { showModeTip.value = false }, 1500) })

function close() { store.closeLyricView() }
function onSeek(time) { emit('seek', time) }
function formatTime(s) { if (!s || isNaN(s)) return '0:00'; return Math.floor(s / 60) + ':' + Math.floor(s % 60).toString().padStart(2, '0') }
</script>

<style scoped>
/* 原有的全局和基础样式保持不变 */
.lyric-overlay { position: fixed; inset: 0; z-index: 1000; background: #000; overflow: hidden; }
.mode-btn { background: transparent !important; border: none !important; outline: none; box-shadow: none !important; color: rgba(255, 255, 255, 0.6); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 8px; border-radius: 50%; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.mode-btn:hover { color: #ffffff; background: rgba(255, 255, 255, 0.1) !important; transform: scale(1.15); }
.close-btn { position: absolute; top: 20px; left: 24px; z-index: 50; background: transparent; border: none; color: rgba(255,255,255,0.7); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all var(--transition-fast); }
.close-btn:hover { color: #fff; transform: translateY(4px); }

.lyric-content { position: relative; z-index: 1; height: 100%; display: flex; padding: 40px 0; }

/* PC端左侧布局 */
.left-panel { margin-left: 10%; width: 30%; min-width: 0; overflow: hidden; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 18px; position: relative; }
.interactive-area { width: min(420px, 95%); aspect-ratio: 1 / 1; position: relative; flex-shrink: 0; cursor: pointer; }
.cover-container {
  position: absolute;  /* 关键：剥夺它撑开父容器的权利 */
  inset: 0;            /* 相当于 top:0; bottom:0; left:0; right:0; 完美填满 */
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.45), 0 8px 24px rgba(0, 0, 0, 0.3);
  background: rgba(255, 255, 255, 0.05);
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover !important;
  display: block;
}
.bottom-controls-wrap { width: 100%; display: flex; flex-direction: column; align-items: center; gap: 18px; }
.track-info { text-align: center; width: min(420px, 95%); padding: 4px 0 0; }
.track-title { font-size: 27px; font-weight: 700; letter-spacing: -0.01em; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.track-artist { font-size: 21px; color: rgba(255, 255, 255, 0.5); margin-top: 5px; text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3); }

/* 进度条与控制台 (保持你原有的精美样式) */
.progress-area { width: min(420px, 95%); }
.progress-bar { position: relative; height: 28px; display: flex; align-items: center; cursor: pointer; touch-action: none; }
.progress-bg { position: absolute; left: 0; right: 0; height: 5px; background: rgba(255, 255, 255, 0.15); border-radius: 3px; top: 50%; transform: translateY(-50%); transition: height 0.15s ease; }
.progress-fill { position: absolute; left: 0; height: 5px; background: rgba(255, 255, 255, 0.85); border-radius: 3px; top: 50%; transform: translateY(-50%); transition: height 0.15s ease; }
.progress-dot { position: absolute; top: 50%; width: 18px; height: 18px; border-radius: 50%; background: #fff; transform: translate(-50%, -50%) scale(0); transition: transform 0.15s ease; box-shadow: 0 1px 6px rgba(0, 0, 0, 0.35); z-index: 2; }
.progress-bar:hover .progress-dot, .progress-bar:active .progress-dot { transform: translate(-50%, -50%) scale(1); }
.progress-bar:hover .progress-bg, .progress-bar:active .progress-bg, .progress-bar:hover .progress-fill, .progress-bar:active .progress-fill { height: 7px; }
.progress-times { display: flex; justify-content: space-between; margin-top: 5px; font-size: 13px; color: rgba(255, 255, 255, 0.35); font-variant-numeric: tabular-nums; }
.controls-row { display: flex; align-items: center; justify-content: space-between; width: min(420px, 95%); }
.ctrl-side { display: flex; align-items: center; gap: 4px; }
.fn-btn { background: none; border: none; color: rgba(255, 255, 255, 0.4); cursor: pointer; padding: 10px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all var(--transition-fast); }
.fn-btn:hover { color: rgba(255, 255, 255, 0.85); background: rgba(255, 255, 255, 0.06); }
.ctrl-main { display: flex; align-items: center; gap: 24px; }
.ctrl-btn { background: none; border: none; color: rgba(255, 255, 255, 0.8); cursor: pointer; padding: 6px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all var(--transition-fast); }
.ctrl-btn:hover:not(:disabled) { color: #fff; transform: scale(1.1); }
.ctrl-btn:disabled { opacity: 0.25; cursor: not-allowed; }
.play-btn { width: 72px; height: 72px; background: rgba(255, 255, 255, 0.9); color: #000 !important; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2); }
.play-btn:hover { background: #fff !important; transform: scale(1.06) !important; }
.volume-row { display: flex; align-items: center; gap: 10px; width: min(420px, 95%); }
.vol-icon { color: rgba(255, 255, 255, 0.35); flex-shrink: 0; }
.vol-track { flex: 1; height: 24px; position: relative; display: flex; align-items: center; cursor: pointer; touch-action: none; }
.vol-fill { position: absolute; left: 0; height: 4px; background: rgba(255, 255, 255, 0.75); border-radius: 2px; top: 50%; transform: translateY(-50%); pointer-events: none; }
.vol-track::before { content: ''; position: absolute; left: 0; right: 0; height: 4px; background: rgba(255, 255, 255, 0.12); border-radius: 2px; top: 50%; transform: translateY(-50%); }
.vol-dot { position: absolute; top: 50%; width: 14px; height: 14px; border-radius: 50%; background: #fff; transform: translate(-50%, -50%) scale(0); transition: transform 0.15s ease; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3); z-index: 2; }
.vol-track:hover .vol-dot, .vol-track:active .vol-dot { transform: translate(-50%, -50%) scale(1); }

/* PC端右侧面板 */
.right-panel { position: absolute; left: 48%; width: 45%; top: 40px; bottom: 40px; overflow: hidden; }

/* 切换动画 */
.fade-flip-enter-active, .fade-flip-leave-active { transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94); position: absolute; inset: 0; }
.fade-flip-enter-from { opacity: 0; transform: scale(0.95); }
.fade-flip-leave-to { opacity: 0; transform: scale(1.05); }


/* =========================================
   📱 移动端沉浸式单列布局 (Apple Music 风格)
   ========================================= */
@media (max-width: 768px) {
  /* 调整整体容器边距，顶部留给返回按钮，底部顶满 */
  .lyric-content {
    padding: 60px 24px 32px;
    flex-direction: column;
    align-items: center;
    justify-content: space-between; /* 上下分布 */
    height: 100vh;
  }

  /* 左面板变为撑满屏幕的主体 */
  .left-panel { margin-left: 0; width: 100%; height: 100%; flex: 1; gap: 0; justify-content: flex-start; }

  /* 交互区（封面/歌词容器）：占据上半部分主要空间 */
  .interactive-area {
    width: 100%;
    height: 55vh; /* 高度动态适配屏幕 */
    aspect-ratio: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
  }

  /* 移动端封面：方正、巨大、带大圆角 */
  .cover-container {
    width: min(85vw, 360px);
    aspect-ratio: 1;
    border-radius: 12px;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
  }

  /* 移动端歌词容器 */
  .mobile-lyrics-container {
    width: 100%;
    height: 100%;
    overflow: hidden;
    mask-image: linear-gradient(to bottom, transparent, black 10%, black 90%, transparent);
    -webkit-mask-image: linear-gradient(to bottom, transparent, black 10%, black 90%, transparent);
  }

  /* 底部控制台：挤在屏幕下半部分 */
  .bottom-controls-wrap {
    width: 100%;
    gap: 16px;
    margin-top: auto; /* 自动推到底部 */
  }

  /* 左对齐的歌曲信息 */
  .track-info {
    width: 100%;
    text-align: left; /* Apple Music 风格左对齐 */
    padding-left: 8px;
  }
  .track-title { font-size: 24px; }
  .track-artist { font-size: 18px; margin-top: 2px; }

  /* 各种进度条宽度撑满 */
  .progress-area, .controls-row, .volume-row { width: 100%; max-width: none; }

  /* 按钮尺寸微调 */
  .play-btn { width: 64px; height: 64px; }
  .ctrl-main { gap: 32px; }

  /* 隐藏频谱（移动端空间宝贵） */
  .visualizer { display: none; }

  /* 顶部下拉按钮改为横杠样式 */
  .close-btn {
    top: 12px;
    left: 50%;
    transform: translateX(-50%);
    width: 44px;
    height: 16px;
    background: transparent;
    backdrop-filter: none;
  }
  .close-btn svg { display: none; } /* 隐藏原来的 SVG */
  .close-btn::after {
    content: '';
    display: block;
    width: 36px;
    height: 5px;
    background: rgba(255,255,255,0.4);
    border-radius: 3px;
  }
}

.track-info {
  text-align: center;
  width: min(420px, 95%);
  padding: 4px 0 0;
  position: relative;
}

/* 爱心绝对定位在右侧 */
.track-fav-btn {
  position: absolute;
  right: -10px;
  top: 6px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.track-fav-btn:hover {
  color: rgba(255, 255, 255, 0.7);
  transform: scale(1.15);
}

.track-fav-btn.fav-active {
  color: var(--accent);
  filter: drop-shadow(0 0 8px rgba(250, 45, 72, 0.5));
}

.track-fav-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.track-fav-btn:hover {
  color: rgba(255, 255, 255, 0.7);
  transform: scale(1.15);
}

.track-fav-btn.fav-active {
  color: var(--accent);
  filter: drop-shadow(0 0 8px rgba(250, 45, 72, 0.5));
}
</style>