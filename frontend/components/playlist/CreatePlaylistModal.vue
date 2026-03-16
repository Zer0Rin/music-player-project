<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-backdrop" @click.self="close">
        <div class="modal-panel liquid-panel">
          <div class="modal-header">
            <h2>创建歌单</h2>
            <button class="modal-close" @click="close">
              <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <!-- ===== 封面上传 + 裁剪 ===== -->
            <div class="cover-section">
              <div
                  class="cover-upload"
                  :class="{ 'has-image': previewUrl }"
                  @dragover.prevent
                  @drop.prevent="onDrop"
              >
                <div v-if="previewUrl" class="crop-area" ref="cropAreaRef" @wheel.prevent="onWheel">
                  <img :src="previewUrl" class="crop-bg" :style="imgTransformStyle" />

                  <svg class="crop-mask" viewBox="0 0 1000 600" preserveAspectRatio="none">
                    <defs>
                      <mask id="cropMask">
                        <rect width="1000" height="600" fill="white" />
                        <rect :x="maskX" :y="maskY" :width="maskSize" :height="maskSize" rx="16" fill="black" />
                      </mask>
                    </defs>
                    <rect width="1000" height="600" fill="rgba(0,0,0,0.35)" mask="url(#cropMask)" />
                  </svg>

                  <div class="crop-clear-wrapper" :style="{ clipPath: clipPathStr }">
                    <img
                        :src="previewUrl"
                        class="crop-clear"
                        :style="imgTransformStyle"
                        @mousedown.prevent="startDrag"
                        @touchstart.prevent="startDragTouch"
                    />
                  </div>

                  <div class="crop-border" :style="cropBorderStyle" />

                  <div class="crop-hint">
                    <span>拖动调整位置 · 滚轮缩放</span>
                  </div>
                </div>

                <div v-else class="upload-placeholder" @click="triggerFileInput">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="36" height="36" class="upload-icon">
                    <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
                  </svg>
                  <span>点击或拖拽上传封面</span>
                  <span class="upload-limit">最大 7MB · JPG / PNG / WebP</span>
                </div>
              </div>

              <input ref="fileInputRef" type="file" accept="image/*" class="hidden-input" @change="onFileSelect" />

              <div v-if="previewUrl" class="crop-actions">
                <button class="crop-action-btn" @click="removeImage">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                  移除
                </button>
                <button class="crop-action-btn" @click="triggerFileInput">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
                  更换
                </button>
              </div>
            </div>

            <!-- 歌单名称 -->
            <div class="field">
              <label>歌单名称</label>
              <input v-model="form.name" type="text" placeholder="输入歌单名称" maxlength="30" class="field-input" />
              <span class="field-count">{{ form.name.length }}/30</span>
            </div>

            <!-- 简介 -->
            <div class="field">
              <label>简介</label>
              <textarea v-model="form.description" placeholder="添加简介..." maxlength="200" rows="3" class="field-input field-textarea" />
              <span class="field-count">{{ form.description.length }}/200</span>
            </div>

            <!-- 标签 -->
            <div class="field">
              <label>标签</label>
              <div class="tags-area">
                <span v-for="(tag, i) in form.tags" :key="i" class="tag-chip liquid-card">
                  {{ tag }}
                  <button @click="removeTag(i)" class="tag-remove">×</button>
                </span>
                <input
                  v-if="form.tags.length < 5"
                  v-model="tagInput"
                  type="text"
                  placeholder="回车添加标签"
                  class="tag-input"
                  @keydown.enter.prevent="addTag"
                />
              </div>
              <span class="field-hint">最多 5 个标签</span>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel liquid-btn" @click="close">取消</button>
            <button class="btn-create" :disabled="!form.name.trim()" @click="submit">创建歌单</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'created'])
const plStore = usePlaylistStore()

const form = ref({ name: '', description: '', tags: [] })
const tagInput = ref('')
const fileInputRef = ref(null)
const cropAreaRef = ref(null)

// ===== 图片状态 =====
const previewUrl = ref(null)
const selectedFile = ref(null)
const imgNaturalW = ref(0)
const imgNaturalH = ref(0)
const imgX = ref(0)
const imgY = ref(0)
const imgScale = ref(1)

// 裁剪框在 SVG viewBox (1000x600) 中的位置（居中正方形）
const maskSize = 280
const maskX = (1000 - maskSize) / 2
const maskY = (600 - maskSize) / 2

const imgTransformStyle = computed(() => ({
  transform: `translate(${imgX.value}px, ${imgY.value}px) scale(${imgScale.value})`,
  transformOrigin: 'center center',
}))

// clip-path 百分比，和裁剪框位置对应
const clipPathStr = computed(() => {
  if (!cropAreaRef.value) return 'inset(0)'
  // 裁剪框占容器比例
  const xPct = (maskX / 1000) * 100
  const yPct = (maskY / 600) * 100
  const wPct = (maskSize / 1000) * 100
  const hPct = (maskSize / 600) * 100
  return `inset(${yPct}% ${100 - xPct - wPct}% ${100 - yPct - hPct}% ${xPct}%)`
})

const cropBorderStyle = computed(() => ({
  left: `${(maskX / 1000) * 100}%`,
  top: `${(maskY / 600) * 100}%`,
  width: `${(maskSize / 1000) * 100}%`,
  height: `${(maskSize / 600) * 100}%`,
}))

// ===== 文件处理 =====
function triggerFileInput() {
  fileInputRef.value?.click()
}

function onFileSelect(e) {
  const file = e.target.files?.[0]
  if (file) loadImage(file)
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function onDrop(e) {
  const file = e.dataTransfer.files?.[0]
  if (file?.type.startsWith('image/')) loadImage(file)
}

function loadImage(file) {
  if (file.size > 7 * 1024 * 1024) {
    alert('图片超过 7MB 限制')
    return
  }
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target.result
    const img = new Image()
    img.onload = () => {
      imgNaturalW.value = img.width
      imgNaturalH.value = img.height
      imgX.value = 0
      imgY.value = 0
      imgScale.value = 1
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
}

function removeImage() {
  previewUrl.value = null
  selectedFile.value = null
  imgX.value = 0
  imgY.value = 0
  imgScale.value = 1
}

// ===== 拖动 =====
let dragging = false
let dragStartX = 0, dragStartY = 0
let dragOriginX = 0, dragOriginY = 0

function startDrag(e) {
  dragging = true
  dragStartX = e.clientX; dragStartY = e.clientY
  dragOriginX = imgX.value; dragOriginY = imgY.value
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

function startDragTouch(e) {
  const t = e.touches[0]
  dragging = true
  dragStartX = t.clientX; dragStartY = t.clientY
  dragOriginX = imgX.value; dragOriginY = imgY.value
  window.addEventListener('touchmove', onDragMoveTouch, { passive: false })
  window.addEventListener('touchend', onDragEndTouch)
}

function onDragMove(e) {
  if (!dragging) return
  imgX.value = dragOriginX + (e.clientX - dragStartX)
  imgY.value = dragOriginY + (e.clientY - dragStartY)
}

function onDragMoveTouch(e) {
  e.preventDefault()
  const t = e.touches[0]
  imgX.value = dragOriginX + (t.clientX - dragStartX)
  imgY.value = dragOriginY + (t.clientY - dragStartY)
}

function onDragEnd() {
  dragging = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

function onDragEndTouch() {
  dragging = false
  window.removeEventListener('touchmove', onDragMoveTouch)
  window.removeEventListener('touchend', onDragEndTouch)
}

// ===== 滚轮缩放 =====
function onWheel(e) {
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  imgScale.value = Math.max(0.3, Math.min(3, imgScale.value + delta))
}

// ===== 标签 =====
function addTag() {
  const t = tagInput.value.trim()
  if (t && form.value.tags.length < 5 && !form.value.tags.includes(t)) {
    form.value.tags.push(t)
  }
  tagInput.value = ''
}

function removeTag(i) { form.value.tags.splice(i, 1) }

// ===== 裁剪导出 =====
async function getCroppedImage() {
  if (!previewUrl.value || !cropAreaRef.value) return null

  const area = cropAreaRef.value
  const areaW = area.clientWidth
  const areaH = area.clientHeight

  // 裁剪框在实际 DOM 中的像素位置
  const cropPxX = (maskX / 1000) * areaW
  const cropPxY = (maskY / 600) * areaH
  const cropPxW = (maskSize / 1000) * areaW
  const cropPxH = (maskSize / 600) * areaH

  // 图片在容器中的实际渲染尺寸（object-fit: cover 模拟）
  const areaRatio = areaW / areaH
  const imgRatio = imgNaturalW.value / imgNaturalH.value
  let renderW, renderH
  if (imgRatio > areaRatio) {
    renderH = areaH * imgScale.value
    renderW = renderH * imgRatio
  } else {
    renderW = areaW * imgScale.value
    renderH = renderW / imgRatio
  }

  // 图片左上角在容器中的位置
  const imgLeft = (areaW - renderW) / 2 + imgX.value
  const imgTop = (areaH - renderH) / 2 + imgY.value

  // 裁剪区域在原图上的坐标
  const scaleToNatural = imgNaturalW.value / renderW
  const sx = (cropPxX - imgLeft) * scaleToNatural
  const sy = (cropPxY - imgTop) * scaleToNatural
  const sw = cropPxW * scaleToNatural
  const sh = cropPxH * scaleToNatural

  const canvas = document.createElement('canvas')
  canvas.width = 400
  canvas.height = 400
  const ctx = canvas.getContext('2d')

  const srcImg = new Image()
  srcImg.src = previewUrl.value

  return new Promise((resolve) => {
    srcImg.onload = () => {
      ctx.drawImage(srcImg,
        Math.max(0, sx), Math.max(0, sy), Math.max(1, sw), Math.max(1, sh),
        0, 0, 400, 400
      )
      canvas.toBlob((blob) => resolve(blob), 'image/jpeg', 0.92)
    }
  })
}

// ===== 提交 =====
async function submit() {
  if (!form.value.name.trim()) return

  const pl = await plStore.createPlaylist(
    form.value.name.trim(),
    form.value.description.trim(),
    form.value.tags
  )

  if (pl && previewUrl.value) {
    const croppedBlob = await getCroppedImage()
    if (croppedBlob) {
      await plStore.uploadCover(pl.id, croppedBlob)
    } else if (selectedFile.value) {
      // 裁剪失败时直接上传原图
      await plStore.uploadCover(pl.id, selectedFile.value)
    }
  }

  emit('created', pl)
  resetForm()
  close()
}

function close() { emit('close') }

function resetForm() {
  form.value = { name: '', description: '', tags: [] }
  tagInput.value = ''
  removeImage()
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed; inset: 0; z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.modal-panel {
  width: 500px;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  overflow: hidden;
}

.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.modal-header h2 { font-size: 20px; font-weight: 800; letter-spacing: -0.02em; }

.modal-close {
  background: rgba(255, 255, 255, 0.06); border: none;
  color: var(--text-secondary); width: 34px; height: 34px;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s ease;
}
.modal-close:hover { background: rgba(255, 255, 255, 0.12); color: var(--text-primary); transform: rotate(90deg); }

.modal-body { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 22px; }

/* ===== 封面裁剪 ===== */
.cover-section { display: flex; flex-direction: column; align-items: center; gap: 10px; }

.cover-upload {
  width: 100%; height: 240px;
  border-radius: 16px;
  border: 2px dashed rgba(255, 255, 255, 0.1);
  cursor: pointer; overflow: hidden; position: relative;
  transition: border-color 0.3s ease;
}
.cover-upload:hover { border-color: rgba(255, 255, 255, 0.25); }
.cover-upload.has-image { border: none; cursor: default; }

.upload-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; color: var(--text-tertiary); font-size: 14px;
}
.upload-icon { opacity: 0.4; }
.upload-limit { font-size: 11px; opacity: 0.5; }

/* 裁剪区域 */
.crop-area {
  width: 100%; height: 100%;
  position: relative; overflow: hidden;
  background: #0a0a0a; border-radius: 16px;
}


/* 底层模糊图：降低模糊度，去除强制变暗，让 SVG 遮罩来控制明暗 */
.crop-bg {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  filter: blur(6px); /* 之前是 12px 和 brightness(0.4) 导致瞎眼 */
  pointer-events: none;
  user-select: none;
}

/* 清晰层包裹器（固定在原地，充当裁切的“窗户”） */
.crop-clear-wrapper {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  z-index: 3;
  pointer-events: none; /* 让鼠标事件穿透给里面的 img */
}

/* 清晰层图片本身 */
.crop-clear {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  cursor: grab;
  user-select: none;
  pointer-events: auto; /* 恢复自身的鼠标事件响应 */
}
.crop-clear:active { cursor: grabbing; }

/* 裁剪框边框 */
.crop-border {
  position: absolute; z-index: 4;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  pointer-events: none;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.3), inset 0 0 0 1px rgba(255, 255, 255, 0.15);
  transition: border-color 0.2s ease;
}

.crop-hint {
  position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%);
  z-index: 5; pointer-events: none;
  font-size: 11px; color: rgba(255, 255, 255, 0.5);
  background: rgba(0, 0, 0, 0.5); backdrop-filter: blur(8px);
  padding: 4px 14px; border-radius: 20px;
}

.crop-actions {
  display: flex; gap: 10px;
}

.crop-action-btn {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none;
  color: var(--text-secondary); font-size: 12px;
  cursor: pointer; padding: 4px 8px; border-radius: 6px;
  font-family: inherit; transition: all 0.2s ease;
}
.crop-action-btn:hover { color: var(--accent); background: rgba(255, 255, 255, 0.04); }

.hidden-input { display: none; }

/* ===== 表单 ===== */
.field { display: flex; flex-direction: column; gap: 6px; position: relative; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.field-input {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px; padding: 11px 14px;
  color: var(--text-primary); font-size: 14px; font-family: inherit;
  outline: none; resize: none;
  transition: border-color 0.3s ease, background 0.3s ease;
}
.field-input:focus { border-color: rgba(255, 255, 255, 0.2); background: rgba(255, 255, 255, 0.06); }
.field-input::placeholder { color: var(--text-tertiary); }
.field-textarea { min-height: 72px; }
.field-count { position: absolute; top: 0; right: 0; font-size: 11px; color: var(--text-tertiary); }
.field-hint { font-size: 11px; color: var(--text-tertiary); }

/* ===== 标签 ===== */
.tags-area {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding: 10px 14px; min-height: 44px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.tag-chip {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 12px; border-radius: 20px;
  font-size: 13px; font-weight: 500; color: var(--text-primary);
}

.tag-remove {
  background: none; border: none; color: var(--text-tertiary);
  cursor: pointer; font-size: 16px; line-height: 1; padding: 0 2px;
  transition: color 0.2s ease;
}
.tag-remove:hover { color: var(--accent); }

.tag-input {
  background: none; border: none; outline: none;
  color: var(--text-primary); font-size: 13px;
  flex: 1; min-width: 100px; font-family: inherit;
}
.tag-input::placeholder { color: var(--text-tertiary); }

/* ===== 底部 ===== */
.modal-footer {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 18px 24px; border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.btn-cancel {
  padding: 9px 22px; border-radius: 12px;
  font-size: 14px; font-weight: 500; cursor: pointer; font-family: inherit;
  color: var(--text-secondary);
}

.btn-create {
  background: linear-gradient(135deg, var(--accent), #ff7e5f);
  border: none; color: white; padding: 9px 28px;
  border-radius: 12px; font-size: 14px; font-weight: 700;
  cursor: pointer; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 16px rgba(250, 45, 72, 0.3);
}
.btn-create:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(250, 45, 72, 0.5); }
.btn-create:active { transform: scale(0.97); }
.btn-create:disabled { opacity: 0.35; cursor: not-allowed; transform: none; box-shadow: none; }

/* ===== 动画 ===== */
.modal-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.modal-leave-active { transition: all 0.25s ease; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-panel { transform: scale(0.92) translateY(24px); opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-panel { transform: scale(0.96) translateY(12px); opacity: 0; }

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  .modal-backdrop { padding: 0; align-items: flex-end; }
  .modal-panel { width: 100%; max-height: 95vh; border-radius: 24px 24px 0 0; }
  .modal-body { padding: 16px 20px; gap: 18px; }
  .modal-header { padding: 16px 20px 12px; }
  .modal-footer { padding: 14px 20px; }
  .cover-upload { height: 200px; }
  .modal-enter-from .modal-panel { transform: translateY(100%); opacity: 1; }
  .modal-leave-to .modal-panel { transform: translateY(100%); opacity: 1; }
}

/* =========================================
   💡 弹窗的白天模式完美适配 (解决黑幕与隐形输入框)
   ========================================= */

/* 1. 彻底干掉黑幕，换成清透的白玻璃遮罩 */
:global(.light-mode) .modal-backdrop {
  background: rgba(255, 255, 255, 0.25) !important;
}

/* 2. 修复隐形的输入框和标签区域：改成微灰底色和暗色边框 */
:global(.light-mode) .field-input,
:global(.light-mode) .tags-area {
  background: rgba(0, 0, 0, 0.03);
  border-color: rgba(0, 0, 0, 0.1);
  color: var(--text-primary);
}

:global(.light-mode) .field-input:focus {
  background: rgba(0, 0, 0, 0.06);
  border-color: rgba(0, 0, 0, 0.25);
}

/* 3. 修复隐形的上传虚线框 */
:global(.light-mode) .cover-upload {
  border-color: rgba(0, 0, 0, 0.15);
}

:global(.light-mode) .cover-upload:hover {
  border-color: rgba(0, 0, 0, 0.35);
}

/* 4. 分割线与关闭按钮适配 */
:global(.light-mode) .modal-header,
:global(.light-mode) .modal-footer {
  border-color: rgba(0, 0, 0, 0.08);
}

:global(.light-mode) .modal-close {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-secondary);
}

:global(.light-mode) .modal-close:hover {
  background: rgba(0, 0, 0, 0.12);
  color: var(--text-primary);
}

</style>
