import { ref, watch } from 'vue'
import { setAudioMuted } from '~/composables/useAudioPlayer'

let pipVideo = null
let pipCanvas = null
let pipCtx = null
let pipStream = null
let animFrame = null
let cachedCoverImg = null
let cachedSongId = null
let lastVideoState = null
let eventsRegistered = false

const isPip = ref(false)  // 统一用这一个

export function usePictureInPicture() {
    const store = usePlayerStore()

    // 监听切歌
    watch(() => store.currentSong?.id, async (newId, oldId) => {
        if (!newId || newId === oldId) return
        cachedSongId = newId
        cachedCoverImg = null

        if (!isPip.value) return

        try {
            const img = new Image()
            img.crossOrigin = 'anonymous'
            await new Promise((resolve, reject) => {
                img.onload = resolve
                img.onerror = reject
                img.src = `http://localhost:8080/api/songs/${newId}/cover?t=${Date.now()}`
            })
            cachedCoverImg = img
        } catch (e) {
            cachedCoverImg = null
        }
    }, { immediate: false })

    function initCanvas() {
        if (pipCanvas) return

        pipCanvas = document.createElement('canvas')
        pipCanvas.width = 480
        pipCanvas.height = 270
        pipCtx = pipCanvas.getContext('2d')
        pipStream = pipCanvas.captureStream(30)
        pipVideo = document.createElement('video')
        pipVideo.srcObject = pipStream
        pipVideo.muted = true
        pipVideo.play().then(() => {
            pipVideo.muted = false  // 播放成功后立刻取消静音
        })
        pipVideo.addEventListener('volumechange', () => {
            setAudioMuted(pipVideo.muted)
        })

        if (!eventsRegistered) {
            eventsRegistered = true
            document.addEventListener('enterpictureinpicture', () => {
                isPip.value = true
            })
            document.addEventListener('leavepictureinpicture', () => {
                isPip.value = false
                stopDrawLoop()
            })
        }
    }

    async function loadCoverIfNeeded(songId) {
        if (!songId) return
        if (cachedSongId === songId && cachedCoverImg) return
        cachedSongId = songId
        try {
            const img = new Image()
            img.crossOrigin = 'anonymous'
            await new Promise((resolve, reject) => {
                img.onload = resolve
                img.onerror = reject
                img.src = `http://localhost:8080/api/songs/${songId}/cover?t=${Date.now()}`
            })
            cachedCoverImg = img
        } catch (e) {
            cachedCoverImg = null
        }
    }

    function drawFrame() {
        if (!pipCtx || !store.currentSong) return
        const ctx = pipCtx
        const W = 480, H = 270

        ctx.fillStyle = '#0a0a0a'
        ctx.fillRect(0, 0, W, H)

        if (cachedCoverImg) {
            ctx.save()
            ctx.filter = 'blur(20px) brightness(0.4)'
            ctx.drawImage(cachedCoverImg, -20, -20, W + 40, H + 40)
            ctx.restore()
            const size = H - 40
            ctx.save()
            roundRect(ctx, 20, 20, size, size, 12)
            ctx.clip()
            ctx.drawImage(cachedCoverImg, 20, 20, size, size)
            ctx.restore()
        }

        const textX = H + 10
        const textW = W - H - 20

        ctx.fillStyle = '#ffffff'
        ctx.font = 'bold 20px system-ui, sans-serif'
        ctx.fillText(truncate(store.currentSong.title || '', 22), textX, 70, textW)

        ctx.fillStyle = 'rgba(255,255,255,0.6)'
        ctx.font = '15px system-ui, sans-serif'
        ctx.fillText(truncate(store.currentSong.artist || '', 26), textX, 100, textW)

        ctx.fillStyle = store.isPlaying ? '#4ade80' : 'rgba(255,255,255,0.4)'
        ctx.fillText(store.isPlaying ? '▶ 播放中' : '⏸ 已暂停', textX, 130, textW)

        // 当前歌词
        const currentLyric = store.parsedLyrics[store.currentLyricIndex]
        if (currentLyric?.original?.text) {
            ctx.fillStyle = 'rgba(255,255,255,0.85)'
            ctx.font = '14px system-ui, sans-serif'
            ctx.fillText(
                truncate(currentLyric.original.text, 20),
                textX, 158, textW
            )
            // 翻译（如果有）
            if (currentLyric.translation?.text) {
                ctx.fillStyle = 'rgba(255,255,255,0.45)'
                ctx.font = '12px system-ui, sans-serif'
                ctx.fillText(
                    truncate(currentLyric.translation.text, 24),
                    textX, 178, textW
                )
            }
        }

        const barY = H - 40
        const barW = textW
        ctx.fillStyle = 'rgba(255,255,255,0.15)'
        roundRect(ctx, textX, barY, barW, 5, 3)
        ctx.fill()

        ctx.fillStyle = '#fa2d48'
        roundRect(ctx, textX, barY, barW * store.progress, 5, 3)
        ctx.fill()

        ctx.fillStyle = 'rgba(255,255,255,0.5)'
        ctx.font = '12px monospace'
        ctx.textAlign = 'left'
        ctx.fillText(store.formattedCurrentTime, textX, barY + 20)
        ctx.textAlign = 'right'
        ctx.fillText(store.formattedDuration, textX + barW, barY + 20)
        ctx.textAlign = 'left'
    }

    function startDrawLoop() {
        if (animFrame) clearInterval(animFrame)
        lastVideoState = store.isPlaying ? 'playing' : 'paused'
        animFrame = setInterval(() => {
            drawFrame()
            if (pipVideo) {
                if (store.isPlaying) {
                    if (pipVideo.paused) pipVideo.play().catch(() => {})
                    lastVideoState = 'playing'
                } else {
                    if (lastVideoState === 'playing') {
                        lastVideoState = 'pausing'
                        if (pipVideo.paused) pipVideo.play().catch(() => {})
                        setTimeout(() => {
                            if (!store.isPlaying) {
                                pipVideo.pause()
                                lastVideoState = 'paused'
                            }
                        }, 60)
                    } else if (lastVideoState !== 'pausing') {
                        if (!pipVideo.paused) pipVideo.pause()
                    }
                }
            }
        }, 100)
    }

    function stopDrawLoop() {
        if (animFrame) clearInterval(animFrame)
        animFrame = null
    }

    async function enterPip() {
        if (!document.pictureInPictureEnabled) {
            alert('您的浏览器不支持画中画功能')
            return
        }
        initCanvas()

        if ('mediaSession' in navigator) {
            navigator.mediaSession.setActionHandler('play', () => { if (!store.isPlaying) store.togglePlay() })
            navigator.mediaSession.setActionHandler('pause', () => { if (store.isPlaying) store.togglePlay() })
            navigator.mediaSession.setActionHandler('previoustrack', () => store.prevSong())
            navigator.mediaSession.setActionHandler('nexttrack', () => store.nextSong())
            if (store.currentSong) {
                navigator.mediaSession.metadata = new MediaMetadata({
                    title: store.currentSong.title || '',
                    artist: store.currentSong.artist || '',
                    album: store.currentSong.album || '',
                })
                navigator.mediaSession.playbackState = store.isPlaying ? 'playing' : 'paused'
            }
        }

        await loadCoverIfNeeded(store.currentSong?.id)
        startDrawLoop()

        await new Promise((resolve) => {
            if (pipVideo.readyState >= 1) resolve()
            else pipVideo.addEventListener('loadedmetadata', resolve, { once: true })
        })

        try {
            await pipVideo.requestPictureInPicture()
            isPip.value = true
        } catch (e) {
            console.error('画中画启动失败:', e)
        }
    }

    async function exitPip() {
        if (document.pictureInPictureElement) await document.exitPictureInPicture()
        stopDrawLoop()
    }

    function togglePip() {
        if (document.pictureInPictureElement) exitPip()
        else enterPip()
    }

    return { togglePip, isPip }
}

function truncate(str, len) {
    return str.length > len ? str.slice(0, len) + '...' : str
}

function roundRect(ctx, x, y, w, h, r) {
    if (w <= 0) return
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.lineTo(x + w - r, y)
    ctx.quadraticCurveTo(x + w, y, x + w, y + r)
    ctx.lineTo(x + w, y + h - r)
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
    ctx.lineTo(x + r, y + h)
    ctx.quadraticCurveTo(x, y + h, x, y + h - r)
    ctx.lineTo(x, y + r)
    ctx.quadraticCurveTo(x, y, x + r, y)
    ctx.closePath()
}