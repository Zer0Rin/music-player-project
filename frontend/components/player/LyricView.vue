<template>
  <Transition name="lyric-view">
    <div v-if="store.showLyricView" :key="lyricViewKey" class="lyric-overlay">
      <div ref="bgRef" class="amll-bg-container" />

      <button class="close-btn" @click="close" title="关闭 (Esc)">
        <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
          <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/>
        </svg>
      </button>

      <div class="lyric-content">
        <div class="left-panel">

          <!-- 封面区域：移动端点击翻到歌词 -->
          <div
              class="cover-wrapper"
              :class="{ 'cover-hidden': isMobile && isFlipped }"
              @click="flipToLyrics"
          >
            <div class="cover-container">
              <img
                  v-if="store.currentSong"
                  :src="coverUrl(store.currentSong.id)"
                  class="cover-img"
                  @error="e => e.target.style.visibility = 'hidden'"
              />
            </div>
          </div>

          <AudioVisualizer
              :width="420"
              :height="56"
              class="visualizer"
              v-show="!isMobile || !isFlipped"
          />

          <div class="bottom-controls-wrap">
            <div class="track-info" v-if="store.currentSong">
              <div class="track-meta">
                <div class="track-title">{{ store.currentSong.title }}</div>
                <div class="track-artist">{{ store.currentSong.artist }}</div>
              </div>

              <div class="track-actions">
                <button
                    class="track-fav-btn"
                    :class="{ 'fav-active': plStore.isFavorite(store.currentSong.id) }"
                    @click="plStore.toggleFavorite(store.currentSong.id)"
                >
                  <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                  </svg>
                </button>

                <button class="track-comment-btn" @click="showComment = true">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                    <path d="M21 6.5A2.5 2.5 0 0 0 18.5 4h-13A2.5 2.5 0 0 0 3 6.5v8A2.5 2.5 0 0 0 5.5 17H7v3l4-3h7.5a2.5 2.5 0 0 0 2.5-2.5v-8z"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="progress-area">
              <div class="lyric-progress-bar" ref="progRef" @mousedown="onProgDown" @touchstart.prevent="onProgTouchStart">
                <div class="lyric-progress-bg" />
                <div class="lyric-progress-fill" :style="{ width: displayProgress + '%' }" />
                <div class="lyric-progress-dot" :style="{ left: displayProgress + '%' }" />
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
                <button class="fn-btn" title="歌曲信息" @click="showSongInfo = true">
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

        <!-- 歌词容器：PC 端固定在右侧，移动端绝对定位覆盖整个 left-panel -->
        <!-- 始终渲染，不用 v-if，AMLL 只初始化一次，touch 事件永远有效 -->
        <div
            class="lyrics-panel"
            :class="{ 'lyrics-panel--visible': !isMobile || isFlipped }"
        >
          <div ref="lyricsRef" class="amll-lyrics-container" />
        </div>
      </div>

      <!-- 歌曲详情小窗 -->
      <Transition name="info-pop">
        <div v-if="showSongInfo" class="song-info-popup" @click.self="showSongInfo = false">
          <div class="song-info-panel">
            <div class="info-header">
              <span class="info-title">歌曲信息</span>
              <button class="info-close" @click="showSongInfo = false">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
              </button>
            </div>
            <div class="info-cover">
              <img v-if="store.currentSong" :src="coverUrl(store.currentSong.id)" class="info-cover-img" />
            </div>
            <div class="info-rows" v-if="store.currentSong">
              <div class="info-row"><span class="info-label">标题</span><span class="info-value">{{ store.currentSong.title || '—' }}</span></div>
              <div class="info-row"><span class="info-label">艺术家</span><span class="info-value">{{ store.currentSong.artist || '—' }}</span></div>
              <div class="info-row"><span class="info-label">专辑</span><span class="info-value">{{ store.currentSong.album || '—' }}</span></div>
              <div class="info-row"><span class="info-label">流派</span><span class="info-value">{{ store.currentSong.genre || '—' }}</span></div>
              <div class="info-row"><span class="info-label">年份</span><span class="info-value">{{ store.currentSong.year || '—' }}</span></div>
              <div class="info-row"><span class="info-label">时长</span><span class="info-value">{{ store.formattedDuration }}</span></div>
              <div class="info-row">
                <span class="info-label">下载</span>
                <button class="download-song-btn" @click="downloadCurrentSong">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                  下载此歌曲
                </button>
              </div>
              <div class="info-row">
                <span class="info-label">AI 解析</span>
                <button class="download-song-btn ai-analysis-btn" @click="openAiAnalysis">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm1 14.93V15a1 1 0 0 0-2 0v1.93A8 8 0 0 1 4.07 11H6a1 1 0 0 0 0-2H4.07A8 8 0 0 1 11 4.07V6a1 1 0 0 0 2 0V4.07A8 8 0 0 1 19.93 11H18a1 1 0 0 0 0 2h1.93A8 8 0 0 1 13 16.93z"/></svg>
                  AI 歌曲解析
                </button>
              </div>
              <div class="info-row">
                <span class="info-label">定时器</span>
                <button class="download-song-btn ai-analysis-btn" @click="openSleepTimer">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                  睡眠定时器
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <AiSongAnalysis
          :visible="showAiAnalysis"
          :song-id="store.currentSong?.id"
          @close="showAiAnalysis = false"
      />

      <SleepTimer
          :visible="showSleepTimer"
          @close="showSleepTimer = false"
      />

    </div>
  </Transition>
  <CommentPanel :visible="showComment" @close="showComment = false" />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import AudioVisualizer from './AudioVisualizer.vue'
import CommentPanel from '~/components/player/CommentPanel.vue'
import AiSongAnalysis from '~/components/player/AiSongAnalysis.vue'
import SleepTimer from '~/components/player/SleepTimer.vue'
import { useAMLLEngine } from '~/composables/useAMLLEngine'
import { useAMLLBackground } from '~/composables/useAMLLBackground'

const showSleepTimer = ref(false)
function openSleepTimer() {
  showSongInfo.value = false
  setTimeout(() => { showSleepTimer.value = true }, 300)
}

const store = usePlayerStore()
const plStore = usePlaylistStore()
const { coverUrl } = useCoverUrl()
const emit = defineEmits(['seek', 'volume'])

const { initPlayer, seekTo, dispose: disposeEngine } = useAMLLEngine()
const { initRenderer, dispose: disposeBg } = useAMLLBackground()

const lyricsRef = ref(null)
const bgRef = ref(null)

// lyricsRef 始终渲染，watch 到 el 时初始化一次即可
watch(lyricsRef, (el) => { if (el) initPlayer(el) })
watch(bgRef, (el) => { if (el) initRenderer(el) })

const isMobile = ref(false)
const isFlipped = ref(false)
const showComment = ref(false)
const showSongInfo = ref(false)
const showAiAnalysis = ref(false)

// 移动端点封面 → 切到歌词视图
function flipToLyrics() {
  if (isMobile.value && !isFlipped.value) {
    isFlipped.value = true
  }
}

// 移动端歌词视图 → 切回封面（close-btn 长按或其他手势可触发）
function flipToCover() {
  if (isMobile.value) isFlipped.value = false
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) isFlipped.value = false
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
  disposeEngine()
  disposeBg()
})

// 进度条拖动
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

// 音量拖动
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
function onSeek(time) { emit('seek', time); seekTo(time) }
function formatTime(s) { if (!s || isNaN(s)) return '0:00'; return Math.floor(s / 60) + ':' + Math.floor(s % 60).toString().padStart(2, '0') }

const { downloadSong } = useDownload()
function downloadCurrentSong() { downloadSong(store.currentSong); showSongInfo.value = false }

function openAiAnalysis() {
  showSongInfo.value = false
  setTimeout(() => { showAiAnalysis.value = true }, 300)
}

const lyricViewKey = ref(0)
watch(() => store.showLyricView, (val) => {
  if (val) {
    lyricViewKey.value++
  } else {
    disposeEngine()
    disposeBg()
  }
})
</script>

<style scoped>
.lyric-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: #000;
  overflow: hidden;
  color-scheme: dark;
  color: #ffffff;
  --text-primary: #ffffff !important;
  --text-secondary: rgba(255, 255, 255, 0.7) !important;
  --text-tertiary: rgba(255, 255, 255, 0.4) !important;
  --accent: #fa2d48 !important;
}

.mode-btn { background: transparent !important; border: none !important; outline: none; box-shadow: none !important; color: rgba(255, 255, 255, 0.6); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 8px; border-radius: 50%; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.mode-btn:hover { color: #ffffff; background: rgba(255, 255, 255, 0.1) !important; transform: scale(1.15); }
.close-btn { position: absolute; top: 20px; left: 24px; z-index: 50; background: transparent; border: none; color: rgba(255,255,255,0.7); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all var(--transition-fast); }
.close-btn:hover { color: #fff; transform: translateY(4px); }

.lyric-content {
  box-sizing: border-box;
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  padding: 40px 0;
  overflow: hidden;
}

/* ── PC 端左侧 ── */
.left-panel {
  margin-left: 10%;
  width: 30%;
  min-width: 0;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 2vh;
  box-sizing: border-box;
  height: 100%;
  gap: 0;
  position: relative;
}

/* cover-wrapper: PC端占正方形空间，移动端可缩小 */
.cover-wrapper {
  width: min(420px, 95%, 38vh);
  aspect-ratio: 1 / 1;
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
  margin-bottom: auto;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.cover-container {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2), 0 8px 24px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover !important;
  display: block;
}

.bottom-controls-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.track-info { text-align: center; width: min(420px, 95%); padding: 4px 0 0; }
.track-title { font-size: 27px; font-weight: 700; letter-spacing: -0.01em; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.track-artist { font-size: 21px; color: rgba(255, 255, 255, 0.5); margin-top: 5px; text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3); }

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

/* ── 歌词面板（PC + 移动端统一） ── */
.lyrics-panel {
  /* PC端：右侧固定区域 */
  position: absolute;
  left: 48%;
  width: 45%;
  top: 40px;
  bottom: 40px;
  overflow: hidden;
  /* 默认对 PC 端可见，移动端靠 class 控制 */
  opacity: 1;
  pointer-events: auto;
  transition: opacity 0.35s ease;
}

/* 移动端：默认隐藏，isFlipped 时显示 */
/* 通过 JS class 控制，不依赖 media query，避免和 PC 端冲突 */
.lyrics-panel:not(.lyrics-panel--visible) {
  /* 只在移动端生效的隐藏状态由 JS 控制 class 决定 */
}

/* ── PC 端歌曲信息布局 ── */
.track-info {
  position: relative;
  display: flex !important;
  justify-content: center;
  align-items: center;
  width: min(420px, 95%);
  margin: 0 auto;
  min-height: 70px;
}
.track-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  box-sizing: border-box;
}
.track-title, .track-artist { width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.track-title { padding: 0 16px; box-sizing: border-box; font-size: 27px; font-weight: 700; letter-spacing: -0.01em; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4); }
.track-artist { padding: 0 90px; box-sizing: border-box; font-size: 21px; color: rgba(255, 255, 255, 0.5); margin-top: 5px; text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3); }
.track-actions { position: absolute; right: 0; top: auto; bottom: -6px; height: auto; display: flex; align-items: center; gap: 8px; }
.track-fav-btn, .track-comment-btn { background: none; border: none; color: rgba(255, 255, 255, 0.4); cursor: pointer; padding: 8px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.track-fav-btn:hover, .track-comment-btn:hover { color: #ffffff; background: rgba(255, 255, 255, 0.1); transform: scale(1.15) translateY(-2px); }
.track-fav-btn.fav-active { color: var(--accent); filter: drop-shadow(0 0 8px rgba(250, 45, 72, 0.5)); }

/* ── 移动端 ── */
@media (max-width: 768px) {
  .lyric-content {
    box-sizing: border-box;
    padding: 50px 24px 24px;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    height: 100dvh;
    overflow: hidden;
  }

  .left-panel {
    margin-left: 0; width: 100%; height: 100%; flex: 1; gap: 0;
    justify-content: flex-start; display: flex; flex-direction: column;
  }

  /* 封面wrapper：移动端占据 flex 空间，切换到歌词时缩到左上角 */
  .cover-wrapper {
    width: 100%;
    aspect-ratio: unset;
    flex: 1;
    min-height: 0;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
  }

  .cover-container {
    position: relative;
    inset: unset;
    width: min(85vw, 45dvh, 360px);
    height: min(85vw, 45dvh, 360px);
    border-radius: 12px;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2), 0 6px 16px rgba(0, 0, 0, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.08);
    flex-shrink: 0;
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }

  /* 封面隐藏态：缩到左上角小图 */
  .cover-wrapper.cover-hidden {
    flex: 0;
    min-height: 0;
    margin-bottom: 0;
    width: 100%;
    height: 0;
    overflow: hidden;
    opacity: 0;
    pointer-events: none;
  }

  /* 歌词面板：移动端绝对定位覆盖 left-panel 的主体区域 */
  .lyrics-panel {
    position: absolute;
    /* 从 left-panel 的顶部到底部控制区上方 */
    top: 50px;   /* 与 lyric-content 的 padding-top 对齐 */
    left: 0;
    right: 0;
    bottom: 0;
    width: auto;
    /* 遮罩渐变边缘 */
    mask-image: linear-gradient(to bottom, transparent, black 8%, black 92%, transparent);
    -webkit-mask-image: linear-gradient(to bottom, transparent, black 8%, black 92%, transparent);
    /* 默认隐藏 */
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.35s ease;
    z-index: 5;
  }

  /* 歌词显示态 */
  .lyrics-panel.lyrics-panel--visible {
    opacity: 1;
    pointer-events: auto;
  }

  /* 底部控制区在歌词态时仍然可见 */
  .bottom-controls-wrap {
    width: 100%; gap: 12px; margin-top: auto; flex-shrink: 0;
    position: relative;
    z-index: 6;
  }

  .track-info {
    display: flex !important;
    text-align: left;
    width: 100%; max-width: none; padding: 0;
    position: relative;
    flex-direction: column !important;
    justify-content: center;
    align-items: flex-start !important;
  }
  .track-meta { text-align: left; padding: 0 !important; align-items: flex-start !important; }
  .track-title { font-size: 22px; padding: 0 16px 0 0 !important; text-align: left !important; }
  .track-artist { font-size: 16px; margin-top: 2px; padding: 0 90px 0 0 !important; text-align: left !important; }
  .track-actions { position: absolute; right: -8px; top: auto; bottom: -8px; transform: none; justify-content: flex-end; align-items: center; gap: 10px; margin-top: 0; }
  .track-fav-btn, .track-comment-btn { padding: 6px; position: static; transform: none; }
  .track-fav-btn:hover, .track-comment-btn:hover { transform: scale(1.15); }

  .progress-area, .controls-row, .volume-row { width: 100%; max-width: none; }
  .play-btn { width: 56px; height: 56px; }
  .ctrl-main { gap: 24px; }

  .visualizer {
    display: flex; align-items: center; justify-content: center; width: 100%;
    height: 36px; margin: 8px 0 16px; flex-shrink: 0; opacity: 0.8;
  }
  .visualizer :deep(canvas) { max-width: 80%; height: 100% !important; }

  .close-btn {
    top: 12px; left: 50%; transform: translateX(-50%);
    width: 44px; height: 16px; background: transparent; backdrop-filter: none;
  }
  .close-btn svg { display: none; }
  .close-btn::after {
    content: ''; display: block; width: 36px; height: 5px;
    background: rgba(255,255,255,0.4); border-radius: 3px;
  }
}

/* ── 歌曲信息弹窗 ── */
.song-info-popup { position: absolute; inset: 0; z-index: 100; display: flex; align-items: flex-end; justify-content: center; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); }
.song-info-panel { width: 100%; max-width: 480px; background: rgba(20,20,20,0.95); border-radius: 20px 20px 0 0; padding: 20px 24px 40px; display: flex; flex-direction: column; gap: 16px; max-height: 80vh; overflow-y: auto; }
.info-header { display: flex; align-items: center; justify-content: space-between; }
.info-title { font-size: 15px; font-weight: 700; color: white; }
.info-close { background: rgba(255,255,255,0.1); border: none; color: rgba(255,255,255,0.6); width: 28px; height: 28px; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.info-close:hover { background: rgba(255,255,255,0.2); color: white; }
.info-cover { width: 80px; height: 80px; border-radius: 10px; overflow: hidden; margin: 0 auto; }
.info-cover-img { width: 100%; height: 100%; object-fit: cover; }
.info-rows { display: flex; flex-direction: column; gap: 0; }
.info-row { display: flex; align-items: center; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); gap: 16px; }
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 13px; color: rgba(255,255,255,0.4); flex-shrink: 0; }
.info-value { font-size: 13px; color: white; text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 240px; }
.info-pop-enter-active, .info-pop-leave-active { transition: all 0.3s ease; }
.info-pop-enter-from, .info-pop-leave-to { opacity: 0; transform: translateY(20px); }

.download-song-btn { display: flex; align-items: center; gap: 6px; padding: 5px 12px; border-radius: 8px; border: none; background: rgba(9, 0, 0, 0.76); color: #8bdcfa; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.download-song-btn:hover { background: rgba(92, 141, 246, 0.65); }
.ai-analysis-btn { background: rgba(139, 92, 246, 0.15); color: #c4b5fd; }
.ai-analysis-btn:hover { background: rgba(139, 92, 246, 0.35); }

/* AMLL 容器 */
.amll-bg-container { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.amll-lyrics-container { width: 100%; height: 100%; overflow: hidden; }
</style>

<style>
.lyric-progress-bar { position: relative; height: 28px; display: flex; align-items: center; cursor: pointer; touch-action: none; }
.lyric-progress-bg { position: absolute; left: 0; right: 0; height: 5px; background: rgba(255, 255, 255, 0.15); border-radius: 3px; top: 50%; transform: translateY(-50%); transition: height 0.15s ease; }
.lyric-progress-fill { position: absolute; left: 0; height: 5px; background: rgba(255, 255, 255, 0.85); border-radius: 3px; top: 50%; transform: translateY(-50%); transition: height 0.15s ease; }
.lyric-progress-dot { position: absolute; top: 50%; width: 18px; height: 18px; border-radius: 50%; background: #ffffff; transform: translate(-50%, -50%) scale(0); transition: transform 0.15s ease; box-shadow: 0 1px 6px rgba(0, 0, 0, 0.35); z-index: 2; }
.lyric-progress-bar:hover .lyric-progress-dot, .lyric-progress-bar:active .lyric-progress-dot { transform: translate(-50%, -50%) scale(1); }
.lyric-progress-bar:hover .lyric-progress-bg, .lyric-progress-bar:active .lyric-progress-bg,
.lyric-progress-bar:hover .lyric-progress-fill, .lyric-progress-bar:active .lyric-progress-fill { height: 7px; }
</style>