# AI驱动的笑话生成与管理平台

## 项目概述

基于Spring AI的现代化笑话管理系统，支持多主题笑话的AI生成、评分、搜索和管理。集成用户系统和自然语言操作功能，提供完整的笑话内容管理解决方案。

## 核心功能

### 🎭 多主题笑话管理
- **赛诺笑话**: 原神游戏相关的冷笑话和谐音梗
- **鸡煲笑话**: 杀戮尖塔游戏机器人角色相关笑话（本次仅实现这个作为demo）
- **大厂蹲坑笑话**: 字节跳动等大厂在厕所里对于黑话解释和描述
- 支持查看、搜索、增删、随机获取，提供api服务

### 🤖 AI智能能力
- **自动评分**: AI对笑话质量进行评分（默认7分满分10分）
- **内容生成**: 基于知识库生成新笑话
- **对话生成**: 用户通过自然语言对话生成定制笑话
- **质量判断**: 自动判断内容是否符合笑话标准

### 👥 用户系统
- 完整的用户注册、登录、权限管理
- 主题级别的细粒度权限控制
- 支持用户自定义AI API配置

### 🔍 智能搜索
- 基于pgvector的语义搜索
- 全文搜索和标签过滤
- 相似笑话推荐

### 🛠️ 管理员自然语言操作 (MCP)
- "查询5条内容含有战士的笑话"
- "删除刚刚新增的2条笑话"
- "统计各主题的笑话数量分布"
- 完整的操作日志记录

## 技术选型

### 后端技术栈
- **Spring Boot 3.2+** with **Spring AI** - 现代化Java框架 + AI集成
- **PostgreSQL 16+** with **pgvector** - 支持向量搜索的数据库
- **Redis** - 缓存和会话管理
- **Spring Security + JWT** - 认证和授权

### 前端技术栈  
- **React 18 + TypeScript** - 现代化前端框架
- **Zustand** - 轻量级状态管理
- **Ant Design 5.x** - 企业级UI组件库
- **Vite** - 快速构建工具

### AI能力
- 支持 OpenAI 格式的任意AI模型
- 用户可自定义API配置（localStorage存储）
- 向量语义搜索和智能推荐

## 快速开始

```bash
# 启动数据库
docker-compose up -d postgres redis

# 后端启动
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 前端启动
cd frontend && npm install && npm run dev
```

## 文档导航

- [数据库设计](doc/database.md) - 完整的数据库表结构和索引
- [API接口文档](doc/api/) - 模块化的REST API接口规范
  - [用户权限管理](doc/api/user.md) - 用户认证、权限配置
  - [主题管理](doc/api/theme.md) - 主题的增删改查和权限
  - [笑话管理](doc/api/joke.md) - 笑话投稿、审核、评分
  - [评论系统](doc/api/comment.md) - 主题评论管理
  - [AI服务](doc/api/ai.md) - AI评分、生成、知识库
  - [MCP/自然语言操作](doc/api/mcp.md) - 自然语言数据库操作
- [部署指南](doc/deployment.md) - Docker部署和生产环境配置
- [开发指南](doc/development.md) - 本地开发环境搭建
- [MCP集成](doc/mcp-integration.md) - 自然语言数据库操作实现

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。