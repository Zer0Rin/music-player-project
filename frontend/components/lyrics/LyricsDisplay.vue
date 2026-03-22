<template>
  <div class="lyrics-container" ref="containerRef">
    <Transition name="seek-bar-fade">
      <div
          v-if="userScrolling && focusedIndex !== -1"
          class="lyric-seek-bar"
          @click.stop="seekToFocusedLine"
      >
        <div class="seek-line"></div>
        <span class="seek-time">{{ formatTime(store.parsedLyrics[focusedIndex]?.time) }}</span>
        <div class="seek-play-icon">
          <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <path d="M8 5v14l11-7z"/>
          </svg>
        </div>
      </div>
    </Transition>

    <div v-if="!store.parsedLyrics.length" class="no-lyrics">
      <p>暂无歌词</p>
    </div>

    <div class="lyrics-scroll" ref="scrollRef" @scroll="handleScroll">
      <div class="lyrics-spacer-top" />

      <div
          v-for="(line, index) in store.parsedLyrics"
          :key="index"
          :ref="el => setLineRef(el, index)"
          :class="[
          'lyric-block',
          {
            'block-active': index === store.currentLyricIndex,
            'block-past': index < store.currentLyricIndex,
            'block-near': Math.abs(index - store.currentLyricIndex) === 1,
            'block-focused': index === focusedIndex && userScrolling,
          }
        ]"
          :style="getBlockStyle(index)"
      >
        <p class="lyric-original">
          <template v-if="store.lyricMode === 'word' && line.original.words">
            <span
                v-for="(w, wi) in line.original.words"
                :key="wi"
                class="word"
                :class="{
                'word-lit': index < store.currentLyricIndex || (index === store.currentLyricIndex && store.currentWordElapsed >= w.offset),
                'word-dim': index > store.currentLyricIndex || (index === store.currentLyricIndex && store.currentWordElapsed < w.offset),
              }"
            >{{ w.char }}</span>
          </template>
          <template v-else>
            <span :class="{ 'line-lit': index <= store.currentLyricIndex, 'line-dim': index > store.currentLyricIndex }">
              {{ line.original.text }}
            </span>
          </template>
        </p>

        <p v-if="line.translation" class="lyric-translation">
          <template v-if="store.lyricMode === 'word' && line.translation.words">
            <span v-for="(w, wi) in line.translation.words" :key="wi" class="word-t" :class="{ 'word-t-lit': index < store.currentLyricIndex || (index === store.currentLyricIndex && store.currentWordElapsed >= w.offset), 'word-t-dim': index > store.currentLyricIndex || (index === store.currentLyricIndex && store.currentWordElapsed < w.offset) }">{{ w.char }}</span>
          </template>
          <template v-else>
            <span :class="{ 'line-t-lit': index <= store.currentLyricIndex, 'line-t-dim': index > store.currentLyricIndex }">{{ line.translation.text }}</span>
          </template>
        </p>
      </div>

      <div class="lyrics-spacer-bottom" />
    </div>

    <div class="fade-top" />
    <div class="fade-bottom" />
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'

const store = usePlayerStore()
const emit = defineEmits(['seek'])

const containerRef = ref(null)
const scrollRef = ref(null)
const lineRefs = ref({})
const userScrolling = ref(false)
const focusedIndex = ref(-1)
let userScrollTimer = null

// 区分程序滚动和用户滚动
let isProgrammaticScroll = false

function setLineRef(el, index) { if (el) lineRefs.value[index] = el }

function handleScroll() {
  if (isProgrammaticScroll) {
    isProgrammaticScroll = false
    return
  }

  userScrolling.value = true
  clearTimeout(userScrollTimer)
  userScrollTimer = setTimeout(() => {
    userScrolling.value = false
    focusedIndex.value = -1
  }, 1500)

  if (!scrollRef.value) return
  const scrollTop = scrollRef.value.scrollTop
  const containerHeight = scrollRef.value.clientHeight

  const focusY = scrollTop + containerHeight * 0.40

  let closestIndex = 0
  let minDiff = Infinity
  Object.keys(lineRefs.value).forEach(index => {
    const el = lineRefs.value[index]
    const diff = Math.abs(el.offsetTop - focusY)
    if (diff < minDiff) { minDiff = diff; closestIndex = parseInt(index); }
  })
  focusedIndex.value = closestIndex
}

watch(() => store.currentLyricIndex, (newIndex) => {
  if (newIndex < 0 || userScrolling.value) return
  nextTick(() => {
    const el = lineRefs.value[newIndex]
    const container = scrollRef.value
    if (!el || !container) return

    isProgrammaticScroll = true

    container.scrollTo({
      top: el.offsetTop - container.clientHeight * 0.40,
      behavior: 'smooth',
    })
  })
}, { immediate: true })

function seekToFocusedLine() {
  if (focusedIndex.value !== -1) {
    emit('seek', store.parsedLyrics[focusedIndex.value].time)
  }
}

function formatTime(s) {
  if (!s || isNaN(s)) return '0:00'
  return Math.floor(s / 60) + ':' + Math.floor(s % 60).toString().padStart(2, '0')
}

function getBlockStyle(index) {
  const distance = Math.abs(index - store.currentLyricIndex)
  if (distance === 0) return { opacity: 1, filter: 'blur(0px)' }
  if (distance === 1) return { opacity: 0.45, filter: 'blur(0.3px)' }
  return { opacity: 0.1, filter: 'blur(1.5px)' }
}
</script>

<style scoped>
/* ==========================================
   1. 🎯 准星条样式 (结合你的 45% 定位与右侧排版)
   ========================================== */
.lyric-seek-bar {
  position: absolute;
  left: 0; right: 0;
  top: 47.5%;
  transform: translateY(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  padding: 0 7.5% 0 0;
  justify-content: flex-end;
  height: 40px;
  background: transparent;
  cursor: pointer;
  pointer-events: auto;
}

.seek-time {
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.6);
  margin-right: 8px;
}
.seek-line {
  width: 80px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.25) 30%, rgba(255,255,255,0.25) 70%, transparent);
  margin-right: 12px;
}
.seek-play-icon {
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.2s ease;
}
.lyric-seek-bar:hover .seek-play-icon {
  color: #fff;
  transform: scale(1.3);
}

.seek-bar-fade-enter-active, .seek-bar-fade-leave-active { transition: all 0.3s ease; }
.seek-bar-fade-enter-from, .seek-bar-fade-leave-to { opacity: 0; transform: translateY(-50%) scaleX(0.98); }


/* ==========================================
   2. 🎵 基础样式 (逐字逐行颜色)
   ========================================== */
.line-lit { color: #ffffff; }
.line-dim { color: rgba(255, 255, 255, 0.3); }
.line-t-lit { color: rgba(255, 255, 255, 0.65); }
.line-t-dim { color: rgba(255, 255, 255, 0.18); }
.word { display: inline; transition: none; }
.word-lit { color: #ffffff; }
.word-dim { color: rgba(255, 255, 255, 0.3); }
.block-past .word { color: rgba(255, 255, 255, 0.9); }
.word-t { display: inline; transition: none; }
.word-t-lit { color: rgba(255, 255, 255, 0.65); }
.word-t-dim { color: rgba(255, 255, 255, 0.18); }
.block-past .word-t { color: rgba(255, 255, 255, 0.45); }

/* ==========================================
   3. 📦 容器与排版 (PC端大字号，沉浸感拉满)
   ========================================== */
.lyrics-container { width: 100%; height: 100%; position: relative; overflow: hidden; }

.lyrics-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 0 48px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.lyrics-scroll::-webkit-scrollbar { display: none; }

/* 💡 首尾留白与 45% 基准线强绑定 */
.lyrics-spacer-top { height: 50vh; }
.lyrics-spacer-bottom { height: 50vh; }

.no-lyrics {
  height: 100%; display: flex; align-items: center; justify-content: center;
  color: var(--text-tertiary); font-size: 16px;
}

.lyric-block {
  padding: 16px 0; /* PC端加宽行距 */
  cursor: pointer;
  user-select: none;
  transform-origin: left center;
  transition: opacity 0.5s ease, filter 0.5s ease, transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 💡 PC端专属小细节：未激活歌词轻微缩小，焦点更集中 */
.lyric-block:not(.block-active) { transform: scale(0.96); }
.block-active { transform: scale(1); }

/* 当被准星指着时强行清晰 */
.block-focused { opacity: 1 !important; filter: blur(0) !important; transform: scale(1) !important; }

/* 💡 PC端大号字体，解决拉伸空洞感 */
.lyric-original {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.35;
  margin: 0;
  letter-spacing: -0.02em;
}
.lyric-translation {
  font-size: 24px;
  font-weight: 500;
  line-height: 1.4;
  margin: 8px 0 0;
}

/* 遮罩（透明渐变） */
.fade-top, .fade-bottom {
  position: absolute; left: 0; right: 0; height: 100px;
  pointer-events: none; z-index: 2; opacity: 0;
}
.fade-top { top: 0; }
.fade-bottom { bottom: 0; }

/* ==========================================
   4. 📱 移动端适配 (窄屏紧凑模式)
   ========================================== */
@media (max-width: 768px) {
  .lyric-block {
    padding: 12px 0;
    transform: none !important; /* 移动端去掉缩放避免字太小 */
  }
  .lyric-original { font-size: 28px; }
  .lyric-translation { font-size: 16px; margin-top: 4px; }
  .lyrics-scroll { padding: 0 24px; }
}
</style>