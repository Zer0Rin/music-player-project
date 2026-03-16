<template>
  <div class="fluid-bg" :class="{ 'has-cover': !!coverSrc }">
    <!-- 封面底图（极度模糊，作为底色） -->
    <div
      v-if="coverSrc"
      class="cover-base"
      :style="{ backgroundImage: `url(${coverSrc})` }"
    />

    <!-- 色彩球体（带鼠标排斥） -->
    <div
      v-for="(blob, i) in blobs"
      :key="i"
      class="fluid-blob"
      :class="`blob-${i}`"
      :style="{
        background: `radial-gradient(circle, ${blob.color} 0%, ${blob.colorFade} 70%, transparent 100%)`,
        transform: orbTransforms[i] || 'translate(0,0)',
      }"
    />

    <!-- 暗色叠加层（让歌词更易读） -->
    <div class="dark-overlay" />

    <!-- 噪点纹理 -->
    <div class="noise-layer" />
  </div>
</template>

<script setup>
const store = usePlayerStore()
const { coverUrl: makeCoverUrl } = useCoverUrl()

const blobs = ref([
  { color: 'rgba(250, 45, 72, 0.7)', colorFade: 'rgba(250, 45, 72, 0.2)' },
  { color: 'rgba(123, 47, 247, 0.6)', colorFade: 'rgba(123, 47, 247, 0.15)' },
  { color: 'rgba(29, 185, 84, 0.6)', colorFade: 'rgba(29, 185, 84, 0.15)' },
  { color: 'rgba(255, 107, 53, 0.5)', colorFade: 'rgba(255, 107, 53, 0.12)' },
  { color: 'rgba(64, 156, 255, 0.5)', colorFade: 'rgba(64, 156, 255, 0.12)' },
])

// ===== liquidUI 鼠标排斥交互 =====
const orbTransforms = ref(['', '', '', '', ''])

function getRepulsion(mx, my, cx, cy, threshold = 500, maxPush = 200) {
  const dx = cx - mx
  const dy = cy - my
  const distance = Math.sqrt(dx * dx + dy * dy)
  if (distance < threshold && distance > 0) {
    const force = Math.pow((threshold - distance) / threshold, 2)
    return { x: (dx / distance) * force * maxPush, y: (dy / distance) * force * maxPush }
  }
  return { x: 0, y: 0 }
}

function handleMouseMove(e) {
  if (typeof window === 'undefined') return
  const mx = e.clientX
  const my = e.clientY
  const w = window.innerWidth
  const h = window.innerHeight

  // 5 个光球的估算中心位置
  const centers = [
    [w * 0.1, h * 0.1],
    [w * 0.85, h * 0.9],
    [w * 0.5, h * 0.35],
    [w * 0.15, h * 0.75],
    [w * 0.6, h * 0.2],
  ]

  orbTransforms.value = centers.map(([cx, cy]) => {
    const p = getRepulsion(mx, my, cx, cy, 600, 250)
    return `translate(${p.x}px, ${p.y}px)`
  })
}

onMounted(() => { window.addEventListener('mousemove', handleMouseMove) })
onUnmounted(() => { window.removeEventListener('mousemove', handleMouseMove) })

const coverSrc = computed(() => {
  if (!store.currentSong) return null
  return makeCoverUrl(store.currentSong.id)
})

watch(coverSrc, async (url) => {
  if (!url) return
  try {
    const extracted = await extractColors(url)
    if (extracted.length >= 4) {
      blobs.value = extracted.slice(0, 5).map((c, i) => ({
        color: toRgba(c, [0.75, 0.65, 0.6, 0.55, 0.5][i] || 0.5),
        colorFade: toRgba(c, 0.1),
      }))
    }
  } catch (e) {
    console.warn('封面取色失败:', e)
  }
}, { immediate: true })

/** 从 rgb 字符串转为带透明度的 rgba */
function toRgba(rgbStr, alpha) {
  // rgbStr 格式: "r,g,b"
  return `rgba(${rgbStr}, ${alpha})`
}

/**
 * 从图片提取主色调 + 饱和度增强
 */
async function extractColors(imageUrl) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      const size = 80
      canvas.width = size
      canvas.height = size
      ctx.drawImage(img, 0, 0, size, size)

      const imageData = ctx.getImageData(0, 0, size, size).data
      const colorBuckets = {}

      for (let i = 0; i < imageData.length; i += 8) {
        let r = imageData[i]
        let g = imageData[i + 1]
        let b = imageData[i + 2]

        // 跳过太暗或太亮
        const brightness = (r + g + b) / 3
        if (brightness < 25 || brightness > 235) continue

        // 饱和度增强：拉开颜色差距
        const avg = (r + g + b) / 3
        const boost = 1.5
        r = Math.min(255, Math.round(avg + (r - avg) * boost))
        g = Math.min(255, Math.round(avg + (g - avg) * boost))
        b = Math.min(255, Math.round(avg + (b - avg) * boost))

        // 量化到 24 级
        const qr = Math.round(r / 24) * 24
        const qg = Math.round(g / 24) * 24
        const qb = Math.round(b / 24) * 24

        const key = `${qr},${qg},${qb}`
        colorBuckets[key] = (colorBuckets[key] || 0) + 1
      }

      // 按频率排序，取前几个差异化大的颜色
      const sorted = Object.entries(colorBuckets)
        .sort((a, b) => b[1] - a[1])

      // 筛选颜色差异化足够大的
      const result = []
      for (const [key] of sorted) {
        const [r, g, b] = key.split(',').map(Number)
        // 检查和已选颜色的距离
        const tooClose = result.some(existing => {
          const [er, eg, eb] = existing.split(',').map(Number)
          return Math.abs(r - er) + Math.abs(g - eg) + Math.abs(b - eb) < 80
        })
        if (!tooClose) {
          result.push(key)
          if (result.length >= 5) break
        }
      }

      // 不够 5 个就用默认补充
      const defaults = ['250,45,72', '123,47,247', '29,185,84', '255,107,53', '64,156,255']
      while (result.length < 5) {
        result.push(defaults[result.length])
      }

      resolve(result)
    }
    img.onerror = reject
    img.src = imageUrl
  })
}
</script>

<style scoped>
.fluid-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  background: #050505;
}

/* 封面底图作为最底层色彩基调 */
.cover-base {
  position: absolute;
  inset: -40px;
  background-size: cover;
  background-position: center;
  filter: blur(60px) saturate(2) brightness(0.55);
  transform: scale(1.3);
  opacity: 0.85;
  transition: background-image 1.5s ease;
}

/* 色彩球体 - 带排斥交互 */
.fluid-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  mix-blend-mode: screen;
  will-change: transform;
  transition: background 2s cubic-bezier(0.16, 1, 0.3, 1), transform 0.7s ease-out;
  animation: breathe var(--breathe-dur, 14s) ease-in-out infinite;
}

.blob-0 {
  width: 55vw;
  height: 55vw;
  max-width: 700px;
  max-height: 700px;
  top: -15%;
  left: -10%;
  animation: drift-0 14s ease-in-out infinite alternate;
}

.blob-1 {
  width: 50vw;
  height: 50vw;
  max-width: 600px;
  max-height: 600px;
  bottom: -20%;
  right: -10%;
  animation: drift-1 16s ease-in-out infinite alternate;
}

.blob-2 {
  width: 40vw;
  height: 40vw;
  max-width: 480px;
  max-height: 480px;
  top: 20%;
  left: 30%;
  animation: drift-2 18s ease-in-out infinite alternate;
}

.blob-3 {
  width: 35vw;
  height: 35vw;
  max-width: 400px;
  max-height: 400px;
  bottom: 10%;
  left: -5%;
  animation: drift-3 20s ease-in-out infinite alternate;
}

.blob-4 {
  width: 30vw;
  height: 30vw;
  max-width: 360px;
  max-height: 360px;
  top: 10%;
  right: 15%;
  animation: drift-4 22s ease-in-out infinite alternate;
}

/* 暗色叠加让文字可读 */
.dark-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  pointer-events: none;
}

/* 噪点纹理 */
.noise-layer {
  position: absolute;
  inset: 0;
  opacity: 0.035;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 128px 128px;
  pointer-events: none;
}

/* 漂浮动画 - 错开节奏产生流动感 */
@keyframes drift-0 {
  0%   { transform: translate(0, 0) scale(1) rotate(0deg); }
  100% { transform: translate(60px, 40px) scale(1.12) rotate(3deg); }
}
@keyframes drift-1 {
  0%   { transform: translate(0, 0) scale(1) rotate(0deg); }
  100% { transform: translate(-50px, -60px) scale(1.08) rotate(-2deg); }
}
@keyframes drift-2 {
  0%   { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, -30px) scale(0.92); }
}
@keyframes drift-3 {
  0%   { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-30px, 40px) scale(1.15); }
}
@keyframes drift-4 {
  0%   { transform: translate(0, 0) scale(1) rotate(0deg); }
  100% { transform: translate(25px, -20px) scale(1.05) rotate(5deg); }
}

@media (prefers-reduced-motion: reduce) {
  .fluid-blob { animation: none; }
}
</style>
