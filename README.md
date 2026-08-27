# 智汇购 · AI 智能购物商城

## 项目介绍
AI 智能购物商城系统（AIShop），基于 Spring Boot 3 + Vue 3 全栈架构。集成 AI 对话导购 Agent、RAG 语义推荐、秒杀、购物车、商品管理、订单、售后、优惠券、实时客服等完整电商链路，实现「对话式智能导购 + 电商交易」一体化体验。

## 技术栈
后端：Spring Boot 3、MyBatis-Plus、Druid、MySQL、PostgreSQL( PgVector )、Redis、Redisson、RabbitMQ、LangChain4j、Spring AI、JWT、WebSocket、SSE

前端：Vue 3 + Vite + TypeScript、Element Plus、Pinia

## 部署说明
1. 克隆代码，进入项目目录后，按需补充密钥配置（见下方「环境依赖」及 `application.template.yml`）。

### 环境依赖
- JDK 17
- Maven 3.9+
- Node 20+
- Docker & Docker Compose
- 中间件：MySQL、Redis、RabbitMQ（pgvector 向量库可选）

### 1、拉取代码
```bash
git clone https://github.com/LiorDawn/SmartMall.git
cd SmartMall
```

```
# 目录：background/AIshop/AIShop（后端）、front/ai_shop_ui-main（前端）
```

### 2、配置密钥
将后端 `src/main/resources/application.template.yml` 复制为 `application-dev.yml`（或 `application.yml`），按注释填入你自己的密钥：
- MySQL / Redis / RabbitMQ 连接信息
- 大模型 `AI_CHAT_API_KEY`、`AI_CHAT_BASE_URL`、`AI_CHAT_MODEL`（阿里云百炼 DashScope）
- `jwt.secret`（生产务必改为强随机串）
- 支付宝沙箱 `app-id`、`private-key`、`alipay-public-key`、回调地址
- 邮件 SMTP、pgvector 等

### 3、初始化数据库
导入初始化脚本：`docs/sql/aishop.sql`（MySQL 业务库）。

### 4、启动后端
```bash
cd background/AIshop/AIShop
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
> 接口文档（Knife4j）：`http://localhost:8081/doc.html`

### 5、启动前端
```bash
cd front/ai_shop_ui-main
npm install
npm run dev
```
> 默认 `http://localhost:5173`，开发环境接口代理到后端 `8081`

### 6、（可选）Docker 一键部署
仓库提供两套 Docker Compose 方案：
- `docs/deployment/prod/`：公网上线环境（含内存精确分配、低内存优化）
- `docs/deployment/vm/`：虚拟机测试环境

```bash
cd docs/deployment/<prod|vm>
docker compose up -d --build
```

---

## 核心功能
- **AI 导购**：ReAct Agent 自动调用商品/订单/购物车/售后工具，SSE 流式对话；RAG 精准商品推荐
- **交易**：商品浏览与搜索、购物车、下单结算、支付宝支付
- **营销**：秒杀预减库存 + Lua + RabbitMQ 削峰、优惠券、收藏、评价、售后
- **多角色**：用户端 / 商家端 / 管理后台（订单、用户、商品、分类、统计、客服）
- **实时通信**：SSE 流式 AI 回复、WebSocket 在线客服

> 默认关闭 pgvector；如需 AI 商品语义推荐，改为 `true` 并启动 PostgreSQL 容器。