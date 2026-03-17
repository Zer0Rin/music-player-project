<template>
  <aside
      class="sidebar liquid-panel"
      :class="{ 'sidebar-open': open }"
      @click.stop
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
  >
    <div class="sidebar-logo">
      <span class="logo-icon">♫</span>
      <span class="logo-text">Music</span>
    </div>

    <div class="theme-toggle liquid-card">
      <button class="theme-btn" :class="{ active: !isDark }" @click="setTheme('light')">☀️</button>
      <button class="theme-btn" :class="{ active: isDark }" @click="setTheme('dark')">🌙</button>
    </div>

    <nav class="nav-section">
      <div class="nav-label">发现</div>
      <a class="nav-item" :class="{ active: !plStore.activePlaylistId }" @click.prevent="navTo(null)">
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z"/></svg>
        <span>全部歌曲</span>
      </a>
      <a class="nav-item" :class="{ active: plStore.activePlaylistId === 'community' }" @click.prevent="navToCustom('community')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <path stroke-linecap="round" stroke-linejoin="round" d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path stroke-linecap="round" stroke-linejoin="round" d="M23 21v-2a4 4 0 00-3-3.87"></path>
          <path stroke-linecap="round" stroke-linejoin="round" d="M16 3.13a4 4 0 010 7.75"></path>
        </svg>
        <span>社区</span>
      </a>
    </nav>

    <nav class="nav-section">
      <div class="nav-label">我的</div>
      <a class="nav-item" :class="{ active: plStore.activePlaylistId === 'favorites' }" @click.prevent="navTo('favorites')">
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18" class="heart-icon"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
        <span>我喜欢</span>
        <span class="nav-badge" v-if="plStore.favorites?.songIds.length">{{ plStore.favorites.songIds.length }}</span>
      </a>
      <a class="nav-item" :class="{ active: plStore.activePlaylistId === 'recent' }" @click.prevent="navToCustom('recent')">
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
        <span>最近</span>
      </a>
    </nav>

    <nav class="nav-section">
      <div class="nav-label">
        歌单
        <div class="add-pl-wrap" @click.stop>
          <button class="add-pl-btn liquid-btn" @click="addMenuOpen = !addMenuOpen">
            <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          </button>
          <Transition name="dropdown">
            <div v-if="addMenuOpen" class="add-dropdown liquid-panel">
              <div class="add-dropdown-item" @click="openCreateModal">
                创建歌单
              </div>
              <div class="add-dropdown-item disabled">
                导入歌单
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <a v-for="pl in plStore.userPlaylists" :key="pl.id" class="nav-item pl-item" :class="{ active: plStore.activePlaylistId === pl.id }" @click.prevent="navTo(pl.id)" @contextmenu.prevent="openPlMenu($event, pl)">
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>
        <span class="pl-name">{{ pl.name }}</span>
        <span class="nav-badge" v-if="pl.songIds.length">{{ pl.songIds.length }}</span>
      </a>

      <div v-if="!plStore.userPlaylists.length" class="empty-pl">暂无歌单</div>
    </nav>

    <div class="sidebar-spacer" />

    <Teleport to="body">
      <div v-if="menuVisible" class="ctx-menu liquid-panel" :style="{ top: menuY + 'px', left: menuX + 'px' }" @click="menuVisible = false">
        <div class="ctx-item" @click="startRename">重命名</div>
        <div class="ctx-item ctx-danger" @click="deletePl">删除歌单</div>
      </div>
      <div v-if="menuVisible" class="ctx-backdrop" @click="menuVisible = false" />
    </Teleport>
  </aside>
</template>

<script setup>
const plStore = usePlaylistStore()
const props = defineProps({ open: Boolean })
const emit = defineEmits(['close', 'openCreateModal'])

const isDark = ref(true)
function setTheme(mode) {
  isDark.value = mode === 'dark'
  document.documentElement.classList.toggle('light-mode', !isDark.value)
}

function navTo(id) {
  plStore.setActivePlaylist(id)
  setTimeout(() => emit('close'), 200)
}

// 专门处理特殊路由的跳转函数（避开原有的 store 校验逻辑）   最近 社区
function navToCustom(id) {
  plStore.activePlaylistId = id
  setTimeout(() => emit('close'), 200)
}

const addMenuOpen = ref(false)
function openCreateModal() {
  addMenuOpen.value = false
  emit('openCreateModal')
  emit('close')
}

let touchStartX = 0
function onTouchStart(e) { touchStartX = e.touches[0].clientX }
function onTouchEnd(e) {
  if (touchStartX - e.changedTouches[0].clientX > 50) emit('close')
}

const menuVisible = ref(false)
const menuX = ref(0)
const menuY = ref(0)
const menuTarget = ref(null)

function openPlMenu(e, pl) { menuTarget.value = pl; menuX.value = e.clientX; menuY.value = e.clientY; menuVisible.value = true }
function startRename() { const pl = menuTarget.value; if (!pl) return; const name = prompt('重命名歌单', pl.name); if (name?.trim()) plStore.renamePlaylist(pl.id, name.trim()) }
function deletePl() { const pl = menuTarget.value; if (!pl) return; if (confirm(`确定删除歌单「${pl.name}」吗？`)) plStore.deletePlaylist(pl.id) }
</script>

<style scoped>
.sidebar {
  width: 240px; /* 稍微加宽一点，给现代UI留白空间 */
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: 0 0 12px;
  user-select: none;
  overflow-y: auto;
  scrollbar-width: none;
  border-radius: 0 2rem 2rem 0;
  border-right: none;
  z-index: 10;
}
.sidebar::-webkit-scrollbar { display: none; }

.sidebar-logo { display: flex; align-items: center; gap: 12px; padding: 24px 24px 20px; }
.logo-icon { font-size: 26px; color: var(--accent); filter: drop-shadow(0 0 8px rgba(250, 45, 72, 0.4)); }
.logo-text { font-size: 20px; font-weight: 800; letter-spacing: -0.04em; background: linear-gradient(to right, #fff, #aaa); -webkit-background-clip: text; color: transparent; }
.light-mode .logo-text { background: linear-gradient(to right, #111, #666); -webkit-background-clip: text; }

.theme-toggle { display: flex; margin: 0 20px 20px; padding: 4px; border-radius: 20px; }
.theme-btn { flex: 1; display: flex; align-items: center; justify-content: center; padding: 6px 0; border-radius: 16px; border: none; background: transparent; font-size: 14px; cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); color: var(--text-tertiary); }
.theme-btn.active { background: rgba(255, 255, 255, 0.15); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2), inset 0 1px 1px rgba(255, 255, 255, 0.2); color: var(--text-primary); }
.light-mode .theme-btn.active { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

.nav-section { padding: 0 16px; margin-bottom: 8px; overflow: visible; }
.nav-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  opacity: 0.9;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 16px 12px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 20;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  margin-bottom: 2px;
  border-radius: 14px;
  /* 从 --text-secondary 提升到 --text-primary，但用 opacity 压回一点柔和感 */
  color: var(--text-primary);
  opacity: 0.85;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  border: 1px solid transparent;
}

/* 悬浮时恢复 100% 不透明度 */
.nav-item:hover:not(.active) {
  background: rgba(255, 255, 255, 0.08);
  opacity: 1;
  transform: translateX(4px);
  border-color: rgba(255,255,255,0.05);
}


/* 核心灵魂：Liquid UI 动态流体高亮态 */
.nav-item.active {
  color: #fff !important;
  font-weight: 600;
  /* 1. 绝对无色：仅使用微弱的纯白反光作为背景和边框 */
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  /* 2. 极致模糊：拉高模糊度，让它真正具备“隔绝感” */
  backdrop-filter: blur(32px) saturate(150%);
  -webkit-backdrop-filter: blur(32px) saturate(150%);
  /* 3. 干净的物理光影：只保留高光折射和底部的柔和阴影 */
  box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.05),     /* 最内层的微光 */
      inset 1.5px 1.5px 5px rgba(255, 255, 255, 0.1), /* 左上角的玻璃厚度反光 */
      0 6px 16px rgba(0, 0, 0, 0.2);                  /* 悬浮在背景之上的阴影 */
}
/* 补充：为了防止白天模式下白色玻璃看不清，适配一下白天模式 */
.light-mode .nav-item.active {
  color: #111 !important;
  background: rgba(0, 0, 0, 0.03); /* 白天使用极淡的黑色玻璃 */
  border-color: rgba(0, 0, 0, 0.06);
  box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.6),
      inset 1.5px 1.5px 5px rgba(255, 255, 255, 0.4),
      0 4px 12px rgba(0, 0, 0, 0.05);
}

@keyframes flow-gradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.nav-item svg { flex-shrink: 0; opacity: 0.7; transition: transform 0.3s ease; }
.nav-item.active svg, .nav-item:hover svg { opacity: 1; transform: scale(1.1); }
.heart-icon { color: var(--accent); opacity: 1 !important; filter: drop-shadow(0 2px 4px rgba(250, 45, 72, 0.3)); }
.nav-badge { margin-left: auto; font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.8); background: rgba(0,0,0,0.2); padding: 2px 8px; border-radius: 10px; }

.pl-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.empty-pl { padding: 12px 14px; font-size: 13px; color: var(--text-tertiary); font-style: italic; }
.sidebar-spacer { flex: 1; }

.add-pl-wrap { position: relative;z-index: 50; }
.add-pl-btn { width: 24px; height: 24px; padding: 0; justify-content: center; border-radius: 6px; }

/* 💡 删繁就简：去除多余滤镜，使用高不透明度底色，确保文字清晰 */
.add-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 150px;
  padding: 6px;
  border-radius: 12px;
  z-index: 100;

  /* 纯净暗色底色（几乎不透明），不加额外滤镜干扰 */
  background: rgba(30, 30, 30, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.add-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: #ffffff; /* 明确给白色 */
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.add-dropdown-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 🌙 完美适配白天模式：变成干净的白底 */
.light-mode .add-dropdown {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.light-mode .add-dropdown-item {
  color: #111111; /* 白天模式下文字为深色 */
}

.light-mode .add-dropdown-item:hover {
  background: rgba(0, 0, 0, 0.05); /* 白天模式下的悬浮灰色背景 */
}

.dropdown-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.dropdown-leave-active { transition: all 0.2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-10px) scale(0.95); }

.ctx-backdrop { position: fixed; inset: 0; z-index: 9998; }
.ctx-menu { position: fixed; z-index: 9999; border-radius: 12px; padding: 6px; min-width: 140px; }
.ctx-item { padding: 8px 14px; font-size: 14px; font-weight: 500; border-radius: 6px; cursor: pointer; transition: all 0.2s ease; }
.ctx-item:hover { background: rgba(255, 255, 255, 0.1); transform: translateX(2px); }
.ctx-danger { color: #ff4d4f; }
.ctx-danger:hover { background: rgba(255, 77, 79, 0.15); }

@media (max-width: 768px) {
  .sidebar { position: fixed; top: 0; left: 0; width: 280px; z-index: 50; transform: translateX(-110%); transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1); box-shadow: 10px 0 40px rgba(0, 0, 0, 0.5); }
  .sidebar.sidebar-open { transform: translateX(0); }
}
</style>