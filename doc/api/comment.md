# 评论系统 API

## 概述

评论系统模块提供了主题评论和笑话评论的完整功能，包括评论发表、查询、管理和举报功能。用户可以对主题和笑话进行评论交流，管理员可以对评论进行审核和管理。

## 主题评论操作

### 获取主题评论列表
```http
GET /themes/{themeId}/comments?page=0&size=20&sort=createdAt&order=desc
```

**查询参数**:
- `page`: 页码，默认0
- `size`: 每页大小，默认20，最大50
- `sort`: 排序字段，可选值：createdAt/likeCount
- `order`: 排序方向，asc/desc，默认desc
- `authorId`: 按作者筛选
- `keyword`: 内容关键词搜索

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "content": "这个主题很有趣！",
        "author": {
          "id": "uuid",
          "username": "commenter",
          "avatarUrl": "string"
        },
        "authorName": "游客用户",  // 匿名评论时的显示名称
        "theme": {
          "id": "uuid",
          "name": "搞笑段子"
        },
        "statistics": {
          "likeCount": 5,
          "replyCount": 2
        },
        "isLiked": false,        // 当前用户是否点赞（需登录）
        "canEdit": false,        // 当前用户是否可编辑
        "canDelete": false,      // 当前用户是否可删除
        "replies": [             // 最新的2条回复
          {
            "id": "uuid",
            "content": "我也觉得！",
            "author": {
              "id": "uuid",
              "username": "replier"
            },
            "createdAt": "2024-01-01T00:00:00Z"
          }
        ],
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 发表主题评论
```http
POST /themes/{themeId}/comments
Content-Type: application/json
Authorization: Bearer <token> (可选)

{
  "content": "string",       // 必需，5-500字符
  "authorName": "string"     // 可选，匿名评论时的显示名称，最大20字符
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 201,
  "data": {
    "id": "uuid",
    "content": "这个主题很有趣！",
    "author": {
      "id": "uuid",
      "username": "commenter"
    },
    "authorName": "游客用户",
    "theme": {
      "id": "uuid",
      "name": "搞笑段子"
    },
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### 获取评论详情
```http
GET /comments/{commentId}
```

## 笑话评论操作

### 获取笑话评论列表
```http
GET /jokes/{jokeId}/comments?page=0&size=20&sort=createdAt&order=desc
```

**查询参数**:
- `page`: 页码，默认0
- `size`: 每页大小，默认20，最大50
- `sort`: 排序字段，可选值：createdAt/likeCount
- `order`: 排序方向，asc/desc，默认desc
- `authorId`: 按作者筛选
- `keyword`: 内容关键词搜索

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "content": "这个笑话太好笑了！",
        "author": {
          "id": "uuid",
          "username": "commenter",
          "avatarUrl": "string"
        },
        "authorName": "游客用户",  // 匿名评论时的显示名称
        "joke": {
          "id": "uuid",
          "content": "笑话内容"
        },
        "statistics": {
          "likeCount": 5,
          "replyCount": 2
        },
        "isLiked": false,        // 当前用户是否点赞（需登录）
        "canEdit": false,        // 当前用户是否可编辑
        "canDelete": false,      // 当前用户是否可删除
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 发表笑话评论
```http
POST /jokes/{jokeId}/comments
Content-Type: application/json
Authorization: Bearer <token> (可选)

{
  "content": "string",       // 必需，5-500字符
  "authorName": "string"     // 可选，匿名评论时的显示名称，最大20字符
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 201,
  "data": {
    "id": "uuid",
    "content": "这个笑话太好笑了！",
    "author": {
      "id": "uuid",
      "username": "commenter"
    },
    "authorName": "游客用户",
    "joke": {
      "id": "uuid",
      "content": "笑话内容"
    },
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

### 更新笑话评论
```http
PUT /jokes/{jokeId}/comments/{commentId}
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "string"       // 必需，5-500字符
}
```

## 评论管理操作

### 删除评论（作者或管理员）
```http
DELETE /comments/{commentId}
Authorization: Bearer <token>
```
