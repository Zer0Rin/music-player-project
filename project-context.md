# 🎵 Music Player Project Context (Stage Snapshot)

## 1. 项目概述与技术栈 (Tech Stack)
本项目是一个具备现代化 UI、完整用户体系、社区交互与 AI 辅助推荐能力的 Web 全栈音乐平台。

* **前端框架**：Nuxt 3 + Vue 3 (Composition API) + Pinia (状态管理)
* **视觉与交互**：Tailwind CSS (v3) + 自定义纯 CSS 变量 (LiquidUI) + Three.js / Canvas (动效与频谱)
* **后端架构**：Java 21 + Spring Boot 3.2.5 + Spring Data JPA
* **安全与鉴权**：Spring Security + JWT (无状态身份验证)
* **AI 能力集成**：Spring AI (接入大模型 API，支持容错 JSON 解析)
* **数据库**：SQLite (零配置本地数据库，自动建表，文件位于 `music-data/musicplayer.db`)
* **音频与元数据**：HTML5 Web Audio API (前端频谱) + JAudioTagger 3.0.1 (后端 ID3 深度解析)
* **构建工具**：pnpm (前端) / Maven (后端)

## 2. 核心系统模块 (Core Systems)

### 2.1 用户与安全中心 (User & Security)
* **JWT 鉴权机制**：通过 `JwtAuthFilter` 拦截请求，前端配合拦截器与 `auth` 中间件实现路由守护。
* **用户角色与主页**：支持普通用户登录/注册，拥有独立的个人主页 (`ProfileView`) 记录听歌行为与评论管理 (`SongCommentManager`)。
* **后台管理**：提供独立的管理面板 (`AdminPanel`) 供管理员进行平台级数据维护。

### 2.2 智能化与 AI 赋能 (AI Capabilities)
* **日推与智能打碟 (AI DJ & Daily Recommend)**：
  * 利用大模型分析用户偏好（收藏、歌单、最近播放）、当前歌单或输入语境，智能生成播放列表。
  * **鲁棒性设计**：后端在 `DailyRecommendService` 中实现了严格的字符串截取逻辑（定位 `{` 和 `}`），有效防止大模型输出多余解释文本导致的 JSON 解析崩溃 (JsonParseException)。
* **AI 智能评论与解析**：
  * `AiCommentController` 支持生成拟人化的神评或氛围评论。
  * `AiSongAnalysisController` 提供针对特定歌曲的深度 AI 鉴赏与风格分析。

### 2.3 社区与社交生态 (Community Ecosystem)
* **评论系统**：完整的歌曲评论 CRUD，用户可在沉浸式播放页的 `CommentPanel` 中查看和互动。
* **热度榜单**：`HotScoreService` 负责计算和维护歌曲/歌单的热度得分，动态生成排行榜。
* **歌单分享**：支持通过共享码 (`ShareCodeModal`) 将自建歌单快速分享给其他用户。

### 2.4 核心播放与沉浸式歌词 (Playback & Lyrics)
* **播放内核**：封装 `useAudioPlayer` 控制 HTML5 Audio，支持顺序、单曲循环、列表循环和基于 Fisher-Yates 洗牌算法的随机播放，无缝衔接多级伪路由。
* **动态频谱**：结合 Web Audio API 构建 `AudioVisualizer`。
* **多维歌词系统 (`LyricView`)**：
  * 支持解析和渲染多格式歌词（标准 LRC、扩展 `.elrc`、带时间轴的逐字/双语歌词）。
  * 具备精密的 UI 布局控制：PC 端与移动端均采用针对性防撞设计。操作按钮（收藏/评论）采用独立的吸底绝对定位，完美释放超长歌名的展示空间，呈现极致的 Apple Music 风格体验。

## 3. UI 设计规范：LiquidUI (液态玻璃拟物风)

### 3.1 核心 CSS 类与玻璃态
* `.liquid-panel`：重度毛玻璃（`blur(24px) saturate(150%)` + 内发光折射 + 外阴影），应用于侧栏、播放条、全局弹窗。
* `.liquid-card`：轻度毛玻璃（`blur(12px) saturate(120%)`），应用于内容卡片、标签。
* 支持白天/暗色双模式，通过 Tailwind `darkMode: 'class'` 与自定义非 scoped 样式覆盖实现无缝切换。沉浸式页面强制锁定暗色系以保证观感。

### 3.2 环境光球与动态背景 (`FluidBackground`)
* 实时提取当前播放歌曲封面的主色调，动态渲染呼吸光球（如暗色系下的紫/蓝/靛晕染）。
* **移动端深度适配**：侧滑抽屉、底部 Sheet 弹窗、播放页的上下翻转手势切换，以及紧凑型组件的重排。

## 4. 数据与文件管理 (Data Management)
* **自动扫描入库**：后端启动时扫描 `music-data/` 目录，读取音频文件的 ID3 标签并持久化至 SQLite。
* **文件优先级机制**：同名外部资源（`covers/`, `lyrics/`）优先级高于内嵌资源。
* **歌单封面裁剪**：前端提供基于 SVG Mask 的纯 CSS/JS 自定义裁剪器，支持滚轮缩放与拖拽。

## 5. 阶段性暂停记录与未来开发备忘
1. **数据库迁移限制**：SQLite 当前采用 `ddl-auto: update`。若未来重启项目时涉及复杂的表结构重构（如增删字段关联），需注意手动清理旧库或使用专业工具进行数据迁移。
2. **大文件代理问题**：本地开发时，若遇到大于 40MB 的音频文件，Nuxt 的 `devProxy` 可能会超时，建议后续可补充大文件分片传输或直接暴露底层静态资源映射。
3. **AI Prompt 调优**：虽然代码层已加入 JSON 截取兜底方案，但未来可进一步引入大模型的 Function Calling (Tools API) 特性，以更原生的方式确保返回格式的稳定性。




music-player-project/
├── .gitignore
├── README.md
├── project-context.md
├── frontend/                        # 前端 (Nuxt 3)
│   ├── .env.example
│   ├── app.vue
│   ├── nuxt.config.ts
│   ├── package.json
│   ├── tsconfig.json
│   ├── assets/
│   │   └── css/
│   │       └── main.css
│   ├── components/
│   │   ├── community/
│   │   │   └── CommunityView.vue
│   │   ├── home/
│   │   │   └── SongTable.vue
│   │   ├── layout/
│   │   │   └── Sidebar.vue
│   │   ├── lyrics/
│   │   │   └── LyricsDisplay.vue
│   │   ├── player/
│   │   │   ├── AiSongAnalysis.vue    # (新增) AI 歌曲鉴赏组件
│   │   │   ├── AudioVisualizer.vue
│   │   │   ├── CommentPanel.vue
│   │   │   ├── FluidBackground.vue
│   │   │   ├── LyricView.vue
│   │   │   ├── PlayerBar.vue
│   │   │   └── SleepTimer.vue        # (新增) 睡眠定时器组件
│   │   ├── playlist/
│   │   │   ├── AiDjModal.vue
│   │   │   ├── CreatePlaylistModal.vue
│   │   │   ├── EditPlaylistModal.vue
│   │   │   └── ShareCodeModal.vue
│   │   └── user/
│   │       ├── AdminPanel.vue
│   │       ├── ProfileView.vue
│   │       └── SongCommentManager.vue
│   ├── composables/
│   │   ├── useAudioPlayer.js
│   │   ├── useCoverUrl.js
│   │   ├── useDownload.js
│   │   └── usePictureInPicture.js
│   ├── middleware/
│   │   └── auth.js
│   ├── pages/
│   │   ├── index.vue
│   │   └── login.vue
│   ├── plugins/
│   │   └── auth.js
│   └── stores/
│       ├── auth.js
│       ├── player.js
│       └── playlist.js
└── backend/                         # 后端 (Spring Boot)
    ├── .env.example
    ├── pom.xml
    ├── test.http
    ├── .mvn/
    │   └── wrapper/
    │       └── maven-wrapper.properties
    └── src/
        └── main/
            ├── java/com/musicplayer/
            │   ├── MusicPlayerApplication.java
            │   ├── config/
            │   │   ├── AiFunctionConfig.java
            │   │   └── WebConfig.java
            │   ├── controller/
            │   │   ├── AiCommentController.java
            │   │   ├── AiDjController.java
            │   │   ├── AiSongAnalysisController.java # (新增) 歌曲鉴赏接口
            │   │   ├── AuthController.java
            │   │   ├── CommentController.java
            │   │   ├── DailyRecommendController.java # (新增) 日推接口
            │   │   ├── HotController.java
            │   │   ├── PlaylistController.java
            │   │   ├── RecentPlayController.java
            │   │   ├── SongController.java
            │   │   └── UserController.java
            │   ├── model/
            │   │   ├── Comment.java
            │   │   ├── DailyRecommendEntity.java     # (新增) 日推缓存实体
            │   │   ├── Playlist.java
            │   │   ├── RecentPlay.java
            │   │   ├── Song.java
            │   │   └── User.java
            │   ├── repository/
            │   │   ├── CommentRepository.java
            │   │   ├── DailyRecommendRepository.java # (新增) 日推数据访问
            │   │   ├── PlaylistRepository.java
            │   │   ├── RecentPlayRepository.java
            │   │   ├── SongRepository.java
            │   │   └── UserRepository.java
            │   ├── security/
            │   │   ├── JwtAuthFilter.java
            │   │   ├── JwtUtil.java
            │   │   └── SecurityConfig.java
            │   └── service/
            │       ├── AiCommentService.java         # (新增) AI 评论服务拆分
            │       ├── CommentService.java
            │       ├── DailyRecommendService.java    # (新增) 核心日推生成与防崩溃解析
            │       ├── HotScoreService.java
            │       ├── MusicService.java
            │       ├── PlaylistService.java
            │       └── UserService.java
            └── resources/
                └── application.yml





