# 主题管理 API

## 概述

主题管理模块提供了主题的完整生命周期管理功能，包括主题创建、查询、更新、删除，以及主题权限配置和统计信息获取。

## 主题基础操作

### 获取主题列表（公开）
```http
GET /themes
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "鸡煲笑话",
        "prompt": "生成关于《杀戮尖塔》游戏中鸡煲角色的搞笑段子，包含游戏机制、卡牌、战斗等元素",
        "icon": "😄",
        "createdBy": {
          "id": "uuid",
          "username": "admin"
        },
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

## 主题管理（ROOT权限）

### 创建主题
```http
POST /themes
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "string",           // 必需，2-50字符，唯一
  "prompt": "string",        // 可选，AI生成笑话的特征描述，最大500字符
  "icon": "string"          // 可选，emoji或图标URL
}
```

### 更新主题
```http
PUT /themes/{themeId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "string",
  "prompt": "string",
  "icon": "string"
}
```

### 删除主题
```http
DELETE /themes/{themeId}
Authorization: Bearer <token>
```

**注意**: 删除主题会级联删除该主题下的所有笑话和评论，操作不可逆。


## 主题权限管理

### 获取有主题权限用户列表
```http
GET /themes/{themeId}?page=0&size=10
Authorization: Bearer <token>
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "user": {
          "id": "uuid",
          "username": "themeadmin",
          "avatarUrl": "string"
        },
        "permissionLevel": "admin",
      }
    ]
  }
}
```

补充：按照主题中的权限数量排序，先排序有主题admin权限的，再排序有主题delete权限的，然后是add。没有任何权限的用户，需要自己通过uid/username搜索来赋予权限。

### 设置主题权限
```http
POST /themes/{themeId}/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "123",
  "permissionLevel": "write|admin"
}
```

### 更新主题权限
```http
PUT /themes/{themeId}/permissions/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "permissionLevel": "write|admin",
  "note": "string"
}
```

### 移除主题权限
```http
DELETE /themes/{themeId}/permissions/{userId}
Authorization: Bearer <token>
```

## 注意事项

1. **主题名称**: 必须唯一，建议使用有意义的名称
2. **删除限制**: 仅root可以删除主题，会删除所有笑话
