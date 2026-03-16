# 🎵 Music Player — Apple Music 风格歌词播放器

Nuxt 3 + Spring Boot 全栈音乐播放器，带 Apple Music 风格歌词同步 + 流体背景动画。

## 项目结构

```
music-player-project/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/musicplayer/
│   │   ├── MusicPlayerApplication.java    # 启动类
│   │   ├── controller/SongController.java # REST API
│   │   ├── service/MusicService.java      # 音乐文件扫描服务
│   │   ├── model/Song.java                # 歌曲模型
│   │   └── config/WebConfig.java          # CORS 配置
│   ├── src/main/resources/application.yml
│   ├── music-data/             # ⬅️ 把你的音乐文件放这里
│   │   ├── audio/              #   mp3 文件
│   │   ├── lyrics/             #   lrc 歌词文件（文件名要和 mp3 对应）
│   │   └── covers/             #   专辑封面图片（文件名要和 mp3 对应）
│   └── pom.xml
│
└── frontend/                   # Nuxt 3 前端
    ├── pages/index.vue         # 主页面
    ├── components/
    │   ├── player/
    │   │   ├── FluidBackground.vue    # 流体背景动画
    │   │   ├── PlayerControls.vue     # 播放控制条
    │   │   └── SongList.vue           # 歌曲列表
    │   └── lyrics/
    │       └── LyricsDisplay.vue      # 歌词显示（核心）
    ├── composables/
    │   └── useAudioPlayer.js          # 音频播放逻辑
    ├── stores/player.js               # Pinia 状态管理
    └── nuxt.config.ts
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
