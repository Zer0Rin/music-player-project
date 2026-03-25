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
                <span class="tab-count">{{ filteredAndSortedSongs.length }}</span>
              </button>
              <button :class="['admin-tab', { active: activeTab === 'community' }]" @click="activeTab = 'community'">
                社区
              </button>
              <button :class="['admin-tab', { active: activeTab === 'users' }]" @click="activeTab = 'users'">
                用户
                <span class="tab-count">{{ users.length }}</span>
              </button>
            </div>
            <div class="admin-topbar-right">

              <!-- 热度刷新 -->
              <button class="upload-toggle-btn liquid-btn" @click="refreshHotScore" :disabled="isRefreshingHot" title="刷新热度分">
                <svg class="w-4 h-4" :style="isRefreshingHot ? 'animation: spin 1s linear infinite' : ''"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                  <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
                {{ isRefreshingHot ? '更新中...' : '热度' }}
              </button>

              <button class="upload-toggle-btn liquid-btn" @click="showUpload = !showUpload">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                  <path d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                </svg>
                上传
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
                <span>拖拽或点击上传文件</span>
                <span class="drop-hint">音频 / 封面图片 / 歌词（文件名需一致才能匹配）</span>
              </div>
              <div class="pending-files" @click.stop>

                <div class="pending-list-scroll custom-scrollbar">
                  <div v-for="(f, i) in pendingFiles" :key="i" class="pending-item">
                    <span class="file-type-tag" :class="'type-' + getFileType(f.name)">{{ getFileType(f.name) }}</span>
                    <span class="pending-name">{{ f.name }}</span>
                    <span class="pending-size">{{ formatSize(f.size) }}</span>
                    <span v-if="fileStatus[f.name] === 'ok'" class="status-ok">✓</span>
                    <span v-else-if="fileStatus[f.name] === 'error'" class="status-err" title="未匹配或上传失败">✗</span>
                    <div v-else-if="uploading" class="status-spinner" />
                    <button v-if="!uploading && !fileStatus[f.name]" class="remove-btn" @click="removeFile(f)">×</button>
                  </div>
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

                <div class="sort-toggle">
                  <button class="sort-btn" :class="{ active: sortMode === 'newest' }" @click="sortMode = 'newest'" title="最新上传优先">时间</button>
                  <button class="sort-btn" :class="{ active: sortMode === 'default' }" @click="sortMode = 'default'" title="按标题字母排序">A-Z</button>
                </div>
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
                    <span v-if="!song.coverFile || !song.lyricsFile" class="missing-badge" :title="getMissingTip(song)">!</span>
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

                <!-- 删除确认弹窗，放在列表外面 -->
                <Teleport to="body">
                  <Transition name="modal-fade">
                    <div v-if="showDeleteConfirm" class="delete-backdrop" @click.self="showDeleteConfirm = false">
                      <div class="delete-modal liquid-panel">
                        <h3 class="delete-title">{{ deleteTarget?._type === 'user' ? '删除用户' : '删除歌曲' }}</h3>
                        <p class="delete-desc">确定删除「<span class="delete-name">{{ deleteTarget?._type === 'user' ? (deleteTarget?.nickname || deleteTarget?.username) : deleteTarget?.title }}</span>」吗？此操作不可恢复。</p>
                        <div class="delete-actions">
                          <button class="cancel-btn" @click="showDeleteConfirm = false">取消</button>
                          <button class="confirm-btn" @click="confirmDelete">删除</button>
                        </div>
                      </div>
                    </div>
                  </Transition>
                </Teleport>

                <div v-if="!filteredAndSortedSongs.length" class="admin-empty-state">
                  未找到相关歌曲
                </div>
              </div>
            </div>



            <!-- 社区占位 -->
            <div v-show="activeTab === 'community'" class="community-placeholder">
              <p>社区管理功能开发中...</p>
            </div>



            <!-- 用户管理 -->
            <div v-show="activeTab === 'users'" class="song-list-panel">
              <div class="list-header" style="grid-template-columns: 44px 1fr 120px 100px 120px 40px;">
                <span />
                <span>用户</span>
                <span>角色</span>
                <span>注册时间</span>
                <span>用户名</span>
                <span />
              </div>
              <div class="list-body">
                <div v-for="user in users" :key="user.id"
                     class="song-row"
                     style="grid-template-columns: 44px 1fr 120px 100px 120px 40px;"
                     :class="{ selected: selectedUser?.id === user.id }"
                     @click="selectUser(user)">
                  <div class="col-cover">
                    <img v-if="user.avatarFile" :src="`/api/user/avatar/${user.avatarFile}`" class="row-cover" />
                    <div v-else class="row-cover-placeholder" style="border-radius:50%">
                      {{ user.nickname?.[0] || 'U' }}
                    </div>
                  </div>
                  <div class="col-title">
                    <span class="song-title">{{ user.nickname || user.username }}</span>
                  </div>
                  <span>
        <span :class="['role-badge', user.role === 'ADMIN' ? 'badge-admin' : 'badge-user']">
          {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
        </span>
      </span>
                  <span class="col-duration">{{ formatUserDate(user.createdAt) }}</span>
                  <span class="col-artist">{{ user.username }}</span>
                  <div class="col-actions">
                    <button class="delete-btn" @click.stop="deleteUser(user)" title="删除用户">
                      <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 用户详情面板 -->
            <Transition name="slide-in">
              <div v-if="selectedUser && activeTab === 'users'" class="detail-panel liquid-card">
                <div class="detail-header">
                  <h3>用户详情</h3>
                  <button class="close-detail-btn" @click="selectedUser = null">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                    </svg>
                  </button>
                </div>

                <!-- 头像 -->
                <div class="detail-cover">
                  <img v-if="selectedUser.avatarFile" :src="`/api/user/avatar/${selectedUser.avatarFile}`" style="border-radius:50%; width:100%; height:100%; object-fit:cover;" />
                  <div v-else class="detail-cover-placeholder" style="font-size:64px;">
                    {{ selectedUser.nickname?.[0] || 'U' }}
                  </div>
                </div>

                <!-- 用户信息 -->
                <div class="detail-fields">
                  <div class="field-group">
                    <label>用户名</label>
                    <input :value="selectedUser.username" disabled class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>昵称</label>
                    <input :value="selectedUser.nickname" disabled class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>注册时间</label>
                    <input :value="formatUserDate(selectedUser.createdAt)" disabled class="field-input" />
                  </div>
                  <div class="field-group">
                    <label>角色</label>
                    <select v-model="selectedUserRole" class="field-input" @change="updateUserRole">
                      <option value="USER">普通用户</option>
                      <option value="ADMIN">管理员</option>
                    </select>
                  </div>
                </div>

                <div class="detail-actions">
                  <p v-if="userMsg" class="detail-msg" :class="{ error: userMsgError }">{{ userMsg }}</p>
                  <button class="save-detail-btn liquid-btn" style="background:rgba(248,113,113,0.3);color:#f87171;"
                          @click="deleteUser(selectedUser)">
                    删除用户
                  </button>
                </div>
              </div>
            </Transition>



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

                  <div class="file-upload-row"
                       @dragover.prevent="isDraggingCover = true"
                       @dragleave.prevent="isDraggingCover = false"
                       @drop.prevent="onDropCover">
                    <span>替换封面</span>
                    <input type="file" accept="image/*" class="hidden-input" :ref="el => fileRefs.cover = el" @change="uploadCover" />
                    <button class="file-upload-btn" @click="fileRefs.cover?.click()">选择图片</button>

                    <div v-show="isDraggingCover" class="drag-overlay">
                      松开鼠标上传封面...
                    </div>
                  </div>

                  <div class="file-upload-row"
                       @dragover.prevent="isDraggingLyrics = true"
                       @dragleave.prevent="isDraggingLyrics = false"
                       @drop.prevent="onDropLyrics">
                    <span>替换歌词</span>
                    <input type="file" accept=".lrc,.elrc" class="hidden-input" :ref="el => fileRefs.lyrics = el" @change="uploadLyrics" />
                    <button class="file-upload-btn" @click="fileRefs.lyrics?.click()">选择歌词</button>

                    <div v-show="isDraggingLyrics" class="drag-overlay">
                      松开鼠标上传歌词...
                    </div>
                  </div>

                </div>

                <!-- 评论管理 -->
                <button class="comment-manage-btn" @click="showCommentManager = true">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                    <path d="M21 6.5A2.5 2.5 0 0 0 18.5 4h-13A2.5 2.5 0 0 0 3 6.5v8A2.5 2.5 0 0 0 5.5 17H7v3l4-3h7.5a2.5 2.5 0 0 0 2.5-2.5v-8z"/>
                  </svg>
                  管理评论
                </button>

                <SongCommentManager :visible="showCommentManager" :song="selectedSong" @close="showCommentManager = false" />

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
        <input ref="fileInput" type="file" multiple
               accept=".mp3,.flac,.wav,.ogg,.m4a,.jpg,.jpeg,.png,.webp,.lrc,.elrc"
               class="hidden-input" @change="onFileSelect" />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'uploaded', 'hotRefreshed'])
const { $apiFetch } = useNuxtApp()

//管理 删除 歌单
const usePlStore = () => usePlaylistStore()

//搜索歌曲
const activeTab = ref('songs')
const songs = ref([])
const selectedSong = ref(null)
const coverTs = ref(Date.now())

// 搜索关键词
const searchQuery = ref('')

// 排序模式状态，默认设为 'newest' (最新上传)
const sortMode = ref('newest')

// 计算属性（同时处理过滤和动态排序）
const filteredAndSortedSongs = computed(() => {
  // 给每首歌打上一个原始的排序序号（_index）
  // 后端默认返回的列表通常是“旧的在前，新的在后”，这个序号可以作为终极兜底
  let result = songs.value.map((song, index) => {
    return { ...song, _index: index }
  })

  // 1. 搜索过滤
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    result = result.filter(song =>
        (song.title && song.title.toLowerCase().includes(q)) ||
        (song.artist && song.artist.toLowerCase().includes(q)) ||
        (song.album && song.album.toLowerCase().includes(q))
    )
  }

  // 2. 根据用户选择的模式排序
  return result.sort((a, b) => {
    if (sortMode.value === 'newest') {

      // 方案 A：如果后端实体类里写了时间字段 (如 createdAt, uploadTime)，这是最准的，优先用它倒序
      const timeA = a.createdAt || a.createTime || a.uploadTime
      const timeB = b.createdAt || b.createTime || b.uploadTime
      if (timeA && timeB) {
        return new Date(timeB).getTime() - new Date(timeA).getTime()
      }

      // 方案 B：如果你的后端 id 确实是纯数字自增，按数字倒序
      if (!isNaN(a.id) && !isNaN(b.id)) {
        return Number(b.id) - Number(a.id)
      }

      // 方案 C（终极兜底）：如果 id 是乱码 UUID 且没有时间字段，
      // 我们直接根据刚才打上的 `_index` 原始序号反转，确保最后拿到的新歌在最上面！
      return b._index - a._index

    } else {
      // 模式 B：A-Z 拼音字母顺序
      const titleA = a.title || ''
      const titleB = b.title || ''
      return titleA.localeCompare(titleB, 'zh-CN', { numeric: true })
    }
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

// 💡 新增：拖拽状态控制
const isDraggingCover = ref(false)
const isDraggingLyrics = ref(false)

// --- 封面处理逻辑 ---
async function processCoverFile(file) {
  if (!file || !selectedSong.value) return

  // 拦截非图片文件 (允许 jpg, png, webp, gif 等)
  if (!file.type.startsWith('image/')) {
    detailMsg.value = '格式错误：请拖入图片文件'
    detailError.value = true
    setTimeout(() => detailMsg.value = '', 3000)
    return // 拦截，终止上传！
  }

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
  setTimeout(() => detailMsg.value = '', 3000)
}

function uploadCover(e) {
  processCoverFile(e.target.files?.[0])
  e.target.value = '' // 清空 input
}

function onDropCover(e) {
  isDraggingCover.value = false
  processCoverFile(e.dataTransfer.files?.[0])
}


// --- 歌词处理逻辑 ---
async function processLyricsFile(file) {
  if (!file || !selectedSong.value) return

  // 精准拦截非歌词文件 (仅放行 .lrc 和 .elrc)
  const fileName = file.name.toLowerCase()
  if (!fileName.endsWith('.lrc') && !fileName.endsWith('.elrc')) {
    detailMsg.value = '格式错误：仅支持 .lrc 或 .elrc 文件'
    detailError.value = true
    setTimeout(() => detailMsg.value = '', 3000)
    return // 拦截，终止上传！
  }

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
  setTimeout(() => detailMsg.value = '', 3000)
}

function uploadLyrics(e) {
  processLyricsFile(e.target.files?.[0])
  e.target.value = '' // 清空 input
}

function onDropLyrics(e) {
  isDraggingLyrics.value = false
  processLyricsFile(e.dataTransfer.files?.[0])
}

const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

function deleteSong(song) {
  deleteTarget.value = song
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    if (deleteTarget.value._type === 'user') {
      await $apiFetch(`/api/user/${deleteTarget.value.id}`, { method: 'DELETE' })
      users.value = users.value.filter(u => u.id !== deleteTarget.value.id)
      if (selectedUser.value?.id === deleteTarget.value.id) selectedUser.value = null
    } else {
      await $apiFetch(`/api/songs/${deleteTarget.value.id}`, { method: 'DELETE' })
      songs.value = songs.value.filter(s => s.id !== deleteTarget.value.id)
      if (selectedSong.value?.id === deleteTarget.value.id) selectedSong.value = null
      emit('uploaded')
      const plStore = usePlaylistStore()
      await plStore.fetchPlaylists()
    }
  } catch {
  } finally {
    showDeleteConfirm.value = false
    deleteTarget.value = null
  }
}

// 上传相关
function triggerFileInput() { fileInput.value?.click() }
function onFileSelect(e) { addFiles(Array.from(e.target.files || [])); e.target.value = '' }
function onDrop(e) { isDragging.value = false; addFiles(Array.from(e.dataTransfer.files)) }


const pendingAudio = ref([])
const pendingCovers = ref([])
const pendingLyrics = ref([])

function getFileType(filename) {
  const name = filename.toLowerCase()
  if (['.mp3','.flac','.wav','.ogg','.m4a'].some(e => name.endsWith(e))) return '音频'
  if (['.jpg','.jpeg','.png','.webp'].some(e => name.endsWith(e))) return '封面'
  if (['.lrc','.elrc'].some(e => name.endsWith(e))) return '歌词'
  return null
}

function getBaseName(filename) {
  return filename.substring(0, filename.lastIndexOf('.'))
}

function addFiles(files) {
  files.forEach(f => {
    const type = getFileType(f.name)
    if (!type) return
    if (type === '音频' && !pendingAudio.value.find(x => x.name === f.name)) pendingAudio.value.push(f)
    if (type === '封面' && !pendingCovers.value.find(x => x.name === f.name)) pendingCovers.value.push(f)
    if (type === '歌词' && !pendingLyrics.value.find(x => x.name === f.name)) pendingLyrics.value.push(f)
  })
  syncPendingFiles()
}

function syncPendingFiles() {
  pendingFiles.value = [...pendingAudio.value, ...pendingCovers.value, ...pendingLyrics.value]
}





async function startUpload() {
  uploading.value = true
  uploadedCount.value = 0
  fileStatus.value = {}

  // 上传音频
  for (const file of pendingAudio.value) {
    try {
      const formData = new FormData()
      formData.append('files', file)
      const res = await $apiFetch('/api/songs/upload', { method: 'POST', body: formData })
      fileStatus.value[file.name] = res.results?.[0]?.status === 'ok' ? 'ok' : 'error'
    } catch { fileStatus.value[file.name] = 'error' }
    uploadedCount.value++
  }

  // 重新加载确保新歌曲入库
  if (pendingAudio.value.length > 0) await loadSongs()

  // 上传封面（文件名匹配）
  for (const file of pendingCovers.value) {
    const base = getBaseName(file.name)
    const song = songs.value.find(s => getBaseName(s.audioFile) === base)
    if (!song) { fileStatus.value[file.name] = 'error'; uploadedCount.value++; continue }
    try {
      const formData = new FormData()
      formData.append('file', file)
      await $apiFetch(`/api/songs/${song.id}/cover/upload`, { method: 'POST', body: formData })
      fileStatus.value[file.name] = 'ok'
    } catch { fileStatus.value[file.name] = 'error' }
    uploadedCount.value++
  }

  // 上传歌词（文件名匹配）
  for (const file of pendingLyrics.value) {
    const base = getBaseName(file.name)
    const song = songs.value.find(s => getBaseName(s.audioFile) === base)
    if (!song) { fileStatus.value[file.name] = 'error'; uploadedCount.value++; continue }
    try {
      const formData = new FormData()
      formData.append('file', file)
      await $apiFetch(`/api/songs/${song.id}/lyrics/upload`, { method: 'POST', body: formData })
      fileStatus.value[file.name] = 'ok'
    } catch { fileStatus.value[file.name] = 'error' }
    uploadedCount.value++
  }

  uploading.value = false
  uploadDone.value = true
  await loadSongs()
  emit('uploaded')
}

function resetUpload() {
  pendingFiles.value = []
  pendingAudio.value = []
  pendingCovers.value = []
  pendingLyrics.value = []
  fileStatus.value = {}
  uploadDone.value = false
  uploadedCount.value = 0
}
function formatSize(b) { return b < 1048576 ? (b/1024).toFixed(1)+' KB' : (b/1048576).toFixed(1)+' MB' }
function formatDuration(s) { if (!s) return '--'; const m = Math.floor(s/60); return `${m}:${String(s%60).padStart(2,'0')}` }


// 歌名后 ！
function getMissingTip(song) {
  const missing = []
  if (!song.coverFile) missing.push('封面')
  if (!song.lyricsFile) missing.push('歌词')
  return `缺少：${missing.join('、')}`
}


// 上传栏
function removeFile(file) {
  pendingAudio.value = pendingAudio.value.filter(f => f.name !== file.name)
  pendingCovers.value = pendingCovers.value.filter(f => f.name !== file.name)
  pendingLyrics.value = pendingLyrics.value.filter(f => f.name !== file.name)
  syncPendingFiles()
}


//  评论管理
import SongCommentManager from '~/components/user/SongCommentManager.vue'
const showCommentManager = ref(false)
//  评论管理  切换歌曲时重置
watch(selectedSong, () => {
  showCommentManager.value = false
})



// 用户 管理
const users = ref([])
const selectedUser = ref(null)
const selectedUserRole = ref('USER')
const userMsg = ref('')
const userMsgError = ref(false)

// 加载用户
async function loadUsers() {
  users.value = await $apiFetch('/api/user/all')
}

// 监听 tab 切换
watch(activeTab, (tab) => {
  if (tab === 'users') loadUsers()
})

function selectUser(user) {
  selectedUser.value = user
  selectedUserRole.value = user.role
  userMsg.value = ''
}

async function updateUserRole() {
  try {
    await $apiFetch(`/api/user/${selectedUser.value.id}/role`, {
      method: 'PUT',
      body: { role: selectedUserRole.value },
    })
    const idx = users.value.findIndex(u => u.id === selectedUser.value.id)
    if (idx >= 0) users.value[idx].role = selectedUserRole.value
    selectedUser.value.role = selectedUserRole.value
    userMsg.value = '角色更新成功 ✓'
    userMsgError.value = false
  } catch (e) {
    userMsg.value = e?.data?.message || '更新失败'
    userMsgError.value = true
  }
  setTimeout(() => userMsg.value = '', 3000)
}

async function deleteUser(user) {
  deleteTarget.value = { ...user, _type: 'user' }
  showDeleteConfirm.value = true
}

// 用户 管理
function formatUserDate(ts) {
  if (!ts) return '未知'
  return new Date(ts).toLocaleDateString('zh-CN', { year: 'numeric', month: 'numeric', day: 'numeric' })
}



// 热度刷新
const isRefreshingHot = ref(false)

async function refreshHotScore() {
  if (isRefreshingHot.value) return
  isRefreshingHot.value = true
  try {
    await $apiFetch('/api/hot/refresh', { method: 'POST' })
    emit('hotRefreshed') // 加这行
  } catch (e) {
    console.error('热度刷新失败', e)
  } finally {
    isRefreshingHot.value = false
  }
}


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
.file-upload-row {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--text-secondary);
  padding: 8px 10px;
  border-radius: 8px;
  background: transparent;

  /* 为遮罩层提供定位参考，并切掉溢出的圆角 */
  position: relative;
  overflow: hidden;
}
.drag-overlay {
  position: absolute;
  inset: 0; /* 铺满整行 */
  background: rgba(139, 92, 246, 0.15); /* 紫色半透明背景 */
  backdrop-filter: blur(2px); /* 微微模糊底下的原文字和按钮，质感拉满 */
  border: 1px dashed rgba(139, 92, 246, 0.6);
  border-radius: 8px;
  color: #a78bfa;
  font-weight: 600;
  font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  z-index: 10;

  /* 让鼠标事件穿透遮罩层，杜绝疯狂抖动 */
  pointer-events: none;
}

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


/* 排序切换器 */
.sort-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 3px;
  margin-left: 12px;
  flex-shrink: 0;
}
.sort-btn {
  background: transparent; border: none;
  color: var(--text-tertiary); font-size: 12px; font-weight: 500;
  padding: 4px 12px; border-radius: 6px;
  cursor: pointer; transition: all 0.2s;
}
.sort-btn.active {
  background: rgba(139, 92, 246, 0.4);
  color: white; font-weight: 600;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.2);
}
.sort-btn:hover:not(.active) { color: var(--text-primary); }

/* 文件列表滚动区样式 */
.pending-list-scroll {
  max-height: 220px; /* 限制最大高度，大概能显示 6-7 个文件 */
  overflow-y: auto;  /* 超出部分自动显示滚动条 */
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 6px; /* 给滚动条留出一点呼吸空间，不挤压文字 */
}

/* 确保整体布局被压缩时，actions 不会变形 */
.pending-actions {
  display: flex; align-items: center; gap: 10px;
  padding-top: 10px;
  flex-wrap: wrap;
  flex-shrink: 0; /* 防止按钮区被列表挤压变形 */
  border-top: 1px solid rgba(255,255,255,0.05); /* 加一条极淡的分割线更好看 */
}

/* 白天模式下，加深一下分割线 */
:global(.light-mode) .pending-actions {
  border-top-color: rgba(0,0,0,0.05);
}

/* 歌名后 ！ 提示 */
.missing-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px; height: 16px;
  border-radius: 50%;
  background: rgba(251,191,36,0.2);
  color: #fbbf24;
  font-size: 11px;
  font-weight: 700;
  margin-left: 6px;
  flex-shrink: 0;
  cursor: help;
}


/* 上传栏 */
.file-type-tag {
  font-size: 10px; font-weight: 700;
  padding: 1px 6px; border-radius: 4px; flex-shrink: 0;
}
.type-音频 { background: rgba(139,92,246,0.2); color: #a78bfa; }
.type-封面 { background: rgba(59,130,246,0.2); color: #60a5fa; }
.type-歌词 { background: rgba(16,185,129,0.2); color: #34d399; }


/* 删除歌曲 弹窗 */
.delete-backdrop {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
}
.delete-modal {
  width: 320px; border-radius: 16px; padding: 24px;
  display: flex; flex-direction: column; gap: 16px;
}
.delete-title { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.delete-desc { font-size: 14px; color: var(--text-secondary); line-height: 1.5; }
.delete-name { color: var(--text-primary); font-weight: 600; }
.delete-actions { display: flex; gap: 10px; justify-content: flex-end; }
.cancel-btn {
  padding: 8px 20px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.1);
  background: transparent; color: var(--text-secondary);
  font-size: 14px; cursor: pointer; transition: all 0.2s;
}
.cancel-btn:hover { background: rgba(255,255,255,0.06); }
.confirm-btn {
  padding: 8px 20px; border-radius: 10px; border: none;
  background: rgba(248,113,113,0.3); color: #f87171;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.confirm-btn:hover { background: rgba(248,113,113,0.5); }


/* 评论管理 */
/* =========================================
   💬 评论管理按钮专属样式
   ========================================= */
.comment-manage-btn {
  /* 基础布局：让图标和文字完美居中对齐 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;

  /* 视觉设计：半透明紫罗兰色调，显得高级且醒目 */
  background: rgba(139, 92, 246, 0.15);
  color: #c4b5fd; /* 柔和的亮紫色文字 */
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 12px;

  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  /* 动画过渡 */
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 悬浮状态 (Hover)：提亮发光并上浮 */
.comment-manage-btn:hover {
  background: rgba(139, 92, 246, 0.4);
  color: #ffffff;
  border-color: rgba(139, 92, 246, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.25);
}

/* 图标微动效：鼠标放上去时小图标微微放大倾斜 */
.comment-manage-btn svg {
  transition: transform 0.3s ease;
}
.comment-manage-btn:hover svg {
  transform: scale(1.15) rotate(-5deg);
}

/* 点击状态 (Active)：弹性按压感 */
.comment-manage-btn:active {
  transform: translateY(0) scale(0.96);
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.15);
}

/* 🌙 如果你有白天模式，可以加上这个适配： */
:global(.light-mode) .comment-manage-btn {
  background: rgba(139, 92, 246, 0.08);
  color: #7c3aed;
  border-color: rgba(139, 92, 246, 0.2);
}
:global(.light-mode) .comment-manage-btn:hover {
  background: rgba(139, 92, 246, 0.15);
  color: #6d28d9;
  border-color: rgba(139, 92, 246, 0.4);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.15);
}


/* 用户 管理 */
.role-badge { padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 600; }
.badge-admin { background: rgba(251,191,36,0.2); color: #fbbf24; }
.badge-user { background: rgba(99,102,241,0.2); color: #818cf8; }


</style>


<style>
/* ============================================================
   🌅 白天模式终极适配 - 管理面板 (非 scoped)
   ============================================================ */
.light-mode .admin-panel {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08), inset 0 1px 1px rgba(255, 255, 255, 0.8);

  /* 覆盖局部文字颜色变量，确保白天文字清晰 */
  --text-primary: #1a1a1a;
  --text-secondary: #4b5563;
  --text-tertiary: #9ca3af;
}

/* 顶部标签栏 */
.light-mode .admin-tab { color: var(--text-secondary); }
.light-mode .admin-tab.active {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-primary);
}
.light-mode .tab-count {
  background: rgba(0, 0, 0, 0.08);
  color: var(--text-secondary);
}
.light-mode .close-btn {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-secondary);
}
.light-mode .close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: var(--text-primary);
}

/* 搜索栏 */
.light-mode .admin-search-bar {
  background: rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}
.light-mode .admin-search-input { color: #111; }

/* 列表区 */
.light-mode .list-header { border-bottom-color: rgba(0, 0, 0, 0.06); }
.light-mode .song-row:hover { background: rgba(0, 0, 0, 0.03); }
.light-mode .song-row.selected { background: rgba(139, 92, 246, 0.1); }
.light-mode .row-cover-placeholder { background: rgba(0, 0, 0, 0.06); color: var(--text-tertiary); }

/* 右侧详情面板 */
.light-mode .detail-panel {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.5);
}
.light-mode .field-input {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  color: #111;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
}
.light-mode .field-input:focus { border-color: rgba(139, 92, 246, 0.6); }
.light-mode .detail-cover-placeholder { background: rgba(0, 0, 0, 0.04); color: var(--text-tertiary); }
.light-mode .file-upload-btn {
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: var(--text-secondary);
}
.light-mode .file-upload-btn:hover {
  border-color: #9ca3af;
  color: var(--text-primary);
}

/* 上传区 */
.light-mode .upload-zone { border-color: rgba(0, 0, 0, 0.15); }
.light-mode .upload-zone.dragging {
  background: rgba(139, 92, 246, 0.05);
  border-color: rgba(139, 92, 246, 0.4);
}
.light-mode .pending-item { background: rgba(0, 0, 0, 0.04); }
.light-mode .add-more-btn { border-color: rgba(0, 0, 0, 0.15); color: var(--text-secondary); }
.light-mode .progress-bar { background: rgba(0, 0, 0, 0.08); }


/* 白天模式下详情页的拖拽反馈 */
.light-mode .file-upload-row.drag-active {
  background: rgba(139, 92, 246, 0.05);
  border-color: rgba(139, 92, 246, 0.4);
}

</style>