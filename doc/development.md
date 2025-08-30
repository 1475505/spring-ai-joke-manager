# 开发指南

## 环境要求

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 16+ (或使用Docker)

## 本地开发环境搭建

### 1. 克隆项目
```bash
git clone <repository-url>
cd spring-ai-joke-manager
```

### 2. 启动数据库服务
```bash
docker-compose up -d postgres redis
```

### 3. 后端开发环境

#### 配置文件 (application-dev.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/joke_manager
    username: postgres
    password: password
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}

logging:
  level:
    com.example.jokemanager: DEBUG
    org.springframework.ai: INFO
```

#### 启动后端
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 前端开发环境

#### 安装依赖
```bash
cd frontend
npm install
```

#### 环境配置 (.env.development)
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=AI笑话管理平台
```

#### 启动前端
```bash
npm run dev
```

## 项目结构

```
spring-ai-joke-manager/
├── src/main/java/
│   ├── config/           # 配置类
│   │   ├── SecurityConfig.java
│   │   ├── AIConfig.java
│   │   └── DatabaseConfig.java
│   ├── controller/       # REST控制器
│   │   ├── AuthController.java
│   │   ├── JokeController.java
│   │   ├── ThemeController.java
│   │   └── MCPController.java
│   ├── service/          # 业务逻辑
│   │   ├── JokeService.java
│   │   ├── AIService.java
│   │   └── MCPService.java
│   ├── repository/       # 数据访问层
│   │   ├── JokeRepository.java
│   │   └── UserRepository.java
│   ├── entity/           # 实体类
│   │   ├── Joke.java
│   │   ├── User.java
│   │   └── Theme.java
│   ├── dto/              # 数据传输对象
│   │   ├── request/
│   │   └── response/
│   └── security/         # 安全配置
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
├── frontend/
│   ├── src/
│   │   ├── components/   # React组件
│   │   │   ├── common/
│   │   │   ├── auth/
│   │   │   ├── jokes/
│   │   │   └── themes/
│   │   ├── pages/        # 页面组件
│   │   ├── stores/       # Zustand状态管理
│   │   ├── services/     # API服务
│   │   ├── utils/        # 工具函数
│   │   └── types/        # TypeScript类型
│   ├── package.json
│   └── vite.config.ts
└── doc/                  # 文档目录
```

## 开发规范

### 后端代码规范

#### 实体类示例
```java
@Entity
@Table(name = "jokes")
public class Joke {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;
    
    @Column(name = "ai_score", precision = 3, scale = 1)
    private BigDecimal aiScore = BigDecimal.valueOf(7.0);
    
    // getters, setters, constructors
}
```

#### 服务类示例
```java
@Service
@Transactional
public class JokeService {
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private AIService aiService;
    
    public Page<Joke> getJokesByTheme(String themeId, Pageable pageable) {
        return jokeRepository.findByThemeIdAndStatus(
            UUID.fromString(themeId), 
            JokeStatus.APPROVED, 
            pageable
        );
    }
    
    public Joke createJoke(JokeCreateRequest request) {
        Joke joke = new Joke();
        joke.setContent(request.getContent());
        joke.setTheme(themeRepository.findById(request.getThemeId()).orElseThrow());
        
        // AI评分
        BigDecimal score = aiService.scoreJoke(request.getContent());
        joke.setAiScore(score);
        
        return jokeRepository.save(joke);
    }
}
```

### 前端代码规范

#### 组件示例
```typescript
// components/jokes/JokeCard.tsx
interface JokeCardProps {
  joke: Joke;
  onEdit?: (joke: Joke) => void;
  onDelete?: (id: string) => void;
}

export const JokeCard: React.FC<JokeCardProps> = ({ 
  joke, 
  onEdit, 
  onDelete 
}) => {
  return (
    <Card className="joke-card">
      <Card.Body>
        <p>{joke.content}</p>
        <div className="joke-meta">
          <Tag color="blue">评分: {joke.finalScore}</Tag>
          <Tag color="green">{joke.qualityLevel}</Tag>
        </div>
      </Card.Body>
    </Card>
  );
};
```

#### 状态管理示例
```typescript
// stores/useJokeStore.ts
interface JokeStore {
  jokes: Joke[];
  loading: boolean;
  currentTheme: string;
  
  fetchJokes: (params: QueryParams) => Promise<void>;
  createJoke: (request: JokeCreateRequest) => Promise<void>;
  updateJoke: (id: string, request: JokeUpdateRequest) => Promise<void>;
  deleteJoke: (id: string) => Promise<void>;
}

export const useJokeStore = create<JokeStore>((set, get) => ({
  jokes: [],
  loading: false,
  currentTheme: '',
  
  fetchJokes: async (params) => {
    set({ loading: true });
    try {
      const response = await jokeService.getJokes(params);
      set({ jokes: response.content, loading: false });
    } catch (error) {
      set({ loading: false });
      throw error;
    }
  },
  
  // 其他方法实现...
}));
```

## 测试

### 后端测试
```bash
# 单元测试
./mvnw test

# 集成测试
./mvnw test -Dspring.profiles.active=test

# 测试覆盖率
./mvnw jacoco:report
```

### 前端测试
```bash
# 单元测试
npm run test

# E2E测试
npm run test:e2e

# 测试覆盖率
npm run test:coverage
```

## 调试

### 后端调试
```bash
# 启用调试模式
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### 前端调试
- 使用浏览器开发者工具
- React DevTools扩展
- Zustand DevTools

## 常见问题

### 数据库连接问题
```bash
# 检查PostgreSQL状态
docker-compose ps postgres

# 查看数据库日志
docker-compose logs postgres
```

### AI服务配置问题
```bash
# 测试API连接
curl -H "Authorization: Bearer sk-..." https://api.openai.com/v1/models
```

### 前端构建问题
```bash
# 清理缓存
npm run clean
rm -rf node_modules package-lock.json
npm install
```
