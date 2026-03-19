<template>
  <div class="song-table-wrap">
    <header v-if="layoutMode === 'grid'" class="page-header grid-header">
      <div class="header-left">
        <h1 class="gradient-text">{{ pageTitle }}</h1>
        <span class="song-count liquid-card">{{ filteredSongs.length }} 首</span>
      </div>
      <div class="search-box liquid-panel">
        <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="search-icon"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        <input v-model="searchQuery" type="text" placeholder="搜索你的律动..." class="search-input" @keydown.esc="searchQuery = ''" />
        <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''"><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
      </div>
    </header>

    <header v-else class="page-header list-header">
      <div class="playlist-cover-wrap liquid-panel">
        <img
            v-if="playlistCover"
            :key="playlistCover"
            :src="playlistCover"
            class="playlist-cover"
            @error="e => e.target.style.opacity = 0"
        />

        <div v-else class="playlist-cover-fallback">
          <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48">
            <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
          </svg>
        </div>
      </div>

      <div class="playlist-info">
        <h1 class="playlist-title">{{ pageTitle }}</h1>

        <div class="playlist-meta">
          <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Admin&backgroundColor=b6e3f4" class="creator-avatar" />
          <span class="creator-name">My Music</span>
          <span class="create-time">创建于最近</span>
          <span class="song-count-badge liquid-card">{{ filteredSongs.length }} 首</span>
        </div>

        <div class="playlist-actions">
          <button class="btn-play-all" @click="playAll">
            <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M8 5v14l11-7z"/></svg>
            播放全部
          </button>
          <button class="btn-action liquid-btn" title="下载">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
          </button>
          <button class="btn-action liquid-btn" title="编辑歌单" @click="emit('editPlaylist')">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
          </button>
        </div>
      </div>

      <div class="search-box liquid-panel list-search">
        <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="search-icon"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        <input v-model="searchQuery" type="text" placeholder="在歌单内搜索..." class="search-input" @keydown.esc="searchQuery = ''" />
        <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''"><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
      </div>
    </header>

    <template v-if="layoutMode === 'list'">
      <div class="table-header">
        <span class="col-index">#</span>
        <span class="col-title">标题</span>
        <span class="col-artist">艺术家</span>
        <span class="col-album">专辑</span>
        <span class="col-fav">喜欢</span>
        <span class="col-duration">时长</span>
      </div>

      <div class="table-body">
        <div
            v-for="(song, index) in filteredSongs"
            :key="song.id"
            :class="['song-row', { 'row-active liquid-card': store.currentSong?.id === song.id }]"
            @dblclick="playSong(song)"
            @contextmenu.prevent="openMenu($event, song)"
        >
          <span class="col-index">
            <span v-if="store.currentSong?.id === song.id && store.isPlaying" class="playing-eq">
              <i /><i /><i />
            </span>
            <span v-else class="row-index" :class="{ 'index-active': store.currentSong?.id === song.id }">{{ index + 1 }}</span>
            <button class="play-hover-btn" @click.stop="playSong(song)">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M8 5v14l11-7z"/></svg>
            </button>
          </span>

          <span class="col-title">
            <img :src="coverUrl(song.id)" class="row-cover" @error="e => e.target.style.visibility = 'hidden'" />
            <span class="row-title" :class="{ 'title-active': store.currentSong?.id === song.id }">{{ song.title }}</span>
          </span>

          <span class="col-artist">{{ song.artist }}</span>
          <span class="col-album">{{ song.album }}</span>

          <span class="col-fav">
            <button class="fav-btn" :class="{ 'fav-active': plStore.isFavorite(song.id) }" @click.stop="plStore.toggleFavorite(song.id)">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
            </button>
          </span>

          <span class="col-duration">{{ song.duration ? formatDur(song.duration) : '--:--' }}</span>
        </div>
      </div>
    </template>

    <template v-else-if="layoutMode === 'grid'">
      <div class="grid-body custom-scrollbar">
        <div class="song-grid">
          <div
              v-for="song in filteredSongs"
              :key="'grid-'+song.id"
              class="grid-card liquid-card"
              @dblclick="playSong(song)"
              @contextmenu.prevent="openMenu($event, song)"
          >
            <div class="card-cover-wrap">
              <img :src="coverUrl(song.id)" class="card-cover" @error="e => e.target.style.visibility = 'hidden'" />

              <div class="card-play-overlay" @click.stop="playSong(song)">
                <div v-if="store.currentSong?.id === song.id && store.isPlaying" class="playing-eq grid-eq">
                  <i /><i /><i /><i />
                </div>
                <svg v-else viewBox="0 0 24 24" fill="currentColor" width="40" height="40">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </div>

              <button class="card-fav-btn" :class="{ 'fav-active': plStore.isFavorite(song.id) }" @click.stop="plStore.toggleFavorite(song.id)">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
              </button>
            </div>

            <div class="card-info">
              <div class="card-title" :class="{'title-active': store.currentSong?.id === song.id}">{{ song.title }}</div>
              <div class="card-artist">{{ song.artist }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="!filteredSongs.length" class="empty-state">
      <p class="empty-title">{{ songs.length ? '在星海中未找到该歌曲' : '暂无歌曲' }}</p>
    </div>

    <Teleport to="body">
      <div v-if="ctxVisible" class="ctx-menu liquid-panel" :style="{ top: ctxY + 'px', left: ctxX + 'px' }" @click.stop>

        <div class="ctx-item" @click="playSong(ctxSong); ctxVisible = false">播放</div>

        <div class="ctx-divider" />

        <div
            class="ctx-item"
            :class="{ 'ctx-delete': plStore.isFavorite(ctxSong.id) }"
            @click="plStore.toggleFavorite(ctxSong.id); ctxVisible = false"
        >
          {{ plStore.isFavorite(ctxSong.id) ? '取消喜欢' : '添加到我喜欢' }}
        </div>

        <div
            v-if="isCustomPlaylist"
            class="ctx-item ctx-delete"
            @click="removeFromCurrentPlaylist(ctxSong)"
        >
          从此歌单中删除
        </div>

        <div class="ctx-divider" v-if="plStore.userPlaylists.length" />
        <div class="ctx-label" v-if="plStore.userPlaylists.length">添加到歌单</div>

        <div class="ctx-scroll-area custom-scrollbar" v-if="plStore.userPlaylists.length">
          <div
              v-for="pl in plStore.userPlaylists"
              :key="pl.id"
              class="ctx-item ctx-sub"
              @click="addToPlaylist(pl, ctxSong)"
          >
            {{ pl.name }}
          </div>
        </div>

        <Transition name="toast-fade">
          <div v-if="showDuplicateToast" class="duplicate-toast">
            {{ duplicateMsg }}
          </div>
        </Transition>

      </div>
      <div v-if="ctxVisible" class="ctx-backdrop" @click="ctxVisible = false" />
    </Teleport>
  </div>
</template>

<script setup>
const store = usePlayerStore()
const plStore = usePlaylistStore()
const { coverUrl } = useCoverUrl()

const props = defineProps({ songs: { type: Array, default: () => [] }, pageTitle: { type: String, default: '全部歌曲' },layoutMode: { type: String, default: 'list' } })
const emit = defineEmits(['editPlaylist'])
const searchQuery = ref('')

const filteredSongs = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return props.songs
  return props.songs.filter(song => song.title.toLowerCase().includes(q) || song.artist.toLowerCase().includes(q) || song.album.toLowerCase().includes(q))
})

function playSong(song) { const realIndex = props.songs.findIndex(s => s.id === song.id); store.playSong(song, realIndex); ctxVisible.value = false; }

const ctxVisible = ref(false); const ctxX = ref(0); const ctxY = ref(0); const ctxSong = ref(null)

// ====== 新增：处理添加到歌单（包含查重拦截） ======
const showDuplicateToast = ref(false)
const duplicateMsg = ref('')

async function addToPlaylist(playlist, song) {
  // 1. 查重拦截：如果歌单里已经有这首歌
  if (playlist.songIds && playlist.songIds.includes(song.id)) {
    duplicateMsg.value = '! 歌单内歌曲重复'
    showDuplicateToast.value = true

    // 1秒后自动隐藏提示，且【不关闭】右键菜单
    setTimeout(() => {
      showDuplicateToast.value = false
    }, 1000)
    return
  }

  // 2. 正常添加逻辑
  try {
    await plStore.addSong(playlist.id, song.id)
    ctxVisible.value = false // 添加成功后关闭菜单
  } catch (e) {
    console.error('添加失败', e)
  }
}

// 防止菜单跑出屏幕底部的智能定位
function openMenu(e, song) {
  ctxSong.value = song;
  ctxVisible.value = true;
  // 简单计算，防止菜单溢出屏幕下边缘
  setTimeout(() => {
    const menuHeight = 300; // 预估最大高度
    ctxX.value = e.clientX;
    ctxY.value = e.clientY + menuHeight > window.innerHeight ? window.innerHeight - menuHeight : e.clientY;
  }, 0)
}

function formatDur(s) { const m = Math.floor(s / 60); const sec = Math.floor(s % 60); return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}` }

// 获取当前激活的歌单实例
const activePlaylist = computed(() => plStore.activePlaylist)

// 💡 智能判断当前是否是自定义歌单 (非系统歌单、非特殊视图)
const isCustomPlaylist = computed(() => {
  const id = plStore.activePlaylistId
  return id && id !== 'recent' && id !== 'community' && id !== 'favorites'
})

// 💡 从当前歌单中移除歌曲
const removeFromCurrentPlaylist = async (song) => {
  if (plStore.activePlaylistId) {
    await plStore.removeSong(plStore.activePlaylistId, song.id)
    ctxVisible.value = false // 移除后关闭菜单
  }
}

// 智能提取歌单封面：有自定义封面用自定义，没有则取列表第一首歌的封面
const playlistCover = computed(() => {
  if (activePlaylist.value?.coverImage) {
    return `/api/playlists/${activePlaylist.value.id}/cover?v=${Date.now()}`
  }
  if (filteredSongs.value && filteredSongs.value.length > 0) {
    return coverUrl(filteredSongs.value[0].id)
  }
  return ''
})

// 一键播放全部逻辑
function playAll() {
  if (filteredSongs.value.length > 0) {
    store.playSong(filteredSongs.value[0], 0)
  }
}

</script>

<style scoped>
.song-table-wrap { height: 100%; display: flex; flex-direction: column; overflow: hidden; padding: 0 12px; }

.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 32px 24px 24px; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.gradient-text { font-size: 32px; font-weight: 800; letter-spacing: -0.02em; background: linear-gradient(135deg, #fff 0%, rgba(255,255,255,0.6) 100%); -webkit-background-clip: text; color: transparent; }
.light-mode .gradient-text { background: linear-gradient(135deg, #000 0%, #555 100%); -webkit-background-clip: text; }
.song-count { font-size: 13px; font-weight: 600; padding: 4px 12px; border-radius: 20px; color: var(--text-primary); }

.search-box { display: flex; align-items: center; gap: 10px; border-radius: 24px; padding: 0 16px; height: 44px; min-width: 260px; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); cursor: text; }
.search-box:focus-within { transform: scale(1.02); box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2), 0 8px 24px rgba(0, 0, 0, 0.4); }
.search-icon { color: var(--text-secondary); flex-shrink: 0; }
.search-input { background: none; border: none; outline: none; color: var(--text-primary); font-size: 14px; flex: 1; font-weight: 500; }
.search-input::placeholder { color: var(--text-tertiary); font-weight: 400; }
.search-clear { background: rgba(255,255,255,0.1); border: none; color: white; cursor: pointer; padding: 4px; border-radius: 50%; display: flex; align-items: center; transition: all 0.2s ease; }
.search-clear:hover { background: var(--accent); transform: scale(1.1); }

.table-header { display: flex; align-items: center; padding: 0 24px; height: 40px; margin-bottom: 8px; font-size: 13px; color: var(--text-secondary); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid rgba(255,255,255,0.05); }
.table-body { flex: 1; overflow-y: auto; padding-bottom: 32px; }

/* 行的浮雕与悬浮交互 */
.song-row { display: flex; align-items: center; padding: 0 24px; height: 64px; margin-bottom: 4px; border-radius: 16px; border: 1px solid transparent; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); transform: translateZ(0);backface-visibility: hidden;}
.song-row:hover { background: rgba(255, 255, 255, 0.03); border-color: rgba(255, 255, 255, 0.05); box-shadow: 0 4px 16px rgba(0,0,0,0.1); transform: scale(0.995); }
.row-active { border-color: rgba(255, 255, 255, 0.1) !important; transform: scale(1) !important; box-shadow: 0 8px 24px rgba(0,0,0,0.2) !important; }

/* 分配列宽与对齐方式，确保表头和内容严丝合缝 */
.col-index { width: 48px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;position: relative; }
.col-title {
  flex: 4; /* 分配 4 份空间，绝对优先保卫歌名 */
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  padding-right: 16px; /* 防止和右边列贴得太近 */
}

/* 艺术家和专辑左对齐 */
.col-artist {
  flex: 2; /* 分配 2 份空间 */
  min-width: 0; /* 💡 关键：允许它被挤压折叠，不再是钉子户 */
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-size: 14px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 16px;
}

.col-album {
  flex: 2; /* 分配 2 份空间 */
  min-width: 0; /* 💡 关键：允许它被挤压折叠 */
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-size: 14px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 喜欢按钮居中对齐，稍微加宽一点以匹配表头文字 */
.col-fav { width: 64px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }

/* 时长强制靠右对齐 */
.col-duration { width: 64px; flex-shrink: 0; display: flex; align-items: center; justify-content: flex-end; font-size: 14px; color: var(--text-tertiary); text-align: right; font-variant-numeric: tabular-nums; font-family: monospace; }
.row-index { font-size: 15px; color: var(--text-tertiary); font-weight: 600; }
.index-active { color: var(--accent); }
/* 播放列表悬浮播放按钮对比度与放大效果 */
.play-hover-btn {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--text-primary);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.song-row:hover .row-index { opacity: 0; }

.song-row:hover .play-hover-btn {
  opacity: 1;
  /* 增大放大比例，使悬浮反馈更明显 */
  transform: scale(1.35);
  /* 改用通用且具有物理感的阴影，抛弃原本的纯白发光 */
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.25));
}
.song-row:hover .playing-eq { opacity: 0; }

.playing-eq { display: flex; align-items: flex-end; gap: 3px; height: 16px; }
.playing-eq i { display: block; width: 4px; background: var(--accent); border-radius: 2px; animation: eq-bar 0.8s ease-in-out infinite; box-shadow: 0 0 8px var(--accent); }
.playing-eq i:nth-child(1) { height: 55%; animation-delay: 0s; }
.playing-eq i:nth-child(2) { height: 100%; animation-delay: 0.15s; }
.playing-eq i:nth-child(3) { height: 40%; animation-delay: 0.3s; }
@keyframes eq-bar { 0%, 100% { transform: scaleY(0.4); } 50% { transform: scaleY(1); } }

.row-cover { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; flex-shrink: 0; box-shadow: 0 4px 12px rgba(0,0,0,0.3); transition: transform 0.3s ease; }
.song-row:hover .row-cover { transform: scale(1.05); }
.row-title { font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.title-active { color: var(--accent); text-shadow: 0 0 12px rgba(250, 45, 72, 0.4); }

.fav-btn { background: none; border: none; cursor: pointer; padding: 6px; color: var(--text-tertiary); opacity: 0; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); border-radius: 50%; display: flex; align-items: center; }
.song-row:hover .fav-btn { opacity: 0.6; }
.fav-btn:hover { opacity: 1 !important; transform: scale(1.2); background: rgba(255,255,255,0.1); }
.fav-active { color: var(--accent) !important; opacity: 1 !important; filter: drop-shadow(0 0 6px rgba(250, 45, 72, 0.5)); }

.empty-state { padding: 100px 32px; text-align: center; }
.empty-title { font-size: 20px; font-weight: 600; color: var(--text-tertiary); letter-spacing: 0.05em; }

.ctx-backdrop { position: fixed; inset: 0; z-index: 9998; }
.ctx-menu { position: fixed; z-index: 9999; border-radius: 12px; padding: 6px; min-width: 160px; display: flex; flex-direction: column;}
.ctx-item { padding: 10px 14px; font-size: 14px; font-weight: 500; border-radius: 8px; cursor: pointer; transition: all 0.2s ease; }
.ctx-item:hover { background: rgba(255, 255, 255, 0.1); transform: translateX(2px); }

/* 💡 滚动区域样式 */
.ctx-scroll-area { max-height: 180px; overflow-y: auto; overflow-x: hidden; padding-right: 4px;}
.ctx-scroll-area::-webkit-scrollbar { width: 4px; }
.ctx-scroll-area::-webkit-scrollbar-track { background: transparent; }
.ctx-scroll-area::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.2); border-radius: 4px; }

/* 💡 危险操作（删除）样式 */
.ctx-delete { color: #ef4444; }
.ctx-delete:hover { background: rgba(239, 68, 68, 0.15) !important; color: #ef4444; }

.ctx-sub { padding-left: 28px; color: var(--text-secondary); }
.ctx-sub:hover { color: var(--text-primary); }
.ctx-label { padding: 6px 14px 4px; font-size: 12px; font-weight: 700; color: var(--text-tertiary); text-transform: uppercase; }
.ctx-divider { height: 1px; background: rgba(255,255,255,0.08); margin: 6px 10px; flex-shrink: 0;}


/* =========================================
   新增：查重 Toast 悬浮提示样式
   ========================================= */
.duplicate-toast {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(250, 45, 72, 0.9); /* 高对比度苹果红 */
  color: white;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  pointer-events: none; /* 穿透点击，绝不影响用户点其他歌单 */
  z-index: 10000;
  white-space: nowrap;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 4px 16px rgba(250, 45, 72, 0.4);
}

/* Toast 淡入淡出及微微上浮动画 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -35%); /* 从略微偏下的位置弹起 */
}


/* =========================================
   响应式折叠逻辑
   ========================================= */

/* 第一阶段 (< 1024px)：屏幕变窄时，优先隐藏专辑 */
@media (max-width: 1024px) {
  .col-album { display: none; }
  .page-header.list-header {
    flex-wrap: wrap; /* 允许挤不下的元素换行 */
  }
}

/* 第二阶段 (< 900px)：平板尺寸时，隐藏艺术家，誓死保卫"歌名"的完整显示 */
@media (max-width: 900px) {
  .col-artist { display: none; }
}
@media (max-width: 768px) {
  .page-header { padding: 20px 16px 16px; padding-left: 64px; flex-wrap: wrap; }
  .gradient-text { font-size: 24px; }
  .search-box { min-width: 0; width: 100%; order: 3; margin-top: 8px; }
  .table-header { padding: 0 16px; }
  .song-row { padding: 0 16px; height: 60px; border-radius: 12px; }
  .col-index { width: 36px; }
  .col-artist, .col-album, .col-duration { display: none; }
  .col-fav { width: 40px; }
}

/* =========================================
   💡 新增：网格卡片布局 (Grid Mode)
   ========================================= */
.grid-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px 32px;
}

.song-grid {
  display: grid;
  /* 响应式网格，卡片最小 160px，自动填满整行 */
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 24px;
}

.grid-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid transparent;
}

.grid-card:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.card-cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.grid-card:hover .card-cover {
  transform: scale(1.08) rotate(-1deg);
}

/* 卡片悬浮播放遮罩 */
.card-play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  color: white;
}

.grid-card:hover .card-play-overlay {
  opacity: 1;
}

.card-play-overlay svg {
  transform: translateY(10px);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.4));
}

.grid-card:hover .card-play-overlay svg {
  transform: translateY(0);
}

.card-play-overlay svg:hover {
  color: var(--accent);
  transform: scale(1.15) !important;
}

/* 卡片快捷红心 */
.card-fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(8px);
  border: none;
  border-radius: 50%;
  padding: 6px;
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
  display: flex;
}
.grid-card:hover .card-fav-btn { opacity: 1; }
.card-fav-btn:hover { color: white; background: rgba(0,0,0,0.6); transform: scale(1.1); }
.card-fav-btn.fav-active { color: var(--accent); opacity: 1; filter: drop-shadow(0 0 6px rgba(250, 45, 72, 0.6)); }

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-artist {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 适配网格的动态 EQ */
.grid-eq i { width: 5px; margin: 0 2px; box-shadow: 0 0 10px var(--accent); }


/* =========================================
   💡 歌单详情头部样式 (List Header / 大图模式)
   ========================================= */
.list-header {
  display: flex;
  align-items: flex-end;
  padding: 40px 32px 32px;
  gap: 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 8px;
}

.playlist-cover-wrap {
  width: 190px;
  height: 190px;
  flex-shrink: 0;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.3);
  padding: 0; /* 抵消 liquid-panel 的默认 padding */
}

.playlist-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.playlist-cover-wrap:hover .playlist-cover {
  transform: scale(1.05);
}

.playlist-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 16px;
  padding-bottom: 4px;
}

.playlist-title {
  font-size: 38px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  text-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.playlist-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.creator-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.song-count-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 12px;
}

.playlist-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 6px;
}

.btn-play-all {
  display: flex;
  align-items: center;
  justify-content: center; /* 确保内容居中 */
  white-space: nowrap;     /* 强制文字不换行 */
  flex-shrink: 0;          /* 防止按钮被其他元素挤压缩小 */
  gap: 6px;
  background: linear-gradient(135deg, var(--accent), #ff7e5f);
  color: white;
  border: none;
  padding: 12px 28px;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(250, 45, 72, 0.4);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.btn-play-all:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 32px rgba(250, 45, 72, 0.6);
}
.btn-play-all:active {
  transform: scale(0.95);
}

.btn-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: var(--text-primary);
  cursor: pointer;
}
.btn-action:hover {
  color: var(--accent);
}

.list-search {
  align-self: flex-start; /* 将搜索框推向右上角 */
}

/* 适配原有 grid-header 的内边距 */
.grid-header { padding: 32px 24px 24px; }

/* 没有封面时的完美占位符 */
.playlist-cover-fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary); /* 淡淡的图标颜色 */
}

/* 白天模式下微调占位符底色 */
:global(.light-mode) .playlist-cover-fallback {
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.05), rgba(0, 0, 0, 0.01));
}

/* =========================================
   📱 移动端终极适配 (请务必放在 <style> 的最底部)
   ========================================= */
@media (max-width: 768px) {
  /* 1. 修复 Grid 模式（全部歌曲主页）遮挡 */
  .page-header.grid-header {
    /* 上 右 下 左 (左边留出 64px 完美避开汉堡按钮) */
    padding: 20px 16px 16px 64px;
  }

  /* 2. 修复 List 模式（我喜欢/歌单详情）遮挡与排版 */
  .page-header.list-header {
    padding: 24px 20px 20px 64px;
    flex-direction: column;       /* 移动端改为垂直排列，避免拥挤 */
    align-items: flex-start;
    gap: 16px;
  }

  /* 3. 移动端屏幕变窄，整体向右推移后需要适当缩小封面，防溢出 */
  .playlist-cover-wrap {
    width: 140px;
    height: 140px;
    border-radius: 16px;
  }

  .playlist-title {
    font-size: 28px; /* 移动端标题字号微调 */
  }

  .playlist-actions {
    flex-wrap: wrap; /* 允许操作按钮在小屏幕换行 */
    margin-top: 0;
  }

  .list-search {
    align-self: stretch; /* 搜索框拉宽，占满整个容器 */
    width: 100%;
    margin-top: 8px;
  }
}

</style>