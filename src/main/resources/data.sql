/*
 * 测试数据说明：
 * 1. 三个用户：
 *    - root (密码: root123) - 哈希值: ff9830c42660c1dd1942844f8069b74a
 *    - admin (密码: admin123) - 哈希值: 0192023a7bbd73250516f069df18b500
 *    - user (密码: user123) - 哈希值: 6ad14ba9986e3615423dfca256d04e3f
 * 2. 三个主题：
 *    - 鸡煲笑话：关于《杀戮尖塔》中鸡煲的搞笑段子
 *    - 赛诺笑话：关于《原神》中赛诺的搞笑段子  
 *    - 字节蹲坑笑话：关于字节跳动的职场搞笑段子
 *    每个主题下包含若干笑话和评论
 * 3. 权限分配：
 *    - root用户：具有最高权限（ROOT角色）
 *    - admin用户：具有三个主题的写入和管理权限
 *    - user用户：普通用户，没有额外权限
 */

-- Drop existing tables in reverse order of dependencies
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS user_theme_permissions;
DROP TABLE IF EXISTS jokes;
DROP TABLE IF EXISTS themes;
DROP TABLE IF EXISTS users;

-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER' CHECK (role IN ('ROOT', 'USER')),
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
    -- H2不支持GENERATED ALWAYS AS，使用默认值代替
    final_score DECIMAL(3,1),
    quality_level VARCHAR(20),
    
    -- 状态
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')),
    is_ai_generate BOOLEAN DEFAULT false,
    
    -- 统计信息
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    
    -- 相似度检测字段
    content_hash VARCHAR(64), -- 内容哈希，用于快速相似度检测
    
    -- 元数据
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
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

-- 评论表 (支持主题和笑话两级评论)
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    joke_id BIGINT REFERENCES jokes(id) ON DELETE CASCADE,
    theme_id BIGINT REFERENCES themes(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    author_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 约束：joke_id 和 theme_id 不能同时为空，但可以只有一个
    CHECK ((joke_id IS NOT NULL AND theme_id IS NULL) OR (joke_id IS NULL AND theme_id IS NOT NULL))
);

-- ================================
-- 插入测试数据
-- ================================

-- 插入用户数据
INSERT INTO users (user_name, password_hash, role, avatar_url) VALUES
('root', 'ff9830c42660c1dd1942844f8069b74a', 'ROOT', 'https://example.com/avatars/root.jpg'),
('admin', '0192023a7bbd73250516f069df18b500', 'USER', 'https://example.com/avatars/admin.jpg'),
('user', '6ad14ba9986e3615423dfca256d04e3f', 'USER', 'https://example.com/avatars/user.jpg');

-- 插入主题数据
INSERT INTO themes (name, description, icon, created_by) VALUES
('鸡煲笑话', '关于《杀戮尖塔》中鸡煲的搞笑段子，分享游戏中的幽默时刻', '🐔', 1),
('赛诺笑话', '关于《原神》中赛诺的搞笑段子，记录旅行者的欢乐时光', '⚡', 1),
('字节蹲坑笑话', '关于字节跳动的职场搞笑段子，程序员的日常生活写照', '💻', 1);

-- 插入用户主题权限数据（admin用户对所有主题具有写入和管理权限）
INSERT INTO user_theme_permissions (user_id, theme_id, write_permission, admin_permission) VALUES
(2, 1, true, true),
(2, 2, true, true),
(2, 3, true, true);

-- 插入笑话数据
-- 鸡煲笑话
INSERT INTO jokes (title, content, theme_id, ai_score, manual_score, final_score, quality_level, status, is_ai_generate, view_count, like_count, content_hash, created_by, updated_by) VALUES
('机器人的困惑', '机器人问：为什么我总是打不过精英怪？因为你没有升级卡牌啊！机器人：那为什么我升级了还是打不过？因为你没氪金！', 1, 8.5, null, 8.5, 'GOOD', 'APPROVED', false, 156, 23, 'hash1', 1, 1),
('卡牌的秘密', '为什么卡牌总是不听话？因为它们有自己的想法！什么想法？想被回收换钱...', 1, 7.8, 8.2, 8.2, 'GOOD', 'APPROVED', false, 89, 15, 'hash2', 2, 2),
('鸡煲的日常', '鸡煲：我每天就是吃吃喝喝睡睡，偶尔被人召唤出来打个架，这是什么神仙生活！', 1, 9.1, null, 9.1, 'EXCELLENT', 'APPROVED', true, 203, 45, 'hash3', 1, 1);

-- 赛诺笑话
INSERT INTO jokes (title, content, theme_id, ai_score, manual_score, final_score, quality_level, status, is_ai_generate, view_count, like_count, content_hash, created_by, updated_by) VALUES
('赛诺的冷笑话', '赛诺：为什么雷电将军总是面无表情？因为她害怕笑起来会漏电！', 2, 8.0, 8.5, 8.5, 'GOOD', 'APPROVED', false, 234, 56, 'hash4', 2, 2),
('旅行者的疑问', '旅行者：赛诺，你的笑话为什么这么冷？赛诺：因为我是雷系的，天生自带冰属性buff！', 2, 7.5, null, 7.5, 'AVERAGE', 'APPROVED', false, 167, 28, 'hash5', 3, 3),
('沙漠探险', '赛诺在沙漠里迷路了，别人问他怎么办？他说：没关系，我自带GPS！什么GPS？Great Pun System（绝佳冷笑话系统）！', 2, 9.2, null, 9.2, 'EXCELLENT', 'APPROVED', true, 189, 67, 'hash6', 1, 1);

-- 字节蹲坑笑话
INSERT INTO jokes (title, content, theme_id, ai_score, manual_score, final_score, quality_level, status, is_ai_generate, view_count, like_count, content_hash, created_by, updated_by) VALUES
('程序员的日常', '为什么程序员总是熬夜？因为bug在夜里更活跃！什么时候最活跃？在你准备下班的那一刻！', 3, 8.8, null, 8.8, 'GOOD', 'APPROVED', false, 445, 89, 'hash7', 2, 2),
('产品经理的需求', '产品经理：这个需求很简单，就是把大象装进冰箱。程序员：好的，需要多大的冰箱？产品经理：不要冰箱，直接装！', 3, 9.5, 9.0, 9.0, 'EXCELLENT', 'APPROVED', false, 678, 134, 'hash8', 3, 3),
('字节的福报', '在字节工作是什么体验？996是福报，007是常态，555是梦想（5点起床，5点睡觉，5天不回家）！', 3, 7.2, 8.0, 8.0, 'GOOD', 'APPROVED', false, 567, 78, 'hash9', 1, 1);

-- 插入评论数据（包含主题级和笑话级评论）
-- 主题级评论
INSERT INTO comments (content, joke_id, theme_id, user_id, author_name) VALUES
('这个主题的笑话都好好笑！鸡煲真的太可爱了！', null, 1, 2, null),
('期待更多鸡煲的搞笑内容，作为杀戮尖塔玩家表示很有共鸣', null, 1, 3, null),
('赛诺的冷笑话确实很有特色，虽然冷但是很有趣！', null, 2, 3, null),
('作为原神玩家，这些笑话真的很贴切！', null, 2, 2, null),
('这些笑话太真实了，字节人的日常写照！', null, 3, 2, null),
('程序员的痛，只有程序员懂！', null, 3, null, '资深码农');

-- 笑话级评论
INSERT INTO comments (content, joke_id, theme_id, user_id, author_name) VALUES
('哈哈哈，机器人也要氪金，太真实了！', 1, null, 2, null),
('这个笑话让我想起了我的游戏经历', 1, null, 3, null),
('卡牌确实有自己的想法，想被回收换钱！', 2, null, null, '杀戮尖塔玩家'),
('鸡煲的生活真的太惬意了，羡慕！', 3, null, 2, null),
('雷电将军漏电这个梗太好笑了', 4, null, 2, null),
('天生自带冰属性buff，绝了！', 5, null, null, '原神玩家'),
('Great Pun System，赛诺真的是冷笑话大师', 6, null, 3, null),
('bug确实在下班时最活跃，太真实了', 7, null, null, '资深码农'),
('产品经理的需求永远这么奇葩', 8, null, 3, null),
('555是梦想，笑死我了', 9, null, null, '互联网打工人');
