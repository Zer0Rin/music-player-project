import { ref, watch } from 'vue'
import { parseLrcToAMLL } from '~/utils/lrcToAMLLAdapter'
import type { LyricLine } from '~/utils/lrcToAMLLAdapter'
import { getGlobalAudio } from '~/composables/useAudioPlayer'

export function useAMLLEngine() {
    const isReady = ref(false)

    let player: any = null
    let rafId: number = 0
    let audioEl: HTMLAudioElement | null = null
    let currentLines: LyricLine[] = []
    let isRunning = false
    let isPaused = false
    let initDone = false

    let lastRafTime: number = -1
    let seekPending = false
    let frozenMs: number = 0

    // 修复：提升到模块级，dispose() 可以访问
    let containerRef: HTMLElement | null = null
    let scrollResetTimer: ReturnType<typeof setTimeout>

    const onContainerTouchMove = (e: TouchEvent) => {
        // 阻止外层组件/页面的滚动干扰 AMLL 内部手势
        e.stopPropagation()
    }

    const onContainerTouchEnd = () => {
        // 修复问题 3：touchend 后 300ms 重置 isUserScrolling，防止永久模糊
        clearTimeout(scrollResetTimer)
        scrollResetTimer = setTimeout(() => {
            if (player) {
                ;(player as any).isUserScrolling = false
            }
        }, 300)
    }

    function tick(rafTime: number) {
        if (!isRunning || !player) return
        if (!audioEl) audioEl = getGlobalAudio()

        if (audioEl) {
            let delta: number
            if (lastRafTime === -1 || seekPending) {
                delta = 0
                seekPending = false
            } else {
                delta = rafTime - lastRafTime
            }
            lastRafTime = rafTime

            const absoluteMs = isPaused ? frozenMs : audioEl.currentTime * 1000
            player.setCurrentTime(absoluteMs)
            player.update(delta)
        }

        rafId = requestAnimationFrame(tick)
    }

    function startTick() {
        if (isRunning) return
        isRunning = true
        lastRafTime = -1
        rafId = requestAnimationFrame(tick)
    }

    function stopTick() {
        isRunning = false
        lastRafTime = -1
        if (rafId) { cancelAnimationFrame(rafId); rafId = 0 }
    }

    async function doSeek(timeMs: number) {
        const audio = getGlobalAudio()
        if (audio) {
            audio.currentTime = timeMs / 1000
        }
        if (isPaused) {
            frozenMs = timeMs
        }

        if (player) {
            if (typeof player.resetScroll === 'function') player.resetScroll()
            ;(player as any).isUserScrolling = false
            ;(player as any).scrollOffset = 0
            ;(player as any).scrollOffsetY = 0

            for (let i = 0; i < 4; i++) {
                player.setCurrentTime(timeMs, true)
                await Promise.resolve()

                if (typeof (player as any).calcLayout === 'function') {
                    await (player as any).calcLayout(false, false)
                }

                const lines = (player as any).currentLyricLineObjects ?? []
                for (const lineObj of lines) {
                    const posY = lineObj.lineTransforms?.posY
                    if (posY && typeof posY.setPosition === 'function' && (lineObj as any).top !== undefined) {
                        posY.setPosition((lineObj as any).top)
                    }
                }

                const bottomLine = (player as any).bottomLine
                const bPosY = bottomLine?.lineTransforms?.posY
                if (bPosY && typeof bPosY.setPosition === 'function' && (bottomLine as any).top !== undefined) {
                    bPosY.setPosition((bottomLine as any).top)
                }

                player.update(0)
                await new Promise(r => requestAnimationFrame(r))
            }
        }

        seekPending = true
    }

    async function initPlayer(container: HTMLElement) {
        if (!import.meta.client) return

        // 容器迁移：PC 切移动端时把已有的 AMLL DOM 搬过来
        if (initDone) {
            if (player && container && !container.contains(player.getElement())) {
                container.appendChild(player.getElement())
                if (typeof (player as any).calcLayout === 'function') {
                    ;(player as any).calcLayout(false, true)
                }
            }
            // 重新绑定 touch 事件到新容器
            if (containerRef && containerRef !== container) {
                containerRef.removeEventListener('touchmove', onContainerTouchMove)
                containerRef.removeEventListener('touchend', onContainerTouchEnd)
            }
            containerRef = container
            container.addEventListener('touchmove', onContainerTouchMove, { passive: true })
            container.addEventListener('touchend', onContainerTouchEnd, { passive: true })
            return
        }

        initDone = true

        try {
            await import('@applemusic-like-lyrics/core/style.css')
            const { DomLyricPlayer } = await import('@applemusic-like-lyrics/core')

            player = new DomLyricPlayer()
            const el = player.getElement()
            el.style.width = '100%'
            el.style.height = '100%'
            container.appendChild(el)

            player.setAlignPosition(0.382)

            // 修复问题 2：注册 touch 事件，passive: false 让 AMLL 内部手势可以工作
            containerRef = container
            container.addEventListener('touchmove', onContainerTouchMove, { passive: true })
            container.addEventListener('touchend', onContainerTouchEnd, { passive: true })

            player.addEventListener('line-click', ((e: any) => {
                const rawLine = e.line?.getLine()
                if (rawLine && rawLine.startTime !== undefined) {
                    doSeek(rawLine.startTime + 50)
                }
            }) as EventListener)

            audioEl = getGlobalAudio()

            const store = usePlayerStore()
            if (store.lyricsText) {
                setLyrics(store.lyricsText, store.duration || 300)
            }

            isPaused = !store.isPlaying
            if (isPaused) {
                player.pause()
            } else {
                player.resume()
            }

            startTick()
            isReady.value = true
            console.log('[AMLL] 初始化成功, audioEl:', !!audioEl, 'lines:', currentLines.length)
        } catch (e) {
            console.error('[AMLL] 引擎初始化失败:', e)
            initDone = false
        }
    }

    function setLyrics(lrcText: string, duration: number) {
        currentLines = parseLrcToAMLL(lrcText, duration * 1000)
        if (player) {
            player.setLyricLines(currentLines)
            seekPending = true
        }
    }

    function seekTo(timeSec: number) {
        if (!player) return
        doSeek(timeSec * 1000)
    }

    function pauseEngine() {
        if (!audioEl) audioEl = getGlobalAudio()
        frozenMs = (audioEl?.currentTime ?? 0) * 1000
        isPaused = true
        player?.pause()
    }

    function resumeEngine() {
        isPaused = false
        seekPending = true
        player?.resume()
        if (player) startTick()
    }

    function dispose() {
        stopTick()
        clearTimeout(scrollResetTimer)
        if (containerRef) {
            containerRef.removeEventListener('touchmove', onContainerTouchMove)
            containerRef.removeEventListener('touchend', onContainerTouchEnd)
            containerRef = null
        }
        if (player) {
            try { player.dispose() } catch (e) { console.warn('[AMLL] dispose:', e) }
            player = null
        }
        audioEl = null
        currentLines = []
        isPaused = false
        frozenMs = 0
        seekPending = false
        lastRafTime = -1
        isReady.value = false
        initDone = false
    }

    const store = usePlayerStore()

    watch(() => store.lyricsText, (text) => {
        if (text) setLyrics(text, store.duration || 300)
    })

    watch(() => store.duration, (dur) => {
        if (dur && store.lyricsText && currentLines.length > 0) {
            setLyrics(store.lyricsText, dur)
        }
    })

    watch(() => store.isPlaying, (playing) => {
        if (playing) resumeEngine()
        else pauseEngine()
    })

    return { initPlayer, seekTo, dispose, isReady }
}