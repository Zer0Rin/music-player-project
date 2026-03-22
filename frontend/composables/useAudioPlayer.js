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
        if (_audio) {
            startRAF()
            return
        }

        _audio = new Audio()
        _audio.crossOrigin = 'anonymous'
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

        _audio.addEventListener('waiting', () => {
            console.log('音频缓冲中...', _audio.currentTime)
        })


        _audio.addEventListener('suspend', () => {
            console.log('音频挂起', _audio.currentTime)
        })



        _audio.addEventListener('stalled', () => {
            console.log('音频停滞，尝试恢复', _audio.currentTime)
            // 强制重新加载当前位置
            const currentTime = _audio.currentTime
            setTimeout(() => {
                if (_audio && !_audio.paused && store.isPlaying) {
                    _audio.load()
                    _audio.currentTime = currentTime
                    _audio.play().catch(console.error)
                }
            }, 200)
        })

        startRAF()
    }

  /** 播放结束处理（根据播放模式） */
  function handlePlayEnd() {
      if (store.playMode === 'loop-one') {
          if (_audio) {
              _audio.currentTime = 0
              _audio.play().catch(console.error)
              store.isPlaying = true
          }
      } else {
          store.nextSong(true) // 传 true，告知是自然播完
      }
  }

    function startRAF() {
        if (rafId) clearInterval(rafId)
        rafId = setInterval(() => {
            if (_audio) {
                const t = _audio.currentTime
                if (t !== store.currentTime) {
                    store.updateTime(t)
                }
            }
        }, 250) // 每250ms同步一次，足够流畅且不卡顿
    }

    function loadAndPlay(songId) {
        if (!_audio) init()

        // 先暂停当前播放
        if (playPromise) {
            playPromise.then(() => {
                _audio.pause()
                playPromise = null
                _startLoad(songId)
            }).catch(() => _startLoad(songId))
        } else {
            if (_audio.src) _audio.pause()
            _startLoad(songId)
        }
    }

    function _startLoad(songId) {
        const config = useRuntimeConfig()
        _audio.src = `${config.public.apiBase}/api/songs/${songId}/audio`
        _audio.load()
        if (_audioCtx && _audioCtx.state === 'suspended') _audioCtx.resume()
        setTimeout(() => {
            playPromise = _audio.play().catch(e => {
                if (e.name !== 'AbortError') console.error(e)
            })
        }, 500)
    }

    let playPromise = null

    function play() {
        if (_audioCtx && _audioCtx.state === 'suspended') _audioCtx.resume()
        if (_audio) {
            playPromise = _audio.play().catch(e => {
                if (e.name !== 'AbortError') console.error(e)
            })
        }
    }

    function pause() {
        if (_audio) {
            if (playPromise) {
                playPromise.then(() => {
                    _audio.pause()
                    playPromise = null
                }).catch(() => {})
            } else {
                _audio.pause()
            }
        }
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
    onUnmounted(() => {
        if (rafId) clearInterval(rafId)
        rafId = null
    })

  return { seek, setVolume, getAudio, getAnalyser }
}


export function stopAudio() {
    console.log('stopAudio called', _audio)
    if (_audio) {
        _audio.pause()
        _audio.currentTime = 0
        _audio.src = ''
        _audio.load() // 强制清空缓冲
    }
}