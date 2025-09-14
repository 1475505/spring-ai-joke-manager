# AI服务 API

## 概述

AI服务模块提供了基于人工智能的笑话评分、生成、相似度检测和知识库管理功能。系统集成了DeepSeek AI模型，为内容质量提升和用户体验优化提供智能化支持。

系统采用后端调用AI模型的架构，用户的API密钥通过前端localStorage存储，在调用AI接口时传递给后端，后端负责与DeepSeek API通信并返回结果。支持JSON格式输出和工具调用功能。


## 笑话AI评分服务

### 评分标准
| 分数基准 | 描述 |
|----------|------|
| 10分 | 非常优秀的笑话，有很强的创意和笑点 |
| 8分 | 优秀的笑话，有明显笑点 |
| 6分 | 一般的笑话，无法让用户笑起来 |
| 4分 | 偏离主题，不知所云，或没有笑点的笑话 |
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

### AI智能评分
```http
POST /ai/score
Authorization: Bearer <token>
Content-Type: application/json

{
  "jokeId": "uuid",
  "apiKey": "sk-xxxxxxxxxxxxxxxx",
  "modelName": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com"
}
```

**请求参数**:
- `jokeId`: 笑话ID
- `apiKey`: DeepSeek API密钥
- `modelName`: 模型名称，默认为"deepseek-chat"
- `baseUrl`: API基础URL，默认为"https://api.deepseek.com"

**响应示例**:
```json
{
  "success": true,
  "message": "AI评分完成",
  "data": {
    "jokeId": "uuid",
    "aiScore": {
      "score": 8,
      "feedback": "这是一个很有创意的笑话，运用了巧妙的谐音梗，语言表达生动有趣，完全符合主题要求。笑点明确，能够引起读者的共鸣和笑声。"
    }
  }
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "AI评分失败",
  "error": "API_KEY_INVALID",
  "details": "DeepSeek API密钥无效或已过期"
}
```

### 批量AI评分
```http
POST /ai/score/batch
Authorization: Bearer <token>
Content-Type: application/json

{
  "jokeIds": ["uuid1", "uuid2", "uuid3"],
  "apiKey": "sk-xxxxxxxxxxxxxxxx",
  "modelName": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com"
}
```

**请求参数**:
- `jokeIds`: 笑话ID数组（最多10个）
- `apiKey`: DeepSeek API密钥
- `modelName`: 模型名称，默认为"deepseek-chat"
- `baseUrl`: API基础URL，默认为"https://api.deepseek.com"

**响应示例**:
```json
{
  "success": true,
  "message": "批量AI评分完成",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failedCount": 0,
    "results": [
      {
        "jokeId": "uuid1",
        "success": true,
        "aiScore": {
          "score": 8.0,
          "feedback": "评价说明..."
        }
      }
    ]
  }
}
```



## 笑话AI生成服务

### AI智能生成笑话
```http
POST /ai/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "themeId": "uuid",
  "apiKey": "sk-xxxxxxxxxxxxxxxx",
  "prompt": "碎心失败",
  "modelName": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com"
}
```

**请求参数**:
- `themeId`: 主题ID
- `apiKey`: DeepSeek API密钥
- `count`: 生成数量，默认1，最大5
- `modelName`: 模型名称，默认为"deepseek-chat"
- `baseUrl`: API基础URL，默认为"https://api.deepseek.com"

**响应示例**:
```json
{
  "success": true,
  "message": "AI生成笑话完成",
  "data": {
    "themeId": "uuid",
    "themeName": "搞笑段子",
    "generatedJokes": [
      {
        "id": "uuid",
        "title": "程序员的日常",
        "content": "程序员最怕的不是bug，而是产品经理说：'这个需求很简单，就是把大象装进冰箱里，但是要保证大象还能正常工作。'",
        "status": "PENDING",
        "isAiGenerated": true,
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "AI生成笑话失败",
  "error": "GENERATION_FAILED",
  "details": "DeepSeek API调用失败：模型响应超时"
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

