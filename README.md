# 🎵 MusicPlayer



<div align="center">

**一个具备现代化 UI、完整用户体系、社区交互与 AI 辅助能力的全栈 Web 音乐平台**

*A full-stack web music platform with modern UI, complete user system, community interaction and AI capabilities*

![Nuxt 3](https://img.shields.io/badge/Nuxt-3-00DC82?style=flat-square&logo=nuxt.js)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat-square&logo=spring)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?style=flat-square&logo=vue.js)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

</div>

---

## ✨ 功能特性 / Features

### 🎧 播放核心 / Playback
- HTML5 Web Audio API 驱动，支持 MP3 / FLAC / WAV / OGG / M4A
- 顺序、列表循环、单曲循环、Fisher-Yates 随机四种播放模式
- 画中画（PiP）小窗播放，支持封面 / 歌词 / 进度 / 控制
- 音频停滞自动恢复，进度条拖拽流畅无抖动
- 48 柱频谱可视化

### 🎤 歌词系统 / Lyrics
- 支持标准 LRC、扩展 ELRC 逐字、双语歌词三种格式
- 逐字 / 逐行两种高亮模式自动切换
- 准星点击歌词跳转播放，鼠标滚轮自由浏览
- 沉浸式全屏歌词界面（LiquidUI 玻璃拟物风）
<img width="2555" height="1183" alt="image" src="https://github.com/user-attachments/assets/a1e3b410-96c9-45bb-8b3b-3d5a353be915" />

- 沉浸式全屏歌词界面（AMLL -https://github.com/amll-dev/applemusic-like-lyrics/tree/full-refractor）
<img width="2141" height="1195" alt="image" src="https://github.com/user-attachments/assets/2cf63ef7-5994-4d16-8cb8-7d9fcd2952db" />


### 🤖 AI 能力 / AI Features
- **AI DJ**：根据用户输入语境智能生成歌单，搜索本地曲库匹配
- **AI 歌曲解析**：SSE 流式输出，打字机效果，Markdown 渲染，分析情感基调 / 歌词意境 / 金句赏析 / 创作视角
- **AI 乐评人**：固定 AI 账号，新歌入库自动生成评论，随机混合情绪流 / 专业风两种风格
- **AI 评论管理**：对评论进行正面 / 负面 / 垃圾三分类智能检索

### 👤 用户系统 / User System
- JWT 无状态鉴权，支持登录 / 注册 / 修改密码 / 退出登录
- 个人主页：头像裁剪上传、昵称修改、账号统计
- 管理员后台：歌曲 / 用户 / 社区三 Tab 管理面板
- 多用户歌单隔离，数据完全独立

### 🎼 歌单系统 / Playlist
- 创建 / 编辑 / 删除歌单，自定义封面裁剪上传
- 推荐码分享（7 天有效期），导入生成独立副本
- 歌单显示创建者头像 / 昵称 / 创建时间
- AI DJ 一键智能生成歌单

### 🔥 热门推荐 / Hot Recommendations
- 热度算法：播放 × 0.1 + 收藏 × 3 + 评论 × 2 + 导入 × 5
- 每小时自动更新，支持管理员手动刷新
- 主页横向滚动推荐栏（歌单优先 + 歌曲）

### 🔍 搜索 / Search
- 标题 / 艺术家 / 专辑本地搜索
- **歌词全文搜索**：输入一句歌词找到对应歌曲，悬停显示完整匹配句
- 防抖 400ms，本地结果与歌词结果自动合并

### 🌙 其他 / Others
- 亮色 / 暗色双模式，LiquidUI 液态玻璃拟物风 UI
- 睡眠定时器（预设 / 自定义分钟 / 播完此曲）
- 单曲下载 / 歌单批量下载（间隔释放内存）
- 最近播放数据库持久化，跨设备同步
- 全局键盘快捷键（Space 播放 / Esc 关闭）
- 移动端适配：侧滑抽屉、底部 Sheet、手势切换

---

## 🛠️ 技术栈 / Tech Stack

| 层级 | 技术 |
|------|------|
| 前端框架 | Nuxt 3 + Vue 3 (Composition API) |
| 状态管理 | Pinia |
| UI 样式 | Tailwind CSS v3 + 自定义 LiquidUI CSS 变量 |
| 3D / 动效 | Three.js |
| 后端框架 | Java 17 + Spring Boot 3.2.5 |
| 数据持久化 | Spring Data JPA + SQLite |
| 安全鉴权 | Spring Security + JWT |
| AI 集成 | Spring AI（接入 OpenAI 兼容 API） |
| 音频元数据 | JAudioTagger 3.0.1（ID3 解析） |
| 构建工具 | pnpm（前端）/ Maven（后端） |

---

## 🚀 快速开始 / Quick Start

### 环境要求 / Prerequisites

- Node.js >= 18
- pnpm >= 8
- Java 17+
- Maven 3.8+

### 1. 克隆仓库 / Clone

```bash
git clone https://github.com/your-username/music-player.git
cd music-player
```

### 2. 配置后端 / Backend Setup

```bash
cd backend
cp .env.example .env
```

编辑 `.env` 填入必要配置：

```env
# JWT 密钥（随机字符串即可）
JWT_SECRET=your-secret-key-at-least-32-chars

# AI API 配置（支持 OpenAI 兼容接口）
SPRING_AI_OPENAI_API_KEY=your-api-key
SPRING_AI_OPENAI_BASE_URL=https://api.openai.com
```

或直接编辑 `src/main/resources/application.yml`。

启动后端：

```bash
./mvnw spring-boot:run
# Windows 用户：mvnw.cmd spring-boot:run
```

后端启动后自动扫描 `music-data/audio/` 目录入库。

### 3. 添加音乐文件 / Add Music

```
music-data/
├── audio/          # 放入音频文件（mp3/flac/wav/ogg/m4a）
├── covers/         # 可选：同名封面图片（优先级高于内嵌封面）
└── lyrics/         # 可选：同名歌词文件（.lrc / .elrc）
```

> 文件名需与音频文件一致才能自动匹配，例如 `song.mp3` 对应 `song.lrc`

### 4. 配置前端 / Frontend Setup

```bash
cd frontend
cp .env.example .env
pnpm install
pnpm dev
```

前端默认运行在 `http://localhost:3000`，后端运行在 `http://localhost:8080`。

### 5. 首次登录 / First Login

访问 `http://localhost:3000`，注册账号后登录。

若需要管理员权限，在数据库中将对应用户的 `role` 字段改为 `ADMIN`，或通过已有管理员账号在后台修改。

---

## 📁 项目结构 / Project Structure

```
music-player-project/
├── frontend/                   # Nuxt 3 前端
│   ├── components/
│   │   ├── home/               # 主页歌曲列表
│   │   ├── layout/             # 侧栏布局
│   │   ├── lyrics/             # 歌词显示
│   │   ├── player/             # 播放器组件（PlayerBar / LyricView / PiP / SleepTimer）
│   │   ├── playlist/           # 歌单相关（AI DJ / 分享码 / 编辑）
│   │   └── user/               # 用户相关（个人主页 / 管理面板 / 评论管理）
│   ├── composables/            # 可复用逻辑（useAudioPlayer / useDownload / usePiP）
│   ├── stores/                 # Pinia 状态（player / playlist / auth）
│   └── pages/                  # 页面（index / login）
│
└── backend/                    # Spring Boot 后端
    └── src/main/java/com/musicplayer/
        ├── controller/         # REST 接口
        ├── service/            # 业务逻辑
        ├── model/              # 实体类
        ├── repository/         # 数据访问层
        └── security/           # JWT 鉴权
```

---

## 📡 API 文档 / API Reference

### 认证 / Auth

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT Token |

### 歌曲 / Songs

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/songs` | 获取所有歌曲 | 登录 |
| GET | `/api/songs/{id}/audio` | 流式获取音频 | 登录 |
| GET | `/api/songs/{id}/cover` | 获取封面图片 | 公开 |
| GET | `/api/songs/{id}/lyrics` | 获取歌词文本 | 登录 |
| GET | `/api/songs/search/lyrics?q=` | 歌词全文搜索 | 登录 |
| PUT | `/api/songs/{id}` | 修改歌曲信息 | 管理员 |
| DELETE | `/api/songs/{id}` | 删除歌曲及关联文件 | 管理员 |
| POST | `/api/songs/upload` | 批量上传音频 | 管理员 |
| POST | `/api/songs/{id}/cover/upload` | 上传 / 替换封面 | 管理员 |
| POST | `/api/songs/{id}/lyrics/upload` | 上传 / 替换歌词 | 管理员 |

### 歌单 / Playlists

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/playlists` | 获取当前用户歌单 | 登录 |
| POST | `/api/playlists` | 创建歌单 | 登录 |
| PUT | `/api/playlists/{id}` | 编辑歌单 | 登录 |
| DELETE | `/api/playlists/{id}` | 删除歌单 | 登录 |
| POST | `/api/playlists/{id}/songs/{songId}` | 添加歌曲到歌单 | 登录 |
| DELETE | `/api/playlists/{id}/songs/{songId}` | 从歌单移除歌曲 | 登录 |
| POST | `/api/playlists/{id}/share` | 生成分享码 | 登录 |
| POST | `/api/playlists/import` | 导入分享码 | 登录 |

### 评论 / Comments

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/comments/song/{songId}` | 获取歌曲评论 | 登录 |
| POST | `/api/comments` | 发布评论 | 登录 |
| DELETE | `/api/comments/{id}` | 删除评论 | 登录 |
| POST | `/api/comments/{id}/like` | 点赞评论 | 登录 |

### AI / AI Features

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/ai-dj/generate` | AI DJ 生成歌单 | 登录 |
| GET | `/api/ai/analysis/{songId}` | AI 歌曲解析（SSE 流式） | 登录 |
| POST | `/api/ai-comment/generate/{songId}` | 生成 AI 乐评人评论 | 管理员 |
| POST | `/api/admin/comments/ai-analyze` | AI 评论分类检索 | 管理员 |

### 热门 / Hot

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/hot` | 获取热门歌单 + 歌曲 | 登录 |
| POST | `/api/hot/refresh` | 手动刷新热度分 | 管理员 |

### 用户 / Users

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/user/me` | 获取当前用户信息 | 登录 |
| PUT | `/api/user/me` | 修改昵称 | 登录 |
| PUT | `/api/user/me/password` | 修改密码 | 登录 |
| POST | `/api/user/avatar` | 上传头像 | 登录 |
| GET | `/api/user/all` | 获取所有用户 | 管理员 |
| PUT | `/api/user/{id}/role` | 修改用户角色 | 管理员 |
| DELETE | `/api/user/{id}` | 删除用户 | 管理员 |

---

## ⚙️ 配置说明 / Configuration

### `application.yml` 关键配置

```yaml
music:
  data:
    path: ./music-data        # 音乐文件根目录

spring:
  ai:
    openai:
      api-key: ${SPRING_AI_OPENAI_API_KEY}
      base-url: ${SPRING_AI_OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-4o-mini  # 可替换为任意兼容模型

jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000       # 7天，单位毫秒
```

### 支持的 AI 提供商 / Supported AI Providers

只需修改 `base-url` 即可接入任意 OpenAI 兼容 API：

```yaml
# DeepSeek
base-url: https://api.deepseek.com

# 阿里云百炼
base-url: https://dashscope.aliyuncs.com/compatible-mode

# 本地 Ollama
base-url: http://localhost:11434
```

---

## 🗄️ 数据库说明 / Database

项目使用 SQLite 零配置本地数据库，文件位于 `music-data/musicplayer.db`。

Spring Data JPA 使用 `ddl-auto: update` 自动维护表结构。

> ⚠️ 若涉及复杂表结构变更（如新增外键），建议删除 `musicplayer.db` 重启服务重新生成。

---

## 🤝 贡献 / Contributing

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交改动：`git commit -m 'feat: add your feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 创建 Pull Request

---

## 📄 许可证 / License

本项目基于 [MIT License](LICENSE) 开源。


---

## ⚠️ 免责声明 (Disclaimer)

本项目（包括开源的代码仓库、文档以及相关的 UI 截图）仅供**个人编程学习、技术交流及 UI/UX 设计展示**之用。

1. **零真实音源**：本仓库**不包含、不托管、也不分发**任何受版权保护的真实音频文件（如 MP3、FLAC、WAV 等格式）。
2. **元数据仅作演示**：README 截图及项目演示中出现的歌曲名称、歌手信息、专辑封面、歌词等图文元数据，仅为了直观地展示播放器的各项前端功能、排版逻辑与视觉动效（"合理使用"），**绝无任何商业盈利目的**。
3. **版权归属**：演示中涉及的所有音乐相关图片及文字资料，其著作权均归属于原版权方或原唱片公司所有。
4. **侵权处理**：如果您是相关数字资产的版权所有者，且认为本项目的演示截图不当使用了您的版权物，请通过 Issue 或邮件联系本人，我将在核实后第一时间删除并替换相关视觉素材。

---

<div align="center">
Made with ❤️ and 🎵
</div>


by 古月素星
