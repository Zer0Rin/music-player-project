/**
 * 多格式歌词 → AMLL LyricLine[] 适配器
 *
 * 支持格式：
 *   1. 标准 LRC（逐行）:         [mm:ss.ms]歌词文字
 *   2. LRC 逐字（LDDC风格）:     [mm:ss.ms]字[mm:ss.ms]字[mm:ss.ms]...
 *   3. 增强型 LRC（ESlyric）:    [mm:ss.ms]<mm:ss.ms>字<mm:ss.ms>字...
 *   4. ELRC（{offset}风格）:     [mm:ss.ms]字{0.00}字{0.15}... / 翻译
 *   5. SRT:                      序号\n时间 --> 时间\n文字
 *   6. ASS/SSA（卡拉OK）:        Dialogue: ...,{\kf数字}字{\kf数字}字...
 *   7. 网易云混合格式:            头尾JSON元信息行 + 中间标准LRC
 */

export interface LyricWord {
    startTime: number  // ms
    endTime: number    // ms
    word: string
}

export interface LyricLine {
    words: LyricWord[]
    translatedLyric: string
    romanLyric: string
    startTime: number  // ms
    endTime: number    // ms
    isBG: boolean
    isDuet: boolean
}

// ─── 通用工具 ─────────────────────────────────────────────────

/** [mm:ss.ms] → ms */
function parseLrcTimestamp(raw: string): number {
    const m = raw.match(/(\d+):(\d+)(?:[.:](\d+))?/)
    if (!m) return -1
    const min = parseInt(m[1], 10)
    const sec = parseInt(m[2], 10)
    let ms = 0
    if (m[3]) ms = parseInt(m[3].padEnd(3, '0').slice(0, 3), 10)
    return min * 60000 + sec * 1000 + ms
}

/** 00:00:00,000 或 00:00:00.000 → ms */
function parseSrtTimestamp(raw: string): number {
    const m = raw.trim().match(/(\d+):(\d+):(\d+)[,.](\d+)/)
    if (!m) return -1
    return parseInt(m[1], 10) * 3600000
        + parseInt(m[2], 10) * 60000
        + parseInt(m[3], 10) * 1000
        + parseInt(m[4].padEnd(3, '0').slice(0, 3), 10)
}

/** 去除 {offset} 标记 */
function stripOffsets(text: string): string {
    return text.replace(/\{[\d.]+\}/g, '').trim()
}

/** 分离 " / " 斜杠翻译，翻译部分去除offset标记 */
function splitSlash(content: string): { lyric: string; translation: string } {
    const idx = content.indexOf(' / ')
    if (idx === -1) return { lyric: content, translation: '' }
    return {
        lyric: content.slice(0, idx).trim(),
        translation: stripOffsets(content.slice(idx + 3)),
    }
}

/** 确保 words 的 startTime 严格单调递增 */
function fixMonotonic(words: LyricWord[]): LyricWord[] {
    for (let i = 1; i < words.length; i++) {
        if (words[i].startTime <= words[i - 1].startTime) {
            words[i].startTime = words[i - 1].startTime + 1
        }
        if (words[i].endTime <= words[i].startTime) {
            words[i].endTime = words[i].startTime + 1
        }
    }
    return words
}

/** 构造单词列表，自动补全 endTime */
function buildWords(
    items: { text: string; startMs: number }[],
    lineEndMs: number,
): LyricWord[] {
    // 过滤空文本，确保 startMs 单调递增
    const valid = items.filter(w => w.text.length > 0)
    if (valid.length === 0) return []

    // 修正单调性
    for (let i = 1; i < valid.length; i++) {
        if (valid[i].startMs <= valid[i - 1].startMs) {
            valid[i].startMs = valid[i - 1].startMs + 1
        }
    }

    return valid.map((w, i) => ({
        word: w.text,
        startTime: w.startMs,
        endTime: i < valid.length - 1
            ? Math.max(valid[i + 1].startMs, w.startMs + 1)
            : Math.max(lineEndMs, w.startMs + 1),
    }))
}

// ─── 格式检测 ─────────────────────────────────────────────────

function detectFormat(text: string): 'srt' | 'ass' | 'lrc' {
    const lines = text.trim().split('\n').map(l => l.trim()).filter(Boolean)
    // ASS：含 [Events] 或 Dialogue: 行
    if (lines.some(l => l.startsWith('Dialogue:'))) return 'ass'
    // SRT：有 " --> " 时间行
    if (lines.some(l => /\d+:\d+:\d+[,.]\d+\s*-->\s*\d+:\d+:\d+[,.]\d+/.test(l))) return 'srt'
    return 'lrc'
}

// ─── ELRC {offset} 逐字解析 ───────────────────────────────────

function parseElrcWords(content: string, lineStartMs: number, lineEndMs: number): LyricWord[] {
    const regex = /([^{]+)\{([\d.]+)\}/g
    let match: RegExpExecArray | null
    const parts: { text: string; offsetMs: number }[] = []

    while ((match = regex.exec(content)) !== null) {
        parts.push({ text: match[1], offsetMs: Math.round(parseFloat(match[2]) * 1000) })
    }
    if (parts.length === 0) {
        return [{ startTime: lineStartMs, endTime: lineEndMs, word: content }]
    }

    // 尾部文字
    const lastBrace = content.lastIndexOf('}')
    if (lastBrace >= 0 && lastBrace < content.length - 1) {
        const tail = content.slice(lastBrace + 1).trim()
        if (tail) {
            const lastOff = parts[parts.length - 1].offsetMs
            const gap = parts.length > 1
                ? Math.round((parts[parts.length - 1].offsetMs - parts[0].offsetMs) / (parts.length - 1))
                : 150
            parts.push({ text: tail, offsetMs: lastOff + gap })
        }
    }

    return fixMonotonic(parts.map((p, i) => ({
        word: p.text,
        startTime: lineStartMs + p.offsetMs,
        endTime: i < parts.length - 1
            ? Math.max(lineStartMs + parts[i + 1].offsetMs, lineStartMs + p.offsetMs + 1)
            : Math.max(lineEndMs, lineStartMs + p.offsetMs + 1),
    })))
}

// ─── LRC 逐字（LDDC风格）解析 ────────────────────────────────
// 格式: [行时间]字[时间]字[时间]字[时间]（最后时间戳为行结束）

function parseLddcWordLine(content: string, lineStartMs: number, lineEndMs: number): LyricWord[] | null {
    // 检测：内容中有 [mm:ss.ms] 时间戳
    if (!/\[\d+:\d+[.:]\d+\]/.test(content)) return null

    // 提取所有 [时间戳]文字 对
    const regex = /\[(\d+:\d+[.:]\d+)\]([^\[]*)/g
    let match: RegExpExecArray | null
    const items: { text: string; startMs: number }[] = []
    let endMs = lineEndMs

    while ((match = regex.exec(content)) !== null) {
        const ms = parseLrcTimestamp(match[1])
        if (ms < 0) continue
        const text = match[2]
        if (text === '') {
            // 空文字的时间戳作为结束时间
            endMs = ms
        } else {
            items.push({ text, startMs: ms })
        }
    }

    if (items.length === 0) return null
    return buildWords(items, endMs)
}

// ─── 增强型 LRC（ESlyric <时间戳>）解析 ──────────────────────
// 格式: [行时间]<时间>字<时间>字<时间>（最后时间戳为行结束）

function parseEslyricWordLine(content: string, lineStartMs: number, lineEndMs: number): LyricWord[] | null {
    if (!content.includes('<')) return null

    const regex = /<(\d+:\d+[.:]\d+)>([^<]*)/g
    let match: RegExpExecArray | null
    const items: { text: string; startMs: number }[] = []
    let endMs = lineEndMs

    while ((match = regex.exec(content)) !== null) {
        const ms = parseLrcTimestamp(match[1])
        if (ms < 0) continue
        const text = match[2]
        if (text === '') {
            endMs = ms
        } else {
            items.push({ text, startMs: ms })
        }
    }

    if (items.length === 0) return null
    return buildWords(items, endMs)
}

// ─── LRC 解析（涵盖标准/逐字/ELRC/网易云混合）────────────────

function parseLrc(lrcText: string, duration: number): LyricLine[] {
    interface RawEntry {
        timeMs: number
        lyric: string
        translation: string
        words?: LyricWord[]  // 若已解析逐字则直接存
    }

    const entries: RawEntry[] = []
    const seen = new Map<number, number>() // timeMs → index in entries

    for (const raw of lrcText.split('\n')) {
        const line = raw.trim()
        if (!line) continue
        // 跳过网易云JSON元信息行
        if (line.startsWith('{') && line.includes('"t"')) continue
        // 跳过 [tag:value] 元数据
        if (/^\[[a-zA-Z]{2,}:/.test(line)) continue

        // 匹配行时间戳
        const tsMatch = line.match(/^\[(\d+:\d+[.:]\d+)\](.*)$/)
        if (!tsMatch) continue

        const timeMs = parseLrcTimestamp(tsMatch[1])
        if (timeMs < 0) continue
        const rest = tsMatch[2] // 时间戳之后的内容

        // 判断逐字格式（优先级：ESlyric > LDDC > ELRC > 标准）
        let words: LyricWord[] | undefined
        let lyricText = rest
        let translation = ''

        const eslyricWords = parseEslyricWordLine(rest, timeMs, timeMs + 100)
        if (eslyricWords) {
            words = eslyricWords
            lyricText = eslyricWords.map(w => w.word).join('')
        } else {
            const lddcWords = parseLddcWordLine(rest, timeMs, timeMs + 100)
            if (lddcWords) {
                words = lddcWords
                lyricText = lddcWords.map(w => w.word).join('')
            } else {
                // 斜杠翻译 / ELRC {offset}
                const split = splitSlash(rest)
                lyricText = split.lyric
                translation = split.translation
                if (/\{[\d.]+\}/.test(lyricText)) {
                    words = parseElrcWords(lyricText, timeMs, timeMs + 100)
                    lyricText = words.map(w => w.word).join('')
                }
            }
        }

        if (!lyricText.trim()) continue

        const idx = seen.get(timeMs)
        if (idx !== undefined) {
            // 同时间戳第二次：翻译行
            if (!entries[idx].translation) entries[idx].translation = lyricText
        } else {
            seen.set(timeMs, entries.length)
            entries.push({ timeMs, lyric: lyricText, translation, words })
        }
    }

    // 补全 endTime 并构建 LyricLine[]
    const result: LyricLine[] = []
    for (let i = 0; i < entries.length; i++) {
        const e = entries[i]
        if (!e.lyric.trim()) continue
        const endMs = Math.max(
            i < entries.length - 1 ? entries[i + 1].timeMs : duration,
            e.timeMs + 100,
        )

        let words: LyricWord[]
        if (e.words) {
            // 已有逐字数据，补全最后一个词的 endTime
            words = e.words
            if (words.length > 0) {
                words[words.length - 1].endTime = Math.max(
                    words[words.length - 1].endTime,
                    endMs,
                )
            }
            words = fixMonotonic(words)
        } else {
            words = [{ startTime: e.timeMs, endTime: endMs, word: e.lyric }]
        }

        result.push({
            words,
            translatedLyric: e.translation,
            romanLyric: '',
            startTime: e.timeMs,
            endTime: endMs,
            isBG: false,
            isDuet: false,
        })
    }
    return result
}

// ─── SRT 解析 ────────────────────────────────────────────────

function parseSrt(srtText: string, duration: number): LyricLine[] {
    const result: LyricLine[] = []
    // SRT 块以空行分隔
    const blocks = srtText.trim().split(/\n\s*\n/)

    for (const block of blocks) {
        const lines = block.trim().split('\n').map(l => l.trim()).filter(Boolean)
        if (lines.length < 2) continue

        // 找时间行
        const timeLine = lines.find(l => l.includes('-->'))
        if (!timeLine) continue

        const parts = timeLine.split('-->')
        const startMs = parseSrtTimestamp(parts[0])
        const endMs = parseSrtTimestamp(parts[1])
        if (startMs < 0 || endMs < 0) continue

        // 时间行之后的所有行合并为歌词
        const timeIdx = lines.indexOf(timeLine)
        const text = lines.slice(timeIdx + 1).join(' ').trim()
        if (!text) continue

        result.push({
            words: [{ startTime: startMs, endTime: endMs, word: text }],
            translatedLyric: '',
            romanLyric: '',
            startTime: startMs,
            endTime: endMs,
            isBG: false,
            isDuet: false,
        })
    }
    return result
}

// ─── ASS 解析（{\kf} 卡拉OK）────────────────────────────────

function parseAss(assText: string, duration: number): LyricLine[] {
    const result: LyricLine[] = []
    const dialogueRegex = /^Dialogue:\s*\d+,(\d+:\d+:\d+\.\d+),(\d+:\d+:\d+\.\d+),[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,(.*)/

    for (const raw of assText.split('\n')) {
        const line = raw.trim()
        const m = line.match(dialogueRegex)
        if (!m) continue

        const lineStartMs = parseSrtTimestamp(m[1].replace('.', ','))
        const lineEndMs = parseSrtTimestamp(m[2].replace('.', ','))
        if (lineStartMs < 0) continue

        const content = m[3]

        // 解析 {\kfN} 卡拉OK标记：{\kfN} 表示该字持续 N×10ms
        // 格式: {\kf20}字{\kf30}字 ...
        const kfRegex = /\{\\kf(\d+)\}([^{]*)/g
        let km: RegExpExecArray | null
        const items: { text: string; startMs: number }[] = []
        let cursor = lineStartMs

        while ((km = kfRegex.exec(content)) !== null) {
            const durationMs = parseInt(km[1], 10) * 10
            const text = km[2]
            if (text.trim()) {
                items.push({ text, startMs: cursor })
            }
            cursor += durationMs
        }

        if (items.length === 0) {
            // 无逐字标记，降级为整行
            const plainText = content.replace(/\{[^}]*\}/g, '').trim()
            if (!plainText) continue
            result.push({
                words: [{ startTime: lineStartMs, endTime: lineEndMs, word: plainText }],
                translatedLyric: '',
                romanLyric: '',
                startTime: lineStartMs,
                endTime: lineEndMs,
                isBG: false,
                isDuet: false,
            })
            continue
        }

        const words = buildWords(items, lineEndMs)
        if (words.length === 0) continue

        result.push({
            words,
            translatedLyric: '',
            romanLyric: '',
            startTime: lineStartMs,
            endTime: lineEndMs,
            isBG: false,
            isDuet: false,
        })
    }
    return result
}

// ─── 主入口 ──────────────────────────────────────────────────

export function parseLrcToAMLL(lrcText: string, duration: number = 300000): LyricLine[] {
    if (!lrcText?.trim()) return []
    const fmt = detectFormat(lrcText)
    if (fmt === 'ass') return parseAss(lrcText, duration)
    if (fmt === 'srt') return parseSrt(lrcText, duration)
    return parseLrc(lrcText, duration)
}

export function findLineIndexAtTime(lines: LyricLine[], timeMs: number): number {
    let idx = 0
    for (let i = 0; i < lines.length; i++) {
        if (lines[i].startTime <= timeMs) idx = i
        else break
    }
    return idx
}