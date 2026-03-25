export function useDownload() {
    function downloadSong(song) {
        if (!song?.audioFile) return

        const token = localStorage.getItem('token')

        // 用 fetch 带 token 下载，再转成 blob
        fetch(`http://localhost:8080/api/songs/${song.id}/audio`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(res => res.blob())
            .then(blob => {
                const url = URL.createObjectURL(blob)
                const a = document.createElement('a')
                a.href = url
                a.download = `${song.title || song.audioFile}`
                document.body.appendChild(a)
                a.click()
                document.body.removeChild(a)
                URL.revokeObjectURL(url)
            })
            .catch(e => console.error('下载失败:', e))
    }

    function downloadPlaylist(songs) {
        if (!songs?.length) return
        songs.forEach((song, i) => {
            setTimeout(() => downloadSong(song), i * 800)
        })
    }

    return { downloadSong, downloadPlaylist }
}