/**
 * useAudioPlayer - 音频播放核心
 * 暴露 audio 元素供频谱可视化使用
 */

// 全局单例：audio 元素和 AudioContext（跨组件共享）
let _audio = null
let _audioCtx = null
let _analyser = null
let _sourceNode = null

export function useAudioPlayer() {
  const store = usePlayerStore()
  let rafId = null

  function getAudio() {
    return _audio
  }

  /** 获取 AnalyserNode（供频谱可视化） */
  function getAnalyser() {
      if (_analyser) {
          if (_audioCtx && _audioCtx.state === 'suspended') _audioCtx.resume()
          return _analyser
      }
    if (!_audio) return null

    try {
      _audioCtx = _audioCtx || new (window.AudioContext || window.webkitAudioContext)()
      if (!_sourceNode) {
        _sourceNode = _audioCtx.createMediaElementSource(_audio)
      }
      _analyser = _audioCtx.createAnalyser()
      _analyser.fftSize = 256
      _analyser.smoothingTimeConstant = 0.8
      _sourceNode.connect(_analyser)
      _analyser.connect(_audioCtx.destination)

      if (_audioCtx.state === 'suspended') {
          _audioCtx.resume()
      }
      return _analyser
    } catch (e) {
      console.warn('创建音频分析器失败:', e)
      return null
    }
  }

  function init() {
    if (_audio) return

    _audio = new Audio()
    _audio.volume = store.volume

    _audio.addEventListener('loadedmetadata', () => {
      store.setDuration(_audio.duration)
    })

    _audio.addEventListener('ended', () => {
      store.isPlaying = false
      handlePlayEnd()
    })

    _audio.addEventListener('error', (e) => {
      console.error('音频加载失败:', e)
      store.isPlaying = false
    })

    startRAF()
  }

  /** 播放结束处理（根据播放模式） */
  function handlePlayEnd() {
    switch (store.playMode) {
      case 'loop-one':
        // 单曲循环
        if (_audio) {
          _audio.currentTime = 0
          _audio.play().catch(console.error)
          store.isPlaying = true
        }
        break
      case 'loop-all':
        // 列表循环
        if (store.hasNext) {
          store.nextSong()
        } else if (store.playlist.length > 0) {
          store.playSong(store.playlist[0], 0)
        }
        break
      case 'shuffle':
        // 随机播放
        if (store.playlist.length > 1) {
          let idx
          do { idx = Math.floor(Math.random() * store.playlist.length) }
          while (idx === store.currentIndex)
          store.playSong(store.playlist[idx], idx)
        }
        break
      default: // 'sequence'
        store.nextSong()
        break
    }
  }


    function startRAF() {
        function tick() {
            if (_audio) {
                const t = _audio.currentTime
                if (t !== store.currentTime) {
                    store.updateTime(t)
                }
            }
            rafId = requestAnimationFrame(tick)
        }
        rafId = requestAnimationFrame(tick)
    }

  function loadAndPlay(songId) {
    if (!_audio) init()
    _audio.src = `/api/songs/${songId}/audio`
    _audio.load()

    // 恢复 AudioContext（浏览器要求用户交互后才能播放）
    if (_audioCtx && _audioCtx.state === 'suspended') {
      _audioCtx.resume()
    }

    _audio.play().catch(console.error)
  }

  function play() {
    if (_audioCtx && _audioCtx.state === 'suspended') _audioCtx.resume()
    _audio?.play().catch(console.error)
  }

  function pause() {
    _audio?.pause()
  }

  function seek(time) {
    if (_audio) {
      _audio.currentTime = time
      store.updateTime(time)
    }
  }

  function setVolume(v) {
    if (_audio) _audio.volume = v
    store.volume = v
  }

  watch(() => store.isPlaying, (playing) => {
    if (playing) play()
    else pause()
  })

  watch(() => store.currentSong, (song) => {
    if (song) loadAndPlay(song.id)
  })

  function destroy() {
    if (rafId) cancelAnimationFrame(rafId)
  }

  onMounted(() => init())
  onUnmounted(() => destroy())

  return { seek, setVolume, getAudio, getAnalyser }
}
