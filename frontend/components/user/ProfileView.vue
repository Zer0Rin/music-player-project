<template>
  <div class="profile-container custom-scrollbar">
    <div class="profile-header">
      <h2 class="profile-title">个人资料</h2>
    </div>

    <div class="profile-layout">
      <div class="left-settings">
        <div class="avatar-section liquid-card">
          <div class="crop-container">
            <div v-if="!cropImageSrc" class="avatar-upload-placeholder" @click="triggerAvatarUpload">
              <img v-if="avatarUrl" :src="avatarUrl" class="current-avatar-preview" />
              <div v-else class="avatar-placeholder-big">{{ displayName[0] }}</div>
              <div class="avatar-hover-overlay">
                <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
                  <path d="M12 15.2A3.2 3.2 0 0 1 8.8 12 3.2 3.2 0 0 1 12 8.8 3.2 3.2 0 0 1 15.2 12 3.2 3.2 0 0 1 12 15.2M12 7a5 5 0 0 0-5 5 5 5 0 0 0 5 5 5 5 0 0 0 5-5 5 5 0 0 0-5-5m0-5.5c-2.8 0-5.3.9-7.3 2.4L6.1 5.3A8.44 8.44 0 0 1 12 3.5c5.25 0 9.5 4.25 9.5 9.5S17.25 22.5 12 22.5 2.5 18.25 2.5 13c0-2.8 1.3-5.3 3.3-7L4.4 4.6A11.5 11.5 0 0 0 .5 13c0 6.35 5.15 11.5 11.5 11.5S23.5 19.35 23.5 13 18.35 1.5 12 1.5z"/>
                </svg>
                <span>更换头像</span>
              </div>
            </div>

            <div v-else class="crop-area" ref="cropAreaRef"
                 @mousedown="onDragStart" @mousemove="onDragMove" @mouseup="onDragEnd" @mouseleave="onDragEnd"
                 @wheel.prevent="onWheel" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onDragEnd">

              <img ref="cropImgRef" :src="cropImageSrc" class="crop-movable-img"
                   :style="{ transform: `translate(calc(-50% + ${imgX}px), calc(-50% + ${imgY}px)) scale(${imgScale})` }"
                   draggable="false" />

              <div class="crop-overlay-mask" />

              <div class="crop-circle-border" />

              <div class="crop-hint">拖动、缩放以适配</div>
            </div>

            <div class="crop-actions">
              <button class="crop-action-btn" @click="triggerAvatarUpload">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 13H5v-2h14v2z"/><path d="M12 5l-7 7 1.41 1.41L11 8.83V21h2V8.83l4.59 4.58L19 12l-7-7z" transform="rotate(180 12 13)"/></svg>
                重新选择
              </button>
              <button v-if="cropImageSrc" class="crop-action-btn confirm-btn" @click="confirmCrop" :disabled="uploading">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                {{ uploading ? '上传中...' : '确认使用' }}
              </button>
              <button v-if="cropImageSrc" class="crop-action-btn" @click="cropImageSrc = null">取消</button>
            </div>

          </div>
          <input ref="avatarInput" type="file" accept="image/*" class="hidden-input" @change="onAvatarFileChange" />
          <p v-if="uploadMsg" class="upload-msg" :class="{ error: uploadError }">{{ uploadMsg }}</p>
        </div>

        <div class="info-section liquid-card">
          <h3 class="section-title">基本信息</h3>
          <div class="form-group">
            <label>用户名</label>
            <input :value="authStore.user?.username" disabled class="form-input disabled" />
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input v-model="nickname" class="form-input" placeholder="设置你的昵称" maxlength="20" />
          </div>
          <div class="form-group">
            <label>角色</label>
            <input :value="authStore.user?.role === 'ADMIN' ? '管理员' : '普通用户'" disabled class="form-input disabled" />
          </div>
          <div class="form-actions">
            <p v-if="saveMsg" class="save-msg" :class="{ error: isError }">{{ saveMsg }}</p>
            <div class="action-btns">
              <button v-if="authStore.isAdmin" class="admin-btn liquid-btn" @click="showAdmin = true">
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
                管理
              </button>
              <button class="save-btn liquid-btn" :disabled="saving" @click="saveProfile">
                {{ saving ? '保存中...' : '保存修改' }}
              </button>
            </div>
          </div>
          <!-- 修改密码 -->
          <div class="password-section">
            <div class="section-divider" />
            <h3 class="section-title">修改密码</h3>
            <div class="form-group">
              <label>原密码</label>
              <input v-model="pwForm.old" type="password" class="form-input" placeholder="输入原密码" />
            </div>
            <div class="form-group">
              <label>新密码</label>
              <input v-model="pwForm.new" type="password" class="form-input" placeholder="至少6位" />
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <input v-model="pwForm.confirm" type="password" class="form-input" placeholder="再次输入新密码" />
            </div>
            <div class="form-actions">
              <p v-if="pwMsg" class="save-msg" :class="{ error: pwError }">{{ pwMsg }}</p>
              <button class="save-btn liquid-btn" :disabled="pwSaving" @click="changePassword">
                {{ pwSaving ? '修改中...' : '修改密码' }}
              </button>
            </div>
          </div>
          <AdminPanel :visible="showAdmin" @close="showAdmin = false" @uploaded="onSongsUploaded" />
        </div>
      </div>

      <div class="right-content custom-scrollbar">
        <div class="stats-section liquid-card">
          <h3 class="section-title stats-title">账号统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-num">{{ plStore.userPlaylists.length }}</div>
              <div class="stat-label">创建歌单</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ plStore.favorites?.songIds?.length || 0 }}</div>
              <div class="stat-label">收藏歌曲</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ playerStore.recentSongs.length }}</div>
              <div class="stat-label">最近播放</div>
            </div>
          </div>
        </div>

        <div class="my-playlists-section">
          <h3 class="content-heading">我的歌单</h3>
          <div class="playlist-grid">
            <div v-if="plStore.favorites" class="playlist-card liquid-card" @click="navTo(plStore.favorites.id)">
              <div class="fav-cover">
                <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
              </div>
              <div class="pl-meta">
                <div class="pl-name">我喜欢</div>
                <div class="pl-count">{{ plStore.favorites.songIds.length }} 首歌曲</div>
              </div>
            </div>

            <div v-for="pl in plStore.userPlaylists" :key="pl.id" class="playlist-card liquid-card" @click="navTo(pl.id)">
              <div class="pl-cover-wrap">
                <img v-if="pl.coverImage" :src="`/api/playlists/${pl.id}/cover?t=${Date.now()}`" class="pl-cover" />
                <div v-else class="pl-cover-placeholder">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="40" height="40"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                </div>
              </div>
              <div class="pl-meta">
                <div class="pl-name">{{ pl.name }}</div>
                <div class="pl-count">{{ pl.songIds?.length || 0 }} 首歌曲</div>
              </div>
            </div>
          </div>
        </div>

        <div class="community-content-section">
          <h3 class="content-heading">社区动态 (建设中)</h3>
          <div class="community-placeholder liquid-panel">
            <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <p>你的分享点滴将呈现在此</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import AdminPanel from '~/components/user/AdminPanel.vue'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const plStore = usePlaylistStore()
const playerStore = usePlayerStore()
const { $apiFetch } = useNuxtApp()

const nickname = ref(authStore.user?.nickname || '')
const saving = ref(false)
const saveMsg = ref('')
const isError = ref(false)
const avatarInput = ref(null)
const avatarTimestamp = ref(Date.now())
const cropAreaRef = ref(null)

const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'U')
const avatarUrl = computed(() => {
  if (authStore.user?.avatarFile) {
    return `/api/user/avatar/${authStore.user.avatarFile}?v=${avatarTimestamp.value}`
  }
  return null
})

// 裁剪相关状态
const cropImageSrc = ref(null)
const imgX = ref(0)
const imgY = ref(0)
const imgScale = ref(1)
const dragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragStartImgX = ref(0)
const dragStartImgY = ref(0)
const uploading = ref(false)
const uploadMsg = ref('')
const uploadError = ref(false)
const originalImageSize = ref({ width: 0, height: 0 })
const cropImgRef = ref(null)

async function saveProfile() {
  if (!nickname.value.trim()) return
  saving.value = true
  saveMsg.value = ''
  try {
    const data = await $apiFetch('/api/user/me', { method: 'PUT', body: { nickname: nickname.value.trim() } })
    authStore.user = { ...authStore.user, ...data }
    localStorage.setItem('user', JSON.stringify(authStore.user))
    saveMsg.value = '保存成功 ✓'
    isError.value = false
  } catch {
    saveMsg.value = '保存失败，请重试'; isError.value = true
  } finally {
    saving.value = false; setTimeout(() => saveMsg.value = '', 3000)
  }
}

function triggerAvatarUpload() { avatarInput.value?.click() }

function onAvatarFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    const img = new Image()
    img.src = ev.target.result
    img.onload = () => {
      originalImageSize.value = { width: img.naturalWidth, height: img.naturalHeight }
      cropImageSrc.value = ev.target.result

      // 智能计算初始缩放率，确保图片正好能盖住 200px 的裁剪圆
      const baseScaleX = 240 / img.naturalWidth
      const baseScaleY = 240 / img.naturalHeight
      // 取最大值并稍微放大 5% 作为初始 scale，保证没有黑边
      imgScale.value = Math.max(baseScaleX, baseScaleY) * 1.02

      imgX.value = 0
      imgY.value = 0
    }
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function onDragStart(e) { dragging.value = true; dragStartX.value = e.clientX; dragStartY.value = e.clientY; dragStartImgX.value = imgX.value; dragStartImgY.value = imgY.value }
function onDragMove(e) { if (dragging.value) { imgX.value = dragStartImgX.value + (e.clientX - dragStartX.value); imgY.value = dragStartImgY.value + (e.clientY - dragStartY.value) } }
function onDragEnd() { dragging.value = false }
function onWheel(e) { const delta = e.deltaY > 0 ? -0.05 : 0.05; imgScale.value = Math.min(3, Math.max(0.5, imgScale.value + delta)) }
function onTouchStart(e) { dragging.value = true; dragStartX.value = e.touches[0].clientX; dragStartY.value = e.touches[0].clientY; dragStartImgX.value = imgX.value; dragStartImgY.value = imgY.value }
function onTouchMove(e) { if (dragging.value) { e.preventDefault(); imgX.value = dragStartImgX.value + (e.touches[0].clientX - dragStartX.value); imgY.value = dragStartImgY.value + (e.touches[0].clientY - dragStartY.value) } }

const PC_DISPLAY_WIDTH = 240;
async function confirmCrop() {
  if (!cropImageSrc.value || !cropAreaRef.value) return
  uploading.value = true; uploadMsg.value = ''
  try {
    const canvas = document.createElement('canvas'); canvas.width = 200; canvas.height = 200
    const ctx = canvas.getContext('2d')
    const img = new Image(); img.src = cropImageSrc.value
    await new Promise(r => img.onload = r)

    // 终极精准数学：抛弃相对容器尺寸，直接建立 屏幕坐标系 -> 原图物理坐标系 的映射
    // 1. 获取图片和容器在屏幕上的【真实渲染尺寸和位置】
    const imgEl = cropImgRef.value;
    const containerEl = cropAreaRef.value;
    const imgRect = imgEl.getBoundingClientRect();
    const containerRect = containerEl.getBoundingClientRect();

    // 2. 屏幕上裁剪圆的圆心坐标 (相对于浏览器视口)
    const cx_screen = containerRect.left + (containerRect.width / 2);
    const cy_screen = containerRect.top + (containerRect.height / 2);

    // 3. 计算屏幕上的圆心，距离图片左上角边缘的偏移量 (屏幕像素)
    const offsetX_screen = cx_screen - imgRect.left;
    const offsetY_screen = cy_screen - imgRect.top;

    // 4. 计算终极映射比例：原图物理像素 / 屏幕最终渲染像素
    // 无论你 CSS 加了多少 scale 或者 width限制，这个比例永远绝对准确！
    const scaleX = img.naturalWidth / imgRect.width;
    const scaleY = img.naturalHeight / imgRect.height;

    // 5. 换算得出 Canvas 截取所需的原图坐标
    const screenCropR = 100; // 对应你界面上 200x200 的圆
    const r_native = screenCropR * scaleX; // 映射到原图的半径

    const sx = (offsetX_screen * scaleX) - r_native;
    const sy = (offsetY_screen * scaleY) - r_native;
    const sw = r_native * 2;
    const sh = r_native * 2;

    // 开始裁剪！
    ctx.beginPath();
    ctx.arc(100, 100, 100, 0, Math.PI * 2);
    ctx.clip();

    // 完美切割，绝不偏移！
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, 200, 200);
// ====== 之后的 blob 导出和上传代码保持不变 ======

    const blob = await new Promise(r => canvas.toBlob(r, 'image/jpeg', 0.95))
    const formData = new FormData(); formData.append('file', blob, 'avatar.jpg')
    const data = await $apiFetch('/api/user/me/avatar', { method: 'POST', body: formData })

    authStore.user = { ...authStore.user, avatarFile: data.avatarFile }
    localStorage.setItem('user', JSON.stringify(authStore.user))
    avatarTimestamp.value = Date.now(); cropImageSrc.value = null
    uploadMsg.value = '头像更新成功 ✓'; uploadError.value = false; setTimeout(() => uploadMsg.value = '', 3000)
  } catch {
    uploadMsg.value = '上传失败，请重试'; uploadError.value = true
  } finally { uploading.value = false }
}

const router = useRouter()
function navTo(id) { plStore.setActivePlaylist(id); router.push('/') }



const showAdmin = ref(false)
async function onSongsUploaded() { await usePlayerStore().refreshSongs($apiFetch) }

/* 修改密码 */
const pwForm = reactive({ old: '', new: '', confirm: '' })
const pwSaving = ref(false)
const pwMsg = ref('')
const pwError = ref(false)

async function changePassword() {
  pwMsg.value = ''
  if (!pwForm.old || !pwForm.new || !pwForm.confirm) {
    pwMsg.value = '请填写所有字段'; pwError.value = true; return
  }
  if (pwForm.new !== pwForm.confirm) {
    pwMsg.value = '两次新密码不一致'; pwError.value = true; return
  }
  if (pwForm.new.length < 6) {
    pwMsg.value = '新密码至少6位'; pwError.value = true; return
  }
  pwSaving.value = true
  try {
    await $apiFetch('/api/user/me/password', {
      method: 'PUT',
      body: { oldPassword: pwForm.old, newPassword: pwForm.new },
    })
    pwMsg.value = '密码修改成功 ✓'
    pwError.value = false
    pwForm.old = ''; pwForm.new = ''; pwForm.confirm = ''
  } catch (e) {
    pwMsg.value = e?.data?.message || '修改失败'
    pwError.value = true
  } finally {
    pwSaving.value = false
    setTimeout(() => pwMsg.value = '', 4000)
  }
}

</script>

<style scoped>
.profile-container { height: 100%; overflow: hidden; padding: 24px; box-sizing: border-box; }
.profile-header { margin-bottom: 24px; flex-shrink: 0;}
.profile-title { font-size: 26px; font-weight: 800; color: var(--text-primary); letter-spacing: -0.02em;}

.profile-layout { display: flex; gap: 32px; height: calc(100% - 60px); }

/* 左侧：信息设置区域 */
.left-settings { flex: 0 0 360px; display: flex; flex-direction: column; gap: 16px; height: 100%; overflow-y: auto; padding-right: 8px; }
.left-settings::-webkit-scrollbar { width: 4px; }
.left-settings::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.05); border-radius: 4px; }

/* 右侧：内容展示区域 */
.right-content { flex: 1; display: flex; flex-direction: column; gap: 24px; height: 100%; overflow-y: auto; padding-right: 12px; }

@media (max-width: 1024px) {
  .profile-layout { flex-direction: column; height: auto; overflow: visible; padding-bottom: 80px; }
  .profile-container { overflow-y: auto; }
  .left-settings { flex: 1; max-width: none; overflow: visible; padding-right: 0; }
  .right-content { flex: 1; overflow: visible; padding-right: 0; }
}

/* 瘦身信息卡片 */
.avatar-section, .info-section { padding: 20px; border-radius: 16px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.section-title { font-size: 14px; font-weight: 700; color: var(--text-tertiary); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.1em; align-self: flex-start; }

.form-group { width: 100%; display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.form-group label { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
.form-input { padding: 12px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.03); color: var(--text-primary); font-size: 14px; outline: none; transition: all 0.2s; }
.form-input:focus { border-color: rgba(139,92,246,0.6); box-shadow: 0 0 0 3px rgba(139,92,246,0.1); }
.form-input.disabled { opacity: 0.4; cursor: not-allowed; }

.form-actions { width: 100%; display: flex; align-items: center; justify-content: space-between; margin-top: 8px; gap: 16px; }
.save-msg { font-size: 13px; color: #4ade80; }
.save-msg.error { color: #f87171; }
.action-btns { display: flex; gap: 10px; flex-shrink: 0; }
.save-btn { padding: 10px 20px; border-radius: 12px; border: none; background: rgba(139,92,246,0.4); color: white; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.save-btn:hover { background: rgba(139,92,246,0.55); }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 统计卡片 */
.stats-section { padding: 20px; border-radius: 16px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 8px; }
.stat-item { text-align: center; padding: 16px 8px; background: rgba(255,255,255,0.03); border-radius: 14px; transition: all 0.2s; }
.stat-item:hover { background: rgba(255,255,255,0.06); transform: translateY(-2px); }
.stat-num { font-size: 28px; font-weight: 800; color: var(--text-primary); font-variant-numeric: tabular-nums; }
.stat-label { font-size: 12px; color: var(--text-tertiary); margin-top: 5px; font-weight: 500; }

.content-heading { font-size: 18px; font-weight: 800; color: var(--text-primary); margin-bottom: 16px; letter-spacing: -0.01em; }

/* 歌单网格布局样式 */
.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.playlist-card {
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.playlist-card:hover { transform: translateY(-4px); background: rgba(255,255,255,0.06); box-shadow: 0 12px 24px rgba(0,0,0,0.15); }

.fav-cover {
  width: 100%; aspect-ratio: 1;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #ff2d55;
  background: linear-gradient(135deg, rgba(255, 45, 85, 0.1) 0%, rgba(255, 45, 85, 0.02) 100%);
  margin-bottom: 10px;
}

.pl-cover-wrap { width: 100%; aspect-ratio: 1; border-radius: 14px; overflow: hidden; margin-bottom: 10px; background: rgba(255,255,255,0.03); }
.pl-cover { width: 100%; height: 100%; object-fit: cover; }
.pl-cover-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); }

.pl-meta { padding: 0 4px; }
.pl-name { font-size: 14px; font-weight: 700; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 3px; }
.pl-count { font-size: 12px; color: var(--text-tertiary); font-weight: 500; }

/* 社区占位区 */
.community-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px;
  min-height: 200px; padding: 40px; border-radius: 24px;
  color: var(--text-tertiary); border: 2px dashed rgba(255,255,255,0.06); background: rgba(0,0,0,0.1); text-align: center;
}
.community-placeholder p { font-size: 15px; font-weight: 500; }

/* 裁剪区域 */
.crop-container { display: flex; flex-direction: column; align-items: center; gap: 16px; width: 100%; }

.crop-area, .avatar-upload-placeholder {
  width: 240px; height: 240px;
  position: relative; overflow: hidden;
  background: #080808; border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(139,92,246,0.1), 0 16px 40px rgba(0,0,0,0.3);
  margin: 16px 0;
}

.avatar-upload-placeholder { cursor: pointer; border: 2px dashed rgba(255,255,255,0.1); background: rgba(255,255,255,0.02); }
.avatar-upload-placeholder:hover { border-color: var(--accent); }
.avatar-placeholder-big {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, rgba(139,92,246,0.3) 0%, rgba(139,92,246,0.1) 100%);
  display: flex; align-items: center; justify-content: center;
  font-size: 72px; font-weight: 800; color: white; letter-spacing: -0.05em; text-shadow: 0 4px 12px rgba(0,0,0,0.2);
}
.current-avatar-preview { width: 100%; height: 100%; object-fit: cover; }
.avatar-hover-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; opacity: 0; transition: all 0.2s; color: white; font-size: 13px; font-weight: 600; }
.avatar-upload-placeholder:hover .avatar-hover-overlay { opacity: 1; }

.crop-area { cursor: grab; }
.crop-area:active { cursor: grabbing; }

.crop-movable-img {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-origin: center center;
  pointer-events: auto;
  user-select: none;

  /* 绝对禁止浏览器自动缩放原图！ */
  max-width: none !important;
  max-height: none !important;
  width: auto !important;
  height: auto !important;
}

.crop-overlay-mask {
  position: absolute; inset: 0; z-index: 2;
  background: radial-gradient(circle at center, transparent 100px, rgba(0, 0, 0, 0.6) 100px);
  pointer-events: none;
}

.crop-circle-border {
  position: absolute; z-index: 3;
  width: 200px; height: 200px;
  top: 20px; left: 20px;
  border-radius: 50%;
  border: 3px solid #fff;
  pointer-events: none;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.1), inset 0 0 20px rgba(0, 0, 0, 0.2);
}

.crop-hint {
  position: absolute; bottom: 32px; left: 50%; transform: translateX(-50%);
  z-index: 5; pointer-events: none;
  font-size: 12px; color: rgba(255,255,255,0.7); font-weight: 600;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  padding: 4px 16px; border-radius: 24px; white-space: nowrap;
}

.crop-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-top: 8px;}
.crop-action-btn { display: flex; align-items: center; gap: 6px; background: rgba(255,255,255,0.05); border: none; color: var(--text-secondary); font-size: 13px; font-weight: 600; cursor: pointer; padding: 8px 16px; border-radius: 12px; transition: all 0.2s; }
.crop-action-btn:hover { color: var(--text-primary); background: rgba(255,255,255,0.1); }
.confirm-btn { background: rgba(74,222,128,0.15) !important; color: #4ade80 !important; }
.confirm-btn:hover { background: rgba(74,222,128,0.25) !important; }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.upload-msg { font-size: 13px; color: #4ade80; text-align: center; }
.upload-msg.error { color: #f87171; }
.hidden-input { display: none; }

.admin-btn { padding: 10px 16px; border-radius: 10px; border: none; background: rgba(245,158,11,0.2); color: #fbbf24; font-size: 14px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 6px; }
.admin-btn:hover { background: rgba(245,158,11,0.35); }

/* =========================================
   白天模式：冷冽冰灰玻璃（极致高斯模糊 + 淡灰滤镜）
   ========================================= */

/* 1. 全局卡片：拉满模糊度（50px），注入淡灰色调，舍弃纯白 */
:global(.light-mode) .avatar-section,
:global(.light-mode) .info-section,
:global(.light-mode) .stats-section,
:global(.light-mode) .playlist-card {
  /* REQUIREMENT 2: 核心修改：注入淡中性灰色调 (rgba 100, 100, 100) */
  background: rgba(100, 100, 100, 0.1);

  /* REQUIREMENT 1: 核心修改：极致增强模糊度（从 20px 提到 50px） */
  backdrop-filter: blur(50px) saturate(180%);
  -webkit-backdrop-filter: blur(50px) saturate(180%);

  /* 补充：极致清晰的白色高光白边，在灰玻璃上更显眼 */
  border: 1px solid rgba(255, 255, 255, 0.3);

  /* 补充：极柔和的底层阴影，增加浮起感 */
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
}

/* 2. 悬浮态增强：悬浮时变得稍微实体一点，提供清脆的交互反馈 */
:global(.light-mode) .playlist-card:hover {
  background: rgba(100, 100, 100, 0.15);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-4px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.06);
}

/* 3. 字体对比度极致加深：确保在灰玻璃上绝对清晰 */
:global(.light-mode) .profile-title,
:global(.light-mode) .section-title,
:global(.light-mode) .content-heading,
:global(.light-mode) .stat-num,
:global(.light-mode) .pl-name {
  color: #111 !important; /* 核心数据和标题用极致纯黑 */
}

:global(.light-mode) .form-group label,
:global(.light-mode) .stat-label,
:global(.light-mode) .pl-count,
:global(.light-mode) .community-placeholder p {
  color: #333 !important; /* 次要说明文字用深灰色 */
  font-weight: 500;
}

/* 4. 内部凹陷元素（输入框、统计数字底块）：用淡淡的暗灰色压下去 */
:global(.light-mode) .form-input,
:global(.light-mode) .stat-item,
:global(.light-mode) .pl-cover-wrap {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.04);
  color: #111;
  font-weight: 600;
}
:global(.light-mode) .form-input:focus {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(139,92,246,0.5);
}

/* 占位区风格保持虚线，但融入整体冰灰质感 */
:global(.light-mode) .community-placeholder {
  background: rgba(100, 100, 100, 0.05);
  backdrop-filter: blur(50px);
  -webkit-backdrop-filter: blur(50px);
  border: 2px dashed rgba(0, 0, 0, 0.1);
  color: #333;
}

:global(.light-mode) .fav-cover {
  background: rgba(255, 45, 85, 0.1);
}

/* 修改密码 */
.password-section { width: 100%; display: flex; flex-direction: column; }
.section-divider { width: 100%; height: 1px; background: rgba(255,255,255,0.06); margin: 8px 0 16px; }

</style>