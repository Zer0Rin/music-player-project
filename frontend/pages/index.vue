<template>
  <div class="app-shell">
    <div class="ambient-orbs">
      <div class="ambient-orb orb-a" />
      <div class="ambient-orb orb-b" />
      <div class="ambient-orb orb-c" />
    </div>

    <div class="content-area">
      <Transition name="fade">
        <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false" @touchmove.prevent />
      </Transition>

      <Sidebar
          :open="sidebarOpen"
          @close="sidebarOpen = false"
          @openCreateModal="showCreateModal = true"
      />

      <div class="main-content">
        <button class="hamburger" @click="sidebarOpen = true">
          <svg width="22" height="22" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
          </svg>
        </button>

        <SongTable
            v-if="plStore.activePlaylistId !== 'recent' && plStore.activePlaylistId !== 'community' && plStore.activePlaylistId !== 'profile'"
            :songs="displaySongs"
            :page-title="pageTitle"
            :layout-mode="layoutMode"
            @editPlaylist="showEditModal = true"
        />

        <div v-else-if="plStore.activePlaylistId === 'recent'" class="recent-view-container">
          <div class="recent-tabs-header">
            <div class="recent-tab" :class="{ active: recentActiveTab === 'music' }" @click="recentActiveTab = 'music'">
              <span>音乐</span><sup class="tab-count">{{ store.recentSongs.length }}</sup>
            </div>
            <div class="recent-tab" :class="{ active: recentActiveTab === 'community' }" @click="recentActiveTab = 'community'">
              <span>社区</span><sup class="tab-count">0</sup>
            </div>
          </div>
          <div class="recent-content">
            <div v-show="recentActiveTab === 'music'" class="tab-pane">
              <SongTable :songs="store.recentSongs" page-title="最近播放记录" layout-mode="list" />
            </div>
            <div v-show="recentActiveTab === 'community'" class="tab-pane community-placeholder">
              <div class="placeholder-content liquid-card">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="48" height="48" class="ghost-icon"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <h3>社区板块建设中</h3>
                <p>这里将成为律动者的精神角落...</p>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="plStore.activePlaylistId === 'community'" class="community-wrapper" style="height: 100%; width: 100%; overflow: hidden;">
          <CommunityView @toggle-modal="onCommunityModalToggle" />
        </div>

        <div v-else-if="plStore.activePlaylistId === 'profile'" class="profile-wrapper" style="height: 100%; overflow-y: auto;">
          <ProfileView />
        </div>

      </div>
    </div>

    <Transition name="slide-down">
      <PlayerBar v-show="!hidePlayerBar" @seek="onSeek" @volume="onVolume" />
    </Transition>

    <LyricView @seek="onSeek" @volume="onVolume" />

    <CreatePlaylistModal
        :visible="showCreateModal"
        @close="showCreateModal = false"
        @created="showCreateModal = false"
    />

    <EditPlaylistModal
        :visible="showEditModal"
        :playlist="plStore.activePlaylist"
        @close="showEditModal = false"
        @saved="showEditModal = false"
    />

  </div>
</template>

<script setup>
import Sidebar from '~/components/layout/Sidebar.vue'
import SongTable from '~/components/home/SongTable.vue'
import PlayerBar from '~/components/player/PlayerBar.vue'
import LyricView from '~/components/player/LyricView.vue'
import CreatePlaylistModal from '~/components/playlist/CreatePlaylistModal.vue'
import EditPlaylistModal from '~/components/playlist/EditPlaylistModal.vue'
import CommunityView from '~/components/community/CommunityView.vue'
import ProfileView from '~/components/user/ProfileView.vue'

const store = usePlayerStore()
const plStore = usePlaylistStore()
const { seek, setVolume } = useAudioPlayer()

const allSongs = computed(() => store.allSongs)
const showCreateModal = ref(false)
const sidebarOpen = ref(false)
const showEditModal = ref(false)
//关闭播放栏
const hidePlayerBar = ref(false)
function onCommunityModalToggle(isOpen) {
  hidePlayerBar.value = isOpen
}
const recentActiveTab = ref('music')  // 最近

onMounted(async () => {
  try {
    const { $apiFetch } = useNuxtApp()
    await store.refreshSongs($apiFetch)
  } catch (err) {
    console.error('加载歌曲列表失败:', err)
  }

  await plStore.fetchPlaylists()
  window.addEventListener('keydown', onGlobalKey)
})

onUnmounted(() => { window.removeEventListener('keydown', onGlobalKey) })

const displaySongs = computed(() => {
  // 特殊视图不计算
  const id = plStore.activePlaylistId
  if (id === 'recent' || id === 'community' || id === 'profile') return []

  const playlist = plStore.activePlaylist
  if (!playlist) return allSongs.value
  return playlist.songIds.map(id => allSongs.value.find(s => s.id === id)).filter(Boolean)
})

// 监听 activePlaylistId 的变化，主动切换播放列表
watch(() => plStore.activePlaylistId, (newId) => {
  // 1. 如果是社区或个人主页，不更新播放列表（保持正在播放的歌单继续播放）
  if (newId === 'community' || newId === 'profile') {
    return
  }

  // 2. 如果点击的是“最近”，把最近播放的歌曲列表扔给播放器！
  if (newId === 'recent') {
    if (store.recentSongs.length > 0) {
      store.setPlaylist([...store.recentSongs])
    }
    return
  }

  // 3. 其他情况（全部歌曲 或 具体某个歌单）
  const songs = displaySongs.value
  if (songs && songs.length > 0) {
    store.setPlaylist([...songs])
  }
}, { immediate: true }) // immediate: true 确保页面刚加载时也能初始化列表

const pageTitle = computed(() => {
  const playlist = plStore.activePlaylist
  return playlist ? playlist.name : '全部歌曲'
})
const layoutMode = computed(() => {
  const id = plStore.activePlaylistId
  // 如果没有选中歌单 (主页)，或者选中的是最近播放 (假设你把它的ID设为 'recent')，就用网格模式
  if (id === null || id === 'recent') {
    return 'grid'
  }
  // 其他情况（我喜欢、具体歌单）使用列表模式
  return 'list'
})

function onSeek(time) { seek(time) }
function onVolume(vol) { setVolume(vol) }

function onGlobalKey(e) {
  if (e.code === 'Space') { e.preventDefault(); if (store.currentSong) store.togglePlay() }
  if (e.key === 'Escape') {
    if (showCreateModal.value) { showCreateModal.value = false; return }
    if (showEditModal.value) { showEditModal.value = false; return }
    if (sidebarOpen.value) { sidebarOpen.value = false; return }
    store.closeLyricView()
  }
}

// login
definePageMeta({
  middleware: 'auth'
})

</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
  position: relative;
}

.ambient-orbs { position: absolute; inset: 0; pointer-events: none; z-index: 0; overflow: hidden; }
.ambient-orb { position: absolute; border-radius: 50%; filter: blur(140px); opacity: 0.35; }
.orb-a { width: 50vw; height: 50vw; min-width: 30rem; min-height: 30rem; top: -15%; left: -10%; background: rgba(107, 33, 168, 0.5); animation: breathe1 12s ease-in-out infinite; }
.orb-b { width: 55vw; height: 55vw; min-width: 35rem; min-height: 35rem; bottom: -20%; right: -8%; background: rgba(30, 58, 138, 0.5); animation: breathe2 18s ease-in-out infinite; }
.orb-c { width: 40vw; height: 40vw; min-width: 25rem; min-height: 25rem; top: 25%; left: 35%; background: rgba(55, 48, 163, 0.5); animation: breathe3 15s ease-in-out infinite; }

@keyframes breathe1 { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.1); } }
@keyframes breathe2 { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.08); } }
@keyframes breathe3 { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.12); } }

.content-area { flex: 1; display: flex; overflow: hidden; min-height: 0; position: relative; z-index: 1; }
.main-content { flex: 1; overflow: hidden; position: relative; }

/* 移动端遮罩 */
.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(0, 0, 0, 0.25);
}

/* 汉堡按钮（桌面端隐藏） */
.hamburger {
  display: none;
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 20;
  padding: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: var(--text-primary);
  cursor: pointer;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  transition: all var(--transition-fast);
}

.hamburger:active { transform: scale(0.95); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  .sidebar-overlay { display: block; }
  .hamburger { display: flex; }

  .ambient-orb { filter: blur(100px); opacity: 0.25; }
  .orb-a { min-width: 20rem; min-height: 20rem; }
  .orb-b { min-width: 22rem; min-height: 22rem; }
  .orb-c { min-width: 16rem; min-height: 16rem; }
}

/* ===== 最近  UI ===== */
/* =========================================
   最近视图 (Recent View) 高级 Tab 样式
   ========================================= */
.recent-view-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.recent-tabs-header {
  display: flex;
  align-items: center;
  gap: 32px;
  /* 左侧留出 64px 适配移动端汉堡菜单 */
  padding: 32px 32px 16px 64px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.recent-tab {
  position: relative;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
  padding-bottom: 8px; /* 底部给红线留空间 */
}

/* 上标数字角标 */
.tab-count {
  font-size: 11px;
  font-weight: 800;
  margin-left: 4px;
  color: var(--text-tertiary);
  transition: color 0.3s ease;
}

/* 激活状态：字体放大，颜色变亮 */
.recent-tab.active {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 800;
}

.recent-tab.active .tab-count {
  color: var(--text-secondary);
}

/* 底部红色下划线动画 */
.recent-tab::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 24px;
  height: 4px;
  background: linear-gradient(90deg, #fa2d48, #ff7e5f); /* 你的主题渐变色 */
  border-radius: 2px;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.recent-tab.active::after {
  transform: translateX(-50%) scaleX(1);
}

/* 内容区 */
.recent-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.tab-pane {
  height: 100%;
}

/* 社区占位符 */
.community-placeholder { display: flex; align-items: center; justify-content: center; padding: 32px; }
.placeholder-content { text-align: center; padding: 64px; border-radius: 24px; border: 1px dashed rgba(255,255,255,0.1); background: rgba(255, 255, 255, 0.02); }
.ghost-icon { color: var(--text-tertiary); margin-bottom: 16px; }
.placeholder-content h3 { font-size: 20px; color: var(--text-primary); margin-bottom: 8px; font-weight: 700; }
.placeholder-content p { color: var(--text-secondary); font-size: 14px; }

/* =========================================
   💡 播放栏下沉收起动画
   ========================================= */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

</style>
