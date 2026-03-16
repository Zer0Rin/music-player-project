<template>
  <div class="lyrics-container" ref="containerRef">
    <div v-if="!store.parsedLyrics.length" class="no-lyrics">
      <p>暂无歌词</p>
    </div>

    <div v-else class="lyrics-scroll" ref="scrollRef">
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
            'block-far': Math.abs(index - store.currentLyricIndex) > 1,
          }
        ]"
        :style="getBlockStyle(index)"
        @click="onClickLine(line.time)"
      >
        <!-- 原文行：始终用逐字渲染（generateAutoWords 保证 words 一定存在） -->
        <p class="lyric-original">
          <!-- 逐字模式 -->
          <template v-if="store.lyricMode === 'word' && line.original.words">
    <span
        v-for="(w, wi) in line.original.words"
        :key="wi"
        class="word"
        :class="{
        'word-lit': index < store.currentLyricIndex
          || (index === store.currentLyricIndex && store.currentWordElapsed >= w.offset),
        'word-dim': index > store.currentLyricIndex
          || (index === store.currentLyricIndex && store.currentWordElapsed < w.offset),
      }"
        :style="{}"
    >{{ w.char }}</span>
          </template>
          <!-- 逐行模式 -->
          <template v-else>
    <span :class="{ 'line-lit': index <= store.currentLyricIndex, 'line-dim': index > store.currentLyricIndex }">
      {{ line.original.text }}
    </span>
          </template>
        </p>

        <!-- 翻译行 -->
        <p v-if="line.translation" class="lyric-translation">
          <template v-if="store.lyricMode === 'word' && line.translation.words">
    <span
        v-for="(w, wi) in line.translation.words"
        :key="wi"
        class="word-t"
        :class="{
        'word-t-lit': index < store.currentLyricIndex
          || (index === store.currentLyricIndex && store.currentWordElapsed >= w.offset),
        'word-t-dim': index > store.currentLyricIndex
          || (index === store.currentLyricIndex && store.currentWordElapsed < w.offset),
      }"
        :style="{}"
    >{{ w.char }}</span>
          </template>
          <template v-else>
    <span :class="{ 'line-t-lit': index <= store.currentLyricIndex, 'line-t-dim': index > store.currentLyricIndex }">
      {{ line.translation.text }}
    </span>
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
const store = usePlayerStore()
const emit = defineEmits(['seek'])

const containerRef = ref(null)
const scrollRef = ref(null)
const lineRefs = ref({})
const userScrolling = ref(false)
let userScrollTimer = null

function setLineRef(el, index) {
  if (el) lineRefs.value[index] = el
}

function onClickLine(time) {
  emit('seek', time)
}

/** 逐字过渡样式 */
function getWordStyle(word, wordIndex, words) {
  if (!words || wordIndex >= words.length - 1) {
    return { transitionDuration: '0.03s' }
  }
  const gap = words[wordIndex + 1].offset - word.offset
  const dur = Math.max(0.02, Math.min(0.15, gap * 0.04))
  return { transitionDuration: dur + 's' }
}

/** 行级视觉样式 */
function getBlockStyle(index) {
  const distance = Math.abs(index - store.currentLyricIndex)
  if (distance === 0) return { opacity: 1, filter: 'blur(0px)' }
  if (distance === 1) return { opacity: 0.45, filter: 'blur(0.3px)' }
  if (distance === 2) return { opacity: 0.3, filter: 'blur(0.6px)' }
  if (distance <= 4) return { opacity: 0.18, filter: 'blur(1px)' }
  return { opacity: 0.1, filter: 'blur(1.5px)' }
}

/** 自动滚动 */
watch(() => store.currentLyricIndex, (newIndex) => {
  if (newIndex < 0 || userScrolling.value) return
  nextTick(() => {
    const el = lineRefs.value[newIndex]
    const container = scrollRef.value
    if (!el || !container) return
    const containerHeight = container.clientHeight
    container.scrollTo({
      top: Math.max(0, el.offsetTop - containerHeight * 0.38),
      behavior: 'smooth',
    })
  })
}, { immediate: true })

onMounted(() => {
  const container = scrollRef.value
  if (!container) return
  container.addEventListener('wheel', onUserScroll, { passive: true })
  container.addEventListener('touchmove', onUserScroll, { passive: true })
})

function onUserScroll() {
  userScrolling.value = true
  clearTimeout(userScrollTimer)
  userScrollTimer = setTimeout(() => { userScrolling.value = false }, 3000)
}

onUnmounted(() => { clearTimeout(userScrollTimer) })
</script>

<style scoped>

/* 逐行模式 */
.line-lit {
  color: #ffffff;
}

.line-dim {
  color: rgba(255, 255, 255, 0.3);
}

.line-t-lit {
  color: rgba(255, 255, 255, 0.65);
}

.line-t-dim {
  color: rgba(255, 255, 255, 0.18);
}



.lyrics-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.lyrics-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 0 48px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.lyrics-scroll::-webkit-scrollbar { display: none; }

.lyrics-spacer-top { height: 38vh; }
.lyrics-spacer-bottom { height: 55vh; }

.no-lyrics {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 16px;
}

/* ===== 歌词块 ===== */
.lyric-block {
  padding: 12px 0;
  cursor: pointer;
  user-select: none;
  transition:
    opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    filter 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.lyric-original {
  font-size: 45px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0;
}

.lyric-translation {
  font-size: 24px;
  font-weight: 400;
  line-height: 1.4;
  margin: 6px 0 0;
}

/* ===== 逐字：原文 ===== */
.word {
  display: inline;
  transition: none;
}

/* 已亮的字 */
.word-lit {
  color: #ffffff;
}

/* 未亮的字 */
.word-dim {
  color: rgba(255, 255, 255, 0.3);
}

/* 已过行的所有字都亮 */
.block-past .word {
  color: rgba(255, 255, 255, 0.9);
}

/* ===== 逐字：翻译 ===== */
.word-t {
  display: inline;
  transition: none;
}

.word-t-lit {
  color: rgba(255, 255, 255, 0.65);
}

.word-t-dim {
  color: rgba(255, 255, 255, 0.18);
}

.block-past .word-t {
  color: rgba(255, 255, 255, 0.45);
}

/* 悬停 */
.lyric-block:hover {
  opacity: 0.7 !important;
  filter: blur(0px) !important;
}

/* 遮罩（透明） */
.fade-top, .fade-bottom {
  position: absolute;
  left: 0; right: 0;
  height: 100px;
  pointer-events: none;
  z-index: 2;
  opacity: 0;
}
.fade-top { top: 0; }
.fade-bottom { bottom: 0; }

@media (max-width: 768px) {
  .lyric-original { font-size: 28px; }
  .lyric-translation { font-size: 16px; }
  .lyrics-scroll { padding: 0 24px; }
}
</style>
