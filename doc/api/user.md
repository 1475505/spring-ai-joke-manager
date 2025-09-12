# 用户权限管理 API

## 概述

用户权限管理模块提供了完整的用户认证、用户管理和权限配置功能。支持三级权限体系（ROOT/ADMIN/USER）和细粒度的主题权限控制。

## 用户认证接口

### 用户登录
```http
POST /auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "user": {
      "id": "123",
      "username": "testuser",
      "role": "USER",
      "avatarUrl": "string"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "string",
    "expiresIn": 3600
  }
}
```


### 用户登出
```http
POST /auth/logout
Authorization: Bearer <token>
```

### 获取当前用户信息
```http
GET /auth/me
Authorization: Bearer <token>
```

### 刷新Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

### 修改密码
```http
PUT /auth/password
Authorization: Bearer <token>
Content-Type: application/json

{
  "oldPassword": "string",
  "newPassword": "string"
}
```

### 更新个人信息
```http
PUT /auth/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "avatarUrl": "string"
}
```

## 用户管理接口（ROOT权限）

### 获取用户列表
```http
GET /users?page=0&size=10&role=USER&isActive=true&keyword=username
Authorization: Bearer <token>
```

**查询参数**:
- `page`: 页码，默认0
- `size`: 每页大小，默认10
- `role`: 用户角色筛选，可选值：ROOT/ADMIN/USER
- `isActive`: 用户状态筛选，true/false
- `keyword`: 用户名或邮箱关键词搜索

### 创建用户
```http
POST /users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "role": "USER|ROOT"
}
```
补充：未登录时可以创建role为USER的用户，创建ROOT用户需要检查携带的token是不是root。

### 获取用户详情
```http
GET /users/{userId}
Authorization: Bearer <token>
```

### 更新用户信息
```http
PUT /users/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "string",
  "role": "USER|ROOT"
}
```

### 删除用户
```http
DELETE /users/{userId}
Authorization: Bearer <token>
```

### 重置用户密码
```http
PUT /users/{userId}/password
Authorization: Bearer <token>
Content-Type: application/json

{
  "newPassword": "string"
}
```



## 权限管理接口

### 获取用户权限列表
```http
GET /users/{userId}/permissions
Authorization: Bearer <token>
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "globalRole": "USER",
    "themePermissions": [
      {
        "themeId": "uuid",
        "themeName": "搞笑段子",
        "permissionLevel": "admin",
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### 设置用户主题权限
```http
POST /themes/{themeId}/permissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "uuid",
  "permissionLevel": "read|write|admin"
}
```

### 更新用户主题权限
```http
PUT /themes/{themeId}/permissions/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "permissionLevel": "read|write|admin"
}
```

### 删除用户主题权限
```http
DELETE /themes/{themeId}/permissions/{userId}
Authorization: Bearer <token>
```

### 获取主题权限列表
```http
GET /themes/{themeId}/permissions?page=0&size=10
Authorization: Bearer <token>
```


## 注意事项

1. **密码安全**: 所有密码均采用加密存储
2. **Token管理**: Access Token有效期为1小时，Refresh Token有效期为30天
3. **权限**: ROOT用户拥有所有权限