<template>
  <Teleport to="body">
    <Transition name="comment-slide">
      <div v-if="visible" class="comment-panel liquid-panel">

        <!-- 头部 -->
        <div class="comment-header">
          <div class="comment-title">
            <template v-if="viewMode === 'list'">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M21 6.5A2.5 2.5 0 0 0 18.5 4h-13A2.5 2.5 0 0 0 3 6.5v8A2.5 2.5 0 0 0 5.5 17H7v3l4-3h7.5a2.5 2.5 0 0 0 2.5-2.5v-8z"/>
              </svg>
              <span>评论</span>
              <span class="comment-count">{{ comments.length }}</span>
            </template>
            <template v-else>
              <button class="back-btn" @click="backToList">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M15 18l-6-6 6-6"/></svg>
              </button>
              <span>回复详情</span>
            </template>
          </div>

          <div class="header-right-actions">
            <div class="comment-sort-toggle">
              <button :class="{ active: sortMode === 'hottest' }" @click="sortMode = 'hottest'">最热</button>
              <button :class="{ active: sortMode === 'newest' }" @click="sortMode = 'newest'">最新</button>
            </div>

            <button class="close-btn" @click="$emit('close')">
              <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 歌曲信息 -->
        <div class="song-info" v-if="store.currentSong">
          <img :src="`/api/songs/${store.currentSong.id}/cover`" class="song-mini-cover" />
          <div class="song-mini-meta">
            <span class="song-mini-title">{{ store.currentSong.title }}</span>
            <span class="song-mini-artist">{{ store.currentSong.artist }}</span>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="comment-input-wrap">
          <div class="reply-hint" v-if="replyingTo">
            回复 <span class="reply-name">@{{ replyingTo.nickname }}</span>
            <button class="cancel-reply" @click="replyingTo = null">取消</button>
          </div>
          <div class="input-row">
            <div class="input-avatar">
              <img v-if="authStore.user?.avatarFile" :src="`/api/user/avatar/${authStore.user.avatarFile}`" class="avatar-img" />
              <div v-else class="avatar-placeholder">{{ authStore.user?.nickname?.[0] || 'U' }}</div>
            </div>
            <textarea v-model="inputText" class="comment-textarea"
                      :placeholder="replyingTo ? `回复 @${replyingTo.nickname}...` : '说点什么...'"
                      rows="2" maxlength="500"
                      @keydown.ctrl.enter="submitComment" />
            <button class="submit-btn" :disabled="!inputText.trim() || submitting" @click="submitComment">
              {{ submitting ? '...' : '发送' }}
            </button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="comment-body-wrapper">
          <Transition name="view-slide">

            <div v-if="viewMode === 'list'" class="comment-view-layer comment-list custom-scrollbar" ref="listRef">
              <div v-if="loading" class="comment-loading">加载中...</div>
              <div v-else-if="!comments.length" class="comment-empty">还没有评论，来说点什么吧</div>

              <div v-for="comment in sortedComments" :key="comment.id" class="comment-item">
                <div class="comment-main">
                  <div class="comment-avatar">
                    <img v-if="comment.avatarFile" :src="`/api/user/avatar/${comment.avatarFile}`" class="avatar-img" />
                    <div v-else class="avatar-placeholder">{{ comment.nickname?.[0] || 'U' }}</div>
                  </div>
                  <div class="comment-content">
                    <div class="comment-meta">
                      <span class="comment-name">{{ comment.nickname }}</span>
                      <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                    </div>
                    <p class="comment-text">{{ comment.content }}</p>
                    <div class="comment-actions">
                      <button class="action-btn like-btn" @click="likeComment(comment)">👍 {{ comment.likes }}</button>
                      <button class="action-btn reply-btn" @click="startReply(comment)">回复</button>
                    </div>

                    <div v-if="replies[comment.id]?.length" class="replies-wrap">
                      <div v-for="reply in getPreviewReplies(comment.id)" :key="reply.id" class="reply-item">
                        <span class="reply-preview-name">{{ reply.nickname }}: </span>
                        <span class="reply-preview-text">{{ reply.content }}</span>
                      </div>
                      <div v-if="replies[comment.id].length > 2" class="view-more-replies" @click="openDetail(comment)">
                        共 {{ replies[comment.id].length }} 条回复 &gt;
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="viewMode === 'detail'" class="comment-view-layer comment-list custom-scrollbar">
              <div class="detail-parent-comment comment-item">
                <div class="comment-main">
                  <div class="comment-avatar">
                    <img v-if="currentDetailComment.avatarFile" :src="`/api/user/avatar/${currentDetailComment.avatarFile}`" class="avatar-img" />
                    <div v-else class="avatar-placeholder">{{ currentDetailComment.nickname?.[0] || 'U' }}</div>
                  </div>
                  <div class="comment-content">
                    <div class="comment-meta">
                      <span class="comment-name">{{ currentDetailComment.nickname }}</span>
                      <span class="comment-time">{{ formatTime(currentDetailComment.createdAt) }}</span>
                    </div>
                    <p class="comment-text" style="font-size: 14px;">{{ currentDetailComment.content }}</p>
                  </div>
                </div>
              </div>

              <div class="detail-divider">全部回复</div>

              <div v-for="reply in sortedDetailReplies" :key="reply.id" class="comment-item reply-item-full">
                <div class="comment-main">
                  <div class="comment-avatar small">
                    <img v-if="reply.avatarFile" :src="`/api/user/avatar/${reply.avatarFile}`" class="avatar-img" />
                    <div v-else class="avatar-placeholder small">{{ reply.nickname?.[0] || 'U' }}</div>
                  </div>
                  <div class="comment-content">
                    <div class="comment-meta">
                      <span class="comment-name">{{ reply.nickname }}</span>
                      <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                    </div>
                    <p class="comment-text">{{ reply.content }}</p>
                    <div class="comment-actions">
                      <button class="action-btn like-btn" @click="likeComment(reply)">👍 {{ reply.likes }}</button>
                      <button class="action-btn reply-btn" @click="startReply(reply)">回复</button>
                      <button v-if="reply.userId === authStore.user?.id" class="action-btn delete-btn" @click="deleteComment(reply)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </Transition>
        </div>

      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])
const store = usePlayerStore()
const authStore = useAuthStore()
const { $apiFetch } = useNuxtApp()

const comments = ref([])
const replies = ref({})
const loading = ref(false)
// 排序模式状态，默认按热度倒序 (hottest)
const sortMode = ref('hottest') // 可选值: 'newest' (最新), 'hottest' (最热)

// 一个通用的排序函数，用于复用
const sortList = (list) => {
  return [...list].sort((a, b) => {
    if (sortMode.value === 'hottest') {
      // 按点赞数降序，如果点赞数相同，按时间降序
      if (b.likes !== a.likes) {
        return (b.likes || 0) - (a.likes || 0)
      }
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    } else {
      // 默认按时间倒序（最新的在前面）
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    }
  })
}

// 计算属性，返回排序后的主评论
const sortedComments = computed(() => {
  return sortList(comments.value)
})

//一个方法，用于获取排序后的子评论
const getSortedReplies = (commentId) => {
  const reps = replies.value[commentId]
  if (!reps || !reps.length) return []
  return sortList(reps)
}


const inputText = ref('')
const submitting = ref(false)
const replyingTo = ref(null)
const listRef = ref(null)

// 视图切换与详情数据
const viewMode = ref('list') // 视图模式：'list' (主列表) 或 'detail' (评论详情)
const currentDetailComment = ref(null) // 当前正在查看详情的母评论

// 获取预览回复 (主列表只取最热的2条)
const getPreviewReplies = (commentId) => {
  const reps = replies.value[commentId]
  if (!reps || !reps.length) return []
  return [...reps].sort((a, b) => (b.likes || 0) - (a.likes || 0)).slice(0, 2)
}

// 打开详情页
const openDetail = (comment) => {
  currentDetailComment.value = comment
  viewMode.value = 'detail'
  sortMode.value = 'hottest' // 进入详情页时，默认切换为“最热”
}

// 返回列表页
const backToList = () => {
  viewMode.value = 'list'
  currentDetailComment.value = null
  replyingTo.value = null
}

// 获取详情页的子评论
const sortedDetailReplies = computed(() => {
  if (!currentDetailComment.value) return []
  const reps = replies.value[currentDetailComment.value.id]
  if (!reps) return []
  return sortList(reps)
})

watch(() => [props.visible, store.currentSong?.id], async ([visible, songId]) => {
  if (visible && songId) await loadComments(songId)
})

async function loadComments(songId) {
  loading.value = true
  try {
    comments.value = await $apiFetch(`/api/comments/song/${songId}`)
    // 加载每条评论的回复
    for (const c of comments.value) {
      const r = await $apiFetch(`/api/comments/replies/${c.id}`)
      if (r.length) replies.value[c.id] = r
    }
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!inputText.value.trim() || submitting.value) return
  submitting.value = true
  try {
    // 智能判断父级 ID：如果是手动 @ 回复，就用 replyingTo 的 id；如果是处在详情页，就默认用当前母评论的 id
    const targetParentId = replyingTo.value?.id || (viewMode.value === 'detail' ? currentDetailComment.value?.id : null)

    const comment = await $apiFetch(`/api/comments/song/${store.currentSong.id}`, {
      method: 'POST',
      body: {
        content: inputText.value.trim(),
        parentId: targetParentId,
      },
    })

    if (targetParentId) {
      // 插入到子评论列表
      if (!replies.value[targetParentId]) replies.value[targetParentId] = []
      replies.value[targetParentId].push(comment)
    } else {
      // 插入到主评论列表的最前面
      comments.value.unshift(comment)
    }

    inputText.value = ''
    replyingTo.value = null
  } catch (e) {
    alert(e?.data?.message || '发送失败')
  } finally {
    submitting.value = false
  }
}

async function likeComment(comment) {
  const updated = await $apiFetch(`/api/comments/${comment.id}/like`, { method: 'POST' })
  comment.likes = updated.likes
}

async function deleteComment(comment) {
  if (!confirm('确定删除这条评论？')) return
  await $apiFetch(`/api/comments/${comment.id}`, { method: 'DELETE' })
  if (comment.parentId) {
    replies.value[comment.parentId] = replies.value[comment.parentId]?.filter(r => r.id !== comment.id)
  } else {
    comments.value = comments.value.filter(c => c.id !== comment.id)
    delete replies.value[comment.id]
  }
}

function startReply(comment) {
  replyingTo.value = comment
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.comment-panel {
  position: fixed;
  top: 16px;    /* 距离顶部留出一点呼吸空间 */
  left: 16px;   /* 距离左侧留出空隙 */
  right: 16px;  /* 距离右侧留出空隙 */
  bottom: 100px; /* 避开你的播放栏(如果你的播放栏更高，可以把 100px 改成 110px 等) */
  z-index: 1000;
  border-radius: 20px;
  display: flex; flex-direction: column;
  overflow: hidden;
}

.comment-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 12px; flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.comment-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 700; color: var(--text-primary); }
.comment-count { font-size: 12px; background: rgba(255,255,255,0.1); padding: 1px 8px; border-radius: 10px; color: var(--text-tertiary); }
.close-btn { background: none; border: none; color: var(--text-tertiary); cursor: pointer; display: flex; }

.song-info {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 20px; flex-shrink: 0;
  background: rgba(255,255,255,0.03);
}
.song-mini-cover { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; }
.song-mini-meta { display: flex; flex-direction: column; }
.song-mini-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.song-mini-artist { font-size: 11px; color: var(--text-tertiary); }

.comment-input-wrap { padding: 12px 20px; flex-shrink: 0; border-bottom: 1px solid rgba(255,255,255,0.06); }
.reply-hint { font-size: 12px; color: var(--text-tertiary); margin-bottom: 6px; display: flex; align-items: center; gap: 6px; }
.reply-name { color: #a78bfa; }
.cancel-reply { background: none; border: none; color: var(--text-tertiary); font-size: 11px; cursor: pointer; }
.input-row { display: flex; align-items: flex-end; gap: 10px; }
.input-avatar { flex-shrink: 0; }
.avatar-img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.avatar-placeholder {
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(139,92,246,0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; color: white;
}
.avatar-placeholder.small { width: 24px; height: 24px; font-size: 10px; }
.comment-textarea {
  flex: 1; padding: 8px 12px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05);
  color: var(--text-primary); font-size: 13px;
  outline: none; resize: none; font-family: inherit;
  transition: border-color 0.2s;
}
.comment-textarea:focus { border-color: rgba(139,92,246,0.5); }
.submit-btn {
  padding: 8px 16px; border-radius: 10px; border: none;
  background: rgba(246, 92, 92, 0.4); color: white;
  font-size: 13px; cursor: pointer; flex-shrink: 0;
  transition: all 0.2s;
}
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.submit-btn:not(:disabled):hover { background: rgba(246, 92, 92, 0.86); }

.comment-list { flex: 1; overflow-y: auto; padding: 12px 20px; display: flex; flex-direction: column; gap: 16px; }
.comment-loading, .comment-empty { text-align: center; color: var(--text-tertiary); font-size: 13px; padding: 20px; }

.comment-main { display: flex; gap: 10px; }
.comment-avatar { flex-shrink: 0; }
.comment-content { flex: 1; min-width: 0; }
.comment-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.comment-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.comment-time { font-size: 11px; color: var(--text-tertiary); }
.comment-text { font-size: 13px; color: var(--text-secondary); line-height: 1.5; margin: 0 0 6px; }
.comment-actions { display: flex; align-items: center; gap: 12px; }
.action-btn { background: none; border: none; color: var(--text-tertiary); font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 4px; transition: color 0.2s; }
.action-btn:hover { color: var(--text-primary); }
.delete-btn:hover { color: #f87171; }

.replies-wrap { margin-top: 10px; padding-left: 12px; border-left: 2px solid rgba(255,255,255,0.06); display: flex; flex-direction: column; gap: 10px; }
.reply-item { display: flex; gap: 8px; }


/* 评论 */
.comment-slide-enter-active, .comment-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.25, 1, 0.5, 1);
}
.comment-slide-enter-from, .comment-slide-leave-to {
  opacity: 0;
  transform: translateY(40px) scale(0.98); /* 加上轻微的缩放，毛玻璃弹出的质感更好 */
}

/* 头部右侧操作区 */
.header-right-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 评论排序切换器 */
.comment-sort-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  padding: 2px;
}
.comment-sort-toggle button {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.comment-sort-toggle button.active {
  background: rgba(246, 75, 75, 0.5); /* 使用红色的点缀色 */
  color: white;
  box-shadow: 0 2px 6px rgb(119, 1, 1);
}
.comment-sort-toggle button:hover:not(.active) {
  color: var(--text-secondary);
}

/* 评论详情 */
/* 视图包装器，用于限制绝对定位的滑动范围 */
.comment-body-wrapper {
  flex: 1; /* 这个 flex: 1 会让它自动占满下方所有的剩余空间 */
  position: relative;
  overflow: hidden;
  display: block;
  min-height: 200px; /* 给一个保底高度，防止完全收缩 */
}

/* 绝对定位的滑动层，必须占满包装器并允许内部滚动 */
.comment-view-layer {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.comment-list {
  padding: 12px 20px;
}

.comment-item {
  margin-bottom: 16px;
}

/* 视图滑动动画 */
.view-slide-enter-active, .view-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.25, 1, 0.5, 1), opacity 0.3s ease;
}
.view-slide-enter-from { transform: translateX(50%); opacity: 0; }
.view-slide-leave-to { transform: translateX(-50%); opacity: 0; }

/* 返回按钮 */
.back-btn { background: none; border: none; color: var(--text-secondary); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0 4px 0 0; }
.back-btn:hover { color: var(--text-primary); }

/* 子评论预览 */
.replies-wrap { margin-top: 10px; padding: 10px 12px; border-radius: 8px; background: rgba(255, 255, 255, 0.03); display: flex; flex-direction: column; gap: 6px; }
.reply-preview-name { font-size: 12px; color: rgb(255, 255, 255); font-weight: 500; }
.reply-preview-text { font-size: 12px; color: var(--text-secondary); }
.view-more-replies { font-size: 12px; color: var(--accent); cursor: pointer; font-weight: 500; margin-top: 2px; display: inline-block; transition: opacity 0.2s; }
.view-more-replies:hover { opacity: 0.8; }

/* 详情页特有样式 */
.detail-parent-comment { padding-bottom: 16px; border-bottom: 1px dashed rgba(255, 255, 255, 0.1); margin-bottom: 12px; }
.detail-divider { font-size: 12px; font-weight: 600; color: var(--text-tertiary); margin-bottom: 12px; }
.reply-item-full { margin-bottom: 16px; }

/* 白天模式适配补充 (如果需要) */
:global(.light-mode) .replies-wrap { background: rgba(0, 0, 0, 0.03); }
:global(.light-mode) .detail-parent-comment { border-bottom-color: rgba(0, 0, 0, 0.08); }


/* =========================================
   📱 移动端评论面板适配：悬浮在控制台上方
   ========================================= */
@media (max-width: 768px) {
  .comment-panel {
    top: 6px; /* 距离顶部留出空间，避免遮挡手机状态栏和关闭按钮 */
    left: 6px;
    right: 6px;

    /* 这个 bottom 值决定了评论面板的下边缘位置。
       你需要根据你实际手机端控制台（进度条及以下部分）的高度来微调这个值。
       如果你发现它遮挡了进度条，就把这个值改大（例如 260px）。
       如果离进度条太远了，就把这个值改小（例如 220px）。 */
    bottom: 200px;

    border-radius: 16px; /* 手机端圆角可以稍微小一点 */

    /* 增加一层更重的阴影，让它和底部的控制台有明显的层次感 */
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5), 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  /* 针对手机端稍微压缩一下内部的间距，让空间更紧凑 */
  .comment-header {
    padding: 12px 16px 8px;
  }
  .comment-input-wrap {
    padding: 10px 16px;
  }
  .comment-list {
    padding: 10px 16px;
  }
}


</style>

<style>
.light-mode .comment-panel { background: rgba(255,255,255,0.9); }
.light-mode .comment-textarea { background: rgba(0,0,0,0.03); border-color: rgba(0,0,0,0.1); color: #111; }
.light-mode .comment-text { color: #444; }
.light-mode .comment-header { border-bottom-color: rgba(0,0,0,0.06); }
.light-mode .replies-wrap { border-left-color: rgba(0,0,0,0.08); }
/* 白天模式下的排序切换器 */
.light-mode .comment-sort-toggle {
  background: rgba(0, 0, 0, 0.05);
}
.light-mode .comment-sort-toggle button {
  color: var(--text-secondary);
}
.light-mode .comment-sort-toggle button.active {
  background: rgba(246, 92, 92, 0.8);
  color: white;
}
.light-mode .comment-sort-toggle button:hover:not(.active) {
  color: var(--text-primary);
}
</style>