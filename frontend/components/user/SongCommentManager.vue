<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="comment-manager-backdrop" @click.self="$emit('close')">
        <div class="comment-manager liquid-panel">

          <div class="cm-header">
            <div class="cm-title">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M21 6.5A2.5 2.5 0 0 0 18.5 4h-13A2.5 2.5 0 0 0 3 6.5v8A2.5 2.5 0 0 0 5.5 17H7v3l4-3h7.5a2.5 2.5 0 0 0 2.5-2.5v-8z"/>
              </svg>
              <span>评论管理</span>
              <span class="cm-song-name" :title="song?.title">— {{ song?.title }}</span>
              <span class="cm-count">{{ allComments.length }} 条</span>
            </div>

            <div class="cm-header-actions">
              <button class="cm-close-btn" @click="$emit('close')" title="关闭">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="cm-toolbar">
            <div class="cm-search-wrap">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" class="search-icon">
                <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
              </svg>
              <input v-model="searchQuery" type="text" placeholder="搜索评论内容或用户名..." class="cm-search-input" />
            </div>

            <div class="cm-tools-right">
              <button class="ai-btn" @click="handleAiSearch" :disabled="isAiLoading">
                <svg v-if="!isAiLoading" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                  <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-8 14H9v-2h2v2zm0-4H9V7h2v6zm4 4h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                </svg>
                <svg v-else class="spin-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56"></path>
                </svg>
                {{ isAiLoading ? 'Agent 分析中...' : 'AI 检索' }}
              </button>

              <div class="cm-sort-toggle">
                <button :class="{ active: sortMode === 'hottest' }" @click="sortMode = 'hottest'">最热</button>
                <button :class="{ active: sortMode === 'newest' }" @click="sortMode = 'newest'">最新</button>
              </div>
            </div>
          </div>

          <div v-if="hasAiAnalyzed" class="ai-filter-bar">
            <span class="ai-filter-label">✧ AI 智能分类：</span>
            <button :class="['ai-filter-btn', { active: activeAiFilter === 'all' }]" @click="activeAiFilter = 'all'">全部</button>
            <button :class="['ai-filter-btn pos', { active: activeAiFilter === 'positive' }]" @click="activeAiFilter = 'positive'">🟢 正面友好</button>
            <button :class="['ai-filter-btn neg', { active: activeAiFilter === 'negative' }]" @click="activeAiFilter = 'negative'">🔴 负面/引战</button>
            <button :class="['ai-filter-btn spam', { active: activeAiFilter === 'spam' }]" @click="activeAiFilter = 'spam'">🟡 垃圾/灌水</button>
            <button
                v-if="activeAiFilter !== 'all' && filteredAndSortedComments.length > 0"
                class="batch-delete-btn"
                @click="batchDeleteFiltered"
                :disabled="isBatchDeleting"
            >
              <svg v-if="!isBatchDeleting" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
              <svg v-else class="spin-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"></path>
              </svg>
              {{ isBatchDeleting ? '正在清理...' : `一键清理当前 ${filteredAndSortedComments.length} 条评论` }}
            </button>
          </div>

          <div class="cm-stats">
            <div class="stat-card">
              <span class="stat-num">{{ allComments.length }}</span>
              <span class="stat-label">总评论</span>
            </div>
            <div class="stat-card">
              <span class="stat-num">{{ totalLikes }}</span>
              <span class="stat-label">总点赞</span>
            </div>
            <div class="stat-card">
              <span class="stat-num">{{ totalReplies }}</span>
              <span class="stat-label">总回复</span>
            </div>
          </div>

          <div class="cm-body custom-scrollbar">
            <div v-if="loading" class="cm-empty">加载中...</div>
            <div v-else-if="!filteredAndSortedComments.length" class="cm-empty">
              {{ searchQuery ? '没有找到符合条件的评论' : '暂无评论' }}
            </div>

            <div v-for="comment in filteredAndSortedComments" :key="comment.id" class="cm-comment-item">
              <div class="cm-comment-main">
                <div class="cm-avatar">
                  <img v-if="comment.avatarFile" :src="`/api/user/avatar/${comment.avatarFile}`" class="avatar-img" />
                  <div v-else class="avatar-placeholder">{{ comment.nickname?.[0] || 'U' }}</div>
                </div>
                <div class="cm-comment-body">
                  <div class="cm-comment-meta">
                    <span class="cm-name" v-html="highlight(comment.nickname)"></span>
                    <span v-if="comment.aiTag" :class="['ai-badge', `badge-${comment.aiTag}`]">
                      {{ getAiTagName(comment.aiTag) }}
                    </span>
                    <span class="cm-time">{{ formatTime(comment.createdAt) }}</span>
                    <span class="cm-likes">👍 {{ comment.likes }}</span>
                    <span class="cm-reply-count" v-if="getFilteredReplies(comment.id).length">
                      💬 {{ getFilteredReplies(comment.id).length }} 条回复
                    </span>
                  </div>
                  <p class="cm-content" v-html="highlight(comment.content)"></p>
                </div>
                <button class="cm-delete-btn" @click="deleteComment(comment)" title="删除评论">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                  </svg>
                </button>
              </div>

              <div v-if="getFilteredReplies(comment.id).length" class="cm-replies">
                <div v-for="reply in getFilteredReplies(comment.id)" :key="reply.id" class="cm-reply-item">
                  <div class="cm-avatar small">
                    <img v-if="reply.avatarFile" :src="`/api/user/avatar/${reply.avatarFile}`" class="avatar-img" />
                    <div v-else class="avatar-placeholder small">{{ reply.nickname?.[0] || 'U' }}</div>
                  </div>
                  <div class="cm-comment-body">
                    <div class="cm-comment-meta">
                      <span class="cm-name" v-html="highlight(reply.nickname)"></span>
                      <span class="cm-time">{{ formatTime(reply.createdAt) }}</span>
                      <span class="cm-likes">👍 {{ reply.likes }}</span>
                    </div>
                    <p class="cm-content" v-html="highlight(reply.content)"></p>
                  </div>
                  <button class="cm-delete-btn" @click="deleteReply(reply, comment.id)" title="删除回复">
                    <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                      <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({ visible: Boolean, song: Object })
const emit = defineEmits(['close'])
const { $apiFetch } = useNuxtApp()

const allComments = ref([])
const replies = ref({})
const loading = ref(false)

// === 状态 ===
const searchQuery = ref('')
const sortMode = ref('hottest') // 默认值为 'hottest'

// === 评论管理 AI Agent 状态 ===
const isAiLoading = ref(false)
const hasAiAnalyzed = ref(false)
const activeAiFilter = ref('all') // 'all', 'positive', 'negative', 'spam'

// === 统计 ===
const totalLikes = computed(() => {
  let n = allComments.value.reduce((s, c) => s + c.likes, 0)
  Object.values(replies.value).forEach(rs => rs.forEach(r => n += r.likes))
  return n
})

const totalReplies = computed(() =>
    Object.values(replies.value).reduce((s, rs) => s + rs.length, 0)
)

// === 核心逻辑：排序与搜索 ===
const sortFn = (a, b) => {
  if (sortMode.value === 'hottest') {
    if (b.likes !== a.likes) return (b.likes || 0) - (a.likes || 0)
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  }
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
}

// === 主评论计算属性 (支持 AI 过滤) ===
const filteredAndSortedComments = computed(() => {
  let result = [...allComments.value]

  // 1. AI 标签过滤
  if (hasAiAnalyzed.value && activeAiFilter.value !== 'all') {
    result = result.filter(c => c.aiTag === activeAiFilter.value)
  }

  // 2. 搜索过滤 (原有逻辑)
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(c =>
        c.content.toLowerCase().includes(query) ||
        c.nickname.toLowerCase().includes(query) ||
        (replies.value[c.id] && replies.value[c.id].some(r =>
            r.content.toLowerCase().includes(query) || r.nickname.toLowerCase().includes(query)
        ))
    )
  }

  // 3. 排序 (原有逻辑)
  return result.sort(sortFn)
})

// 子回复过滤获取方法
function getFilteredReplies(commentId) {
  const reps = replies.value[commentId]
  if (!reps) return []

  let result = [...reps]
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(r =>
        r.content.toLowerCase().includes(query) || r.nickname.toLowerCase().includes(query)
    )
  }
  return result.sort(sortFn)
}

// 高亮搜索词
function highlight(text) {
  if (!searchQuery.value || !text) return text
  const regex = new RegExp(`(${searchQuery.value})`, 'gi')
  return text.replace(regex, '<span style="color: #f60303; font-weight: bold;">$1</span>')
}

// AI 检索
async function handleAiSearch() {
  if (!allComments.value.length) {
    return alert('当前没有评论可以分析哦')
  }

  isAiLoading.value = true
  hasAiAnalyzed.value = false

  try {
    // 1. 组装发给后端的 payload (只挑出 id 和 content，不要把时间、头像全发过去，给 DeepSeek 节省 Token！)
    const payload = allComments.value.map(c => ({
      id: c.id,
      content: c.content
    }))

    // 2. 调用后端接口
    const resData = await $apiFetch('/api/admin/comments/ai-analyze', {
      method: 'POST',
      body: payload
    })

    console.log("🤖 DeepSeek 返回的原始数据:", resData);
    const finalData = typeof resData === 'string' ? JSON.parse(resData) : resData;

    // 3. finalData 取值
    allComments.value.forEach(c => {
      c.aiTag = finalData[c.id] || 'neutral'
    })

    // 4. 展开分类过滤器
    hasAiAnalyzed.value = true
    activeAiFilter.value = 'all'

  } catch (e) {
    alert('AI 分析好像出了点问题：' + (e?.data?.message || e?.message || '未知错误'))
  } finally {
    isAiLoading.value = false
  }
}

// 辅助方法：把英文 tag 转成中文显示在评论上
function getAiTagName(tag) {
  const map = { 'positive': '正面', 'negative': '负面/恶评', 'spam': '垃圾/广告', 'neutral': '中性' }
  return map[tag] || tag
}

// === 数据加载与删除 ===
watch(() => [props.visible, props.song?.id], async ([visible, songId]) => {
  if (visible && songId) await loadComments(songId)
})

async function loadComments(songId) {
  loading.value = true
  allComments.value = []
  replies.value = {}
  searchQuery.value = '' // 打开面板时重置搜索
  try {
    allComments.value = await $apiFetch(`/api/comments/song/${songId}`)
    for (const c of allComments.value) {
      const rs = await $apiFetch(`/api/comments/replies/${c.id}`)
      if (rs.length) replies.value[c.id] = rs
    }
  } finally {
    loading.value = false
  }
}

async function deleteComment(comment) {
  if(!confirm('确定删除这条主评论及其所有回复吗？')) return
  try {
    await $apiFetch(`/api/comments/${comment.id}`, { method: 'DELETE' })
    allComments.value = allComments.value.filter(c => c.id !== comment.id)
    delete replies.value[comment.id]
  } catch (e) {
    alert(e?.data?.message || '删除失败')
  }
}

// === 批量删除状态与方法 ===
const isBatchDeleting = ref(false)

async function batchDeleteFiltered() {
  const commentsToDelete = filteredAndSortedComments.value
  if (!commentsToDelete.length) return

  const tagName = getAiTagName(activeAiFilter.value)

  // 🚨 严肃的二次确认拦截
  const confirmMsg = `🚨 危险操作确认 🚨\n\n确定要永久删除当前列表中的 ${commentsToDelete.length} 条【${tagName}】主评论及其包含的所有回复吗？\n\n此操作不可恢复！`
  if (!confirm(confirmMsg)) {
    return
  }

  isBatchDeleting.value = true
  try {
    // 这里我们使用 Promise.all 并发调用单条删除接口。
    // (如果你的评论数量一次性极大，建议以后在 Java 后端写一个接收 List<Long> ids 的批量删除接口)
    await Promise.all(
        commentsToDelete.map(c =>
            $apiFetch(`/api/comments/${c.id}`, { method: 'DELETE' })
                .catch(e => console.warn(`评论 ${c.id} 删除失败:`, e))
        )
    )

    // 删除成功后，从前端本地数据中把它们剔除
    const idsToDelete = new Set(commentsToDelete.map(c => c.id))
    allComments.value = allComments.value.filter(c => !idsToDelete.has(c.id))

    // 同步清理本地的子回复数据
    idsToDelete.forEach(id => {
      delete replies.value[id]
    })

    alert(`✅ 爽！已成功清理 ${commentsToDelete.length} 条【${tagName}】评论。`)

  } catch (e) {
    alert('批量删除过程中出现错误：' + (e?.data?.message || e?.message || '未知错误'))
  } finally {
    isBatchDeleting.value = false
  }
}


async function deleteReply(reply, parentId) {
  if(!confirm('确定删除这条回复吗？')) return
  try {
    await $apiFetch(`/api/comments/${reply.id}`, { method: 'DELETE' })
    replies.value[parentId] = replies.value[parentId]?.filter(r => r.id !== reply.id)
  } catch (e) {
    alert(e?.data?.message || '删除失败')
  }
}

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.comment-manager-backdrop {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.6); /* 稍微降低背景黑度 */
  backdrop-filter: blur(12px);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}

.comment-manager {
  width: 100%; max-width: 760px; height: 85vh;
  border-radius: 20px; display: flex; flex-direction: column; overflow: hidden;
  /* 💡 提亮主面板背景 */
  background: rgba(30, 30, 35, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 48px rgba(0,0,0,0.5);
}

.cm-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px; flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08); /* 提亮分割线 */
}
.cm-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #ffffff; }
.cm-song-name { color: rgba(255,255,255,0.6); font-weight: 400; font-size: 13px; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cm-count { font-size: 11px; background: rgba(255,255,255,0.15); padding: 2px 8px; border-radius: 12px; color: rgba(255,255,255,0.8); }
.cm-close-btn { background: none; border: none; color: rgba(255,255,255,0.5); cursor: pointer; display: flex; padding: 4px; border-radius: 50%; transition: all 0.2s; }
.cm-close-btn:hover { color: #f87171; background: rgba(248,113,113,0.15); transform: rotate(90deg); }

/* --- 工具栏 --- */
.cm-toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 12px 24px; flex-shrink: 0;
  background: rgba(255,255,255,0.03); /* 提亮工具栏底色 */
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.cm-search-wrap { flex: 1; max-width: 320px; position: relative; display: flex; align-items: center; }
.search-icon { position: absolute; left: 12px; color: rgba(255,255,255,0.5); }
.cm-search-input {
  width: 100%; padding: 8px 12px 8px 34px; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.06); /* 💡 提亮搜索框 */
  color: #ffffff; font-size: 13px; outline: none; transition: all 0.2s;
}
.cm-search-input:focus { border-color: rgba(139, 92, 246, 0.6); background: rgba(255,255,255,0.1); }

.cm-tools-right { display: flex; align-items: center; gap: 16px; }

.ai-btn {
  display: flex; align-items: center; gap: 6px; padding: 6px 12px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(236, 72, 153, 0.25));
  border: 1px solid rgba(139, 92, 246, 0.4); border-radius: 8px;
  color: #e9d5ff; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.ai-btn:hover { background: linear-gradient(135deg, rgba(139, 92, 246, 0.4), rgba(236, 72, 153, 0.4)); color: #fff; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3); }

.cm-sort-toggle { display: flex; background: rgba(255, 255, 255, 0.08); border-radius: 6px; padding: 2px; }
.cm-sort-toggle button {
  background: transparent; border: none; color: rgba(255,255,255,0.6); font-size: 12px; font-weight: 600;
  padding: 4px 12px; border-radius: 4px; cursor: pointer; transition: all 0.2s ease;
}
.cm-sort-toggle button.active { background: rgba(139, 92, 246, 0.7); color: white; box-shadow: 0 2px 6px rgba(139, 92, 246, 0.3); }
.cm-sort-toggle button:hover:not(.active) { color: #ffffff; background: rgba(255,255,255,0.05); }

/* --- 统计栏 --- */
.cm-stats {
  display: flex; gap: 16px; padding: 16px 24px; flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.stat-card {
  flex: 1; padding: 16px; border-radius: 12px;
  background: rgba(255,255,255,0.05); /* 💡 提亮统计卡片 */
  text-align: center; border: 1px solid rgba(255,255,255,0.02); transition: all 0.2s;
}
.stat-card:hover { background: rgba(255,255,255,0.08); border-color: rgba(255,255,255,0.15); transform: translateY(-2px); }
.stat-num { display: block; font-size: 24px; font-weight: 700; color: #ffffff; font-family: monospace; }
.stat-label { font-size: 12px; color: rgba(255,255,255,0.6); font-weight: 500; }

/* --- 列表体 --- */
.cm-body { flex: 1; overflow-y: auto; padding: 16px 24px; display: flex; flex-direction: column; gap: 16px; }
.cm-empty { text-align: center; color: rgba(255,255,255,0.5); font-size: 14px; padding: 60px; font-weight: 500; }

.cm-comment-item {
  padding: 16px; border-radius: 14px;
  background: rgba(255,255,255,0.05); /* 💡 提亮评论卡片 */
  border: 1px solid rgba(255,255,255,0.08);
  transition: border-color 0.2s;
}
.cm-comment-item:hover { border-color: rgba(255,255,255,0.15); background: rgba(255,255,255,0.07); }

.cm-comment-main { display: flex; gap: 12px; align-items: flex-start; }
.cm-avatar { flex-shrink: 0; }
.avatar-img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }
.avatar-img.small { width: 28px; height: 28px; }
.avatar-placeholder { width: 36px; height: 36px; border-radius: 50%; background: rgba(139,92,246,0.4); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; color: white; }
.avatar-placeholder.small { width: 28px; height: 28px; font-size: 11px; }

.cm-comment-body { flex: 1; min-width: 0; }
.cm-comment-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; flex-wrap: wrap; }
.cm-name { font-size: 14px; font-weight: 600; color: #ffffff; }
.cm-time { font-size: 12px; color: rgba(255,255,255,0.5); }
.cm-likes { font-size: 12px; color: rgba(255,255,255,0.6); background: rgba(255,255,255,0.08); padding: 2px 6px; border-radius: 6px;}
.cm-reply-count { font-size: 12px; color: #c4b5fd; background: rgba(139, 92, 246, 0.2); padding: 2px 6px; border-radius: 6px; }
.cm-content { font-size: 14px; color: rgba(255,255,255,0.85); line-height: 1.6; margin: 0; word-break: break-all; }

.cm-delete-btn {
  flex-shrink: 0; background: none; border: 1px solid transparent;
  color: rgba(255,255,255,0.4); cursor: pointer;
  padding: 8px; border-radius: 8px; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.cm-delete-btn:hover { color: #fca5a5; background: rgba(248,113,113,0.15); border-color: rgba(248,113,113,0.3); transform: scale(1.05); }

.cm-replies {
  margin-top: 14px; padding: 12px 16px;
  background: rgba(255,255,255,0.03); /* 💡 提亮回复区域 */
  border-radius: 12px;
  border-left: 3px solid rgba(139, 92, 246, 0.5);
  display: flex; flex-direction: column; gap: 12px;
}
.cm-reply-item { display: flex; gap: 10px; align-items: flex-start; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.96) translateY(10px); }


/* AI检索 */
/* AI 按钮加载动画 */
.spin-icon { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }
.ai-btn:disabled { opacity: 0.7; cursor: not-allowed; }

/* AI 过滤器面板 */
.ai-filter-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 24px; background: rgba(139, 92, 246, 0.08);
  border-bottom: 1px solid rgba(139, 92, 246, 0.2);
  flex-wrap: wrap;
}
.ai-filter-label { font-size: 12px; color: #c4b5fd; font-weight: 600; margin-right: 4px; }
.ai-filter-btn {
  background: rgba(255,255,255,0.05); border: 1px solid transparent; color: rgba(255,255,255,0.7);
  padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.ai-filter-btn:hover { background: rgba(255,255,255,0.1); color: #fff; }

/* 选中状态的绚丽颜色 */
.ai-filter-btn.active { background: rgba(255,255,255,0.2); color: #fff; border-color: rgba(255,255,255,0.4); box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.ai-filter-btn.pos.active { background: rgba(34, 197, 94, 0.2); color: #4ade80; border-color: rgba(34, 197, 94, 0.5); }
.ai-filter-btn.neg.active { background: rgba(239, 68, 68, 0.2); color: #f87171; border-color: rgba(239, 68, 68, 0.5); }
.ai-filter-btn.spam.active { background: rgba(234, 179, 8, 0.2); color: #facc15; border-color: rgba(234, 179, 8, 0.5); }

/* 评论列表中的 AI 小徽章 */
.ai-badge { font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 600; border: 1px solid transparent; }
.badge-positive { color: #4ade80; background: rgba(34, 197, 94, 0.1); border-color: rgba(34, 197, 94, 0.2); }
.badge-negative { color: #f87171; background: rgba(239, 68, 68, 0.1); border-color: rgba(239, 68, 68, 0.2); }
.badge-spam { color: #facc15; background: rgba(234, 179, 8, 0.1); border-color: rgba(234, 179, 8, 0.2); }
.badge-neutral { color: #9ca3af; background: rgba(156, 163, 175, 0.1); }

/* 批量删除 AI分类 */
/* 💡 批量清理按钮 */
.batch-delete-btn {
  margin-left: auto; /* 把按钮推到最右边 */
  display: flex; align-items: center; gap: 6px;
  background: rgba(239, 68, 68, 0.15); /* 警示红 */
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #fca5a5;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.batch-delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.8);
  color: #ffffff;
  border-color: #ef4444;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.batch-delete-btn:active:not(:disabled) {
  transform: translateY(1px) scale(0.96);
}

.batch-delete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}


</style>

<style>
.light-mode .comment-manager-backdrop { background: rgba(255, 255, 255, 0.6); }
.light-mode .comment-manager {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 24px 48px rgba(0,0,0,0.1);
  border: 1px solid rgba(0,0,0,0.05);
}

.light-mode .cm-header,
.light-mode .cm-toolbar,
.light-mode .cm-stats {
  border-bottom-color: rgba(0,0,0,0.06);
}

/* 文字颜色覆盖 */
.light-mode .cm-title,
.light-mode .stat-num,
.light-mode .cm-name { color: #111827; }

.light-mode .cm-song-name,
.light-mode .stat-label,
.light-mode .cm-time,
.light-mode .search-icon { color: #6b7280; }

.light-mode .cm-content { color: #374151; }

.light-mode .cm-close-btn { color: #9ca3af; }
.light-mode .cm-close-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); }

/* 工具栏元素 */
.light-mode .cm-toolbar { background: rgba(0,0,0,0.01); }
.light-mode .cm-search-input {
  background: rgba(0,0,0,0.03);
  border-color: rgba(0,0,0,0.1);
  color: #111;
}
.light-mode .cm-search-input:focus {
  background: rgba(255,255,255,1);
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139,92,246,0.1);
}

.light-mode .cm-count { background: rgba(0,0,0,0.05); color: #4b5563; }

.light-mode .cm-sort-toggle { background: rgba(0,0,0,0.05); }
.light-mode .cm-sort-toggle button { color: #4b5563; }
.light-mode .cm-sort-toggle button.active { background: #8b5cf6; color: white; box-shadow: 0 2px 6px rgba(139,92,246,0.3); }
.light-mode .cm-sort-toggle button:hover:not(.active) { background: rgba(0,0,0,0.05); color: #111; }

.light-mode .ai-btn {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(236, 72, 153, 0.1));
  color: #7c3aed;
  border-color: rgba(139, 92, 246, 0.2);
}
.light-mode .ai-btn:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(236, 72, 153, 0.15));
  border-color: rgba(139, 92, 246, 0.4);
}

/* 列表容器 */
.light-mode .stat-card { background: rgba(0,0,0,0.02); border-color: transparent; }
.light-mode .stat-card:hover { background: rgba(0,0,0,0.04); border-color: rgba(0,0,0,0.05); }

.light-mode .cm-comment-item {
  background: rgba(255,255,255,1);
  border-color: rgba(0,0,0,0.06);
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
.light-mode .cm-comment-item:hover { border-color: rgba(0,0,0,0.15); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.light-mode .cm-likes { background: rgba(0,0,0,0.04); color: #4b5563; }
.light-mode .cm-reply-count { background: rgba(139, 92, 246, 0.1); color: #7c3aed; }

.light-mode .cm-replies {
  background: rgba(0,0,0,0.02);
  border-left-color: rgba(139, 92, 246, 0.4);
}

.light-mode .cm-delete-btn { color: #9ca3af; }
.light-mode .cm-delete-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); border-color: rgba(239, 68, 68, 0.2); }

/* 白天模式下的批量删除按钮 (补充到全局 .light-mode 样式中) */
:global(.light-mode) .batch-delete-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.2);
}
:global(.light-mode) .batch-delete-btn:hover:not(:disabled) {
  background: #ef4444;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
}


</style>