import { defineStore } from 'pinia'

export const usePlaylistStore = defineStore('playlist', {
    state: () => ({
        playlists: [],
        activePlaylistId: null,  // 当前查看的歌单（null = 全部歌曲）
    }),

    getters: {
        /** "我喜欢"歌单 */
        favorites(state) {
            return state.playlists.find(p => p.system === true || p.isSystem === true)
        },

        /** 用户创建的歌单 */
        userPlaylists(state) {
            return state.playlists.filter(p => !p.system)
        },

        /** 当前查看的歌单 */
        activePlaylist(state) {
            // 如果没选中任何歌单 (比如点击了主页的"全部歌曲")
            if (!state.activePlaylistId) return null

            // 💡 核心修复：拦截特殊的 'favorites' 暗号，直接返回系统的"我喜欢"歌单
            if (state.activePlaylistId === 'favorites') {
                return state.playlists.find(p => p.system === true || p.isSystem === true)
            }

            // 否则，老老实实按真实的数据库 ID 查找
            return state.playlists.find(p => p.id === state.activePlaylistId)
        },

        /** 检查某首歌是否被收藏 */
        isFavorite(state) {
            return (songId) => {
                const fav = state.playlists.find(p => p.system === true || p.isSystem === true)
                return fav ? fav.songIds.includes(songId) : false
            }
        },
    },

    actions: {
        /** 加载所有歌单 */
        async fetchPlaylists() {
            try {
                const { $apiFetch } = useNuxtApp()
                this.playlists = await $apiFetch('/api/playlists')
            } catch (e) {
                console.error('加载歌单失败:', e)
            }
        },

        /** 创建歌单 */
        async createPlaylist(name, description, tags) {
            try {
                const { $apiFetch } = useNuxtApp()
                const pl = await $apiFetch('/api/playlists', {
                    method: 'POST',
                    body: { name, description, tags },
                })
                this.playlists.push(pl)
                return pl
            } catch (e) {
                console.error('创建歌单失败:', e)
            }
            return null
        },

        /** 上传歌单封面 */
        async uploadCover(playlistId, blob) {
            try {
                const formData = new FormData()
                formData.append('file', blob, 'cover.jpg')
                const { $apiFetch } = useNuxtApp()
                await $apiFetch(`/api/playlists/${playlistId}/cover`, {
                    method: 'POST',
                    body: formData,
                })
                await this.fetchPlaylists()
            } catch (e) {
                console.error('上传封面失败:', e)
            }
        },

        /** 删除歌单封面 */
        async deleteCover(playlistId) {
            try {
                const { $apiFetch } = useNuxtApp()
                await $apiFetch(`/api/playlists/${playlistId}/cover`, { method: 'DELETE' })
                await this.fetchPlaylists()
            } catch (e) {
                console.error('删除封面失败:', e)
            }
        },

        /** 删除歌单 */
        async deletePlaylist(id) {
            try {
                const { $apiFetch } = useNuxtApp()
                await $apiFetch(`/api/playlists/${id}`, { method: 'DELETE' })
                this.playlists = this.playlists.filter(p => p.id !== id)
                if (this.activePlaylistId === id) this.activePlaylistId = null
                return true
            } catch (e) {
                console.error('删除歌单失败:', e)
            }
            return false
        },

        /** 重命名歌单 */
        async renamePlaylist(id, newName) {
            try {
                const { $apiFetch } = useNuxtApp()
                const pl = await $apiFetch(`/api/playlists/${id}/rename`, {
                    method: 'PUT',
                    body: { name: newName },
                })
                const idx = this.playlists.findIndex(p => p.id === id)
                if (idx >= 0) this.playlists[idx] = pl
            } catch (e) {
                console.error('重命名失败:', e)
            }
        },

        /** 更新歌单信息（名称/简介/标签） */
        async updatePlaylist(id, { name, description, tags }) {
            try {
                const { $apiFetch } = useNuxtApp()
                const pl = await $apiFetch(`/api/playlists/${id}`, {
                    method: 'PUT',
                    body: { name, description, tags },
                })
                const idx = this.playlists.findIndex(p => p.id === id)
                if (idx >= 0) this.playlists[idx] = pl
                return pl
            } catch (e) {
                console.error('更新歌单失败:', e)
            }
            return null
        },

        /** 添加歌曲到歌单 */
        async addSong(playlistId, songId) {
            try {
                const { $apiFetch } = useNuxtApp()
                const pl = await $apiFetch(`/api/playlists/${playlistId}/songs`, {
                    method: 'POST',
                    body: { songId },
                })
                const idx = this.playlists.findIndex(p => p.id === playlistId)
                if (idx >= 0) this.playlists[idx] = pl
            } catch (e) {
                console.error('添加歌曲失败:', e)
            }
        },

        /** 移除歌曲 */
        async removeSong(playlistId, songId) {
            try {
                const { $apiFetch } = useNuxtApp()
                const pl = await $apiFetch(`/api/playlists/${playlistId}/songs/${songId}`, {
                    method: 'DELETE',
                })
                const idx = this.playlists.findIndex(p => p.id === playlistId)
                if (idx >= 0) this.playlists[idx] = pl
            } catch (e) {
                console.error('移除歌曲失败:', e)
            }
        },

        /** 切换收藏 */
        async toggleFavorite(songId) {
            try {
                const { $apiFetch } = useNuxtApp()
                await $apiFetch('/api/playlists/favorites/toggle', {
                    method: 'POST',
                    body: { songId },
                })
                // 重新加载歌单数据以同步
                await this.fetchPlaylists()
            } catch (e) {
                console.error('收藏切换失败:', e)
            }
        },

        /** 切换查看的歌单 */
        setActivePlaylist(id) {
            this.activePlaylistId = id
        },
    },
})