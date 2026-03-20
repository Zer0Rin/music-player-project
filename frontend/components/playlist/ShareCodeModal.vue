<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="share-backdrop" @click.self="$emit('close')">
        <div class="share-modal liquid-panel">

          <div class="share-header">
            <h3>分享歌单</h3>
            <button class="close-btn" @click="$emit('close')">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/>
              </svg>
            </button>
          </div>

          <!-- 生成推荐码（仅歌单详情页） -->
          <div v-if="playlistId" class="share-section">
            <p class="share-desc">生成推荐码，分享给好友，有效期 7 天</p>
            <div v-if="shareCode" class="code-display">
              <span class="code-text">{{ shareCode }}</span>
              <button class="copy-btn" @click="copyCode">
                {{ copied ? '已复制 ✓' : '复制' }}
              </button>
            </div>
            <div v-if="expiry" class="expiry-hint">过期时间：{{ formatExpiry(expiry) }}</div>
            <button class="gen-btn liquid-btn" :disabled="generating" @click="generate">
              {{ generating ? '生成中...' : (shareCode ? '重新生成' : '生成推荐码') }}
            </button>
          </div>

          <div v-if="playlistId" class="divider" />

          <!-- 导入推荐码（始终显示） -->
          <div class="import-section">
            <p class="share-desc">输入推荐码，导入他人的歌单</p>
            <div class="import-input-wrap">
              <input v-model="importCode" class="import-input" placeholder="输入推荐码，如 ADMIN-AB12CD" @keyup.enter="previewImport" />
              <button class="preview-btn" @click="previewImport" :disabled="!importCode.trim()">查询</button>
            </div>

            <div v-if="preview" class="preview-card liquid-card">
              <div class="preview-info">
                <div class="preview-name">{{ preview.name }}</div>
                <div class="preview-meta">{{ preview.songCount }} 首歌曲</div>
                <div v-if="preview.description" class="preview-desc">{{ preview.description }}</div>
              </div>
              <button class="import-btn liquid-btn" :disabled="importing" @click="doImport">
                {{ importing ? '导入中...' : '导入到我的歌单' }}
              </button>
            </div>

            <p v-if="importMsg" class="import-msg" :class="{ error: importError }">{{ importMsg }}</p>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean, playlistId: String })
const emit = defineEmits(['close'])
const plStore = usePlaylistStore()

const shareCode = ref('')
const expiry = ref(null)
const generating = ref(false)
const copied = ref(false)

const importCode = ref('')
const preview = ref(null)
const importing = ref(false)
const importMsg = ref('')
const importError = ref(false)

// 打开时加载已有推荐码
watch(() => props.visible, (v) => {
  if (v && props.playlistId) {
    const pl = plStore.playlists.find(p => p.id === props.playlistId)
    if (pl?.shareCode) {
      shareCode.value = pl.shareCode
      expiry.value = pl.shareCodeExpiry
    }
  }
})

async function generate() {
  generating.value = true
  try {
    const res = await plStore.generateShareCode(props.playlistId)
    shareCode.value = res.shareCode
    expiry.value = res.expiry
    await plStore.fetchPlaylists()
  } catch (e) {
    importMsg.value = '生成失败'
  } finally {
    generating.value = false
  }
}

async function copyCode() {
  await navigator.clipboard.writeText(shareCode.value)
  copied.value = true
  setTimeout(() => copied.value = false, 2000)
}

async function previewImport() {
  if (!importCode.value.trim()) return
  preview.value = null
  importMsg.value = ''
  try {
    preview.value = await plStore.previewShareCode(importCode.value.trim())
  } catch (e) {
    importMsg.value = e?.data?.message || '推荐码无效或已过期'
    importError.value = true
    setTimeout(() => importMsg.value = '', 3000)
  }
}

async function doImport() {
  importing.value = true
  try {
    await plStore.importByShareCode(importCode.value.trim())
    importMsg.value = '导入成功 ✓'
    importError.value = false
    preview.value = null
    importCode.value = ''
    setTimeout(() => { importMsg.value = ''; emit('close') }, 1500)
  } catch (e) {
    importMsg.value = e?.data?.message || '导入失败'
    importError.value = true
  } finally {
    importing.value = false
    setTimeout(() => importMsg.value = '', 3000)
  }
}

function formatExpiry(ts) {
  return new Date(ts).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.share-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.share-modal {
  width: 100%; max-width: 420px;
  border-radius: 20px; padding: 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.share-header { display: flex; align-items: center; justify-content: space-between; }
.share-header h3 { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.close-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; display: flex; }

.share-desc { font-size: 13px; color: var(--text-tertiary); margin-bottom: 12px; }

.code-display {
  display: flex; align-items: center; gap: 10px;
  background: rgba(255,255,255,0.05); border-radius: 10px;
  padding: 10px 14px; margin-bottom: 8px;
}
.code-text { flex: 1; font-size: 18px; font-weight: 700; letter-spacing: 0.1em; color: var(--text-primary); font-family: monospace; }
.copy-btn { padding: 5px 12px; border-radius: 7px; border: none; background: rgba(139,92,246,0.3); color: #a78bfa; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.copy-btn:hover { background: rgba(139,92,246,0.5); }
.expiry-hint { font-size: 11px; color: var(--text-tertiary); margin-bottom: 10px; }
.gen-btn { padding: 10px 20px; border-radius: 10px; border: none; background: rgba(139,92,246,0.3); color: white; font-size: 13px; cursor: pointer; }

.divider { height: 1px; background: rgba(255,255,255,0.06); }

.import-input-wrap { display: flex; gap: 8px; margin-bottom: 12px; }
.import-input {
  flex: 1; padding: 10px 12px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05);
  color: var(--text-primary); font-size: 13px; outline: none;
}
.import-input:focus { border-color: rgba(139,92,246,0.5); }
.preview-btn { padding: 10px 16px; border-radius: 10px; border: none; background: rgba(255,255,255,0.08); color: var(--text-primary); font-size: 13px; cursor: pointer; }
.preview-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.preview-card { padding: 14px; border-radius: 12px; display: flex; align-items: center; gap: 12px; }
.preview-info { flex: 1; }
.preview-name { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.preview-meta { font-size: 12px; color: var(--text-tertiary); margin-top: 2px; }
.preview-desc { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.import-btn { padding: 8px 14px; border-radius: 9px; border: none; background: rgba(74,222,128,0.2); color: #4ade80; font-size: 12px; cursor: pointer; white-space: nowrap; }
.import-msg { font-size: 13px; color: #4ade80; }
.import-msg.error { color: #f87171; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.96); }
</style>


<style>
.light-mode .share-modal {
  background: rgba(255,255,255,0.85);
}
.light-mode .share-modal .share-desc,
.light-mode .share-modal .expiry-hint {
  color: #666;
}
.light-mode .share-modal .code-display {
  background: rgba(0,0,0,0.05);
}
.light-mode .share-modal .code-text {
  color: #111;
}
.light-mode .share-modal .import-input {
  border-color: rgba(0,0,0,0.1);
  background: rgba(0,0,0,0.03);
  color: #111;
}
.light-mode .share-modal .preview-card {
  background: rgba(0,0,0,0.03);
}
.light-mode .share-modal .preview-name {
  color: #111;
}
.light-mode .share-modal .preview-meta,
.light-mode .share-modal .preview-desc {
  color: #666;
}
.light-mode .share-modal h3 {
  color: #111;
}
</style>