# 数据库设计 - 最简版本

## 表结构设计

### 核心业务表结构

```sql
-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('ROOT', 'USER')),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 主题表
CREATE TABLE themes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(50),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 笑话表
CREATE TABLE jokes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT NOT NULL,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    
    -- AI评分系统
    ai_score DECIMAL(3,1) DEFAULT 7.0 CHECK (ai_score >= 0 AND ai_score <= 10),
    manual_score DECIMAL(3,1) CHECK (manual_score >= 0 AND manual_score <= 10),
    final_score DECIMAL(3,1) GENERATED ALWAYS AS (
        COALESCE(manual_score, ai_score, 7.0)
    ) STORED, -- 计算列：优先人工评分，其次AI评分，默认7.0
    quality_level VARCHAR(20) GENERATED ALWAYS AS (
        CASE 
            WHEN COALESCE(manual_score, ai_score, 7.0) >= 9.0 THEN 'EXCELLENT'
            WHEN COALESCE(manual_score, ai_score, 7.0) >= 8.0 THEN 'GOOD'
            WHEN COALESCE(manual_score, ai_score, 7.0) >= 7.0 THEN 'AVERAGE'
            ELSE 'POOR'
        END
    ) STORED, -- 质量等级计算列
    
    -- 状态
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')),
    is_ai_generate BOOLEAN DEFAULT false,
    
    -- 统计信息
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    
    -- 元数据
    created_by BIGINT REFERENCES users(id), -- 允许NULL以支持匿名投稿
    updated_by BIGINT REFERENCES users(id), -- 允许NULL以支持匿名投稿
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户主题权限表
CREATE TABLE user_theme_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    write_permission BOOLEAN NOT NULL DEFAULT FALSE,
    admin_permission BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, theme_id)
);

-- 评论表
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 知识库配置表【一期暂不实现】
CREATE TABLE knowledge_base_config (
    id BIGINT PRIMARY KEY,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    min_score_threshold DECIMAL(3,1) DEFAULT 8.0,
    max_jokes_count INTEGER DEFAULT 100,
    auto_update_enabled BOOLEAN DEFAULT true,
    last_updated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(theme_id)
);
```

## 测试数据示例

为了提高可读性和维护性，在测试数据中使用递增的简洁UUID格式：

```sql
-- 示例：用户数据
INSERT INTO users (id, user_name, password_hash, role, avatar_url) VALUES
(1, 'root', 'hash_value_1', 'ROOT', 'avatar1.jpg'),
(2, 'admin', 'hash_value_2', 'USER', 'avatar2.jpg'),
(3, 'user', 'hash_value_3', 'USER', 'avatar3.jpg');

-- 示例：主题数据
INSERT INTO themes (id, name, description, icon, created_by) VALUES
(1, '鸡煲笑话', '关于《杀戮尖塔》中鸡煲的搞笑段子', '🐔', 1),
(2, '赛诺笑话', '关于《原神》中赛诺的搞笑段子', '⚡', 1),
(3, '字节蹲坑笑话', '关于字节跳动的职场搞笑段子', '💻', 1);

-- 示例：笑话数据
INSERT INTO jokes (id, title, content, theme_id, ai_score, final_score, quality_level, status, created_by, updated_by) VALUES
(1, '机器人的困惑', '机器人问：为什么我总是打不过精英怪？因为你没有升级卡牌啊！', 1, 8.5, 8.5, 'GOOD', 'APPROVED', 1, 1),
(2, '赛诺的冷笑话', '赛诺：为什么雷电将军总是面无表情？因为她害怕笑起来会漏电！', 2, 8.0, 8.0, 'GOOD', 'APPROVED', 2, 2);
```

## 权限说明

- **ROOT用户**: 拥有所有权限，可以创建主题、管理所有内容、普通用户和主题管理员的所有权限
- **非主题管理员用户**: 可以浏览笑话、点赞、评论，投稿笑话
- **主题管理员用户**: 对特定主题有管理权限，可以审核笑话、管理评论、进行评分

## 业务规则

1. 笑话投稿后默认为PENDING状态，需要主题管理员或ROOT用户审核
2. 只有APPROVED状态的笑话才会在前端展示
3. 用户只能删除自己创建的笑话（除非是管理员）
4. 评论不需要审核，但管理员可以删除不当评论
5. 点赞数据实时更新到jokes表的like_count字段

