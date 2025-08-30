# MCP/Tool Call 集成详解

## 功能概述

MCP (Model Context Protocol) 集成允许用户通过自然语言直接操作笑话数据库，提供智能化的数据管理能力。

## 支持的自然语言操作

### 查询操作
- "查询5条内容含有战士的笑话"
- "找出评分最高的10个鸡煲笑话"
- "显示最近一周新增的笑话"
- "统计各主题的笑话数量分布"

### 管理操作
- "删除刚刚新增的2条笑话"
- "将评分低于6分的笑话标记为待审核"
- "批量更新所有赛诺笑话的标签"

### 统计分析
- "分析用户最喜欢的笑话类型"
- "生成本月笑话质量报告"
- "查看最活跃的用户列表"

## 技术实现

### Spring AI Function Calling
```java
@Component
public class JokeMCPService {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private MCPOperationLogService logService;
    
    public String processNaturalLanguageQuery(String query, String userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            String result = chatClient.prompt()
                .user(query)
                .functions("queryJokes", "deleteJokes", "updateJokes", "generateReport")
                .call()
                .content();
                
            logService.logSuccess(userId, query, result, System.currentTimeMillis() - startTime);
            return result;
            
        } catch (Exception e) {
            logService.logError(userId, query, e.getMessage(), System.currentTimeMillis() - startTime);
            throw e;
        }
    }
    
    @Bean
    public FunctionCallback queryJokes() {
        return FunctionCallback.builder()
            .function("queryJokes", this::executeQuery)
            .description("查询笑话数据库，支持内容搜索、主题过滤、评分范围等条件")
            .inputType(QueryJokesRequest.class)
            .build();
    }
    
    @Bean
    public FunctionCallback deleteJokes() {
        return FunctionCallback.builder()
            .function("deleteJokes", this::executeDelete)
            .description("删除指定的笑话记录")
            .inputType(DeleteJokesRequest.class)
            .build();
    }
    
    @Bean
    public FunctionCallback updateJokes() {
        return FunctionCallback.builder()
            .function("updateJokes", this::executeUpdate)
            .description("批量更新笑话信息，如标签、评分、状态等")
            .inputType(UpdateJokesRequest.class)
            .build();
    }
    
    @Bean
    public FunctionCallback generateReport() {
        return FunctionCallback.builder()
            .function("generateReport", this::executeReport)
            .description("生成统计报告和数据分析")
            .inputType(GenerateReportRequest.class)
            .build();
    }
}
```

### Function 实现示例

#### 查询功能
```java
public String executeQuery(QueryJokesRequest request) {
    Specification<Joke> spec = Specification.where(null);
    
    if (request.getContent() != null) {
        spec = spec.and((root, query, cb) -> 
            cb.like(cb.lower(root.get("content")), 
                   "%" + request.getContent().toLowerCase() + "%"));
    }
    
    if (request.getThemeId() != null) {
        spec = spec.and((root, query, cb) -> 
            cb.equal(root.get("theme").get("id"), request.getThemeId()));
    }
    
    if (request.getMinScore() != null) {
        spec = spec.and((root, query, cb) -> 
            cb.greaterThanOrEqualTo(root.get("finalScore"), request.getMinScore()));
    }
    
    PageRequest pageRequest = PageRequest.of(0, request.getLimit());
    Page<Joke> jokes = jokeRepository.findAll(spec, pageRequest);
    
    return formatQueryResult(jokes);
}
```

#### 删除功能
```java
public String executeDelete(DeleteJokesRequest request) {
    List<Joke> jokesToDelete = new ArrayList<>();
    
    if (request.getIds() != null && !request.getIds().isEmpty()) {
        jokesToDelete = jokeRepository.findAllById(request.getIds());
    } else if (request.getConditions() != null) {
        // 根据条件查找要删除的笑话
        Specification<Joke> spec = buildDeleteConditions(request.getConditions());
        jokesToDelete = jokeRepository.findAll(spec);
    }
    
    if (jokesToDelete.isEmpty()) {
        return "未找到符合条件的笑话";
    }
    
    jokeRepository.deleteAll(jokesToDelete);
    return String.format("成功删除 %d 条笑话", jokesToDelete.size());
}
```

### 请求对象定义
```java
public class QueryJokesRequest {
    private String content;
    private UUID themeId;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String qualityLevel;
    private JokeStatus status;
    private Integer limit = 10;
    
    // getters and setters
}

public class DeleteJokesRequest {
    private List<UUID> ids;
    private Map<String, Object> conditions;
    
    // getters and setters
}

public class UpdateJokesRequest {
    private List<UUID> ids;
    private Map<String, Object> conditions;
    private Map<String, Object> updates;
    
    // getters and setters
}
```

## 操作日志系统

### 日志记录服务
```java
@Service
public class MCPOperationLogService {
    
    @Autowired
    private MCPOperationLogRepository logRepository;
    
    public void logSuccess(String userId, String query, String result, long executionTime) {
        MCPOperationLog log = new MCPOperationLog();
        log.setUserId(UUID.fromString(userId));
        log.setNaturalLanguageQuery(query);
        log.setExecutionTimeMs((int) executionTime);
        log.setSuccess(true);
        log.setOperationType(extractOperationType(query));
        
        logRepository.save(log);
    }
    
    public void logError(String userId, String query, String error, long executionTime) {
        MCPOperationLog log = new MCPOperationLog();
        log.setUserId(UUID.fromString(userId));
        log.setNaturalLanguageQuery(query);
        log.setExecutionTimeMs((int) executionTime);
        log.setSuccess(false);
        log.setErrorMessage(error);
        log.setOperationType(extractOperationType(query));
        
        logRepository.save(log);
    }
    
    private String extractOperationType(String query) {
        if (query.contains("查询") || query.contains("找") || query.contains("显示")) {
            return "QUERY";
        } else if (query.contains("删除")) {
            return "DELETE";
        } else if (query.contains("更新") || query.contains("修改")) {
            return "UPDATE";
        } else if (query.contains("统计") || query.contains("分析") || query.contains("报告")) {
            return "REPORT";
        }
        return "UNKNOWN";
    }
}
```

## 安全控制

### 权限验证
```java
@Component
public class MCPSecurityService {
    
    public boolean canExecuteOperation(String userId, String operationType, Map<String, Object> params) {
        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
            return false;
        }
        
        // 管理员可以执行所有操作
        if ("admin".equals(user.getRole())) {
            return true;
        }
        
        // 普通用户权限检查
        switch (operationType) {
            case "QUERY":
                return true; // 所有用户都可以查询
            case "DELETE":
            case "UPDATE":
                return hasWritePermission(user, params);
            default:
                return false;
        }
    }
    
    private boolean hasWritePermission(User user, Map<String, Object> params) {
        // 检查用户对相关主题的写权限
        UUID themeId = extractThemeId(params);
        if (themeId != null) {
            return userThemePermissionRepository
                .existsByUserIdAndThemeIdAndPermissionLevelIn(
                    user.getId(), themeId, 
                    Arrays.asList("write", "admin")
                );
        }
        return false;
    }
}
```

## 使用示例

### 前端集成
```typescript
// services/mcpService.ts
export class MCPService {
  async executeQuery(query: string): Promise<string> {
    const response = await api.post('/api/mcp/query', { query });
    return response.data.result;
  }
  
  async getOperationHistory(page: number = 0): Promise<MCPOperationLog[]> {
    const response = await api.get(`/api/mcp/history?page=${page}`);
    return response.data.content;
  }
}

// components/MCPChat.tsx
export const MCPChat: React.FC = () => {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  
  const handleSubmit = async () => {
    setLoading(true);
    try {
      const response = await mcpService.executeQuery(query);
      setResult(response);
    } catch (error) {
      setResult('执行失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="mcp-chat">
      <Input.TextArea
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="输入自然语言查询，如：查询5条内容含有战士的笑话"
        rows={3}
      />
      <Button 
        onClick={handleSubmit} 
        loading={loading}
        type="primary"
      >
        执行查询
      </Button>
      {result && (
        <Card className="result-card">
          <pre>{result}</pre>
        </Card>
      )}
    </div>
  );
};
```

## 最佳实践

### 1. 查询优化
- 使用数据库索引优化常见查询
- 限制返回结果数量避免性能问题
- 缓存频繁查询的结果

### 2. 安全考虑
- 严格的权限验证
- 操作日志记录
- 敏感操作需要二次确认

### 3. 错误处理
- 友好的错误提示
- 详细的日志记录
- 操作回滚机制

### 4. 性能监控
- 查询执行时间监控
- 资源使用情况跟踪
- 异常操作告警
