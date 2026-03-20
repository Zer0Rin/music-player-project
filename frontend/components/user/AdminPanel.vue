<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="admin-modal">
        <div class="admin-panel liquid-panel">

          <!-- 顶部栏 -->
          <div class="admin-topbar">
            <div class="admin-tabs">
              <button :class="['admin-tab', { active: activeTab === 'songs' }]" @click="activeTab = 'songs'">
                歌曲
                <span class="tab-count">{{ songs.length }}</span>
              </button>
              <button :class="['admin-tab', { active: activeTab === 'community' }]" @click="activeTab = 'community'">
                社区
              </button>
            </div>
            <div class="admin-topbar-right">
              <button class="upload-toggle-btn liquid-btn" @click="showUpload = !showUpload">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                  <path d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                </svg>
                上传歌曲
              </button>
              <button class="close-btn" @click="$emit('close')">
                <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 上传区（折叠） -->
          <Transition name="slide-down">
            <div v-if="showUpload" class="upload-zone liquid-card"
                 :class="{ dragging: isDragging }"
                 @dragover.prevent="isDragging = true"
                 @dragleave="isDragging = false"
                 @drop.prevent="onDrop"
                 @click="triggerFileInput">
              <div v-if="!pendingFiles.length" class="drop-placeholder">
                <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32" style="opacity:0.4">
                  <path d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                </svg>
                <span>拖拽或点击上传音频文件</span>
                <span class="drop-hint">MP3 / FLAC / WAV / OGG / M4A</span>
              </div>
              <div v-else class="pending-files" @click.stop>
                <div v-for="(f, i) in pendingFiles" :key="i" class="pending-item">
                  <span class="pending-name">{{ f.name }}</span>
                  <span class="pending-size">{{ formatSize(f.size) }}</span>
                  <span v-if="fileStatus[f.name] === 'ok'" class="status-ok">✓</span>
                  <span v-else-if="fileStatus[f.name] === 'error'" class="status-err">✗</span>
                  <div v-else-if="uploading" class="status-spinner" />
                  <button v-if="!uploading && !fileStatus[f.name]" class="remove-btn" @click="pendingFiles.splice(i,1)">×</button>
                </div>
                <div class="pending-actions" @click.stop>
                  <button class="add-more-btn" @click="triggerFileInput">+ 添加更多</button>
                  <div class="progress-wrap" v-if="uploading || uploadDone">
                    <div class="progress-bar"><div class="progress-fill" :style="{ width: progress + '%' }" /></div>
                    <span class="progress-text">{{ uploadDone ? '完成' : `${uploadedCount}/${pendingFiles.length}` }}</span>
                  </div>
                  <button v-if="!uploading && !uploadDone" class="do-upload-btn liquid-btn" @click="startUpload">
                    上传 {{ pendingFiles.length }} 个文件
                  </button>
                  <button v-if="uploadDone" class="do-upload-btn liquid-btn" @click="resetUpload">继续上传</button>
                </div>
              </div>
            </div>
          </Transition>

          <!-- 主体：列表 + 详情 -->
          <div class="admin-body">

            <!-- 歌曲列表 -->
            <div class="song-list-panel" v-show="activeTab === 'songs'">

              <!-- 搜索栏 -->
              <div class="admin-search-bar">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" class="search-icon">
                  <path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
                </svg>
                <input v-model="searchQuery" type="text" placeholder="搜索歌曲标题、艺术家或专辑..." class="admin-search-input" />
                <button v-if="searchQuery" class="clear-search-btn" @click="searchQuery = ''">×</button>
              </div>

              <div class="list-header">
                <span class="col-num">#</span>
                <span class="col-cover" />
                <span class="col-title">标题</span>
                <span class="col-artist">艺术家</span>
                <span class="col-album">专辑</span>
                <span class="col-duration">时长</span>
                <span class="col-actions" />
              </div>
              <div class="list-body">
                <div v-for="(song, i) in filteredAndSortedSongs" :key="song.id"
                     class="song-row"
                     :class="{ selected: selectedSong?.id === song.id }"
                     @click="selectSong(song)">
                  <span class="col-num">{{ i + 1 }}</span>
                  <div class="col-cover">
                    <img v-if="song.coverFile" :src="`/api/songs/${song.id}/cover`" class="row-cover" />
                    <div v-else class="row-cover-placeholder">♫</div>
                  </div>
                  <div class="col-title">
                    <span class="song-title">{{ song.title }}</span>
                  </div>
                  <span class="col-artist">{{ song.artist || '—' }}</span>
                  <span class="col-album">{{ song.album || '—' }}</span>
                  <span class="col-duration">{{ formatDuration(song.duration) }}</span>
                  <div class="col-actions">
                    <button class="delete-btn" @click.stop="deleteSong(song)" title="删除">
                      <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                      </svg>
                    </button>
                  </div>
                </div>

                <div v-if="!filteredAndSortedSongs.length" class="admin-empty-state">
                  未找到相关歌曲
                </div>
              </div>
            </div>

            <!-- 社区占位 -->
            <div v-show="activeTab === 'community'" class="community-placeholder">
              <p>社区管理功能开发中...</p>
            </div>

            <!-- 歌曲详情面板 -->
            <Transition name="slide-in">
              <div v-if="selectedSong" class="detail-panel liquid-card">
                <div class="detail-header">
                  <h3>歌曲详情</h3>
                  <button class="close-detail-btn" @click="selectedSong = null">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/>
                    </svg>
                  </button>
                </div>

                <!-- 封面预览 -->
                <div class="detail-cover">
                  <img v-if="selectedSong.coverFile" :src="`/api/songs/${selectedSong.id}/cover?v=${coverTs}`" class="detail-cover-img" />
                  <div v-else class="detail-cover-placeholder">♫</div>
                </div>

                <!-- 可编辑字段 -->
                <div class="detail-fields">
                  <div class="field-group">
                    <label>标题</label>
                    <input v-model="editForm.title" class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>艺术家</label>
                    <input v-model="editForm.artist" class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>专辑</label>
                    <input v-model="editForm.album" class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>流派</label>
                    <input v-model="editForm.genre" class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>年份</label>
                    <input v-model="editForm.year" class="field-input" />
                  </div>
                </div>

                <!-- 文件状态 -->
                <div class="file-status">
                  <div class="status-row">
                    <span class="status-label">音频文件</span>
                    <span class="status-value">{{ selectedSong.audioFile }}</span>
                  </div>
                  <div class="status-row">
                    <span class="status-label">封面</span>
                    <span :class="['status-badge', selectedSong.coverFile ? 'badge-ok' : 'badge-missing']">
                      {{ selectedSong.coverFile ? '已有' : '缺失' }}
                    </span>
                  </div>
                  <div class="status-row">
                    <span class="status-label">歌词</span>
                    <span :class="['status-badge', selectedSong.lyricsFile ? 'badge-ok' : 'badge-missing']">
                      {{ selectedSong.lyricsFile ? '已有' : '缺失' }}
                    </span>
                  </div>
                </div>


                <!-- 音频预览 -->
                <div class="audio-preview">
                  <audio controls :src="`/api/songs/${selectedSong.id}/audio`" class="native-player" />
                </div>


                <!-- 文件上传 -->
                <div class="file-uploads">
                  <div class="file-upload-row">
                    <span>替换封面</span>
                    <input type="file" accept="image/*" class="hidden-input" :ref="el => fileRefs.cover = el" @change="uploadCover" />
                    <button class="file-upload-btn" @click="fileRefs.cover?.click()">选择图片</button>
                  </div>
                  <div class="file-upload-row">
                    <span>替换歌词</span>
                    <input type="file" accept=".lrc,.elrc" class="hidden-input" :ref="el => fileRefs.lyrics = el" @change="uploadLyrics" />
                    <button class="file-upload-btn" @click="fileRefs.lyrics?.click()">选择歌词</button>
                  </div>
                </div>

                <!-- 保存按钮 -->
                <div class="detail-actions">
                  <p v-if="detailMsg" class="detail-msg" :class="{ error: detailError }">{{ detailMsg }}</p>
                  <button class="save-detail-btn liquid-btn" :disabled="saving" @click="saveDetail">
                    {{ saving ? '保存中...' : '保存修改' }}
                  </button>
                </div>
              </div>
            </Transition>

          </div>
        </div>
        <input ref="fileInput" type="file" multiple accept=".mp3,.flac,.wav,.ogg,.m4a" class="hidden-input" @change="onFileSelect" />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'uploaded'])
const { $apiFetch } = useNuxtApp()

//搜索歌曲
const activeTab = ref('songs')
const songs = ref([])
const selectedSong = ref(null)
const coverTs = ref(Date.now())

// 搜索关键词
const searchQuery = ref('')

// 计算属性（同时处理过滤和默认排序）
const filteredAndSortedSongs = computed(() => {
  let result = songs.value

  // 1. 搜索过滤
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    result = result.filter(song =>
        (song.title && song.title.toLowerCase().includes(q)) ||
        (song.artist && song.artist.toLowerCase().includes(q)) ||
        (song.album && song.album.toLowerCase().includes(q))
    )
  }

  // 2. 默认排序：按标题数字、字母、中文拼音顺序（升序）
  return result.slice().sort((a, b) => {
    const titleA = a.title || ''
    const titleB = b.title || ''
    // localeCompare 是处理字符串排序的神器，{ numeric: true } 能让 "2" 排在 "10" 前面
    return titleA.localeCompare(titleB, 'zh-CN', { numeric: true })
  })
})

// 编辑表单
const editForm = reactive({ title: '', artist: '', album: '', genre: '', year: '' })
const saving = ref(false)
const detailMsg = ref('')
const detailError = ref(false)
const fileRefs = reactive({ cover: null, lyrics: null })

// 上传区
const showUpload = ref(false)
const isDragging = ref(false)
const pendingFiles = ref([])
const uploading = ref(false)
const uploadDone = ref(false)
const uploadedCount = ref(0)
const fileStatus = ref({})
const fileInput = ref(null)

const progress = computed(() =>
    pendingFiles.value.length ? Math.round(uploadedCount.value / pendingFiles.value.length * 100) : 0
)





// 加载歌曲列表
async function loadSongs() {
  songs.value = await $apiFetch('/api/songs')
}

watch(() => props.visible, (v) => { if (v) loadSongs() })

function selectSong(song) {
  selectedSong.value = song
  editForm.title = song.title || ''
  editForm.artist = song.artist || ''
  editForm.album = song.album || ''
  editForm.genre = song.genre || ''
  editForm.year = song.year || ''
  detailMsg.value = ''
}

async function saveDetail() {
  if (!selectedSong.value) return
  saving.value = true
  try {
    const updated = await $apiFetch(`/api/songs/${selectedSong.value.id}`, {
      method: 'PUT',
      body: { ...editForm },
    })
    // 更新列表里的数据
    const idx = songs.value.findIndex(s => s.id === selectedSong.value.id)
    if (idx >= 0) songs.value[idx] = updated
    selectedSong.value = updated
    detailMsg.value = '保存成功 ✓'
    detailError.value = false
  } catch {
    detailMsg.value = '保存失败'
    detailError.value = true
  } finally {
    saving.value = false
    setTimeout(() => detailMsg.value = '', 3000)
  }
}

async function uploadCover(e) {
  const file = e.target.files?.[0]
  if (!file || !selectedSong.value) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    await $apiFetch(`/api/songs/${selectedSong.value.id}/cover/upload`, { method: 'POST', body: formData })
    coverTs.value = Date.now()
    selectedSong.value.coverFile = file.name
    detailMsg.value = '封面更新成功 ✓'
    detailError.value = false
  } catch {
    detailMsg.value = '封面上传失败'
    detailError.value = true
  }
  e.target.value = ''
  setTimeout(() => detailMsg.value = '', 3000)
}

async function uploadLyrics(e) {
  const file = e.target.files?.[0]
  if (!file || !selectedSong.value) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    await $apiFetch(`/api/songs/${selectedSong.value.id}/lyrics/upload`, { method: 'POST', body: formData })
    selectedSong.value.lyricsFile = file.name
    detailMsg.value = '歌词上传成功 ✓'
    detailError.value = false
  } catch {
    detailMsg.value = '歌词上传失败'
    detailError.value = true
  }
  e.target.value = ''
  setTimeout(() => detailMsg.value = '', 3000)
}

async function deleteSong(song) {
  if (!confirm(`确定删除「${song.title}」吗？此操作不可恢复`)) return
  try {
    await $apiFetch(`/api/songs/${song.id}`, { method: 'DELETE' })
    songs.value = songs.value.filter(s => s.id !== song.id)
    if (selectedSong.value?.id === song.id) selectedSong.value = null
    emit('uploaded')
  } catch {
    alert('删除失败')
  }
}

// 上传相关
function triggerFileInput() { fileInput.value?.click() }
function onFileSelect(e) { addFiles(Array.from(e.target.files || [])); e.target.value = '' }
function onDrop(e) { isDragging.value = false; addFiles(Array.from(e.dataTransfer.files)) }
function addFiles(files) {
  const exts = ['.mp3','.flac','.wav','.ogg','.m4a']
  const existing = new Set(pendingFiles.value.map(f => f.name))
  files.filter(f => exts.some(ext => f.name.toLowerCase().endsWith(ext)))
      .forEach(f => { if (!existing.has(f.name)) pendingFiles.value.push(f) })
}

async function startUpload() {
  uploading.value = true
  uploadedCount.value = 0
  fileStatus.value = {}
  for (const file of pendingFiles.value) {
    try {
      const formData = new FormData()
      formData.append('files', file)
      const res = await $apiFetch('/api/songs/upload', { method: 'POST', body: formData })
      fileStatus.value[file.name] = res.results?.[0]?.status === 'ok' ? 'ok' : 'error'
    } catch { fileStatus.value[file.name] = 'error' }
    uploadedCount.value++
  }
  uploading.value = false
  uploadDone.value = true
  await loadSongs()
  emit('uploaded')
}

function resetUpload() { pendingFiles.value = []; fileStatus.value = {}; uploadDone.value = false; uploadedCount.value = 0 }
function formatSize(b) { return b < 1048576 ? (b/1024).toFixed(1)+' KB' : (b/1048576).toFixed(1)+' MB' }
function formatDuration(s) { if (!s) return '--'; const m = Math.floor(s/60); return `${m}:${String(s%60).padStart(2,'0')}` }
</script>

<style scoped>
.admin-modal {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.85);
  display: flex; align-items: stretch; justify-content: center;
}

.admin-panel {
  width: 100%; max-width: 1100px;
  margin: 20px;
  border-radius: 20px;
  display: flex; flex-direction: column;
  overflow: hidden;
}

/* 顶部栏 */
.admin-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 0; flex-shrink: 0;
}
.admin-tabs { display: flex; gap: 4px; }
.admin-tab {
  padding: 8px 20px; border-radius: 10px; border: none;
  background: transparent; color: var(--text-secondary);
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all 0.2s; display: flex; align-items: center; gap: 6px;
}
.admin-tab.active { background: rgba(255,255,255,0.08); color: var(--text-primary); }
.tab-count {
  font-size: 11px; background: rgba(255,255,255,0.1);
  padding: 1px 7px; border-radius: 10px;
}
.admin-topbar-right { display: flex; align-items: center; gap: 8px; }
.upload-toggle-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 10px; border: none;
  background: rgba(139,92,246,0.25); color: #a78bfa;
  font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s;
}
.upload-toggle-btn:hover { background: rgba(139,92,246,0.4); }
.close-btn {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: rgba(255,255,255,0.06); color: var(--text-secondary);
  cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.close-btn:hover { background: rgba(255,255,255,0.12); color: var(--text-primary); }

/* 上传区 */
.upload-zone {
  margin: 12px 24px 0; border-radius: 12px; padding: 16px;
  border: 2px dashed rgba(255,255,255,0.1); cursor: pointer;
  transition: all 0.2s; flex-shrink: 0;
}
.upload-zone.dragging { border-color: rgba(139,92,246,0.5); background: rgba(139,92,246,0.05); }
.drop-placeholder { display: flex; align-items: center; gap: 12px; color: var(--text-tertiary); font-size: 13px; }
.drop-hint { font-size: 11px; opacity: 0.6; }
.pending-files { display: flex; flex-direction: column; gap: 6px; }
.pending-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 8px; background: rgba(255,255,255,0.04); font-size: 12px;
}
.pending-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-size { color: var(--text-tertiary); flex-shrink: 0; }
.status-ok { color: #4ade80; }
.status-err { color: #f87171; }
.status-spinner { width: 12px; height: 12px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.2); border-top-color: #a78bfa; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.remove-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; font-size: 14px; }
.pending-actions { display: flex; align-items: center; gap: 10px; padding-top: 6px; flex-wrap: wrap; }
.add-more-btn { background: none; border: 1px dashed rgba(255,255,255,0.15); color: var(--text-tertiary); font-size: 12px; padding: 4px 10px; border-radius: 6px; cursor: pointer; }
.progress-wrap { display: flex; align-items: center; gap: 8px; flex: 1; }
.progress-bar { flex: 1; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.1); overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(to right, #7c3aed, #a78bfa); transition: width 0.3s; }
.progress-text { font-size: 11px; color: var(--text-tertiary); white-space: nowrap; }
.do-upload-btn { padding: 6px 16px; border-radius: 8px; border: none; background: rgba(139,92,246,0.4); color: white; font-size: 12px; cursor: pointer; }

/* 主体 */
.admin-body {
  display: flex; flex: 1; overflow: hidden;
  padding: 12px 24px 20px; gap: 16px;
}

/* 歌曲列表 */
.song-list-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }
.list-header {
  display: grid;
  grid-template-columns: 36px 44px 1fr 160px 160px 70px 40px;
  padding: 8px 12px; font-size: 11px; font-weight: 700;
  color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.list-body { flex: 1; overflow-y: auto; scrollbar-width: thin; scrollbar-color: rgba(255,255,255,0.1) transparent; }
.song-row {
  display: grid;
  grid-template-columns: 36px 44px 1fr 160px 160px 70px 40px;
  padding: 8px 12px; align-items: center;
  border-radius: 8px; cursor: pointer; transition: background 0.15s;
  font-size: 13px; color: var(--text-secondary);
}
.song-row:hover { background: rgba(255,255,255,0.04); }
.song-row.selected { background: rgba(139,92,246,0.12); }
.col-num { color: var(--text-tertiary); font-size: 12px; }
.row-cover { width: 32px; height: 32px; border-radius: 6px; object-fit: cover; }
.row-cover-placeholder { width: 32px; height: 32px; border-radius: 6px; background: rgba(255,255,255,0.06); display: flex; align-items: center; justify-content: center; font-size: 14px; }
.song-title { color: var(--text-primary); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.col-artist, .col-album { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-duration { color: var(--text-tertiary); font-size: 12px; }
.delete-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; padding: 4px; border-radius: 4px; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.delete-btn:hover { color: #f87171; background: rgba(248,113,113,0.1); }

/* 详情面板 */
.detail-panel {
  width: 280px; flex-shrink: 0; border-radius: 16px;
  padding: 16px; display: flex; flex-direction: column; gap: 14px;
  overflow-y: auto; scrollbar-width: none;
}
.detail-panel::-webkit-scrollbar { display: none; }
.detail-header { display: flex; align-items: center; justify-content: space-between; }
.detail-header h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.close-detail-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; display: flex; }
.detail-cover { width: 100%; aspect-ratio: 1; border-radius: 12px; overflow: hidden; flex-shrink: 0; background: rgba(0, 0, 0, 0.2);}
.detail-cover-img { width: 100%; height: 100%; object-fit: cover; }
.detail-cover-placeholder { width: 100%; height: 100%; background: rgba(255,255,255,0.06); display: flex; align-items: center; justify-content: center; font-size: 48px; }

.detail-fields { display: flex; flex-direction: column; gap: 8px; }
.field-group { display: flex; flex-direction: column; gap: 4px; }
.field-group label { font-size: 11px; color: var(--text-tertiary); }
.field-input {
  padding: 7px 10px; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: var(--text-primary); font-size: 13px; outline: none;
  transition: border-color 0.2s;
}
.field-input:focus { border-color: rgba(139,92,246,0.5); }

.file-status { display: flex; flex-direction: column; gap: 6px; }
.status-row { display: flex; align-items: center; justify-content: space-between; font-size: 12px; }
.status-label { color: var(--text-tertiary); }
.status-value { color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }
.status-badge { padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 600; }
.badge-ok { background: rgba(74,222,128,0.15); color: #4ade80; }
.badge-missing { background: rgba(248,113,113,0.15); color: #f87171; }

.file-uploads { display: flex; flex-direction: column; gap: 8px; }
.file-upload-row { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: var(--text-secondary); }
.file-upload-btn {
  padding: 5px 12px; border-radius: 7px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: var(--text-secondary);
  font-size: 11px; cursor: pointer; transition: all 0.2s;
}
.file-upload-btn:hover { border-color: rgba(255,255,255,0.2); color: var(--text-primary); }

.detail-actions { display: flex; flex-direction: column; gap: 6px; }
.detail-msg { font-size: 12px; color: #4ade80; }
.detail-msg.error { color: #f87171; }
.save-detail-btn { padding: 9px; border-radius: 10px; border: none; background: rgba(139,92,246,0.4); color: white; font-size: 13px; font-weight: 500; cursor: pointer; }
.save-detail-btn:disabled { opacity: 0.5; }

.community-placeholder { flex: 1; display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); }

.hidden-input { display: none; }

/* 动画 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.slide-in-enter-active, .slide-in-leave-active { transition: all 0.3s ease; }
.slide-in-enter-from, .slide-in-leave-to { opacity: 0; transform: translateX(20px); }
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.25s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

/* admin音频预览*/
.audio-preview { width: 100%; }
.native-player { width: 100%; height: 36px; border-radius: 8px; }


/* 搜索栏样式 */
.admin-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.15); /* 稍微深一点的底色区分区域 */
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.search-icon { color: var(--text-tertiary); flex-shrink: 0; }
.admin-search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}
.admin-search-input::placeholder { color: var(--text-tertiary); }
.clear-search-btn {
  background: none;
  border: none;
  color: var(--text-tertiary);
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;
  transition: color 0.2s;
}
.clear-search-btn:hover { color: var(--text-primary); }

.admin-empty-state {
  padding: 40px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 13px;
}

</style>