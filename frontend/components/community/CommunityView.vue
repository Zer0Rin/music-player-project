<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['toggle-modal']) // 💡 新增：定义弹窗状态抛出事件

const searchQuery = ref('')

// === 1. 模拟数据 ===
const posts = ref([
  { id: 1, category: '日常', title: '阳光落在我的鳄鱼肠上，春日小狗日记', content: '谢谢大家对仙贝的喜爱！！\n\n#小狗书 #博美 #宠物春日放风计划', author: '王仙贝', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=1&backgroundColor=b6e3f4', images: ['https://picsum.photos/seed/dog1/600/800', 'https://picsum.photos/seed/dog2/600/800'], likes: 17000, collects: 892, commentsCount: 820, isLiked: false, isCollected: false, comments: [{ id: 101, user: '云朵有点甜', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=A', text: '啊啊啊啊太可爱了！想猛吸一口！', time: '2小时前', likes: 128 }] },
  { id: 2, category: '日常', title: '洗完六辆车真的呼呼大睡一下午', content: '一口气洗了六辆“跑车”（家里的猫咪们洗澡）。洗完累瘫在沙发上...', author: '夜猫小助理', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=2&backgroundColor=ffdfbf', images: ['https://picsum.photos/seed/cat/600/600'], likes: 105000, collects: 12000, commentsCount: 3400, isLiked: true, isCollected: false, comments: [] },
  { id: 3, category: '旅行', title: '推开世界的门，新疆自驾游指南', content: '整理了半个月的新疆自驾攻略，绝对干货！#新疆旅游 #自驾游', author: '旅行日记', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=3', images: ['https://picsum.photos/seed/travel/600/1000'], likes: 16000, collects: 5000, commentsCount: 230, isLiked: false, isCollected: true, comments: [] },
  { id: 4, category: '摄影', title: '成都｜垂钓仙子，公园里的白鹭', content: '周末在浣花溪公园拍到的白鹭，生态越来越好了。', author: '摄影老法师', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=4', images: ['https://picsum.photos/seed/bird/600/700'], likes: 8402, collects: 300, commentsCount: 45, isLiked: false, isCollected: false, comments: [] },
  { id: 5, category: '穿搭', title: '今日份打工穿搭，简约不简单', content: '优衣库这件衬衫绝了，随便搭个西装裤都好看。', author: '穿搭小能手', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=5', images: ['https://picsum.photos/seed/fashion/600/900'], likes: 32000, collects: 8900, commentsCount: 560, isLiked: false, isCollected: false, comments: [] },
  { id: 6, category: '数码', title: '桌面改造计划：打造沉浸式学习环境', content: '花了一周时间重新布置了书桌，终于有了学习的氛围。', author: '数码极客', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=6', images: ['https://picsum.photos/seed/desk/600/500'], likes: 5000, collects: 1200, commentsCount: 89, isLiked: false, isCollected: false, comments: [] },
])

const categories = ['推荐', '日常', '闲置', '数码', '穿搭', '美妆', '摄影', '旅行', '图书', '日用', '其它']
const activeCategory = ref('推荐')

const handleSearchInput = () => { if (searchQuery.value.trim()) activeCategory.value = '推荐' }
const handleSearchEnter = () => { if (searchQuery.value.trim() && filteredPosts.value.length > 0) activeCategory.value = filteredPosts.value[0].category }

const highlightMatch = (text, keyword) => {
  if (!keyword || !keyword.trim()) return text;
  const escapedKeyword = keyword.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escapedKeyword})`, 'gi');
  return text.replace(regex, '<span class="text-[var(--accent)] bg-[var(--accent)]/10 px-0.5 rounded transition-colors">$1</span>');
}

const filteredPosts = computed(() => {
  let result = posts.value
  if (searchQuery.value.trim()) {
    const keyword = searchQuery.value.toLowerCase()
    result = result.filter(post => post.title.toLowerCase().includes(keyword) || post.author.toLowerCase().includes(keyword))
  }
  if (activeCategory.value !== '推荐') result = result.filter(post => post.category === activeCategory.value)
  return result
})

const formatNumber = (num) => { return num >= 10000 ? (num / 10000).toFixed(1).replace('.0', '') + '万' : num }

const toggleLike = (post, event) => { if(event) event.stopPropagation(); post.isLiked = !post.isLiked; post.isLiked ? post.likes++ : post.likes-- }
const toggleCollect = (post, event) => { if(event) event.stopPropagation(); post.isCollected = !post.isCollected; post.isCollected ? post.collects++ : post.collects-- }
const handleShare = () => { alert('分享链接已复制到剪贴板！'); }

const selectedPost = ref(null)
const mainScrollArea = ref(null)
const modalScrollArea = ref(null)
const currentImageIndex = ref(0)

const openPost = (post) => {
  selectedPost.value = post
  currentImageIndex.value = 0
  emit('toggle-modal', true) // 💡 触发父组件收起播放栏
  if (typeof document !== 'undefined') document.body.style.overflow = 'hidden'
}

const closePost = () => {
  selectedPost.value = null
  emit('toggle-modal', false) // 💡 触发父组件恢复播放栏
  if (typeof document !== 'undefined') document.body.style.overflow = 'auto'
}

const selectCategoryAndClose = (category) => { activeCategory.value = category; closePost() }
const nextImage = (e) => { if (e) e.stopPropagation(); if (selectedPost.value && currentImageIndex.value < selectedPost.value.images.length - 1) currentImageIndex.value++; }
const prevImage = (e) => { if (e) e.stopPropagation(); if (selectedPost.value && currentImageIndex.value > 0) currentImageIndex.value--; }

let modalTouchStartX = 0, modalTouchStartY = 0
const handleModalTouchStart = (e) => { modalTouchStartX = e.touches[0].clientX; modalTouchStartY = e.touches[0].clientY }
const handleModalTouchEnd = (e) => {
  const deltaX = e.changedTouches[0].clientX - modalTouchStartX
  const deltaY = e.changedTouches[0].clientY - modalTouchStartY
  if (Math.abs(deltaX) > Math.abs(deltaY)) {
    if (deltaX > 50) prevImage(); else if (deltaX < -50) nextImage();
  } else {
    if (deltaY > 80) closePost()
  }
}

const handleKeyDown = (e) => {
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
  if (selectedPost.value) {
    if (e.key === 'ArrowRight') { e.preventDefault(); nextImage(); return; }
    if (e.key === 'ArrowLeft') { e.preventDefault(); prevImage(); return; }
  }
  const targetArea = selectedPost.value ? modalScrollArea.value : mainScrollArea.value;
  if (!targetArea) return;
  const pageScrollAmount = window.innerHeight * 0.6;
  const arrowScrollAmount = 60;
  switch (e.key) {
    case 'PageDown': case ' ': e.preventDefault(); targetArea.scrollBy({ top: pageScrollAmount, behavior: 'smooth' }); break;
    case 'PageUp': e.preventDefault(); targetArea.scrollBy({ top: -pageScrollAmount, behavior: 'smooth' }); break;
    case 'ArrowDown': e.preventDefault(); targetArea.scrollBy({ top: arrowScrollAmount, behavior: 'smooth' }); break;
    case 'ArrowUp': e.preventDefault(); targetArea.scrollBy({ top: -arrowScrollAmount, behavior: 'smooth' }); break;
  }
}

onMounted(() => { window.addEventListener('keydown', handleKeyDown) })
onUnmounted(() => { window.removeEventListener('keydown', handleKeyDown) })
</script>

<template>
  <div class="relative w-full h-full px-4 md:px-8 py-4 md:py-6 flex flex-col">

    <div class="relative w-full flex items-start min-h-[48px] shrink-0">
      <div class="absolute top-0 right-0 w-[calc(100%-4rem)] sm:w-[calc(100%-5rem)] md:w-full md:left-1/2 md:-translate-x-1/2 md:max-w-2xl z-20">
        <div class="relative group">
          <div class="absolute inset-y-0 left-0 pl-5 flex items-center pointer-events-none">
            <svg class="w-5 h-5 text-[var(--text-secondary)] group-focus-within:text-[var(--accent)] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
          <input
              v-model="searchQuery"
              @input="handleSearchInput"
              @keyup.enter="handleSearchEnter"
              type="text"
              placeholder="探寻你感兴趣的内容..."
              class="w-full py-3.5 pl-14 pr-6 liquid-card rounded-full text-[var(--text-primary)] placeholder-[var(--text-tertiary)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)] transition-all duration-300"
          />
        </div>
      </div>
    </div>

    <div class="mt-8 mb-6 flex items-baseline gap-6 shrink-0 overflow-x-auto custom-scrollbar pb-2">
      <h2 class="text-3xl font-extrabold text-[var(--text-primary)] tracking-tight shrink-0">发现</h2>
      <div class="flex gap-6 text-base font-medium shrink-0">
        <button
            v-for="cat in categories"
            :key="cat"
            @click="activeCategory = cat"
            class="transition-colors whitespace-nowrap"
            :class="activeCategory === cat ? 'text-[var(--text-primary)] relative after:content-[\'\'] after:absolute after:-bottom-1.5 after:left-1/2 after:-translate-x-1/2 after:w-1/2 after:h-[3px] after:bg-[var(--accent)] after:rounded-full' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <div ref="mainScrollArea" class="flex-1 overflow-y-auto pb-10 pr-1 md:pr-2 -mr-1 md:-mr-2 custom-scrollbar scroll-smooth">
      <div v-if="filteredPosts.length === 0" class="flex flex-col items-center justify-center py-20 opacity-60">
        <p class="text-[var(--text-secondary)] text-sm">暂无相关内容~</p>
      </div>

      <div v-else class="columns-2 md:columns-3 lg:columns-4 xl:columns-5 gap-3 md:gap-5 space-y-3 md:space-y-5">
        <div
            v-for="post in filteredPosts"
            :key="post.id"
            @click="openPost(post)"
            class="break-inside-avoid relative flex flex-col liquid-card rounded-2xl overflow-hidden cursor-pointer group hover:-translate-y-1 hover:shadow-lg transition-all duration-300"
        >
          <div class="w-full relative overflow-hidden">
            <img :src="post.images[0]" loading="lazy" class="w-full h-auto object-cover transition-transform duration-700 group-hover:scale-105" />
          </div>
          <div class="p-3 flex flex-col gap-2">
            <h3 class="text-sm font-semibold text-[var(--text-primary)] line-clamp-2 leading-snug" v-html="highlightMatch(post.title, searchQuery)"></h3>
            <div class="flex items-center justify-between mt-1">
              <div class="flex items-center gap-1.5 overflow-hidden">
                <img :src="post.avatar" class="w-5 h-5 rounded-full shrink-0" />
                <span class="text-xs text-[var(--text-secondary)] truncate" v-html="highlightMatch(post.author, searchQuery)"></span>
              </div>
              <div class="flex items-center gap-1 shrink-0 transition-colors" :class="post.isLiked ? 'text-[var(--accent)]' : 'text-[var(--text-tertiary)] hover:text-[var(--text-primary)]'" @click="(e) => toggleLike(post, e)">
                <svg v-if="post.isLiked" class="w-4 h-4 bounce-anim" fill="currentColor" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
                <span class="text-xs font-medium">{{ formatNumber(post.likes) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ClientOnly>
      <Teleport to="body">
        <Transition name="fade">
          <div v-if="selectedPost" class="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 md:p-8 lg:p-12">
            <div class="absolute inset-0 bg-[var(--bg-secondary)] backdrop-blur-md transition-opacity" @click="closePost"></div>

            <div class="liquid-panel relative w-full max-w-[1100px] h-[100dvh] sm:h-[90vh] md:h-[85vh] flex flex-col md:flex-row rounded-none sm:rounded-3xl overflow-hidden shadow-2xl">
              <button @click="closePost" class="liquid-btn absolute top-4 left-4 z-30 w-9 h-9 md:w-10 md:h-10 flex items-center justify-center text-[var(--text-primary)] rounded-full transition-all hover:scale-105">
                <svg class="w-5 h-5 md:w-6 md:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
              </button>

              <div @touchstart="handleModalTouchStart" @touchend="handleModalTouchEnd" class="w-full md:w-3/5 h-[40vh] md:h-full shrink-0 md:shrink flex items-center justify-center relative group bg-transparent">
                <img :src="selectedPost.images[currentImageIndex]" class="max-w-full max-h-full object-contain drop-shadow-lg pointer-events-none transition-opacity duration-300" :key="'carousel-img-' + currentImageIndex" />
              </div>

              <div class="w-full md:w-2/5 flex-1 md:h-full flex flex-col border-t md:border-t-0 md:border-l border-[var(--border)] bg-transparent">
                <div class="flex items-center justify-between px-4 md:px-6 py-3 md:py-4 border-b border-[var(--border)] shrink-0">
                  <div class="flex items-center gap-2 md:gap-3 cursor-pointer group">
                    <img :src="selectedPost.avatar" class="w-8 h-8 md:w-10 md:h-10 rounded-full shadow-sm group-hover:scale-105 transition-transform" />
                    <span class="font-bold text-[var(--text-primary)] text-sm md:text-base" v-html="highlightMatch(selectedPost.author, searchQuery)"></span>
                  </div>
                  <button class="px-4 md:px-5 py-1.5 text-white text-xs md:text-sm font-semibold rounded-full transition-all hover:scale-105" style="background: var(--accent); box-shadow: 0 4px 12px rgba(250, 45, 72, 0.3);">关注</button>
                </div>

                <div ref="modalScrollArea" class="flex-1 overflow-y-auto p-4 md:p-6 custom-scrollbar scroll-smooth">
                  <h1 class="text-xl font-bold text-[var(--text-primary)] mb-4 leading-relaxed" v-html="highlightMatch(selectedPost.title, searchQuery)"></h1>
                  <p class="text-[15px] text-[var(--text-secondary)] leading-relaxed whitespace-pre-line mb-4">{{ selectedPost.content }}</p>

                  <div class="border-t border-[var(--border)] pt-6">
                    <h4 class="text-sm font-medium text-[var(--text-secondary)] mb-6">共 {{ selectedPost.commentsCount }} 条评论</h4>
                    <div v-if="selectedPost.comments && selectedPost.comments.length > 0" class="space-y-6">
                      <div v-for="comment in selectedPost.comments" :key="comment.id" class="flex gap-3 group">
                        <img :src="comment.avatar" class="w-8 h-8 rounded-full shrink-0 cursor-pointer" />
                        <div class="flex-1">
                          <div class="text-[13px] font-medium text-[var(--text-secondary)] cursor-pointer">{{ comment.user }}</div>
                          <p class="text-[14px] text-[var(--text-primary)] mt-1 leading-snug">{{ comment.text }}</p>
                          <div class="flex items-center gap-4 mt-2 text-xs text-[var(--text-tertiary)]">
                            <span>{{ comment.time }}</span><span class="hover:text-[var(--text-primary)] cursor-pointer">回复</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="px-4 md:px-6 py-3 md:py-4 border-t border-[var(--border)] shrink-0 flex items-center gap-3 md:gap-5 bg-transparent">
                  <div class="flex-1 liquid-card rounded-full px-4 py-2 flex items-center transition-colors">
                    <input type="text" placeholder="说点什么..." class="w-full bg-transparent text-[14px] focus:outline-none text-[var(--text-primary)] placeholder-[var(--text-tertiary)]" />
                  </div>
                </div>

              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </ClientOnly>

  </div>
</template>