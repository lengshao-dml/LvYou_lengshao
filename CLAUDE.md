# CLAUDE.md — 旅游推荐系统

## 项目概述

前后端分离的旅游推荐系统。基于用户偏好（兴趣、地理位置、出行时间），结合天气、城市热度等动态信息，生成个性化旅游城市推荐。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot + Spring Data JPA (Hibernate) + Spring Security + JWT |
| 缓存 | Valkey (Redis 兼容) |
| 数据库 | MariaDB (兼容 MySQL) |
| 前端 | Vue 3 + Vue Router + Pinia + Bootstrap 5 + Axios |
| 数据处理 | Scala + Spark + Maven |
| 部署 | Docker Compose (本地) / K8s (生产) |
| 日志 | SLF4J / Logback |
| 监控 | Prometheus + Grafana |

## 后端包结构

```
src/main/java/com/textoasis/
├── controller/   # REST 控制器
├── service/      # 业务逻辑层
├── model/        # 数据模型 (JPA Entity)
├── repository/   # 数据访问层 (Spring Data JPA)
├── config/       # 配置类 (DB, Cache, Security)
├── exception/    # 异常处理模块
└── util/         # 工具类
```

## 开发命令

```bash
# 后端
cd backend/
./mvnw clean install        # 构建
./mvnw spring-boot:run      # 运行 (localhost:8080)
./mvnw test                 # 测试

# 前端
cd frontend/
npm install                 # 安装依赖
npm run serve               # 运行 (localhost:8081)
npm run build               # 生产构建
npm run lint                # 代码检查

# 数据处理
cd spark-job/
mvn clean package           # 构建
```

## 编码规范

### 后端 (Java)
- **类名**: PascalCase (`CityController`)
- **变量/方法/包名**: snake_case (`get_city_data`)
- **常量**: ALL_CAPS (`MAX_RETRY_COUNT`)
- **数据库访问**: 使用 JPA，禁止手写 SQL
- **异常**: 自定义异常 + `@ControllerAdvice` 全局处理
- **表名/字段名**: 全小写下划线分隔 (`user_info`, `city_features`)
- **每张表必备字段**: `id` (自增主键), `create_time`, `update_time`

### 前端 (Vue)
- **组件**: PascalCase (`CityCard.vue`)
- **变量/函数**: camelCase (`fetchCityData`)
- **样式**: 使用 `scoped` 防止全局污染
- **API 调用**: 集中在 `src/api/` 目录

### 日志规范
- **DEBUG**: 开发调试
- **INFO**: 核心业务流程（如 "成功生成推荐列表，耗时 X ms"）
- **WARN**: 潜在风险（如 "天气 API 请求超时，本次评分未计入天气因子"）
- **ERROR**: 业务或系统异常
- 每条日志含：时间戳、日志级别、来源模块、消息内容

## API 响应格式

成功:
```json
{
  "status": "success",
  "data": [...]
}
```

失败:
```json
{
  "status": "error",
  "error": {
    "code": 40001,
    "message": "出发城市名称无效或无法识别"
  }
}
```

## 安全要求
- 敏感配置放 `application-local.properties`，严禁提交密码/密钥
- 后端 CORS 严格白名单
- 所有用户输入必须校验，防注入

## 性能目标
- 推荐接口平均响应时间 < 500ms
- 天气 API 结果缓存 1 小时（同城市同天）
- 常用数据库查询走 Valkey 缓存

## Behavior Guidelines

> From [andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills) — inspired by Karpathy's observations on LLM coding pitfalls. **Tradeoff:** these bias toward caution over speed. For trivial tasks (typo fixes, one-liners), use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: *"Would a senior engineer say this is overcomplicated?"* If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

### 用户附加偏好

- **先出方案再写代码** — 大改动先看方案，确认后再实现
- **危险命令需确认** — `rm -rf` 等操作执行前再次确认
