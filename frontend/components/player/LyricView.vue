<template>
  <Transition name="lyric-view">
    <div v-if="store.showLyricView" class="lyric-overlay">
      <FluidBackground />

      <!-- 左上角返回按钮 -->
      <button class="close-btn" @click="close" title="关闭 (Esc)">
        <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
          <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/>
        </svg>
      </button>

      <div class="lyric-content">
        <!-- 左面板 -->
        <div class="left-panel">
          <!-- 封面 -->
          <div class="cover-container">
            <img
              v-if="store.currentSong"
              :src="coverUrl(store.currentSong.id)"
              class="cover-img"
              @error="e => e.target.style.visibility = 'hidden'"
            />
          </div>

          <!-- 频谱 -->
          <AudioVisualizer :width="420" :height="56" class="visualizer" />

          <!-- 歌曲信息 -->
          <div class="track-info" v-if="store.currentSong">
            <div class="track-title">{{ store.currentSong.title }}</div>
            <div class="track-artist">{{ store.currentSong.artist }}</div>
          </div>

          <!-- 可拖动进度条 -->
          <div class="progress-area">
            <div
              class="progress-bar"
              ref="progRef"
              @mousedown="onProgDown"
              @touchstart.prevent="onProgTouchStart"
            >
              <div class="progress-bg" />
              <div class="progress-fill" :style="{ width: displayProgress + '%' }" />
              <div class="progress-dot" :style="{ left: displayProgress + '%' }" />
            </div>
            <div class="progress-times">
              <span>{{ dragging ? formatTime(dragTime) : store.formattedCurrentTime }}</span>
              <span>{{ store.formattedDuration }}</span>
            </div>
          </div>

          <!-- 控制按钮行 -->
          <div class="controls-row">
            <div class="ctrl-side">
              <!-- 播放模式 -->
              <button
                  class="ctrl mode-btn"
                  @click="store.togglePlayMode()"
                  :title="playModeLabel"
              >
                <svg v-if="store.playMode === 'sequence'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                  <path d="M4 12h14" />
                  <path d="M12 6l6 6-6 6" />
                </svg>

                <svg v-else-if="store.playMode === 'loop-all'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                  <path d="M17 1l4 4-4 4" />
                  <path d="M3 11V9a4 4 0 0 1 4-4h14" />
                  <path d="M7 23l-4-4 4-4" />
                  <path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>

                <svg v-else-if="store.playMode === 'loop-one'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                  <path d="M17 1l4 4-4 4" />
                  <path d="M3 11V9a4 4 0 0 1 4-4h14" />
                  <path d="M11 15h2v-4h-2" />
                  <path d="M7 23l-4-4 4-4" />
                  <path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>

                <svg v-else-if="store.playMode === 'shuffle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                  <path d="M16 3h5v5" />
                  <path d="M4 20L21 3" />
                  <path d="M21 16v5h-5" />
                  <path d="M15 15l6 6" />
                  <path d="M4 4l5 5" />
                </svg>
              </button>
            </div>

            <!-- 中间播放控制 -->
            <div class="ctrl-main">
              <button class="ctrl-btn" :disabled="!store.hasPrev" @click="store.prevSong()">
                <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
                  <path d="M6 6h2v12H6zm3.5 6 8.5 6V6z"/>
                </svg>
              </button>

              <button class="ctrl-btn play-btn" @click="store.togglePlay()">
                <svg v-if="store.isPlaying" viewBox="0 0 24 24" fill="currentColor" width="30" height="30">
                  <path d="M6 4h4v16H6zM14 4h4v16h-4z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="currentColor" width="30" height="30">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </button>

              <button class="ctrl-btn" :disabled="!store.hasNext" @click="store.nextSong()">
                <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
                  <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
                </svg>
              </button>
            </div>

            <div class="ctrl-side">
              <button class="fn-btn" title="设置">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Apple Music 水平音量条 -->
          <div class="volume-row">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="vol-icon">
              <path d="M3 9v6h4l5 5V4L7 9H3z"/>
            </svg>
            <div class="vol-track" ref="volRef" @mousedown="onVolDown" @touchstart.prevent="onVolTouchStart">
              <div class="vol-fill" :style="{ width: (volDisplay * 100) + '%' }" />
              <div class="vol-dot" :style="{ left: (volDisplay * 100) + '%' }" />
            </div>
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="vol-icon">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 8.5v7a4.5 4.5 0 0 0 2.5-3.5zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
            </svg>
          </div>

          <!-- 播放模式提示 -->
          <Transition name="mode-tip">
            <div v-if="showModeTip" class="mode-tip">{{ playModeLabel }}</div>
          </Transition>
        </div>

        <!-- 右面板：歌词 -->
        <div class="right-panel">
          <LyricsDisplay @seek="onSeek" />
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import FluidBackground from './FluidBackground.vue'
import AudioVisualizer from './AudioVisualizer.vue'
import LyricsDisplay from '~/components/lyrics/LyricsDisplay.vue'

const store = usePlayerStore()
const { coverUrl } = useCoverUrl()
const emit = defineEmits(['seek', 'volume'])
const progRef = ref(null)
const volRef = ref(null)
const showModeTip = ref(false)
let modeTipTimer = null

// ===== 可拖动进度条 =====
const dragging = ref(false)
const dragTime = ref(0)

const displayProgress = computed(() => {
  if (dragging.value) return (dragTime.value / store.duration) * 100
  return store.progress * 100
})

function onProgDown(e) {
  dragging.value = true
  updateDragFromEvent(e)
  window.addEventListener('mousemove', onProgMove)
  window.addEventListener('mouseup', onProgUp)
}

function onProgMove(e) {
  if (dragging.value) updateDragFromEvent(e)
}

function onProgUp() {
  window.removeEventListener('mousemove', onProgMove)
  window.removeEventListener('mouseup', onProgUp)
  if (dragging.value) {
    emit('seek', dragTime.value)
    dragging.value = false
  }
}

function onProgTouchStart(e) {
  dragging.value = true
  updateDragFromTouch(e)
  window.addEventListener('touchmove', onProgTouchMove, { passive: false })
  window.addEventListener('touchend', onProgTouchEnd)
}

function onProgTouchMove(e) {
  e.preventDefault()
  if (dragging.value) updateDragFromTouch(e)
}

function onProgTouchEnd() {
  window.removeEventListener('touchmove', onProgTouchMove)
  window.removeEventListener('touchend', onProgTouchEnd)
  if (dragging.value) {
    emit('seek', dragTime.value)
    dragging.value = false
  }
}

function updateDragFromEvent(e) {
  const rect = progRef.value.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  dragTime.value = ratio * store.duration
}

function updateDragFromTouch(e) {
  const touch = e.touches[0]
  const rect = progRef.value.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (touch.clientX - rect.left) / rect.width))
  dragTime.value = ratio * store.duration
}

// ===== 可拖动音量条 =====
const volDragging = ref(false)
const volDragVal = ref(0)

const volDisplay = computed(() => {
  return volDragging.value ? volDragVal.value : store.volume
})

function onVolDown(e) {
  volDragging.value = true
  updateVolFromEvent(e)
  window.addEventListener('mousemove', onVolMove)
  window.addEventListener('mouseup', onVolUp)
}

function onVolMove(e) {
  if (volDragging.value) updateVolFromEvent(e)
}

function onVolUp() {
  window.removeEventListener('mousemove', onVolMove)
  window.removeEventListener('mouseup', onVolUp)
  if (volDragging.value) {
    emit('volume', volDragVal.value)
    volDragging.value = false
  }
}

function onVolTouchStart(e) {
  volDragging.value = true
  updateVolFromTouch(e)
  window.addEventListener('touchmove', onVolTouchMove, { passive: false })
  window.addEventListener('touchend', onVolTouchEnd)
}

function onVolTouchMove(e) {
  e.preventDefault()
  if (volDragging.value) updateVolFromTouch(e)
}

function onVolTouchEnd() {
  window.removeEventListener('touchmove', onVolTouchMove)
  window.removeEventListener('touchend', onVolTouchEnd)
  if (volDragging.value) {
    emit('volume', volDragVal.value)
    volDragging.value = false
  }
}

function updateVolFromEvent(e) {
  const rect = volRef.value.getBoundingClientRect()
  volDragVal.value = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  emit('volume', volDragVal.value)
}

function updateVolFromTouch(e) {
  const touch = e.touches[0]
  const rect = volRef.value.getBoundingClientRect()
  volDragVal.value = Math.max(0, Math.min(1, (touch.clientX - rect.left) / rect.width))
  emit('volume', volDragVal.value)
}

// ===== 其他 =====
const playModeLabel = computed(() => {
  return {
    'sequence': '顺序播放',
    'loop-all': '列表循环',
    'loop-one': '单曲循环',
    'shuffle': '随机播放'
  }[store.playMode]
})

watch(() => store.playMode, () => {
  showModeTip.value = true
  clearTimeout(modeTipTimer)
  modeTipTimer = setTimeout(() => { showModeTip.value = false }, 1500)
})

function close() { store.closeLyricView() }
function onSeek(time) { emit('seek', time) }

function formatTime(s) {
  if (!s || isNaN(s)) return '0:00'
  return Math.floor(s / 60) + ':' + Math.floor(s % 60).toString().padStart(2, '0')
}

onMounted(() => {
  const handler = (e) => { if (e.key === 'Escape') close() }
  window.addEventListener('keydown', handler)
  onUnmounted(() => {
    window.removeEventListener('keydown', handler)
    clearTimeout(modeTipTimer)
  })
})
</script>

<style scoped>
.lyric-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #000;
  overflow: hidden;
}

/* 切换按钮，使其完全透明融入背景 */
.mode-btn {
  background: transparent !important; /* 杀掉白底 */
  border: none !important;            /* 杀掉边框 */
  outline: none;
  box-shadow: none !important;        /* 杀掉可能存在的阴影 */
  color: rgba(255, 255, 255, 0.6);    /* 默认半透明白色，不抢戏 */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border-radius: 50%;                 /* 悬浮时的底色限制为圆形 */
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 悬浮时的交互反馈 */
.mode-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1) !important; /* 悬浮时给一个极其微弱的透明白底 */
  transform: scale(1.15);
}

/* 左上角返回按钮 */
.close-btn {
  position: absolute;
  top: 20px;
  left: 24px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: var(--text-primary);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  transform: scale(1.08);
}

.lyric-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  padding: 40px 0;
}

/* ===== 左面板 ===== */
.left-panel {
  margin-left: 10%;
  width: 30%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  position: relative;
}

.cover-container {
  width: min(420px, 95%);
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.45),
    0 8px 24px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.05);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.track-info {
  text-align: center;
  width: min(420px, 95%);
  padding: 4px 0 0;
}

.track-title {
  font-size: 27px;
  font-weight: 700;
  letter-spacing: -0.01em;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-artist {
  font-size: 21px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 5px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

/* ===== 可拖动进度条 ===== */
.progress-area { width: min(420px, 95%); }

.progress-bar {
  position: relative;
  height: 28px;
  display: flex;
  align-items: center;
  cursor: pointer;
  touch-action: none;
}

.progress-bg {
  position: absolute;
  left: 0; right: 0;
  height: 5px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
  top: 50%;
  transform: translateY(-50%);
  transition: height 0.15s ease;
}

.progress-fill {
  position: absolute;
  left: 0;
  height: 5px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 3px;
  top: 50%;
  transform: translateY(-50%);
  transition: height 0.15s ease;
}

.progress-dot {
  position: absolute;
  top: 50%;
  width: 18px; height: 18px;
  border-radius: 50%;
  background: #fff;
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.15s ease;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
  z-index: 2;
}

/* hover 或拖动时放大 */
.progress-bar:hover .progress-dot,
.progress-bar:active .progress-dot {
  transform: translate(-50%, -50%) scale(1);
}

.progress-bar:hover .progress-bg,
.progress-bar:active .progress-bg,
.progress-bar:hover .progress-fill,
.progress-bar:active .progress-fill {
  height: 7px;
}

.progress-times {
  display: flex;
  justify-content: space-between;
  margin-top: 5px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  font-variant-numeric: tabular-nums;
}

/* ===== 控制按钮 ===== */
.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(420px, 95%);
}

.ctrl-side {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fn-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  padding: 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.fn-btn:hover {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.06);
}

.ctrl-main {
  display: flex;
  align-items: center;
  gap: 24px;
}

.ctrl-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.ctrl-btn:hover:not(:disabled) {
  color: #fff;
  transform: scale(1.1);
}

.ctrl-btn:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.play-btn {
  width: 72px; height: 72px;
  background: rgba(255, 255, 255, 0.9);
  color: #000 !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

.play-btn:hover {
  background: #fff !important;
  transform: scale(1.06) !important;
}

/* ===== Apple Music 水平音量条 ===== */
.volume-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: min(420px, 95%);
}

.vol-icon {
  color: rgba(255, 255, 255, 0.35);
  flex-shrink: 0;
}

.vol-track {
  flex: 1;
  height: 24px;
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
  touch-action: none;
}

.vol-fill {
  position: absolute;
  left: 0;
  height: 4px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 2px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.vol-track::before {
  content: '';
  position: absolute;
  left: 0; right: 0;
  height: 4px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  top: 50%;
  transform: translateY(-50%);
}

.vol-dot {
  position: absolute;
  top: 50%;
  width: 14px; height: 14px;
  border-radius: 50%;
  background: #fff;
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.15s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  z-index: 2;
}

.vol-track:hover .vol-dot,
.vol-track:active .vol-dot {
  transform: translate(-50%, -50%) scale(1);
}

/* 播放模式提示 */
.mode-tip {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 20px;
  white-space: nowrap;
  pointer-events: none;
}

.mode-tip-enter-active, .mode-tip-leave-active { transition: all 0.25s ease; }
.mode-tip-enter-from, .mode-tip-leave-to { opacity: 0; transform: translateX(-50%) translateY(6px); }

/* ===== 右面板 ===== */
.right-panel {
  position: absolute;
  left: 48%;
  width: 45%;
  top: 40px;
  bottom: 40px;
  overflow: hidden;
}

/* ===== 动画 ===== */
.lyric-view-enter-active { transition: all 0.55s cubic-bezier(0.16, 1, 0.3, 1); }
.lyric-view-leave-active { transition: all 0.35s cubic-bezier(0.4, 0, 1, 1); }
.lyric-view-enter-from { opacity: 0; transform: translateY(100%); }
.lyric-view-leave-to { opacity: 0; transform: translateY(50%); }

@media (max-width: 960px) {
  .lyric-content { flex-direction: column; padding: 24px; padding-top: 60px; align-items: center; }
  .left-panel { margin-left: 0; width: 100%; flex: 0; gap: 10px; }
  .right-panel { position: relative; left: auto; width: 100%; top: auto; bottom: auto; flex: 1; }
  .cover-container { width: 180px; }
  .track-info, .progress-area, .controls-row, .volume-row { width: 100%; max-width: 320px; }
}

@media (max-width: 768px) {
  .lyric-content { padding: 16px; padding-top: 56px; gap: 0; }
  .left-panel { gap: 8px; }
  .cover-container { width: 140px; }
  .track-title { font-size: 18px; }
  .track-artist { font-size: 14px; }
  .controls-row { max-width: 280px; }
  .play-btn { width: 52px; height: 52px; }
  .volume-row { max-width: 280px; }
  .close-btn { top: 12px; left: 12px; width: 36px; height: 36px; }
  .right-panel { min-height: 200px; }
  .lyric-original { font-size: 28px !important; }
  .lyric-translation { font-size: 16px !important; }
}


</style>
