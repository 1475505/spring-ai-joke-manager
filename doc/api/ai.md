# AI服务 API

## 概述

AI服务模块提供了基于人工智能的笑话评分、生成、相似度检测和知识库管理功能。系统集成了OpenAI GPT等先进AI模型，为内容质量提升和用户体验优化提供智能化支持。

这里由于api key由用户在前端定义，需要用户在浏览器调用model。可以将prompt传给前端，解析后传给后端。支持json output 和 tool call

## 笑话AI评分服务

### 评分标准
| 分数基准 | 描述 |
|----------|------|
| 10分 | 非常优秀的笑话，有很强的创意和笑点 |
| 8分 | 优秀的笑话，有明显笑点 |
| 6分 | 一般的笑话，无法让用户笑起来 |
| 4分 | 偏离主题，或没有笑点的笑话 |
| 2分 | 和主题有一部分相关性，但不算笑话 |
| 0分 | 无关内容，不是笑话 |

### 高质量笑话示例
- ✅ 改编经典文学作品、影视台词的笑话
- ✅ 包含游戏内梗和网络梗的内容
- ✅ 涉及谐音梗、创新的冷笑话
- ✅ 包含情感表达和情绪变化的笑话

### 低质量内容示例
- ❌ "鸡煲又死了"（纯游戏状况描述）
- ❌ "鸡煲好强啊"（简单情绪表达）
- ❌ "这个视频真好看"（普通观赏评论）

### 获取AI评分提示词
```http
GET /ai/score/prompt/{jokeId}
Authorization: Bearer <token>
```

**路径参数**:
- `jokeId`: 笑话ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "prompt": "请对主题为【】笑话进行评分。主题说明：=；评分标准：10分-非常优秀的笑话，有很强的创意和笑点；8分-优秀的笑话，有明显笑点；6分-一般的笑话，无法让用户笑起来；4分-偏离主题，不知所云，或没有笑点的笑话；2分-和主题有一部分相关性，但不算笑话；0分-无关内容，不是笑话。高质量笑话包括：改编经典文学作品、影视台词的笑话，包含游戏内梗和网络梗的内容，涉及谐音梗、创新的冷笑话，包含情感表达和情绪变化的笑话。请返回JSON格式：{\"score\": 8, \"feedback\": \"评价说明\"}",
    "jokeContent": "笑话内容...",
    "themeInfo": {
      "name": "主题名称",
      "prompt": "主题描述"
    }
  }
}
```

### 提交AI评分结果
```http
POST /ai/score/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "jokeId": "uuid",
  "humorScore": 8,
  "creativityScore": 7,
  "languageScore": 9,
  "themeScore": 8,
  "overallScore": 8.0,
  "feedback": "评价说明"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "AI评分提交成功",
  "data": {
    "jokeId": "uuid",
    "aiScore": 8.0,
    "finalScore": 8.2
  }
}
```

## 笑话AI生成服务

### 获取AI生成提示词
```http
GET /ai/generate/prompt/{themeId}
Authorization: Bearer <token>
```

**路径参数**:
- `themeId`: 主题ID

**查询参数**:
- `count`: 生成数量，默认1，最大5
- `style`: 生成风格，可选值：funny/witty/absurd/wordplay

**响应示例**:
```json
{
  "success": true,
  "data": {
    "prompt": "请根据主题'搞笑段子'生成1个幽默笑话。要求：1. 内容健康正面 2. 语言生动有趣 3. 符合主题特色 4. 长度适中(50-200字)。请返回JSON格式：{\"jokes\": [{\"title\": \"标题\", \"content\": \"内容\"}]}",
    "themeInfo": {
      "id": "uuid",
      "name": "搞笑段子",
      "prompt": "主题描述",
      "icon": "😄"
    },
    "parameters": {
      "count": 1,
      "style": "funny"
    }
  }
}
```

### 提交AI生成结果
```http
POST /ai/generate/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "themeId": "uuid",
  "jokes": [
    {
      "title": "笑话标题",
      "content": "笑话内容..."
    }
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "AI生成笑话提交成功",
  "data": {
    "createdJokes": [
      {
        "id": "uuid",
        "title": "笑话标题",
        "content": "笑话内容...",
        "status": "PENDING",
        "isAiGenerated": true
      }
    ]
  }
}
```

## 知识库管理

### 获取主题知识库状态
```http
GET /ai/knowledge/{themeId}/status
Authorization: Bearer <token>
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "themeId": "uuid",
    "themeName": "搞笑段子",
    "knowledgeBase": {
      "totalJokes": 150,
      "lastUpdated": "2024-01-01T00:00:00Z",
      "status": "READY",
      "version": "v1.2"
    },
    "statistics": {
      "averageScore": 7.8,
      "topKeywords": ["搞笑", "幽默", "段子"]
    }
  }
}
```

### 重建主题知识库
```http
POST /ai/knowledge/{themeId}/rebuild
Authorization: Bearer <token>
```

**权限要求**: ADMIN或ROOT用户

**响应示例**:
```json
{
  "success": true,
  "message": "知识库重建任务已启动",
  "data": {
    "taskId": "uuid",
    "themeId": "uuid",
    "status": "PROCESSING",
    "estimatedTime": "5-10分钟"
  }
}
```

### 获取知识库重建状态
```http
GET /ai/knowledge/rebuild/{taskId}/status
Authorization: Bearer <token>
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "taskId": "uuid",
    "status": "COMPLETED",
    "progress": 100,
    "startTime": "2024-01-01T00:00:00Z",
    "endTime": "2024-01-01T00:05:00Z",
    "result": {
      "processedJokes": 150,
      "newVersion": "v1.3"
    }
  }
}
```

