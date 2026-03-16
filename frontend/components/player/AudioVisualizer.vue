<template>
  <canvas
    ref="canvasRef"
    class="visualizer-canvas"
    :width="width"
    :height="height"
  />
</template>

<script setup>
const props = defineProps({
  width: { type: Number, default: 320 },
  height: { type: Number, default: 48 },
})

const store = usePlayerStore()
const canvasRef = ref(null)
let analyser = null
let animId = null
let dataArray = null

onMounted(() => {
  // 延迟获取 analyser，等 audio 初始化完成
  tryConnect()
})

function tryConnect() {
  const { getAnalyser } = useAudioPlayer()
  analyser = getAnalyser()

  if (analyser) {
    dataArray = new Uint8Array(analyser.frequencyBinCount)
    draw()
  } else {
    // 如果还没准备好，等 500ms 再试
    setTimeout(tryConnect, 500)
  }
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas || !analyser) return
  const ctx = canvas.getContext('2d')

  function render() {
    animId = requestAnimationFrame(render)

    analyser.getByteFrequencyData(dataArray)

    const w = canvas.width
    const h = canvas.height

    ctx.clearRect(0, 0, w, h)

    if (!store.isPlaying) {
      // 暂停时画静止的低矮线条
      const barCount = 48
      const barWidth = (w / barCount) * 0.65
      const gap = w / barCount

      ctx.fillStyle = 'rgba(255, 255, 255, 0.15)'
      for (let i = 0; i < barCount; i++) {
        const x = i * gap + (gap - barWidth) / 2
        ctx.fillRect(x, h - 2, barWidth, 2)
      }
      return
    }

    // 取前 48 个频段
    const barCount = 48
    const barWidth = (w / barCount) * 0.65
    const gap = w / barCount

    for (let i = 0; i < barCount; i++) {
      // 映射频率数据（跳过最低几个频段，避免太重的低音）
      const dataIndex = Math.floor((i + 2) * (dataArray.length * 0.6) / barCount)
      const value = dataArray[dataIndex] || 0
      const barHeight = Math.max(2, (value / 255) * h * 0.9)

      const x = i * gap + (gap - barWidth) / 2

      // 渐变颜色：中间亮，两侧暗
      const centerDist = Math.abs(i - barCount / 2) / (barCount / 2)
      const alpha = 0.3 + (1 - centerDist) * 0.45

      ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`
      // 从底部向上画
      ctx.fillRect(x, h - barHeight, barWidth, barHeight)

      // 顶部小圆角效果
      ctx.beginPath()
      ctx.arc(x + barWidth / 2, h - barHeight, barWidth / 2, 0, Math.PI, true)
      ctx.fill()
    }
  }

  render()
}

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
})
</script>

<style scoped>
.visualizer-canvas {
  display: block;
  opacity: 0.85;
}
</style>
