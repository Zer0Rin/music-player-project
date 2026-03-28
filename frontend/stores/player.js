import { defineStore } from 'pinia'
import { parseLrcToAMLL, findLineIndexAtTime } from '~/utils/lrcToAMLLAdapter'

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
        parsedLyrics: [],   // 保留，兼容旧组件
        volume: 0.5,
        playMode: 'sequence',
        lyricMode: 'line',
        showLyricView: false,
        recentSongs: [],
        allSongs: [],

        // 新增：AMLL 解析后的行数据，供画中画读取当前行文本
        _amllLines: [],         // LyricLine[] by lrcToAMLLAdapter
        currentLyricText: '',   // 当前行原文（已去除时间戳/offset标记）
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

        currentWordElapsed(state) {
            const idx = this.currentLyricIndex
            if (idx < 0 || !state.parsedLyrics[idx]) return 0
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
                const rest = []
                for (let i = 0; i < n; i++) {
                    if (i !== this.currentIndex) rest.push(i)
                }
                for (let i = rest.length - 1; i > 0; i--) {
                    const j = Math.floor(Math.random() * (i + 1));
                    [rest[i], rest[j]] = [rest[j], rest[i]]
                }
                this.playOrder = this.currentIndex >= 0 ? [this.currentIndex, ...rest] : rest
                this.orderCursor = this.currentIndex >= 0 ? 0 : -1
            } else {
                this.playOrder = Array.from({length: n}, (_, i) => i)
                this.orderCursor = this.currentIndex >= 0 ? this.currentIndex : -1
            }
        },

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

            const cursor = this.playOrder.indexOf(index)
            if (cursor !== -1) {
                this.orderCursor = cursor
            } else {
                this.updatePlayOrder()
            }

            this.addRecentSong(song)
            await this.loadLyrics(song.id)
        },

        async addRecentSong(song) {
            this.recentSongs = this.recentSongs.filter(s => s.id !== song.id)
            this.recentSongs.unshift(song)
            if (this.recentSongs.length > 100) this.recentSongs.pop()
            try {
                const { $apiFetch } = useNuxtApp()
                $apiFetch(`/api/recent/${song.id}`, { method: 'POST' }).catch(() => {})
            } catch {}
        },

        async fetchRecentSongs() {
            try {
                const { $apiFetch } = useNuxtApp()
                this.recentSongs = await $apiFetch('/api/recent')
            } catch (e) {
                console.error('加载最近播放失败:', e)
            }
        },

        async loadLyrics(songId) {
            try {
                this.lyricsText = await $apiFetch(`/api/songs/${songId}/lyrics`)
                this.parsedLyrics = parseLrc(this.lyricsText)  // 旧解析器，供旧组件兼容

                // 新解析器：供 AMLL 引擎和画中画使用
                const durationMs = (this.duration || 300) * 1000
                this._amllLines = parseLrcToAMLL(this.lyricsText, durationMs)

                const lyricsFile = this.currentSong?.lyricsFile || ''
                this.lyricMode = lyricsFile.toLowerCase().endsWith('.elrc') ? 'word' : 'line'
            } catch {
                this.lyricsText = ''
                this.parsedLyrics = []
                this._amllLines = []
            }
            this.currentLyricText = ''
        },

        async nextSong(isAutoEnd = false) {
            if (!this.playlist.length || this.orderCursor === -1) return
            let nextCursor = this.orderCursor + 1
            if (nextCursor < this.playOrder.length) {
                const nextIdx = this.playOrder[nextCursor]
                await this.playSong(this.playlist[nextIdx], nextIdx)
            } else {
                if (this.playMode === 'sequence' && isAutoEnd) {
                    this.isPlaying = false
                } else if (this.playMode === 'shuffle') {
                    this.updatePlayOrder()
                    const nextIdx = this.playOrder[0]
                    await this.playSong(this.playlist[nextIdx], nextIdx)
                } else {
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
                const prevIdx = this.playOrder[this.playOrder.length - 1]
                await this.playSong(this.playlist[prevIdx], prevIdx)
            }
        },

        updateTime(time) {
            this.currentTime = time

            // 同步更新当前行文本（供画中画使用）
            if (this._amllLines.length > 0) {
                const timeMs = Math.round(time * 1000)
                const idx = findLineIndexAtTime(this._amllLines, timeMs)
                const line = this._amllLines[idx]
                if (line) {
                    // words 里的 word 字段就是纯文本（lrcToAMLLAdapter 已去除标记）
                    this.currentLyricText = line.words.map(w => w.word).join('')
                } else {
                    this.currentLyricText = ''
                }
            }
        },

        setDuration(d) { this.duration = d },
        togglePlay() { this.isPlaying = !this.isPlaying },
        openLyricView() { this.showLyricView = true },
        closeLyricView() { this.showLyricView = false },

        toggleLyricMode() {
            this.lyricMode = this.lyricMode === 'word' ? 'line' : 'word'
        },

        async refreshSongs(apiFetch) {
            const songs = await apiFetch('/api/songs')
            for (let i = songs.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [songs[i], songs[j]] = [songs[j], songs[i]]
            }
            this.allSongs = songs
        },
    },
})

// ========== 旧解析器（保留兼容旧组件）==========

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

    for (let i = 0; i < result.length; i++) {
        const line = result[i]
        const nextTime = (i < result.length - 1) ? result[i + 1].time : line.time + 5
        const lineDuration = Math.max(0.5, nextTime - line.time)

        if (!line.original.words && line.original.text) {
            line.original.words = generateAutoWords(line.original.text, lineDuration)
        }
        if (line.translation && !line.translation.words && line.translation.text) {
            line.translation.words = generateAutoWords(line.translation.text, lineDuration)
        }
    }

    return result
}

function generateAutoWords(text, duration) {
    const chars = text.replace(/\s+/g, '').split('')
    if (chars.length === 0) return null
    const usable = duration * 0.95
    const perChar = usable / chars.length
    return chars.map((char, i) => ({
        char,
        offset: parseFloat((i * perChar).toFixed(3)),
    }))
}

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