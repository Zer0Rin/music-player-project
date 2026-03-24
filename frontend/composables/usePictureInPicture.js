import { ref, watch } from 'vue' // 💡 确保引入了 ref 和 watch
import { setAudioMuted } from '~/composables/useAudioPlayer'

let pipVideo = null
let pipCanvas = null
let pipCtx = null
let pipStream = null
let animFrame = null
let cachedCoverImg = null  // 缓存封面
let cachedSongId = null    // 记录上次加载的歌曲id
let lastVideoState = null  // 记录视频的上一个播放状态，用于解决掉帧

export function usePictureInPicture() {
    const store = usePlayerStore()
    const isPip = ref(false)

    // ==========================================
    // 💡 优化 1：监听当前歌曲变化 (切歌时触发一次)
    // ==========================================
    watch(() => store.currentSong, async (newSong) => {
        if (!newSong || !isPip.value) return

        // 1. 更新系统媒体控制中心的歌曲信息
        if ('mediaSession' in navigator) {
            navigator.mediaSession.metadata = new MediaMetadata({
                title: newSong.title || '',
                artist: newSong.artist || '',
                album: newSong.album || '',
            })
        }

        // 2. 加载新封面
        await loadCoverIfNeeded(newSong.id)
    }, { immediate: true })

    // ==========================================
    // 💡 优化 2：监听播放状态变化 (播放/暂停时触发一次)
    // ==========================================
    watch(() => store.isPlaying, (isPlaying) => {
        if ('mediaSession' in navigator && isPip.value) {
            navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused'
        }
    }, { immediate: true })


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
        pipVideo.play()
    }

    // 💡 改造 loadCoverIfNeeded，让它接收 songId 参数，逻辑更纯粹
    async function loadCoverIfNeeded(songId) {
        if (!songId || songId === cachedSongId) return
        cachedSongId = songId
        cachedCoverImg = null
        try {
            const img = new Image()
            img.crossOrigin = 'anonymous'
            await new Promise((resolve, reject) => {
                img.onload = resolve
                img.onerror = reject
                // 使用绝对或相对路径根据你的实际情况，这里暂时保留你的原样
                img.src = `http://localhost:8080/api/songs/${songId}/cover`
            })
            cachedCoverImg = img
        } catch {
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
            // 模糊背景
            ctx.save()
            ctx.filter = 'blur(20px) brightness(0.4)'
            ctx.drawImage(cachedCoverImg, -20, -20, W + 40, H + 40)
            ctx.restore()
            // 清晰封面
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

        // 此时这个文本能正确响应了，因为不用等繁重的元数据更新
        ctx.fillStyle = store.isPlaying ? '#4ade80' : 'rgba(255,255,255,0.4)'
        ctx.fillText(store.isPlaying ? '▶ 播放中' : '⏸ 已暂停', textX, 130, textW)

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

        // 现在这里只有轻量级的判断和绘制
        animFrame = setInterval(() => {
            // 绘制 Canvas 帧
            drawFrame()

            // 修复原生画中画暂停掉帧的 BUG
            if (pipVideo) {
                if (store.isPlaying) {
                    if (pipVideo.paused) pipVideo.play().catch(() => {})
                    lastVideoState = 'playing'
                } else {
                    if (lastVideoState === 'playing') {
                        lastVideoState = 'pausing' // 进入中间态

                        // 强推最后一帧画面
                        if (pipVideo.paused) {
                            pipVideo.play().catch(() => {})
                        }

                        // 60ms 后真正暂停
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
        }, 100) // 10fps
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

        // 注册 Media Session 控制
        if ('mediaSession' in navigator) {
            navigator.mediaSession.setActionHandler('play', () => {
                if (!store.isPlaying) store.togglePlay()
            })
            navigator.mediaSession.setActionHandler('pause', () => {
                if (store.isPlaying) store.togglePlay()
            })
            navigator.mediaSession.setActionHandler('previoustrack', () => {
                store.prevSong()
            })
            navigator.mediaSession.setActionHandler('nexttrack', () => {
                store.nextSong()
            })

            // 手动触发一次初始化，确保第一次进入画中画就有数据
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
        await loadCoverIfNeeded()
        startDrawLoop()
        await new Promise((resolve) => {
            if (pipVideo.readyState >= 1) {
                resolve()
            } else {
                pipVideo.addEventListener('loadedmetadata', resolve, { once: true })
            }
        })
        try {
            await pipVideo.requestPictureInPicture()
        } catch (e) {
            console.error('画中画启动失败:', e)
        }
    }

    async function exitPip() {
        if (document.pictureInPictureElement) {
            await document.exitPictureInPicture()
        }
        stopDrawLoop()
    }

    function togglePip() {
        if (document.pictureInPictureElement) {
            exitPip()
        } else {
            enterPip()
        }
    }

    if (typeof document !== 'undefined') {
        document.addEventListener('enterpictureinpicture', () => isPip.value = true)
        document.addEventListener('leavepictureinpicture', () => {
            isPip.value = false
            stopDrawLoop()
        })
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