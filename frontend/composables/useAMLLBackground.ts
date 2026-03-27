import { ref, onUnmounted, watch } from 'vue'
import { getGlobalAnalyser } from '~/composables/useAudioPlayer'

export function useAMLLBackground() {
    const isReady = ref(false)

    let renderer: any = null
    let bassRafId: number = 0
    let analyser: AnalyserNode | null = null
    let bassData: Uint8Array | null = null
    let isBassRunning = false
    let initDone = false

    function bassTick() {
        if (!isBassRunning || !renderer || !analyser || !bassData) return
        analyser.getByteFrequencyData(bassData)
        const bass = (bassData[0] + bassData[1]) / 2 / 255
        renderer.setLowFreqVolume(bass)
        bassRafId = requestAnimationFrame(bassTick)
    }

    function startBass() {
        if (isBassRunning) return
        try {
            analyser = getGlobalAnalyser()
            if (!analyser) return
            bassData = new Uint8Array(analyser.frequencyBinCount)
            isBassRunning = true
            bassRafId = requestAnimationFrame(bassTick)
        } catch (e) {
            console.warn('[AMLL-BG] 低频联动失败:', e)
        }
    }

    function stopBass() {
        isBassRunning = false
        if (bassRafId) { cancelAnimationFrame(bassRafId); bassRafId = 0 }
    }

    async function initRenderer(container: HTMLElement) {
        if (!import.meta.client) return
        if (initDone) return
        initDone = true

        try {
            await import('@applemusic-like-lyrics/core/style.css')
            const { PixiRenderer } = await import('@applemusic-like-lyrics/core')

            const canvas = document.createElement('canvas')
            canvas.style.width = '100%'
            canvas.style.height = '100%'
            canvas.style.position = 'absolute'
            canvas.style.inset = '0'
            canvas.style.zIndex = '0'
            container.appendChild(canvas)

            renderer = new PixiRenderer(canvas)
            renderer.setFlowSpeed(2)
            renderer.setRenderScale(0.5)
            renderer.setFPS(30)
            renderer.setHasLyric(true)

            const store = usePlayerStore()
            const song = store.currentSong as any
            if (song) {
                const { coverUrl } = useCoverUrl()
                await setAlbumCover(coverUrl(song.id))
            }

            if (store.isPlaying) startBass()

            isReady.value = true
            console.log('[AMLL-BG] 背景渲染器初始化成功')
        } catch (e) {
            console.error('[AMLL-BG] 初始化失败:', e)
            initDone = false
        }
    }

    async function setAlbumCover(coverSrc: string) {
        if (!renderer) return
        try { await renderer.setAlbum(coverSrc) } catch (e) { console.warn('[AMLL-BG] 封面:', e) }
    }

    function pauseBg() { renderer?.pause(); stopBass() }
    function resumeBg() { renderer?.resume(); startBass() }

    function dispose() {
        stopBass()
        if (renderer) {
            try { renderer.dispose() } catch (e) { console.warn('[AMLL-BG] dispose:', e) }
            renderer = null
        }
        analyser = null
        bassData = null
        isReady.value = false
        initDone = false
    }

    const store = usePlayerStore()
    const { coverUrl } = useCoverUrl()

    watch(() => store.currentSong, (song: any) => {
        if (song) setAlbumCover(coverUrl(song.id))
    })

    watch(() => store.isPlaying, (playing) => {
        if (playing) resumeBg()
        else pauseBg()
    })

    return { initRenderer, dispose, isReady, setAlbumCover }
}