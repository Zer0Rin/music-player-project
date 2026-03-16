import { defineStore } from 'pinia'

export const usePlayerStore = defineStore('player', {
  state: () => ({
    playlist: [],
    currentSong: null,
    currentIndex: -1,
    isPlaying: false,
    currentTime: 0,
    duration: 0,
    lyricsText: '',
    parsedLyrics: [],
    volume: 0.8,
    playMode: 'sequence',
    lyricMode: 'line',
    showLyricView: false,
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
    hasNext(state) { return state.currentIndex < state.playlist.length - 1 },
    hasPrev(state) { return state.currentIndex > 0 },
  },

  actions: {
    setPlaylist(songs) { this.playlist = songs },

    async playSong(song, index) {
      this.currentSong = song
      this.currentIndex = index
      this.currentTime = 0
      this.isPlaying = true
      await this.loadLyrics(song.id)
    },

    async loadLyrics(songId) {
      try {
        const res = await fetch(`/api/songs/${songId}/lyrics`)
        if (res.ok) {
          this.lyricsText = await res.text()
          this.parsedLyrics = parseLrc(this.lyricsText)
        } else {
          this.lyricsText = ''
          this.parsedLyrics = []
        }
      } catch {
        this.lyricsText = ''
        this.parsedLyrics = []
      }
    },

    async nextSong() {
      if (this.hasNext) await this.playSong(this.playlist[this.currentIndex + 1], this.currentIndex + 1)
    },
    async prevSong() {
      if (this.hasPrev) await this.playSong(this.playlist[this.currentIndex - 1], this.currentIndex - 1)
    },
    updateTime(time) { this.currentTime = time },
    setDuration(d) { this.duration = d },
    togglePlay() { this.isPlaying = !this.isPlaying },
    openLyricView() { this.showLyricView = true },
    closeLyricView() { this.showLyricView = false },

    toggleLyricMode() {
          this.lyricMode = this.lyricMode === 'word' ? 'line' : 'word'
      },

    togglePlayMode() {
      const modes = ['sequence', 'loop-all', 'loop-one', 'shuffle']
      const i = modes.indexOf(this.playMode)
      this.playMode = modes[(i + 1) % modes.length]
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
