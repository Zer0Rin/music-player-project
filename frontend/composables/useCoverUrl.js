/**
 * 封面 URL 工具
 * 加启动时间戳防止浏览器缓存旧图片
 */
const _cacheBust = Date.now()

export function useCoverUrl() {
  function coverUrl(songId) {
    if (!songId) return ''
    return `/api/songs/${songId}/cover?v=${_cacheBust}`
  }

  return { coverUrl }
}
