<template>
  <div class="player-bar liquid-panel">
    <div class="bar-progress" ref="progressRef"
         @click="onProgressClick"
         @mousedown="onProgressMouseDown">
      <div class="bar-progress-bg"></div>
      <div class="bar-progress-fill" :style="{ width: (displayProgress * 100) + '%' }">
        <div class="bar-progress-glow"></div>
      </div>
    </div>

    <div class="bar-inner">
      <div class="bar-left">

        <div class="bar-song liquid-card" @click="openLyrics">
          <div class="bar-cover-wrap" v-if="store.currentSong">
            <img :src="coverUrl(store.currentSong.id)" class="bar-cover" @error="e => e.target.style.visibility = 'hidden'" />
            <div class="cover-expand-hint">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/></svg>
            </div>
          </div>
          <div v-if="store.currentSong" class="bar-meta">
            <span class="bar-title">{{ store.currentSong.title }}</span>
            <span class="bar-artist">{{ store.currentSong.artist }}</span>
          </div>
          <div v-else class="bar-meta">
            <span class="bar-title empty">听见宇宙的声音</span>
          </div>
        </div>

        <button v-if="store.currentSong"
                class="bar-fav-btn"
                :class="{ 'fav-active': plStore.isFavorite(store.currentSong?.id) }"
                @click.stop="plStore.toggleFavorite(store.currentSong?.id)">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </button>

        <button class="ctrl mode-btn" @click="showComment = !showComment" title="评论">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M21 6.5A2.5 2.5 0 0 0 18.5 4h-13A2.5 2.5 0 0 0 3 6.5v8A2.5 2.5 0 0 0 5.5 17H7v3l4-3h7.5a2.5 2.5 0 0 0 2.5-2.5v-8z"/>
          </svg>
        </button>

      </div> <CommentPanel :visible="showComment" @close="showComment = false" />

      <div class="bar-controls">

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

        <button class="ctrl" :disabled="!store.hasPrev" @click="store.prevSong()">
          <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22"><path d="M6 6h2v12H6zm3.5 6 8.5 6V6z"/></svg>
        </button>

        <button class="ctrl play" @click="store.togglePlay()">
          <div class="play-glow"></div>
          <svg v-if="store.isPlaying" viewBox="0 0 24 24" fill="currentColor" width="26" height="26"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" width="26" height="26" style="margin-left: 2px;"><path d="M8 5v14l11-7z"/></svg>
        </button>

        <button class="ctrl" :disabled="!store.hasNext" @click="store.nextSong()">
          <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
        </button>

        <button class="ctrl mode-btn" @click="showPlaylist = !showPlaylist" title="播放列表">
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/>
          </svg>
        </button>
      </div>

      <div class="bar-right">
        <span class="bar-time">{{ store.formattedCurrentTime }} / {{ store.formattedDuration }}</span>
        <div class="volume-group liquid-card">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="vol-icon"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 8.5v7a4.5 4.5 0 0 0 2.5-3.5z"/></svg>

          <div class="vol-slider-wrapper">
            <div class="vol-progress-bg"></div>
            <div class="vol-progress-fill" :style="{ width: (store.volume * 100) + '%' }">
              <div class="vol-progress-glow"></div>
            </div>
            <input type="range" min="0" max="1" step="0.01" :value="store.volume" class="vol-slider-input" @input="e => emit('volume', parseFloat(e.target.value))" />
          </div>

        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPlaylist" class="playlist-backdrop" @click="showPlaylist = false"></div>
      </Transition>

      <Transition name="slide-up">
        <div v-if="showPlaylist" class="playlist-popup liquid-panel">
          <div class="playlist-header">
            <h3>当前播放</h3>
            <span class="count">{{ store.playlist?.length || 0 }} 首</span>
            <button class="close-btn" @click="showPlaylist = false">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
            </button>
          </div>

          <div class="playlist-body custom-scrollbar">
            <div
                v-for="(song, idx) in store.playlist"
                :key="song.id"
                class="playlist-item"
                :class="{'active liquid-card': store.currentSong?.id === song.id}"
                @dblclick="store.playSong(song, idx)"
            >
              <div class="item-left">
                <span v-if="store.currentSong?.id === song.id && store.isPlaying" class="playing-eq">
                  <i/><i/><i/>
                </span>
                <span v-else class="idx" :class="{'active-idx': store.currentSong?.id === song.id}">
                  {{ idx + 1 }}
                </span>
              </div>
              <div class="item-info">
                <span class="title" :class="{'active-title': store.currentSong?.id === song.id}">{{ song.title }}</span>
                <span class="artist">- {{ song.artist }}</span>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
const store = usePlayerStore()
const { coverUrl } = useCoverUrl()
const emit = defineEmits(['seek', 'volume'])
const progressRef = ref(null)
const showPlaylist = ref(false)
const playModeLabel = computed(() => {
  return {
    'sequence': '顺序播放',
    'loop-all': '列表循环',
    'loop-one': '单曲循环',
    'shuffle': '随机播放'
  }[store.playMode]
})

const isDragging = ref(false)
const dragProgress = ref(0)

const displayProgress = computed(() => {
  return isDragging.value ? dragProgress.value : store.progress
})

function onProgressMouseDown(e) {
  isDragging.value = true
  dragProgress.value = getRatio(e)

  const onMouseMove = (e) => {
    dragProgress.value = getRatio(e)
  }
  const onMouseUp = (e) => {
    isDragging.value = false
    emit('seek', getRatio(e) * store.duration)
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('mouseup', onMouseUp)
  }

  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

function onProgressClick(e) {
  const ratio = getRatio(e)
  emit('seek', ratio * store.duration)
}

function getRatio(e) {
  const rect = progressRef.value.getBoundingClientRect()
  return Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
}

function openLyrics() { if (store.currentSong) store.openLyricView() }

/* 播放器 ❤ */
const plStore = usePlaylistStore()

/* 评论 按钮*/
import CommentPanel from '~/components/player/CommentPanel.vue'
const showComment = ref(false)

</script>

<style scoped>
/* 播放列表弹窗与遮罩层样式 */
.playlist-backdrop {
  position: fixed;
  inset: 0;
  z-index: 998; /* 层级位于 Navbar 和 Sidebar 之上，Popup 之下 */
  /* 要求：模糊很淡 */
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  background: rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.light-mode .playlist-backdrop {
  background: rgba(255, 255, 255, 0.15); /* 白天模式下使用更清透的淡白遮罩 */
}

.playlist-popup {
  position: fixed;
  bottom: 100px; /* 悬浮在底部播放条之上 */
  right: 24px;
  width: 380px;
  max-height: calc(100vh - 140px);
  z-index: 999;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  padding: 0; /* 内部控制 padding */
  overflow: hidden;
}

.playlist-header {
  display: flex;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.playlist-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-right: 12px;
}

.playlist-header .count {
  font-size: 13px;
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 8px;
  border-radius: 12px;
}

.playlist-header .close-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  transition: all 0.2s ease;
}

.playlist-header .close-btn:hover {
  color: var(--text-primary);
  transform: scale(1.1);
}

.playlist-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 16px;
}

.playlist-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 4px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.playlist-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.playlist-item.active {
  border-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
}

.item-left {
  width: 32px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.item-left .idx {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.item-left .active-idx {
  color: var(--accent);
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-info .title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.item-info .active-title {
  color: var(--accent);
}

.item-info .artist {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 进出场动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}

.player-bar {
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  border-top: none;
  /* 重写圆角，让它看起来像是悬浮在底部的 DOCK */
  border-radius: 2rem 2rem 0 0;
  margin: 0;
  box-shadow:
      inset 0 1px 1px rgba(255, 255, 255, 0.2),
      0 -10px 40px rgba(0, 0, 0, 0.5);
}

/* 悬浮进度条设计 */
.bar-progress { position: absolute; top: -12px; left: 24px; right: 24px; height: 24px; cursor: pointer; z-index: 2; display: flex; align-items: center; }
.bar-progress-bg { position: absolute; left: 0; right: 0; height: 4px; background: rgba(255,255,255,0.1); border-radius: 2px; backdrop-filter: blur(4px); transition: height 0.2s ease; }
.bar-progress-fill { position: relative; height: 4px; background: linear-gradient(90deg, #fa2d48, #ff7e5f); border-radius: 2px; transition: width 0.1s linear, height 0.2s ease; box-shadow: 0 0 8px rgba(250, 45, 72, 0.5); }
.bar-progress-glow { content: ''; position: absolute; right: -4px; top: 50%; transform: translateY(-50%); width: 10px; height: 10px; background: #fff; border-radius: 50%; box-shadow: 0 0 10px #fff, 0 0 20px var(--accent); opacity: 0; transition: opacity 0.2s ease, transform 0.2s ease; }

.bar-progress:hover .bar-progress-bg, .bar-progress:hover .bar-progress-fill { height: 6px; }
.bar-progress:hover .bar-progress-glow { opacity: 1; transform: translateY(-50%) scale(1.2); }

.bar-inner { display: flex; align-items: center; padding: 12px 32px; gap: 24px; height: 84px; }

/* 左侧包裹层样式 */
.bar-left {
  display: flex;
  align-items: center;
  gap: 12px; /* 控制歌曲信息、红心、评论按钮之间的间距 */
  width: 280px; /* 和右侧的 bar-right 保持宽度对称 */
  flex-shrink: 0;
}

.bar-song {
  display: flex; align-items: center; gap: 16px;
  flex: 1; /* 让歌曲信息自动占满左侧剩余空间 */
  min-width: 0; /* 防止长标题撑破 flex 容器 */
  cursor: pointer; padding: 6px 12px 6px 6px; border-radius: 16px; border: none;
}
.bar-song:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.2); transform: translateY(-2px); }

.bar-cover-wrap { position: relative; width: 52px; height: 52px; flex-shrink: 0; }
.bar-cover { width: 100%; height: 100%; border-radius: 10px; object-fit: cover; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4); transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.bar-song:hover .bar-cover { transform: scale(1.08) rotate(-2deg); }

.cover-expand-hint { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.4); border-radius: 10px; opacity: 0; color: white; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(2px); transition: all 0.3s ease; }
.bar-song:hover .cover-expand-hint { opacity: 1; transform: scale(1.1); }

.bar-meta { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.bar-title { font-size: 15px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar-title.empty { color: var(--text-tertiary); font-weight: 500; letter-spacing: 0.1em; text-transform: uppercase; font-size: 13px; }
.bar-artist {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block; /* 确保 span 能正确执行宽度的截断 */
}

.bar-controls { display: flex; align-items: center; gap: 24px; flex-shrink: 0; margin: 0 auto; }
.ctrl {
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  /* 提升基础的不透明度，避免白天模式下看不清 */
  opacity: 0.95;
}

.ctrl:hover:not(:disabled) {
  opacity: 1;
  /* 增大悬浮放大倍率 */
  transform: scale(1.25);
  /* 抛弃白天模式下发虚的纯白发光，改用柔和的暗色实体阴影 */
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.15));
}

/* 🌙 适配白天模式下更锋利的对比度 */
.light-mode .ctrl {
  color: var(--text-primary);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.light-mode .ctrl:hover:not(:disabled) {
  filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.2));
}

.ctrl:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.mode-btn {
  opacity: 0.7;
}
.mode-btn:hover {
  opacity: 1;
}


.ctrl.play { position: relative; width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), #ff7e5f); color: #fff; box-shadow: 0 4px 16px rgba(250, 45, 72, 0.4), inset 0 2px 4px rgba(255,255,255,0.3); opacity: 1; z-index: 1; }
.ctrl.play:hover { transform: scale(1.1) translateY(-2px); box-shadow: 0 8px 24px rgba(250, 45, 72, 0.6), inset 0 2px 4px rgba(255,255,255,0.4); }
.ctrl.play:active { transform: scale(0.95); }
.play-glow { position: absolute; inset: -4px; border-radius: 50%; background: var(--accent); opacity: 0.4; filter: blur(8px); z-index: -1; animation: pulse 2s infinite alternate; }
@keyframes pulse { 0% { transform: scale(0.9); opacity: 0.3; } 100% { transform: scale(1.1); opacity: 0.6; } }

.bar-right { display: flex; align-items: center; gap: 20px; width: 280px; justify-content: flex-end; flex-shrink: 0; }
.bar-time { font-size: 13px; color: var(--text-primary); font-weight: 600; font-family: monospace; letter-spacing: 0.05em; opacity: 0.8; }
.volume-group { display: flex; align-items: center; gap: 10px; padding: 6px 14px; border-radius: 20px; border: none; }
.vol-icon { color: var(--text-primary); opacity: 0.8; }


/* --- 新的音量条样式 --- */
.volume-group { display: flex; align-items: center; gap: 10px; padding: 6px 14px; border-radius: 20px; border: none; }
.vol-icon { color: var(--text-primary); opacity: 0.8; }

.vol-slider-wrapper {
  position: relative;
  width: 90px;
  height: 20px; /* 增加热区高度，方便鼠标/手指交互 */
  display: flex;
  align-items: center;
  cursor: pointer;
}

.vol-progress-bg { position: absolute; left: 0; right: 0; height: 4px; background: rgba(255, 255, 255, 0.15); border-radius: 2px; transition: height 0.2s ease; }
.vol-progress-fill { position: absolute; left: 0; height: 4px; background: linear-gradient(90deg, #fa2d48, #ff7e5f); border-radius: 2px; transition: width 0.1s linear, height 0.2s ease; box-shadow: 0 0 8px rgba(250, 45, 72, 0.5); pointer-events: none; }
.vol-progress-glow { content: ''; position: absolute; right: -4px; top: 50%; transform: translateY(-50%); width: 10px; height: 10px; background: #fff; border-radius: 50%; box-shadow: 0 0 10px #fff, 0 0 20px var(--accent); opacity: 0; transition: opacity 0.2s ease, transform 0.2s ease; }

/* Hover 联动变粗及显示发光小球 */
.vol-slider-wrapper:hover .vol-progress-bg,
.vol-slider-wrapper:hover .vol-progress-fill { height: 6px; }
.vol-slider-wrapper:hover .vol-progress-glow { opacity: 1; transform: translateY(-50%) scale(1.2); }

/* 将原生 range input 设为完全透明并置于最顶层，用于捕捉事件 */
.vol-slider-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  margin: 0;
  z-index: 2;
  -webkit-appearance: none;
  appearance: none;
}

@media (max-width: 768px) {
  /* 1. 调整播放栏整体外形和间距 */
  .player-bar {
    border-radius: 16px 16px 0 0;
  }
  .bar-inner {
    padding: 8px 16px;
    height: 64px; /* 稍微降低高度，显得更精致 */
    justify-content: space-between; /* 💡 关键：让左侧歌曲和右侧按钮各分东西 */
    gap: 12px;
  }

  /* 2. 彻底隐藏不需要的辅助组件 */
  .bar-right,        /* 隐藏右侧音量和时间 */
  .bar-progress,     /* 隐藏顶部的悬浮进度条（容易误触） */
  .bar-fav-btn,      /* 隐藏红心 */
  .bar-left > .mode-btn { /* 隐藏左侧的评论按钮 */
    display: none !important;
  }

  /* 3. 左侧歌曲信息区：自动填满剩余空间，超出截断 */
  .bar-left {
    width: auto;
    flex: 1; /* 撑开占据主导地位 */
    min-width: 0; /* 防止长标题撑破 Flex 容器 */
    gap: 0;
  }
  .bar-song {
    width: 100%;
    padding: 0;
    background: transparent !important;
    box-shadow: none !important;
    border-color: transparent !important;
    gap: 10px;
  }
  .bar-cover-wrap {
    width: 42px;
    height: 42px;
  }
  .bar-title {
    font-size: 14px;
  }
  .bar-artist {
    font-size: 11px;
  }

  /* 4. 中间控制区（现移至最右侧）：只保留播放按钮 */
  .bar-controls {
    width: auto;
    margin: 0; /* 去除原本的居中 margin: 0 auto */
    gap: 0;
    flex-shrink: 0;
  }

  /* 隐藏控制区内除了 .play 之外的所有按钮 (上一首、下一首、播放模式等) */
  .bar-controls .ctrl:not(.play) {
    display: none;
  }

  /* 稍微缩小播放按钮，适应手机比例 */
  .ctrl.play {
    width: 44px;
    height: 44px;
  }
}


/* 播放器 ❤*/
.bar-fav-btn {
  background: none; border: none;
  color: var(--text-tertiary); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  padding: 6px; border-radius: 8px;
  transition: all 0.2s; flex-shrink: 0;
}
.bar-fav-btn:hover { color: #ff2d55; transform: scale(1.1); }
.bar-fav-btn.fav-active { color: #ff2d55; }


</style>