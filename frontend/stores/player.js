import { defineStore } from 'pinia'

export const usePlayerStore = defineStore('player', {
  state: () => ({
    playlist: [],
    currentSong: null,
    currentIndex: -1,
    playOrder: [],
    orderCursor: -1,
    isPlaying: false,
    currentTime: 0,
    duration: 0,
    lyricsText: '',
    parsedLyrics: [],
    volume: 0.8,
    playMode: 'sequence',
    lyricMode: 'line',
    showLyricView: false,
    recentSongs: typeof window !== 'undefined' ? JSON.parse(localStorage.getItem('recentSongs') || '[]') : [],// 为了搭建 ”最近“板块 新增：最近播放列表（安全读取本地存储）
  }),

  getters: {
    currentLyricIndex(state) {
      if (!state.parsedLyrics.length) return -1
      let index = 0
      for (let i = state.parsedLyrics.length - 1; i >= 0; i--) {
        if (state.parsedLyrics[i].time <= state.currentTime + 0.3) {
          index = i
          break
        }
      }
      return index
    },

    /** 当前行内已过去的秒数（用于逐字高亮） */
    currentWordElapsed(state) {
        const idx = this.currentLyricIndex
        if (idx < 0 || !state.parsedLyrics[idx]) return 0
        // 精确到毫秒，避免浮点比较丢帧
        return Math.round((state.currentTime - state.parsedLyrics[idx].time) * 1000) / 1000
    },

    progress(state) {
      if (!state.duration) return 0
      return state.currentTime / state.duration
    },
    formattedCurrentTime(state) { return formatTime(state.currentTime) },
    formattedDuration(state) { return formatTime(state.duration) },
    hasNext(state) { return state.playlist.length > 0 },
    hasPrev(state) { return state.playlist.length > 0 },
  },

  actions: {
      setPlaylist(songs) {
          this.playlist = songs
          this.updatePlayOrder()
      },

      updatePlayOrder() {
          const n = this.playlist.length
          if (n === 0) {
              this.playOrder = []
              this.orderCursor = -1
              return
          }

          if (this.playMode === 'shuffle') {
              // 洗牌模式：当前歌曲固定在最前面，剩下的打乱
              const rest = []
              for (let i = 0; i < n; i++) {
                  if (i !== this.currentIndex) rest.push(i)
              }
              // Fisher-Yates 洗牌算法
              for (let i = rest.length - 1; i > 0; i--) {
                  const j = Math.floor(Math.random() * (i + 1));
                  [rest[i], rest[j]] = [rest[j], rest[i]]
              }
              this.playOrder = this.currentIndex >= 0 ? [this.currentIndex, ...rest] : rest
              this.orderCursor = this.currentIndex >= 0 ? 0 : -1
          } else {
              // 顺序/循环模式：标准的 0 到 n-1
              this.playOrder = Array.from({length: n}, (_, i) => i)
              this.orderCursor = this.currentIndex >= 0 ? this.currentIndex : -1
          }
      },

      // 💡 3. 切换模式时，立刻重新生成队列
      togglePlayMode() {
          const modes = ['sequence', 'loop-all', 'loop-one', 'shuffle']
          const i = modes.indexOf(this.playMode)
          this.playMode = modes[(i + 1) % modes.length]
          this.updatePlayOrder()
      },

      async playSong(song, index) {
          this.currentSong = song
          this.currentIndex = index
          this.currentTime = 0
          this.isPlaying = true

          // 同步 orderCursor
          const cursor = this.playOrder.indexOf(index)
          if (cursor !== -1) {
              this.orderCursor = cursor
          } else {
              this.updatePlayOrder() // 兜底：如果是第一次加载
          }

          // 为添加 最近 新增：每次播放新歌时，记录到最近播放
          this.addRecentSong(song)

          await this.loadLyrics(song.id)
      },
      addRecentSong(song) {
          // 过滤掉已存在的同一首歌，提置顶
          this.recentSongs = this.recentSongs.filter(s => s.id !== song.id)
          this.recentSongs.unshift(song)
          // 限制 100 首防止爆内存
          if (this.recentSongs.length > 100) this.recentSongs.pop()
          // 持久化到本地
          if (typeof window !== 'undefined') {
              localStorage.setItem('recentSongs', JSON.stringify(this.recentSongs))
          }
      },
      async loadLyrics(songId) {
          try {
              const { $apiFetch } = useNuxtApp()
              this.lyricsText = await $apiFetch(`/api/songs/${songId}/lyrics`)
              this.parsedLyrics = parseLrc(this.lyricsText)
          } catch {
              this.lyricsText = ''
              this.parsedLyrics = []
          }
      },

      async nextSong(isAutoEnd = false) {
          if (!this.playlist.length || this.orderCursor === -1) return

          let nextCursor = this.orderCursor + 1

          if (nextCursor < this.playOrder.length) {
              // 还没播完队列，正常下一首
              const nextIdx = this.playOrder[nextCursor]
              await this.playSong(this.playlist[nextIdx], nextIdx)
          } else {
              // 队列播完了（不论是正常的还是洗牌后的）
              if (this.playMode === 'sequence' && isAutoEnd) {
                  this.isPlaying = false
              } else if (this.playMode === 'shuffle') {
                  // 随机模式播完一轮：重新洗牌，继续循环
                  this.updatePlayOrder()
                  const nextIdx = this.playOrder[0]
                  await this.playSong(this.playlist[nextIdx], nextIdx)
              } else {
                  // loop-all：回到队列开头
                  this.orderCursor = -1
                  const nextIdx = this.playOrder[0]
                  await this.playSong(this.playlist[nextIdx], nextIdx)
              }
          }
      },
      async prevSong() {
          if (!this.playlist.length || this.orderCursor === -1) return

          let prevCursor = this.orderCursor - 1

          if (prevCursor >= 0) {
              const prevIdx = this.playOrder[prevCursor]
              await this.playSong(this.playlist[prevIdx], prevIdx)
          } else {
              // 已经是队列第一首，跳到队列最后一首（闭环）
              const prevIdx = this.playOrder[this.playOrder.length - 1]
              await this.playSong(this.playlist[prevIdx], prevIdx)
          }
      },
    updateTime(time) { this.currentTime = time },
    setDuration(d) { this.duration = d },
    togglePlay() { this.isPlaying = !this.isPlaying },
    openLyricView() { this.showLyricView = true },
    closeLyricView() { this.showLyricView = false },

    toggleLyricMode() {
          this.lyricMode = this.lyricMode === 'word' ? 'line' : 'word'
      },


  },
})

// ========== 解析器 ==========

/**
 * 解析歌词，支持三种格式：
 * 1. 带逐字+双语:  [00:43.12]こ{0.00}の{0.16} / 在{0.00}这{0.16}
 * 2. 普通双语:      [00:43.12]歌词内容 / 翻译内容
 * 3. 普通LRC:       [00:43.12]歌词内容
 *
 * 普通LRC会自动生成伪逐字时间戳（字数/行时长均分）
 */
function parseLrc(lrc) {
  if (!lrc) return []
  const result = []
  const lineRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/

  for (const rawLine of lrc.split('\n')) {
    const match = rawLine.match(lineRegex)
    if (!match) continue

    const time = parseInt(match[1]) * 60 + parseInt(match[2]) + parseInt(match[3].padEnd(3, '0')) / 1000
    const content = match[4].trim()
    if (!content) continue

    // 用 ' / ' 分割原文和翻译
    const slashIdx = content.indexOf(' / ')
    let originalRaw, translationRaw

    if (slashIdx > 0) {
      originalRaw = content.substring(0, slashIdx).trim()
      translationRaw = content.substring(slashIdx + 3).trim()
    } else {
      originalRaw = content
      translationRaw = null
    }

    const original = parseWords(originalRaw)
    const translation = translationRaw ? parseWords(translationRaw) : null

    result.push({ time, original, translation })
  }

  result.sort((a, b) => a.time - b.time)

  // ===== 给没有逐字时间戳的行自动生成 =====
  for (let i = 0; i < result.length; i++) {
    const line = result[i]
    // 行持续时长 = 下一行时间 - 当前行时间
    const nextTime = (i < result.length - 1) ? result[i + 1].time : line.time + 5
    const lineDuration = Math.max(0.5, nextTime - line.time)

    // 原文没有逐字 → 自动均分
    if (!line.original.words && line.original.text) {
      line.original.words = generateAutoWords(line.original.text, lineDuration)
    }

    // 翻译没有逐字 → 自动均分
    if (line.translation && !line.translation.words && line.translation.text) {
      line.translation.words = generateAutoWords(line.translation.text, lineDuration)
    }
  }

  return result
}

/**
 * 为普通文本自动生成逐字时间戳
 * 总时长 × 85% / 字符数 = 每字间隔（留 15% 尾部余量）
 */
function generateAutoWords(text, duration) {
  // 过滤空白字符，按可见字符拆分
  const chars = text.replace(/\s+/g, '').split('')
  if (chars.length === 0) return null

  const usable = duration * 0.95
  const perChar = usable / chars.length

  return chars.map((char, i) => ({
    char,
    offset: parseFloat((i * perChar).toFixed(3)),
  }))
}

/**
 * 解析带 {} 逐字标记的文本
 * "こ{0.00}の{0.16}" → { text:"この", words:[{char:"こ",offset:0}, ...] }
 * "普通文本" → { text:"普通文本", words: null }  (后续由 generateAutoWords 补上)
 */
function parseWords(raw) {
  const wordRegex = /([^{}]+)\{(\d+\.?\d*)\}/g
  const words = []
  let plainText = ''
  let lastIndex = 0
  let m

  while ((m = wordRegex.exec(raw)) !== null) {
    if (m.index > lastIndex) {
      const gap = raw.substring(lastIndex, m.index)
      if (gap.trim()) plainText += gap
    }

    const char = m[1]
    const offset = parseFloat(m[2])
    words.push({ char, offset })
    plainText += char
    lastIndex = wordRegex.lastIndex
  }

  if (words.length === 0) {
    return { text: raw, words: null }
  }

  if (lastIndex < raw.length) {
    const tail = raw.substring(lastIndex).trim()
    if (tail) plainText += tail
  }

  return { text: plainText, words }
}

function formatTime(seconds) {
  if (!seconds || isNaN(seconds)) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
