# 🎵 Music Player — Apple Music 风格歌词播放器

Nuxt 3 + Spring Boot 全栈音乐播放器，带 Apple Music 风格歌词同步 + 流体背景动画。

## 项目结构

### 后端 (`backend/`)
```
src/main/java/com/musicplayer/
├── MusicPlayerApplication.java          # 启动入口
├── config/WebConfig.java                # CORS 配置
├── model/
│   ├── Song.java                        # @Entity: id/title/artist/album/genre/year/duration/audioFile/lyricsFile/coverFile/hasEmbeddedCover/hasEmbeddedLyrics
│   └── Playlist.java                    # @Entity: id/name/description/tags(@ElementCollection)/coverImage/songIds(@ElementCollection+@OrderColumn)/isSystem
├── repository/
│   ├── SongRepository.java              # JpaRepository<Song, String>
│   └── PlaylistRepository.java          # JpaRepository + findByIsSystemTrue()
├── service/
│   ├── MusicService.java                # 启动时扫描audio/目录，JAudioTagger读取ID3标签，自动导出内嵌封面/歌词
│   └── PlaylistService.java             # 歌单CRUD + 收藏 + 封面上传 + @Transactional
└── controller/
    ├── SongController.java              # GET /api/songs, /{id}/audio(支持Range请求), /{id}/lyrics, /{id}/cover
    └── PlaylistController.java          # 歌单CRUD + PUT /{id}更新 + POST /{id}/cover上传 + DELETE /{id}/cover删除 + 收藏toggle

src/main/resources/application.yml       # SQLite数据源 + JPA配置 + 50MB上传限制
```

### 数据目录 (`backend/music-data/`)
```
├── audio/             # mp3/flac/wav/ogg/m4a 音频文件
├── lyrics/            # lrc/elrc 歌词文件（外部 + ID3导出）
├── covers/            # 专辑封面图片（外部 + ID3导出）
├── playlist-covers/   # 歌单自定义封面（裁剪后上传）
└── musicplayer.db     # SQLite 数据库（自动创建）
```

### 前端 (`frontend/`)
```
├── nuxt.config.ts                       # Pinia/Google Fonts/Tailwind/devProxy/viewport meta
├── app.vue                              # <NuxtPage />
├── assets/css/main.css                  # 全局样式：CSS变量 + liquid-panel/card/btn + 白天模式 + 呼吸动画
├── pages/index.vue                      # 主页壳：环境光球 + 三路由分发 + 汉堡菜单 + 弹窗挂载 + 播放栏联动
├── stores/
│   ├── player.js                        # 播放核心：playlist/playOrder/playMode + LRC解析器 + 逐字时间戳 + recentSongs(localStorage)
│   └── playlist.js                      # 歌单CRUD + 收藏 + 封面上传/删除 + activePlaylistId
├── composables/
│   ├── useAudioPlayer.js                # HTML5 Audio封装 + rAF时间同步 + AudioContext频谱 + handlePlayEnd播放模式
│   └── useCoverUrl.js                   # 封面URL加时间戳防缓存
├── components/
│   ├── layout/Sidebar.vue               # 侧栏：主题切换(日月) + 导航(全部/我喜欢/最近/社区) + 歌单列表 + "+"下拉菜单 + 右键菜单 + 移动端抽屉
│   ├── home/SongTable.vue               # 双形态：grid网格卡片(主页) / list大图列表(歌单详情) + 搜索 + 红心 + 右键菜单(播放/收藏/添加到歌单/从歌单移除) + 歌单封面+播放全部+编辑按钮
│   ├── player/
│   │   ├── PlayerBar.vue                # 底部播放条：进度条 + 封面/歌名 + 播放控制 + 播放模式 + 音量 + 播放列表弹窗 + liquidUI玻璃
│   │   ├── LyricView.vue               # 沉浸式歌词页：封面+频谱+进度条+控制+音量+播放模式+爱心收藏 + 移动端封面/歌词翻转切换
│   │   ├── FluidBackground.vue          # 流体背景：封面取色5光球 + 鼠标排斥交互 + 呼吸动画
│   │   └── AudioVisualizer.vue          # 音频频谱：Web Audio API AnalyserNode + 48根柱子
│   ├── lyrics/LyricsDisplay.vue         # 歌词渲染：逐字/逐行切换 + 双语 + 自动滚动 + 距离模糊淡出 + 准星线跳转
│   ├── playlist/
│   │   ├── CreatePlaylistModal.vue      # 创建歌单：名称/简介/标签 + 图片裁剪(SVG遮罩+clip-path+拖动+滚轮缩放) + 白天模式适配
│   │   └── EditPlaylistModal.vue        # 编辑歌单：同上裁剪系统(mask ID=editCropMask) + 已有封面纯预览模式 + isNewImage标记
│   └── community/CommunityView.vue      # 社区页：小红书风瀑布流 + 多图轮播 + 详情弹窗 + 手势 + CSS变量+liquid类融合
```


## 快速启动

### 第 1 步：放入音乐文件

将你的 mp3 和 lrc 文件放入 `backend/music-data/` 目录：

```
backend/music-data/
├── audio/
│   ├── 晴天.mp3
│   └── 七里香.mp3
├── lyrics/
│   ├── 晴天.lrc          ← 文件名必须和 mp3 完全一致
│   └── 七里香.lrc
└── covers/
    ├── 晴天.jpg          ← 可选，支持 jpg/png/webp
    └── 七里香.jpg
```

**关键：文件名要一一对应！**（不含扩展名的部分要相同）

### 第 2 步：启动后端

```bash
cd backend

# 方式 A：用 IDEA
# File → Open → 选择 backend 文件夹 → 等待 Maven 下载依赖
# 运行 MusicPlayerApplication.java 的 main 方法

# 方式 B：命令行
./mvnw spring-boot:run
# Windows 用: mvnw.cmd spring-boot:run
```

后端启动后访问 http://localhost:8080/api/songs 验证，应该能看到 JSON 歌曲列表。

### 第 3 步：启动前端

```bash
cd frontend
pnpm install        # 首次需要安装依赖
pnpm dev            # 启动开发服务器
```

打开 http://localhost:3000，你应该能看到完整的播放器界面。

### 第 4 步：在 IDEA 中开发

推荐在 IDEA 中同时打开前后端：

1. **File → Open** → 选择 `music-player-project` 根目录
2. 后端：右键 `MusicPlayerApplication.java` → Run
3. 前端：打开 IDEA 内置终端 → `cd frontend && pnpm dev`

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/songs` | GET | 获取歌曲列表 |
| `/api/songs/{id}` | GET | 获取歌曲详情 |
| `/api/songs/{id}/audio` | GET | 获取音频流 |
| `/api/songs/{id}/lyrics` | GET | 获取 LRC 歌词文本 |
| `/api/songs/{id}/cover` | GET | 获取专辑封面 |

## 技术要点

### 歌词同步原理
```
audio.currentTime → requestAnimationFrame 每帧检查
→ 二分查找当前时间对应的歌词行 → 更新高亮 + 自动滚动
```

### 流体背景
- 从专辑封面提取 4 个主色调（Canvas 采样 + 颜色量化）
- 4 个大模糊圆形（CSS filter: blur）做漂浮动画
- `mix-blend-mode: screen` 混合出流体感
- 后续可升级为 PixiJS / WebGL 方案（参考 AMLL 的实现）

### 后续可升级方向
- [ ] 接入 AMLL Vue 组件（逐字歌词动画）
- [ ] PixiJS 流体背景（更接近真实 Apple Music）
- [ ] 歌词搜索 API（从网络获取歌词）
- [ ] 用户系统 + 收藏 + 播放历史
- [ ] 响应式移动端适配
