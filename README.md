# 旅城 — 个性化旅游城市推荐系统

> 在线地址：[https://lengshangbai.my](https://lengshangbai.my)

基于用户画像的智能旅游城市推荐平台。融合多源行为数据构建兴趣模型，结合空间上下文（距离偏好）和天气信息，通过归一化加权评分生成个性化推荐。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2 + Spring Security + JPA (Hibernate) + JWT |
| 数据库 | H2（开发） / MariaDB（生产） |
| 缓存 | Valkey / Redis |
| 前端 | Vue 3 + Vue Router + Pinia + Bootstrap 5 + Chart.js + Axios |
| 构建 | Vite（前端） / Maven（后端） |
| 部署 | Nginx + Cloudflare Tunnel + Docker Compose |

## 核心推荐机制

```
用户画像构建                    打分与排序
┌─────────────────┐           ┌──────────────────────┐
│ 显式反馈: 注册标签  │           │  标签匹配 (60%)        │
│ 隐式反馈: 搜索挖掘  │ ───归一化──▶ │  · 余弦相似度            │
│ 隐式反馈: 点击挖掘  │           │  · 城市向量=景点数量     │
└─────────────────┘           │                      │
                              │  距离评分 (20%)       │
┌─────────────────┐           │  · 动态出发地          │
│  空间行为分析       │           │  · 省内/周边/全国自适应   │
│  · 最近搜索出发地    │ ────────▶ │                      │
│  · 点击距离偏好     │           │  天气评分 (20%)       │
└─────────────────┘           │  · 和风天气15日预报      │
                              │  · 温度/降水/能见度修正  │
                              └──────────────────────┘
                                       │
                                       ▼
                              归一化加权 → 百分制 → Top 6
```

### 用户画像

三个数据来源融合为标签权重表，支持动态演化：

| 来源 | 方式 | 权重 |
|------|------|------|
| 显式反馈 | 注册时选择的兴趣标签 | 原始权重（0–1） |
| 隐式反馈-搜索 | 搜索过的城市提取标签 | 每标签 +0.3 |
| 隐式反馈-点击 | 点击过的城市提取标签 | 每标签 +0.3 |

### 空间画像

- **动态出发地**：取最近一次搜索填的出发地，优先于注册常居地
- **距离偏好**：分析点击数分布，≥60% 同省 → 省内推荐；≥60% 500km → 周边推荐

### 词云

- **热门标签词云**：全平台统一，全平台用户标签(70%) + 热门城市标签(30%)
- **热门城市词云**：`热度分 = 景点丰富度×0.3 + (搜索×5+点击×10)×0.7`，每 15 秒刷新

## 核心功能

| 板块 | 说明 |
|------|------|
| 猜你喜欢 | 已登录用户显示 6 个个性化城市推荐卡片，未登录回退热门推荐 |
| 智能推荐 | 出发地 + 兴趣标签 + 距离范围 + 出行日期 → 10 条综合评分推荐 |
| 热门标签词云 | 15 个标签，反映全平台用户兴趣风向 |
| 热门城市词云 | 12 个城市，按搜索/点击行为实时排名 |
| 城市详情 | 今日天气卡片 + 15 天预报折线图 + 每日预报网格 |
| 实时刷新 | 搜索、推荐后自动更新猜你喜欢和词云 |

## 快速开始

### 环境要求

- Java 17+ / Node.js 20+ / Maven 3.6+

### 后端

```bash
cd backend/

# 配置本地密钥（复制模板填入真实值）
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# 启动（local profile 加载真实 API 密钥）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

后端运行在 `http://localhost:8080`，H2 控制台在 `/h2-console`。

### 前端

```bash
cd frontend/
npm install
npm run dev
```

开发服务器运行在 `http://localhost:5173`，`/api` 请求自动代理到后端。

### 生产构建

```bash
cd frontend/ && npm run build
sudo cp nginx.conf /etc/nginx/nginx.conf
sudo nginx -s reload
```

## 项目结构

```
LvYou/
├── backend/                         # Spring Boot 后端
│   └── src/main/java/com/textoasis/
│       ├── config/                  # Security, JWT, CORS 配置
│       ├── controller/              # REST 控制器（8 个）
│       ├── service/                 # 业务逻辑层
│       │   ├── RecommendationService       # 智能推荐评分引擎
│       │   ├── PersonalizedRecommendationService  # 猜你喜欢 + 词云
│       │   ├── PopularityService           # 热度计算
│       │   ├── WeatherService / WeatherUpdateService  # 天气
│       │   └── AuthenticationService / JwtService     # 认证
│       ├── model/                   # JPA 实体（11 个）
│       ├── repository/              # 数据访问层
│       ├── dto/                     # 数据传输对象
│       ├── exception/               # 全局异常处理
│       ├── util/                    # 工具类（Haversine 距离）
│       └── startup/                 # DataSeeder 初始化数据
├── frontend/                        # Vue 3 前端
│   └── src/
│       ├── views/                   # 页面（首页/登录/注册/城市详情）
│       ├── components/              # 组件（9 个）
│       ├── store/                   # Pinia 状态管理
│       ├── api/                     # Axios API 封装
│       └── router/                  # Vue Router 路由
├── nginx.conf                       # Nginx 配置（反代 + 安全头 + 限流）
├── docker-compose.yml               # Docker Compose 编排
└── test-cases.md                    # 黑盒测试用例（46 条）
```

## 安全措施

| 措施 | 实现 |
|------|------|
| JWT 认证 | jjwt 0.12 + HS256，密钥外置不入库 |
| CORS 白名单 | Spring Security CorsConfigurationSource，按域名配置 |
| 请求限流 | Nginx `limit_req` 按真实 IP（Cloudflare CF-Connecting-IP） |
| 密码校验 | BCrypt 加密，6–128 字符长度限制 |
| 异常脱敏 | 全局异常处理器，不暴露内部错误详情 |
| 敏感路径屏蔽 | Nginx 拦截 `/.git` `/.env` 等请求 |
| H2 控制台 | 生产环境关闭，开发环境需认证 |
| 配置外置 | `application-local.properties` 不入 Git |

## 部署

```bash
# 1. Cloudflare Tunnel（自动 HTTPS）
cloudflared tunnel login
cloudflared tunnel create lvyou
cloudflared tunnel route dns lvyou lengshangbai.my
cloudflared tunnel run lvyou

# 2. Docker Compose（本地）
docker-compose up -d
```
