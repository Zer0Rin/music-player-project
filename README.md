# MusicPlayer

一个包含本地音乐播放、歌词展示、用户与歌单、社区互动和 AI 辅助能力的全栈 Web 音乐平台。

![Nuxt 3](https://img.shields.io/badge/Nuxt-3-00DC82?style=flat-square&logo=nuxt.js)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat-square&logo=spring)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?style=flat-square&logo=vue.js)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite)

## 核心功能

- **播放与歌词**：支持常见音频格式、多种播放模式、逐行/逐字歌词、画中画、频谱与沉浸式歌词界面；
- **用户与权限**：Spring Security + JWT 登录鉴权、个人资料、管理员能力和多用户数据隔离；
- **歌单与社区**：歌单管理、分享码导入、评论、热门推荐、最近播放和歌词全文搜索；
- **AI 能力**：AI DJ、SSE 流式歌曲解析、AI 乐评和评论分类；
- **全栈体验**：Nuxt 3 / Vue 3 前端与 Spring Boot REST 后端，SQLite 本地持久化。

## 界面

### LiquidUI 沉浸式歌词

<img width="2555" height="1183" alt="LiquidUI 沉浸式歌词界面" src="https://github.com/user-attachments/assets/a1e3b410-96c9-45bb-8b3b-3d5a353be915" />

### Apple Music-like Lyrics

<img width="2141" height="1195" alt="Apple Music-like Lyrics 界面" src="https://github.com/user-attachments/assets/2cf63ef7-5994-4d16-8cb8-7d9fcd2952db" />

## 技术栈

| 层级 | 技术 |
|---|---|
| Frontend | Nuxt 3 · Vue 3 · Pinia · Tailwind CSS · Three.js |
| Backend | Java 17 · Spring Boot 3.2.5 · Spring Security · Spring Data JPA |
| AI | Spring AI · OpenAI-compatible chat API · SiliconFlow embeddings |
| Data | SQLite · JAudioTagger |
| Interfaces | REST · SSE · JWT |

## 架构

```text
Nuxt 3 / Vue 3 Client
  └─ REST + SSE
       └─ Spring Boot
            ├─ Security / JWT
            ├─ Music, Playlist, User and Community Services
            ├─ AI Chat and Embedding Integrations
            └─ SQLite + Local Music Files
```

## 快速开始

### 环境要求

- Node.js 18+
- pnpm 8+
- Java 17+
- Maven 3.8+

### 1. 克隆仓库

```bash
git clone https://github.com/Zer0Rin/music-player-project.git
cd music-player-project
```

### 2. 准备音乐目录

```bash
mkdir -p backend/music-data/audio backend/music-data/covers backend/music-data/lyrics
```

音频、封面和歌词使用相同的基础文件名进行匹配，例如 `song.mp3`、`song.jpg` 和 `song.lrc`。

### 3. 配置 AI Key

后端从环境变量读取聊天与向量模型 Key：

```bash
read -s AI_API_KEY
export AI_API_KEY
read -s SILICONFLOW_API_KEY
export SILICONFLOW_API_KEY
```

不使用 AI 功能时仍可查看播放器与常规业务代码。不要把真实 Key 写入 `application.yml` 或提交到仓库。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。SQLite 数据库位于 `backend/music-data/musicplayer.db`。

### 5. 启动前端

在另一个终端运行：

```bash
cd frontend
pnpm install
pnpm dev
```

前端默认运行在 `http://localhost:3000`。

## 配置与安全边界

- `backend/src/main/resources/application.yml` 当前包含本地演示用 JWT 和管理员默认配置，不应直接用于公网部署；
- 数据库使用 SQLite 与 `ddl-auto: update`，适合本地演示，不代表生产迁移方案；
- AI 聊天默认接入 DeepSeek 兼容接口，向量检索使用 SiliconFlow embeddings；
- 仓库不包含真实音频文件、API Key 或生产凭据。

## 项目结构

```text
music-player-project/
├─ frontend/                    Nuxt 3 / Vue 3 Client
│  ├─ components/              Player, lyrics, playlists and user views
│  ├─ composables/             Reusable playback and UI logic
│  ├─ pages/                   Main and login pages
│  └─ stores/                  Pinia state
└─ backend/
   └─ src/main/java/com/musicplayer/
      ├─ controller/           REST and SSE endpoints
      ├─ service/              Business and AI services
      ├─ repository/           Spring Data repositories
      ├─ model/                JPA entities
      └─ security/             JWT filter and access rules
```

## 验证

当前公开分支没有完整的自动化测试套件或覆盖率报告。可执行以下命令验证后端编译测试阶段与前端生产构建：

```bash
cd backend
mvn test

cd ../frontend
pnpm install
pnpm build
```

`backend/test.http` 提供部分接口的手动请求样例；接口行为仍应结合本地数据和鉴权状态验证。

## 使用范围

仓库不包含或分发受版权保护的真实音频文件。截图中出现的音乐相关名称、封面和歌词仅用于界面展示，相关权利归原权利人所有。

当前仓库未附带开源许可证；如需复制、分发或用于其他项目，请先联系作者。

## Acknowledgements

- [Apple Music-like Lyrics](https://github.com/amll-dev/applemusic-like-lyrics/tree/full-refractor)
