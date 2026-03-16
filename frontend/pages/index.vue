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

        <SongTable :songs="displaySongs" :page-title="pageTitle" :layout-mode="layoutMode" @editPlaylist="showEditModal = true" />
      </div>
    </div>

    <PlayerBar @seek="onSeek" @volume="onVolume" />
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

const store = usePlayerStore()
const plStore = usePlaylistStore()
const { seek, setVolume } = useAudioPlayer()

const allSongs = ref([])
const showCreateModal = ref(false)
const sidebarOpen = ref(false)
const showEditModal = ref(false)

onMounted(async () => {
  try {
    const res = await fetch('/api/songs')
    if (res.ok) {
      allSongs.value = await res.json()
      store.setPlaylist(allSongs.value)
    }
  } catch (err) {
    console.error('加载歌曲列表失败:', err)
  }
  await plStore.fetchPlaylists()
  window.addEventListener('keydown', onGlobalKey)
})

onUnmounted(() => { window.removeEventListener('keydown', onGlobalKey) })

const displaySongs = computed(() => {
  const playlist = plStore.activePlaylist
  if (!playlist) return allSongs.value
  return playlist.songIds.map(id => allSongs.value.find(s => s.id === id)).filter(Boolean)
})

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
</style>
