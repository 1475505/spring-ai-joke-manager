# 部署指南

## Docker部署

### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: joke_manager
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/joke_manager
      REDIS_URL: redis://redis:6379
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - postgres
      - redis
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### 后端Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 前端Dockerfile
```dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
```

## 生产环境配置

### application-prod.yml
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  redis:
    url: ${REDIS_URL}
    timeout: 2000ms
    
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}

logging:
  level:
    com.example.jokemanager: INFO
    org.springframework.ai: WARN
  file:
    name: /var/log/joke-manager.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### nginx.conf
```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    server {
        listen 80;
        server_name localhost;

        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }

        location /api {
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

## 环境变量配置

### 必需环境变量
```bash
# 数据库配置
DATABASE_URL=jdbc:postgresql://localhost:5432/joke_manager
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Redis配置
REDIS_URL=redis://localhost:6379

# AI配置
OPENAI_API_KEY=sk-your-api-key
OPENAI_BASE_URL=https://api.openai.com

# JWT配置
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000
```

### 可选环境变量
```bash
# 应用配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 日志配置
LOG_LEVEL=INFO
LOG_FILE=/var/log/joke-manager.log
```

## 部署步骤

### 1. 准备环境
```bash
# 创建项目目录
mkdir -p /opt/joke-manager
cd /opt/joke-manager

# 复制配置文件
cp docker-compose.yml .
cp init.sql .
```

### 2. 构建镜像
```bash
# 构建后端
./mvnw clean package -DskipTests
docker build -t joke-manager-backend .

# 构建前端
cd frontend
npm run build
docker build -t joke-manager-frontend .
```

### 3. 启动服务
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

### 4. 健康检查
```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端访问
curl http://localhost:3000
```

## 监控和维护

### 日志管理
```bash
# 查看应用日志
docker-compose logs -f backend

# 查看数据库日志
docker-compose logs -f postgres

# 日志轮转配置
logrotate /etc/logrotate.d/joke-manager
```

### 备份策略
```bash
# 数据库备份
docker exec postgres pg_dump -U postgres joke_manager > backup.sql

# 恢复数据库
docker exec -i postgres psql -U postgres joke_manager < backup.sql
```

### 性能监控
```bash
# 查看容器资源使用
docker stats

# 查看数据库连接数
docker exec postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
```

## 扩展部署

### 负载均衡配置
```nginx
upstream backend {
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}

server {
    location /api {
        proxy_pass http://backend;
    }
}
```

### 数据库集群
```yaml
# 主从复制配置
postgres-master:
  image: pgvector/pgvector:pg16
  environment:
    POSTGRES_REPLICATION_MODE: master
    POSTGRES_REPLICATION_USER: replicator
    POSTGRES_REPLICATION_PASSWORD: replicator_password

postgres-slave:
  image: pgvector/pgvector:pg16
  environment:
    POSTGRES_REPLICATION_MODE: slave
    POSTGRES_MASTER_HOST: postgres-master
```
