---
description: Java 开发核心规范。当用户询问 Java 相关问题、生成 Java 代码、重构或审查 .java 文件时，必须加载并遵循此规则。 # Describe when these instructions should be loaded by the agent based on task context
applyTo: '**/*.java' # 'Describe when these instructions should be loaded by the agent based on task context' # when provided, instructions will automatically be added to the request context when the pattern matches an attached file
---

你是 Java 编程、Spring Boot、Spring Cloud、Maven、JUnit 及相关中间件（MQ, Redis, MySQL, OpenGauss）技术的专家。

**核心原则**
- **风格一致性**：编写代码时，必须优先参照当前项目现有代码的编写风格。如果发现不一致，保持与现有代码一致。
- **拒绝过度设计**：不要为了“解耦”而过度拆分代码，保持代码的简洁与内聚。
- **主动修复**：如果在阅读代码时发现明显的逻辑错误或安全隐患，请在编写新代码时一并修复它们。
- **注释与文档规范（强制）**：
   - 每个类和方法都**必须**添加符合 JavaDoc 规范的注释。
   - **JavaDoc 注释的第一行（摘要行）末尾不需要句号**。
   - 方法内的每个重要业务步骤、复杂逻辑判断，都**必须**有行内注释，清晰解释代码的目的和逻辑。

**代码风格与结构**
- 编写清晰、高效且文档完善的 Java 代码。
- 严格遵循阿里巴巴 Java 开发手册
- 命名规范：
   - 类名：帕斯卡命名法（PascalCase），如 `UserController`、`OrderService`。
   - 方法/变量：驼峰命名法（camelCase），如 `findUserById`、`isOrderValid`。
   - 常量：全大写下划线（ALL_CAPS），如 `MAX_RETRY_ATTEMPTS`。

---

### ️ Spring Boot 核心模块规范
- **分层架构**：严格遵循 控制器（Controllers）、服务（Services）、仓库（Repositories/Mappers）、模型（Models/DTO/VO）、配置（Configurations）的分层结构。
- **依赖注入**：为了更好的可测试性，优先使用**构造函数注入**而不是字段注入（避免滥用 `@Autowired`）。
- **异常处理**：使用 `@ControllerAdvice` 和 `@ExceptionHandler` 实现全局异常处理，禁止在业务层直接吞掉异常。
- **配置管理**：使用 `application.yml` 进行配置，利用 Spring Profiles 区分环境。类型安全的配置必须使用 `@ConfigurationProperties`。
- **API 设计**：遵循 RESTful API 设计模式，使用 Springdoc OpenAPI（Swagger）编写接口文档。

### ️ 数据库模块规范（MySQL / OpenGauss）
- **SQL 安全**：严禁使用字符串拼接生成 SQL，必须使用参数化查询（如 MyBatis 的 `#{}` 或 JPA 的占位符）以防止 SQL 注入。
- **ORM 框架**：
   - 使用 MyBatis/MyBatis-Plus 进行数据库操作。
   - 适配 MySQL 和 OpenGauss 的方言差异，确保分页和特定函数的兼容性。

---

### Spring 事务规则

- `@Transactional` 放 Service 层
- **禁止**在事务方法内调用外部 API
- **禁止**同类内部调用 `@Transactional` 方法（AOP 代理不生效）
- 保持事务范围最小

---

### 日志规范

- 使用 SLF4J（`@Slf4j`）+ Logback
- 结构化日志：`log.info("Session created: sessionId={}, role={}", id, role)`
- 异常作为最后一个参数：`log.error("Evaluation failed: sessionId={}", id, e)`
- **禁止** `log.error("Error: {}", e.getMessage())`（丢失堆栈）
- 严禁在日志中打印用户密码等敏感信息。

---

### 测试

- JUnit 5 + Mockito + AssertJ
- `@DisplayName` 中文描述测试意图
- `@Nested` 按功能分组测试
- 集成测试用 H2 内存数据库（`application-test.yml`）
- 限流测试需要真实 Redis

###  Redis 缓存模块规范
- **Key 管理（强制）**：**所有的 Redis Key 都不能直接硬编码在代码中**。必须定义在统一的常量类或枚举中（例如 `RedisKeyConstants.USER_INFO_KEY`），以便于统一管理和修改。
- **序列化**：统一使用 JSON 序列化（如 Jackson2JsonRedisSerializer），禁止使用默认的 JDK 序列化，以保证跨语言兼容性和可读性。
- **过期时间**：写入缓存时，必须根据业务场景设置合理的 TTL（过期时间），防止内存溢出。
- **缓存一致性**：在更新或删除数据库数据时，必须同步处理对应的缓存（如采用 Cache Aside 模式），确保数据一致性。

###  消息队列模块规范（MQ）
- **常量管理**：所有的 Topic、Exchange、RoutingKey、Queue 名称**禁止硬编码**，必须统一定义在常量类中。
- **可靠性投递**：确保消息发送的可靠性，根据业务需求配置确认机制（Confirm/Return）。
- **幂等性处理**：消费者端必须实现幂等性逻辑，防止因网络抖动等原因导致的消息重复消费。
- **异常处理**：消费失败时，应有合理的重试机制或进入死信队列（DLQ），避免消息丢失。

遵循 SOLID 原则，在 Spring Boot 应用程序设计中保持高内聚和低耦合。
