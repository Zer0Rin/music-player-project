<template>
  <Transition name="info-pop">
    <div v-if="visible" class="song-info-popup" @click.self="$emit('close')">
      <div class="song-info-panel">

        <div class="info-header">
          <span class="info-title">🌙 睡眠定时器</span>
          <button class="info-close" @click="$emit('close')">
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>

        <!-- 倒计时显示 -->
        <div v-if="isActive" class="timer-active">
          <div class="timer-countdown">{{ formattedRemaining }}</div>
          <p class="timer-desc">后停止播放</p>
          <button class="timer-cancel-btn" @click="cancelTimer">取消定时</button>
        </div>

        <!-- 选择时间 -->
        <div v-else class="timer-options">
          <div class="timer-grid">
            <button
                v-for="opt in options"
                :key="opt.value"
                class="timer-option-btn"
                @click="startTimer(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>

          <!-- 自定义时间 -->
          <div class="timer-custom">
            <input
                v-model.number="customMinutes"
                type="number"
                min="1"
                max="999"
                placeholder="自定义分钟"
                class="timer-input"
            />
            <button class="timer-start-btn" @click="startTimer(customMinutes)" :disabled="!customMinutes || customMinutes < 1">
              开始
            </button>
          </div>
        </div>

      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'

defineProps({ visible: { type: Boolean, default: false } })
defineEmits(['close'])

const store = usePlayerStore()

const isActive = ref(false)
const remainingSeconds = ref(0)
const customMinutes = ref(null)
let timer = null

const options = [
  { label: '15 分钟', value: 15 },
  { label: '30 分钟', value: 30 },
  { label: '45 分钟', value: 45 },
  { label: '60 分钟', value: 60 },
  { label: '90 分钟', value: 90 },
  { label: '播完此曲', value: -1 },
]

const formattedRemaining = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function startTimer(minutes) {
  if (minutes === -1) {
    // 播完此曲：监听歌曲结束
    isActive.value = true
    remainingSeconds.value = store.duration - store.currentTime || 0
    watchSongEnd()
    return
  }
  if (!minutes || minutes < 1) return
  clearInterval(timer)
  isActive.value = true
  remainingSeconds.value = minutes * 60

  timer = setInterval(() => {
    remainingSeconds.value--
    if (remainingSeconds.value <= 0) {
      store.isPlaying = false
      cancelTimer()
    }
  }, 1000)
}

function watchSongEnd() {
  // 当前歌播完后停止：watch currentSong 变化
  const unwatch = watch(() => store.currentSong?.id, () => {
    store.isPlaying = false
    cancelTimer()
    unwatch()
  })
}

function cancelTimer() {
  clearInterval(timer)
  timer = null
  isActive.value = false
  remainingSeconds.value = 0
  customMinutes.value = null
}

onUnmounted(cancelTimer)
</script>

<style scoped>
/* 复用 LyricView 弹窗体系 */
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
  gap: 20px;
}
.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.info-title {
  font-size: 15px;
  font-weight: 700;
  color: white;
}
.info-close {
  background: rgba(255,255,255,0.1);
  border: none;
  color: rgba(255,255,255,0.6);
  width: 28px; height: 28px;
  border-radius: 50%;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.info-close:hover { background: rgba(255,255,255,0.2); color: white; }

/* 倒计时激活状态 */
.timer-active {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
}
.timer-countdown {
  font-size: 56px;
  font-weight: 800;
  color: white;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
  text-shadow: 0 0 32px rgba(139, 92, 246, 0.6);
}
.timer-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.5);
}
.timer-cancel-btn {
  margin-top: 8px;
  padding: 8px 24px;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.2);
  background: transparent;
  color: rgba(255,255,255,0.6);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.timer-cancel-btn:hover {
  background: rgba(255,255,255,0.1);
  color: white;
}

/* 选项网格 */
.timer-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.timer-option-btn {
  padding: 12px 8px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.8);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.timer-option-btn:hover {
  background: rgba(139, 92, 246, 0.3);
  border-color: rgba(139, 92, 246, 0.5);
  color: white;
}

/* 自定义输入 */
.timer-custom {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}
.timer-input {
  flex: 1;
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 10px 14px;
  color: white;
  font-size: 14px;
  outline: none;
}
.timer-input:focus {
  border-color: rgba(139, 92, 246, 0.5);
}
.timer-input::placeholder { color: rgba(255,255,255,0.3); }
.timer-start-btn {
  padding: 10px 20px;
  border-radius: 10px;
  border: none;
  background: rgba(139, 92, 246, 0.4);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.timer-start-btn:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.7);
}
.timer-start-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.info-pop-enter-active,
.info-pop-leave-active { transition: all 0.3s ease; }
.info-pop-enter-from,
.info-pop-leave-to { opacity: 0; transform: translateY(20px); }
</style>