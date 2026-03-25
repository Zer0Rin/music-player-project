<template>
  <Transition name="info-pop">
    <div v-if="visible" class="song-info-popup" @click.self="$emit('close')">
      <div class="song-info-panel">

        <!-- 头部 -->
        <div class="info-header">
          <span class="info-title">🤖 AI 歌曲解析</span>
          <div class="header-actions">
            <button
                class="info-close refresh-btn"
                :disabled="isStreaming"
                @click="startAnalysis"
                title="重新解析"
            >
              <svg
                  viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2.5" width="14" height="14"
                  :style="isStreaming ? 'animation: spin 1s linear infinite' : ''"
              >
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9
                     m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
            </button>
            <button class="info-close" @click="$emit('close')">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12
                         5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 内容区 -->
        <div class="analysis-content" ref="contentRef">
          <!-- 空状态 -->
          <div v-if="!content && !isStreaming" class="analysis-empty">
            <div class="empty-icon">🎵</div>
            <p>点击右上角刷新按钮开始解析</p>
          </div>

          <!-- Markdown 内容 -->
          <div v-else class="analysis-markdown" v-html="renderedContent" />

          <!-- 打字机光标 -->
          <span v-if="isStreaming" class="type-cursor">▋</span>
        </div>

      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  visible: { type: Boolean, default: false },
  songId: { type: String, default: null }
})
defineEmits(['close'])

const content = ref('')
const isStreaming = ref(false)
const contentRef = ref(null)
let eventSource = null

// Markdown 渲染（带打字机光标占位）
const renderedContent = computed(() => marked.parse(content.value || ''))

// 自动滚动到底部
watch(content, () => {
  if (contentRef.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight
  }
})

// 面板打开时自动开始
watch(() => props.visible, (val) => {
  console.log('AiSongAnalysis visible changed:', val, 'songId:', props.songId)
  if (val && props.songId && !content.value) {
    startAnalysis()
  }
})

// 切换歌曲时重置
watch(() => props.songId, () => {
  content.value = ''
  isStreaming.value = false
  closeEventSource()
})

function closeEventSource() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function startAnalysis() {
  console.log('startAnalysis called, songId:', props.songId, 'isStreaming:', isStreaming.value)
  if (isStreaming.value || !props.songId) return
  closeEventSource()
  content.value = ''
  isStreaming.value = true

  const token = localStorage.getItem('token')
  const url = `/api/ai/analysis/${props.songId}${token ? `?token=${encodeURIComponent(token)}` : ''}`

  eventSource = new EventSource(url)

  eventSource.onmessage = (e) => {
    content.value += e.data
  }

  eventSource.onerror = () => {
    isStreaming.value = false
    closeEventSource()
  }
}

onUnmounted(closeEventSource)
</script>

<style scoped>
/* 复用 LyricView 的弹窗体系 */
.song-info-popup {
  position: absolute;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.song-info-panel {
  width: 100%;
  max-width: 480px;
  background: rgba(20, 20, 20, 0.95);
  border-radius: 20px 20px 0 0;
  padding: 20px 24px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 75vh;
  min-height: 0;
  overflow: hidden;        /* 防止 padding 区域溢出 */
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.info-title {
  font-size: 15px;
  font-weight: 700;
  color: white;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.info-close {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: rgba(255, 255, 255, 0.6);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.info-close:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}
.info-close:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.refresh-btn { /* 继承 info-close */ }

/* 内容区 */
.analysis-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;

}

.analysis-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
}
.empty-icon { font-size: 36px; }
.analysis-empty p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  text-align: center;
}

/* 打字机光标 */
.type-cursor {
  display: inline-block;
  color: rgba(255, 255, 255, 0.6);
  animation: blink 1s step-end infinite;
  margin-left: 1px;
  font-size: 14px;
}
@keyframes blink { 50% { opacity: 0; } }

/* 旋转动画（刷新按钮） */
@keyframes spin { to { transform: rotate(360deg); } }

/* Markdown 样式 */
.analysis-markdown :deep(h2) {
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  margin: 20px 0 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  letter-spacing: 0.02em;
}
.analysis-markdown :deep(h2:first-child) {
  margin-top: 4px;
}
.analysis-markdown :deep(p) {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.75;
  margin-bottom: 6px;
}
.analysis-markdown :deep(blockquote) {
  border-left: 2px solid rgba(255, 255, 255, 0.2);
  margin: 8px 0;
  padding: 4px 12px;
  color: rgba(255, 255, 255, 0.5);
  font-style: italic;
  font-size: 13px;
}

/* 动画（复用 LyricView 的 info-pop） */
.info-pop-enter-active,
.info-pop-leave-active { transition: all 0.3s ease; }
.info-pop-enter-from,
.info-pop-leave-to { opacity: 0; transform: translateY(20px); }
</style>