<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="modal-backdrop" @click.self="$emit('close')">
        <div class="ai-dj-modal liquid-panel">

          <div class="modal-header">
            <div class="header-title">
              <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24" class="ai-icon">
                <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8zm0-14a6 6 0 0 0-6 6h2a4 4 0 0 1 8 0h2a6 6 0 0 0-6-6z"/>
                <circle cx="12" cy="12" r="2"/>
              </svg>
              <h3>AI 智能 DJ</h3>
            </div>
            <button class="close-btn" @click="$emit('close')">
              <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/></svg>
            </button>
          </div>

          <div class="modal-body">

            <div class="input-section" v-if="!result && !loading">
              <p class="section-hint">告诉我你现在的状态、心情，或是想听的风格...</p>
              <textarea
                  v-model="prompt"
                  class="ai-textarea custom-scrollbar"
                  placeholder="例如：适合深夜敲代码的纯音乐、周杰伦的经典慢歌、让人心情变好的轻快旋律..."
                  rows="4"
              ></textarea>
              <button class="generate-btn liquid-btn" :disabled="!prompt.trim()" @click="generatePlaylist">
                ✨ 开始施展魔法
              </button>
              <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
            </div>

            <div class="loading-section" v-if="loading">
              <div class="ai-spinner">
                <i /><i /><i /><i />
              </div>
              <p class="loading-text">DJ 正在曲库中翻找，为你定制专属歌单...</p>
            </div>

            <div class="result-section" v-if="result && !loading">
              <div class="result-header">
                <h2 class="result-title">{{ result.playlistName }}</h2>
                <p class="result-desc">{{ result.description }}</p>
              </div>

              <div class="result-songs custom-scrollbar">
                <div v-for="(song, index) in previewSongs" :key="song.id" class="preview-song-row">
                  <span class="song-num">{{ index + 1 }}</span>
                  <div class="song-info">
                    <span class="song-name">{{ song.title }}</span>
                    <span class="song-artist">{{ song.artist }}</span>
                  </div>
                </div>
                <div v-if="result.songs.length === 0" class="empty-songs">
                  啊哦，曲库里没有找到符合条件的歌曲
                </div>
              </div>

              <div class="result-actions">
                <button class="retry-btn" @click="reset">重新生成</button>
                <button class="save-play-btn liquid-btn" v-if="result.songs.length > 0" @click="saveAndPlay" :disabled="saving">
                  {{ saving ? '保存中...' : '保存并播放' }}
                </button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const { $apiFetch } = useNuxtApp()
const plStore = usePlaylistStore()
const playerStore = usePlayerStore()

const prompt = ref('')
const loading = ref(false)
const result = ref(null)
const errorMsg = ref('')
const saving = ref(false)

// 歌曲预览数据源（由于 AI 只返回 ID，我们需要从前端仓库里匹配真实信息展示）
const previewSongs = computed(() => {
  if (!result.value || !result.value.songs) return []
  // 直接用 AI 吐出来的真实歌名和歌手展示，哪怕本地没有这首歌，UI 也不会崩！
  return result.value.songs
})

async function generatePlaylist() {
  if (!prompt.value.trim()) return
  loading.value = true
  errorMsg.value = ''

  try {
    // 调用后端 API
    const resData = await $apiFetch('/api/ai-dj/generate', {
      method: 'POST',
      body: prompt.value.trim()
    })


    console.log("🤖 DeepSeek 返回的原始数据:", resData);


    let parsedData;

    // 判断 $apiFetch 是否已经自动帮我们把 JSON 字符串解析成了对象
    if (typeof resData === 'object' && resData !== null) {
      parsedData = resData;
    } else {
      let rawStr = String(resData);

      // 1. 暴力截取：只保留第一个 { 和最后一个 } 之间的内容
      const jsonStart = rawStr.indexOf('{');
      const jsonEnd = rawStr.lastIndexOf('}');

      if (jsonStart !== -1 && jsonEnd !== -1) {
        // 截取出纯净的 JSON 字符串
        const cleanJsonStr = rawStr.substring(jsonStart, jsonEnd + 1);
        parsedData = JSON.parse(cleanJsonStr);
      } else {
        throw new Error('DJ太啰嗦了，没有找到格式化的歌单数据');
      }
    }

    // 把大模型返回的真实数据打印到控制台，查明真相
    console.log('👀 抓到 AI 返回的原始数据:', parsedData);

    // 校验数据完整性
    if (!parsedData.playlistName) {
      throw new Error('AI 返回的数据缺少 playlistName');
    }
    if (parsedData.songIds && !parsedData.songs) {
      parsedData.songs = parsedData.songIds.map(id => ({ id, title: '未知歌曲', artist: '未知' }));
    }
    if (!parsedData.songs) {
      throw new Error('AI 返回的数据既没有 songs 也没有 songIds');
    }

    // 赋值给响应式变量，触发界面更新！
    result.value = parsedData
  } catch (err) {
    console.error('AI 解析失败:', err)
    errorMsg.value = 'DJ 似乎遇到了点麻烦，请稍后再试'
  } finally {
    loading.value = false
  }
}

async function saveAndPlay() {
  if (!result.value || result.value.songs.length === 0) return
  saving.value = true

  try {
    // 之前发 POST /api/playlists 的代码保持不变...
    const newPlaylist = await $apiFetch('/api/playlists', {
      method: 'POST',
      body: {
        name: result.value.playlistName,
        description: result.value.description,
        tags: ['AI生成'] // ✅ 必须是数组！
      }
    })

    const validSongIds = previewSongs.value
        .filter(song => song.title !== '未知歌曲')
        .map(song => song.id);

    // 循环的变量名改成 songs
    for (const songId of validSongIds) {
      await plStore.addSong(newPlaylist.id, songId)
    }

    await plStore.fetchPlaylists()
    plStore.setActivePlaylist(newPlaylist.id)
    reset()
    emit('close')
  } catch (err) {
    console.error('保存歌单失败', err)
  } finally {
    saving.value = false
  }
}

function reset() {
  result.value = null
  loading.value = false
  errorMsg.value = ''
}

// 弹窗关闭时重置状态
watch(() => props.visible, (newVal) => {
  if (!newVal) {
    setTimeout(reset, 300) // 等待动画结束
  }
})
</script>

<style scoped>
.modal-backdrop {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
}
.ai-dj-modal {
  width: 90%; max-width: 440px; border-radius: 24px;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 24px 48px rgba(0,0,0,0.4), inset 0 1px 1px rgba(255,255,255,0.1);
  background: rgba(20, 20, 25, 0.85); /* 默认暗色玻璃 */
}

/* 头部 */
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 24px 16px; border-bottom: 1px solid rgba(255,255,255,0.05); }
.header-title { display: flex; align-items: center; gap: 10px; color: var(--text-primary); }
.ai-icon { color: var(--accent); filter: drop-shadow(0 0 8px rgba(250, 45, 72, 0.4)); }
.header-title h3 { font-size: 18px; font-weight: 800; letter-spacing: -0.02em; }
.close-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; transition: color 0.2s; }
.close-btn:hover { color: var(--text-primary); }

/* 主体 */
.modal-body { padding: 24px; min-height: 280px; display: flex; flex-direction: column; }

/* 输入区 */
.input-section { display: flex; flex-direction: column; gap: 16px; height: 100%; justify-content: center; }
.section-hint { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
.ai-textarea {
  width: 100%; padding: 16px; border-radius: 16px;
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08);
  color: var(--text-primary); font-size: 14px; outline: none; resize: none;
  transition: all 0.3s;
}
.ai-textarea:focus { border-color: var(--accent); background: rgba(255,255,255,0.06); box-shadow: 0 0 0 4px rgba(250, 45, 72, 0.1); }
.generate-btn { padding: 14px; border-radius: 14px; border: none; background: linear-gradient(135deg, var(--accent), #ff7e5f); color: white; font-size: 15px; font-weight: 700; cursor: pointer; transition: all 0.2s; margin-top: 8px; box-shadow: 0 8px 24px rgba(250, 45, 72, 0.3); }
.generate-btn:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
.generate-btn:not(:disabled):hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(250, 45, 72, 0.5); }
.error-msg { font-size: 12px; color: #f87171; text-align: center; }

/* 加载区 */
.loading-section { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 24px; padding: 40px 0; }
.ai-spinner { display: flex; align-items: center; gap: 6px; height: 30px; }
.ai-spinner i { width: 6px; background: var(--accent); border-radius: 3px; animation: ai-wave 1s ease-in-out infinite; }
.ai-spinner i:nth-child(2) { animation-delay: 0.15s; }
.ai-spinner i:nth-child(3) { animation-delay: 0.3s; }
.ai-spinner i:nth-child(4) { animation-delay: 0.45s; }
@keyframes ai-wave { 0%, 100% { height: 10px; } 50% { height: 30px; box-shadow: 0 0 12px var(--accent); } }
.loading-text { font-size: 14px; color: var(--text-secondary); font-weight: 500; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }

/* 结果区 */
.result-section { display: flex; flex-direction: column; height: 100%; gap: 16px; }
.result-header { text-align: center; margin-bottom: 8px; }
.result-title { font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; background: linear-gradient(to right, #fff, var(--accent)); -webkit-background-clip: text; color: transparent; }
.result-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; font-style: italic; }
.result-songs { max-height: 180px; overflow-y: auto; padding-right: 8px; display: flex; flex-direction: column; gap: 8px; }
.preview-song-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: rgba(255,255,255,0.03); border-radius: 10px; }
.song-num { font-size: 12px; color: var(--text-tertiary); font-weight: 700; width: 16px; text-align: center; }
.song-info { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.song-name { font-size: 14px; color: var(--text-primary); font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-artist { font-size: 11px; color: var(--text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.empty-songs { text-align: center; color: var(--text-tertiary); font-size: 13px; padding: 20px 0; }
.result-actions { display: flex; gap: 12px; margin-top: auto; padding-top: 16px; }
.retry-btn { flex: 1; padding: 12px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); background: transparent; color: var(--text-secondary); font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.retry-btn:hover { background: rgba(255,255,255,0.05); color: var(--text-primary); }
.save-play-btn { flex: 2; padding: 12px; border-radius: 12px; border: none; background: rgba(139,92,246,0.4); color: white; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.save-play-btn:hover:not(:disabled) { background: rgba(139,92,246,0.6); }

/* 动画 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .ai-dj-modal { transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.modal-fade-enter-from .ai-dj-modal { transform: scale(0.95) translateY(10px); }

</style>

<style>
  /* ============================================================
     🌅 白天模式终极适配 (非 scoped)
     ============================================================ */
.light-mode .ai-dj-modal {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.6);

  /* 覆盖局部文字颜色变量 */
  --text-primary: #1a1a1a;
  --text-secondary: #555555;
  --text-tertiary: #999999;
}

.light-mode .modal-header {
  border-bottom-color: rgba(0, 0, 0, 0.05);
}

.light-mode .ai-textarea {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #111;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
}

.light-mode .ai-textarea:focus {
  border-color: var(--accent);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(250, 45, 72, 0.08);
}

.light-mode .preview-song-row {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.01);
}

.light-mode .result-title {
  background: linear-gradient(135deg, #111111 0%, var(--accent) 100%);
  -webkit-background-clip: text;
  color: transparent;
}

.light-mode .result-desc {
  color: #666666;
  font-style: normal;
}

.light-mode .retry-btn {
  border-color: #d1d5db;
  color: #374151;
  background: #f9fafb;
}

.light-mode .retry-btn:hover {
  background: #f3f4f6;
  color: #111111;
  border-color: #c0c4cc;
}

.light-mode .loading-text,
.light-mode .section-hint,
.light-mode .song-num,
.light-mode .song-artist {
  color: inherit;
}
</style>