# 笑话管理 API

## 概述

笑话管理模块是系统的核心功能，提供了笑话的完整生命周期管理，包括投稿、审核、评分、查询、点赞等功能，以及相似度检测和智能推荐。

## 公开访问接口

### 获取笑话列表
```http
GET /jokes?themeId=uuid&page=0&size=10&sort=finalScore&order=desc&status=APPROVED&minScore=7.0
```

**查询参数**:
- `themeId`: 主题ID筛选
- `page`: 页码，默认0
- `size`: 每页大小，默认10
- `sort`: 排序字段，可选值：finalScore/createdAt/likeCount/viewCount
- `order`: 排序方向，asc/desc，默认desc
- `status`: 状态筛选，APPROVED/PENDING/REJECTED/HIDDEN
- `minScore`: 最低评分筛选
- `maxScore`: 最高评分筛选
- `keyword`: 内容关键词搜索
- `isAiGenerated`: 是否AI生成筛选

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "笑话标题",
        "content": "笑话内容...",
        "theme": {
          "id": "uuid",
          "name": "搞笑段子",
          "icon": "😄"
        },
        "scores": {
          "aiScore": 8.2,
          "manualScore": 8.5,
          "finalScore": 8.5
        },
        "statistics": {
          "viewCount": 150,
          "likeCount": 25
        },
        "status": "APPROVED",
        "isAiGenerated": false,
        "author": {
          "id": "uuid",
          "username": "jokester"
        },
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 10
  }
}
```

### 获取笑话详情
```http
GET /jokes/{jokeId}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "title": "笑话标题",
    "content": "笑话内容...",
    "theme": {
      "id": "uuid",
      "name": "搞笑段子",
      "icon": "😄"
    },
    "scores": {
      "aiScore": 8.2,
      "manualScore": 8.5,
      "finalScore": 8.5
    },
    "statistics": {
      "viewCount": 150,
      "likeCount": 25
    },
    "status": "APPROVED",
    "isAiGenerated": false,
    "author": {
      "id": "uuid",
      "username": "jokester",
      "avatarUrl": "string"
    },
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

### 随机获取笑话
```http
GET /jokes/random?themeId=uuid&minScore=7.0
```

**查询参数**:
- `themeId`: 主题ID筛选，可选
- `minScore`: 最低评分要求，默认0
- `count`: 返回数量，默认1，最大10

### 搜索笑话
```http
GET /jokes/search?keyword=关键词&themeId=uuid&page=0&size=10&minScore=7.0
```

### 点赞笑话
```http
PUT /jokes/{jokeId}/like
Authorization: Bearer <token>
```

### 增加浏览次数
```http
PUT /jokes/{jokeId}/view
```

## 用户投稿管理

### 投稿笑话
```http
POST /jokes
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "string",         // 可选，最大100字符
  "content": "string",       // 必需，10-2000字符
  "themeId": "uuid"          // 必需，目标主题ID
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 201,
  "data": {
    "id": "uuid",
    "title": "笑话标题",
    "content": "笑话内容...",
    "status": "PENDING",
    "aiScore": 7.5,
    "similarityCheck": {
      "hasSimilar": false,
      "maxSimilarity": 0.85,
      "similarJokes": []
    },
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### 检查内容相似度
```http
POST /jokes/similarity-check
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "string",
  "themeId": "uuid",
  "threshold": 0.95          // 相似度阈值，默认0.95
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "hasSimilar": true,
    "maxSimilarity": 0.97,
    "threshold": 0.95,
    "similarJokes": [
      {
        "id": "uuid",
        "content": "相似的笑话内容...",
        "similarity": 0.97,
        "author": "username",
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### 获取我的投稿列表
```http
GET /my/jokes?page=0&size=10&status=PENDING&themeId=uuid
Authorization: Bearer <token>
```

### 获取我的投稿详情
```http
GET /my/jokes/{jokeId}
Authorization: Bearer <token>
```

### 修改我的投稿（仅待审核状态）
```http
PUT /my/jokes/{jokeId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "string",
  "content": "string"
}
```

### 删除我的投稿（仅待审核状态）
```http
DELETE /my/jokes/{jokeId}
Authorization: Bearer <token>
```

## 审核管理（主题管理员权限）

### 获取待审核笑话列表
```http
GET /themes/{themeId}/jokes/pending?page=0&size=10&sort=createdAt&order=asc
Authorization: Bearer <token>
```

### 审批通过
```http
PUT /jokes/{jokeId}/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "manualScore": 8.5         // 可选，人工评分
}
```

### 设置人工评分
```http
PUT /jokes/{jokeId}/score
Authorization: Bearer <token>
Content-Type: application/json

{
  "manualScore": 8.5,      // 必需，0-10分
}
```

### 修改笑话状态
```http
PUT /jokes/{jokeId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "APPROVED|REJECTED|HIDDEN",
}
```

### 删除笑话
```http
DELETE /jokes/{jokeId}
Authorization: Bearer <token>
```

## 注意事项

1. **内容审核**: 所有投稿都需要经过审核才能公开显示
2. **相似度检测**: 系统会自动检测内容相似度，相似度>95%需要二次确认
3. **评分机制**: 最终评分优先采用人工评分，其次是AI评分，默认7.0分
4. **点赞防刷**: 同一用户对同一笑话只能点赞一次，游客防御TODO