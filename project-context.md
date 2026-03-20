# 🎵 Music Player Project Context

## 1. 技术栈 (Tech Stack)
* **前端**：Nuxt 3 + Vue 3 (Composition API) + Pinia (状态管理)
* **样式**：Tailwind CSS (v3, `darkMode: 'class'`) + 自定义纯 CSS 变量 (LiquidUI)
* **后端**：Java 17 (Spring Boot 3.2.5) + Spring Data JPA
* **数据库**：SQLite (文件：`music-data/musicplayer.db`，零配置，`ddl-auto: update` 自动建表)
* **ID3 标签**：JAudioTagger 3.0.1 (读取 MP3/FLAC/OGG/M4A 元数据)
* **包管理器**：pnpm (前端) / Maven (后端)

## 2. 项目结构

music-player-project/
├── .gitignore                  # Git忽略配置：忽略依赖、编译文件、本地数据等
├── README.md                   # 项目说明：结构、启动、核心功能介绍
├── frontend/                   # 前端（Nuxt 3 + Pinia + Tailwind + Web Audio API）
│   ├── app.vue                 # 根组件：挂载NuxtPage，作为页面容器
│   ├── assets/
│   │   └── css/main.css        # 全局样式：CSS变量、流体UI、白天/黑夜模式、呼吸动画
│   ├── components/
│   │   ├── layout/
│   │   │   └── Sidebar.vue     # 侧边栏：主题切换、导航、歌单列表、右键菜单、移动端抽屉
│   │   ├── home/
│   │   │   └── SongTable.vue   # 歌曲展示：网格/列表双形态、搜索、收藏、右键菜单、歌单操作
│   │   ├── player/
│   │   │   ├── PlayerBar.vue   # 底部播放条：进度条、播放控制、音量、播放列表弹窗、玻璃态UI
│   │   │   ├── LyricView.vue   # 沉浸式歌词页：封面/频谱/进度条、移动端翻转切换、收藏
│   │   │   ├── FluidBackground.vue # 流体背景：封面取色光球、鼠标交互、呼吸动画
│   │   │   └── AudioVisualizer.vue # 音频频谱：Web Audio API、48柱形频谱渲染
│   │   ├── lyrics/
│   │   │   └── LyricsDisplay.vue # 歌词渲染：逐字/逐行切换、双语、自动滚动、准星跳转
│   │   └── playlist/
│   │       ├── CreatePlaylistModal.vue # 创建歌单：名称/简介/标签、封面裁剪（SVG遮罩+缩放）
│   │       └── EditPlaylistModal.vue   # 编辑歌单：复用裁剪系统、已有封面预览、新图标记
│   ├── composables/
│   │   ├── useAudioPlayer.js   # 音频核心：HTML5 Audio封装、时间同步、频谱分析、播放模式处理
│   │   └── useCoverUrl.js      # 封面URL：加时间戳防缓存、统一封面地址处理
│   ├── middleware/             # 中间件：Nuxt路由中间件（暂无具体内容）
│   ├── nuxt.config.ts          # Nuxt配置：Pinia/Google Fonts/Tailwind、代理、视口设置
│   ├── package.json            # 前端依赖：Nuxt 3、Pinia、axios、cropperjs等
│   ├── pages/
│   │   └── index.vue           # 主页：环境光球、路由分发、汉堡菜单、弹窗挂载、播放栏联动
│   ├── plugins/                # 插件：Nuxt插件（暂无具体内容）
│   ├── stores/
│   │   ├── player.js           # 播放状态：歌单/播放顺序/模式、LRC解析、最近播放（本地存储）
│   │   └── playlist.js         # 歌单状态：CRUD、收藏、封面上传/删除、激活歌单ID管理
│   └── tsconfig.json           # TypeScript配置：Nuxt 3 TS规则
└── backend/                    # 后端（Spring Boot + JPA + SQLite + JAudioTagger）
    ├── .mvn/                   # Maven包装器：跨平台运行Maven
    ├── music-data/             # 音乐数据目录（自动扫描/生成）
    │   ├── audio/              # 音频文件：mp3/flac/wav/ogg/m4a（需与歌词/封面文件名对应）
    │   ├── lyrics/             # 歌词文件：lrc/elrc（外部 + ID3标签导出）
    │   ├── covers/             # 封面文件：jpg/png/webp（外部 + ID3标签导出）
    │   ├── playlist-covers/    # 歌单封面：用户上传（裁剪后）
    │   └── musicplayer.db      # SQLite数据库：自动创建，存储歌曲/歌单信息
    ├── pom.xml                 # Maven依赖：Spring Boot、JPA、SQLite、JAudioTagger、CORS
    └── src/
        └── main/
            ├── java/com/musicplayer/
            │   ├── MusicPlayerApplication.java # 启动类：Spring Boot入口，启动时扫描音频目录
            │   ├── config/
            │   │   └── WebConfig.java          # 配置类：CORS跨域配置
            │   ├── model/
            │   │   ├── Song.java               # 实体类：歌曲信息（ID/标题/歌手/歌词/封面等）
            │   │   └── Playlist.java           # 实体类：歌单信息（名称/标签/歌曲ID列表/系统歌单标记）
            │   ├── repository/
            │   │   ├── SongRepository.java     # 数据访问：JpaRepository，歌曲CRUD
            │   │   └── PlaylistRepository.java # 数据访问：JpaRepository + 系统歌单查询
            │   ├── service/
            │   │   ├── MusicService.java       # 业务逻辑：音频扫描、ID3解析、封面/歌词导出
            │   │   └── PlaylistService.java    # 业务逻辑：歌单CRUD、收藏、封面上传（事务注解）
            │   └── controller/
            │       ├── SongController.java     # 接口控制器：歌曲相关API
            │       └── PlaylistController.java # 接口控制器：歌单相关API
            └── resources/
                └── application.yml            # 配置文件：SQLite数据源、JPA、50MB上传限制
## 3. 核心 UI 设计规范：LiquidUI (液态玻璃拟物风)

### 全局主题
- `:root` = 暗色模式（默认），`.light-mode` = 白天模式
- Tailwind `darkMode: 'class'`，切换主题时同步 `.dark` 和 `.light-mode`

### 核心 CSS 类
- `.liquid-panel`：重度毛玻璃（`blur(24px) saturate(150%)` + 内发光折射 + 外阴影）— 侧栏/播放条/弹窗
- `.liquid-card`：轻度毛玻璃（`blur(12px) saturate(120%)`）— 卡片/标签/输入框
- `.liquid-btn`：按钮玻璃效果 — hover 上移 + 发光

### 环境光球（index.vue）
- 暗色：`purple-800/50` + `blue-900/50` + `indigo-900/50`
- 白天：`sky-200/80` + `pink-400/70` + `fuchsia-100/60`
- 呼吸动画 12s/18s/15s

### 白天模式适配方式
- 组件内 scoped 样式无法匹配 `.light-mode`
- 解决方案：在组件末尾加**非 scoped 的 `<style>` 块**，用 `.light-mode .xxx` 覆盖
- 或使用 `:global(.light-mode) .xxx` 语法

## 4. 伪路由管理
`plStore.activePlaylistId` 控制 `.main-content` 视图：
- `null` → `<SongTable>` grid 模式（全部歌曲）
- `'favorites'` / 自定义歌单 ID → `<SongTable>` list 模式（歌单详情+大封面头部）
- `'recent'` → 最近播放视图（音乐/社区双 Tab）
- `'community'` → `<CommunityView>`

切换歌单时 `displaySongs` 变化 → `watch` 自动调用 `store.setPlaylist()` 更新播放列表

## 5. 歌词系统
### 支持格式
1. 带逐字+双语：`[00:43.12]こ{0.00}の{0.16} / 在{0.00}这{0.16}`
2. 普通双语：`[00:43.12]歌词内容 / 翻译内容`
3. 普通 LRC：`[00:43.12]歌词内容` → 自动生成伪逐字（行时长÷字数均分）
4. `.elrc` 扩展名也支持

### 显示模式
- `store.lyricMode`：`'word'`（逐字高亮）/ `'line'`（逐行高亮），沉浸式页面设置按钮切换
- 距离越远越模糊淡出，当前行 45% 高度位置
- 准星线：用户滚动时显示，显示时间+播放按钮，点击跳转到对应时间

## 6. 播放系统
### 播放模式 (`store.playMode`)
- `'sequence'`：顺序播放，末尾自然播完停止，手动点下一首回到开头
- `'loop-all'`：列表循环，末尾回第一首
- `'loop-one'`：单曲循环，`handlePlayEnd` 直接 `currentTime=0` 重播
- `'shuffle'`：随机模式，Fisher-Yates 洗牌生成 `playOrder`，当前歌曲固定首位，播完一轮重新洗牌

### 播放队列
- `store.playlist`：当前播放列表（随歌单切换更新）
- `store.playOrder`：播放顺序索引数组（顺序模式=0~n-1，随机模式=洗牌后）
- `store.orderCursor`：当前在 playOrder 中的位置
- `togglePlayMode()` 时调用 `updatePlayOrder()` 重新生成队列

### 上/下一首
- `hasNext` / `hasPrev` 只要列表有歌就返回 true（永远可点）
- 第一首点上一首 → 跳到最后一首
- 最后一首点下一首 → 跳到第一首

## 7. ID3 标签读取
- JAudioTagger 读取：标题/艺术家/专辑/流派/年份/时长/内嵌封面/内嵌歌词
- **封面优先级**：外部 covers/ 同名文件 > ID3 内嵌封面（自动导出）
- **歌词优先级**：外部 lyrics/ 同名文件 > ID3 内嵌歌词（自动导出）
- 新文件读 ID3 入库，已有文件只更新外部文件关联
- ID 基于文件名 hash（稳定，不随排序变化）
- 添加新歌曲：放入 audio/ 目录 → 重启后端自动扫描

## 8. 歌单系统
### 后端
- "我喜欢"：系统歌单 (id="favorites", isSystem=true)，不可删除
- 新收藏的歌曲插入列表头部 (`add(0, songId)`)
- 歌单封面：上传裁剪后的图片到 `playlist-covers/`，7MB 限制

### 前端创建/编辑弹窗
- 名称(30字) + 简介(200字) + 标签(5个) + 封面裁剪
- 裁剪系统：SVG mask + clip-path + 模糊底图 + 清晰层 + 边框 + 拖动定位 + 滚轮缩放
- 编辑时已有封面为纯预览模式（点击打开文件选择器），选新图片才进裁剪模式
- `isNewImage` / `coverRemoved` 标记控制上传/删除逻辑

## 9. 移动端适配
- `<768px` 侧栏变抽屉（左滑关闭 + 遮罩 + 汉堡按钮）
- 歌曲列表隐藏艺术家/专辑/时长列
- 播放条隐藏右侧音量/时间，高度缩小
- 沉浸式歌词页：上下布局，封面/歌词翻转切换（点击 `interactive-area`）
- 创建/编辑歌单弹窗：底部 sheet 风格
- `viewport meta`：禁止缩放 + `100dvh` + Safari 全屏支持

## 10. 已知注意事项
- **浏览器兼容**：部分 FLAC 文件在 Edge 无法播放（编码规格问题，非代码 bug）
- **Nuxt devProxy**：大文件（>40MB）通过代理可能超时，音频直接走 `/api` 代理即可
- **scoped 样式 + .light-mode**：scoped 组件内无法用 `.light-mode .xxx`，需用非 scoped `<style>` 块或 `:global()` 语法
- **SVG mask ID 冲突**：CreatePlaylistModal 用 `cropMask`，EditPlaylistModal 用 `editCropMask`
- **重复方法**：检查 `stores/player.js` 是否有重复的 `togglePlayMode`（只保留带 `updatePlayOrder()` 的版本）
- **删除数据库重建**：添加新字段后需删除 `musicplayer.db` 重启，或用数据库工具手动 ALTER TABLE