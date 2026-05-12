# 药师祖庭会员管理系统 - 部署指南

## 📋 目录

- [系统要求](#系统要求)
- [快速开始](#快速开始)
- [详细部署步骤](#详细部署步骤)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [运维指南](#运维指南)

---

## 系统要求

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 磁盘 | 20GB | 50GB+ SSD |

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **操作系统**: Windows 10/11, macOS 10.15+, Ubuntu 18.04+, CentOS 7+

---

## 快速开始

### 方式一：Docker 部署（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd yaishizuting

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 访问应用
# 前端: http://localhost:3001
# 后端: http://localhost:8090/api
```

### 方式二：手动部署

#### 后端部署

```bash
# 1. 安装依赖
mvn clean install

# 2. 运行应用
java -jar target/yaoshizuting-admin-1.0.0.jar
```

#### 前端部署

```bash
# 1. 安装依赖
cd frontend
npm install

# 2. 开发模式
npm run dev:h5

# 3. 生产构建
npm run build:h5
```

---

## 详细部署步骤

### 1. 环境准备

#### 1.1 安装 Docker

**Windows/macOS**:
- 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

**Linux (Ubuntu)**:
```bash
# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 2. 服务端口

| 服务 | 容器端口 | 主机端口 | 说明 |
|------|----------|----------|------|
| 前端 | 80 | 3001 | Vue3 应用，可通过 FRONTEND_PORT 覆盖 |
| 后端 | 8080 | 8090 | Spring Boot |
| MySQL | 3306 | 3307 | 数据库 |
| Redis | 6379 | 6380 | 缓存 |

### 3. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建并启动
docker-compose up -d --build
```

### 4. 验证部署

```bash
# 检查后端服务
curl http://localhost:8090/api/health

# 检查前端和前端代理
curl http://localhost:3001/
curl http://localhost:3001/api/health

# 检查数据库连接
docker exec yaoshizuting-mysql mysql -uroot -proot -e "SELECT 1"

# 检查 Redis 连接
docker exec yaoshizuting-redis redis-cli ping
```

---

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| SPRING_DATASOURCE_HOST | mysql | MySQL主机 |
| SPRING_DATASOURCE_PORT | 3306 | MySQL端口 |
| SPRING_DATASOURCE_USERNAME | root | MySQL用户名 |
| SPRING_DATASOURCE_PASSWORD | root | MySQL密码 |
| SPRING_DATA_REDIS_HOST | redis | Redis主机 |

### 应用配置

编辑 `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yaoshizuting
    username: root
    password: root
  
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: your-jwt-secret-key
  expiration: 604800000
```

---

## API接口

### 认证接口
```
POST /api/auth/sendCode/{mobile}  - 发送验证码
POST /api/auth/login              - 用户登录
```

### 财务接口
```
GET /api/finance/wallet - 获取钱包信息
```

### 团队接口
```
GET /api/team/tree - 获取团队树
```

### 管理接口（需管理员权限）
```
GET /api/admin/policy/{key} - 获取配置
PUT /api/admin/policy       - 更新配置
```

### 支付回调
```
POST /api/v1/pay/wechat/notify - 微信支付回调
POST /api/v1/pay/alipay/notify - 支付宝回调
```

---

## 数据库

### 表结构
- `gyt_user` - 用户表
- `gyt_order` - 订单表
- `gyt_profit_log` - 分润记录表
- `gyt_config_policy` - 策略配置表
- `gyt_withdrawal` - 提现记录表
- `gyt_product` - 产品表

### 数据备份

```bash
# 备份 MySQL
docker exec yaoshizuting-mysql mysqldump -uroot -proot yaoshizuting > backup.sql

# 恢复 MySQL
docker exec -i yaoshizuting-mysql mysql -uroot -proot yaoshizuting < backup.sql
```

---

## 常见问题

### 1. 端口冲突

```bash
# 查看端口占用
netstat -ano | findstr :8090  # Windows
lsof -i :8090                  # Linux/macOS

# 修改端口（docker-compose.yml）
ports:
  - "8091:8080"
```

### 2. MySQL 连接失败

```bash
# 检查 MySQL 状态
docker-compose ps mysql
docker-compose logs mysql
docker-compose restart mysql
```

### 3. Redis 连接失败

```bash
# 测试 Redis 连接
docker exec yaoshizuting-redis redis-cli ping
```

### 4. 前端无法访问后端

- 检查 `vite.config.js` 中的代理配置
- 确认后端服务已启动
- 检查 CORS 配置

---

## 运维指南

### 日志管理

```bash
# 查看实时日志
docker-compose logs -f app

# 查看最近 100 行
docker-compose logs --tail=100 app

# 导出日志
docker-compose logs app > app.log
```

### 性能监控

```bash
# 查看容器资源
docker stats

# 查看容器详情
docker inspect yaoshizuting-app
```

### 服务管理

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建
docker-compose up -d --build
```

---

## 测试账号

| 角色 | 手机号 | 说明 |
|------|--------|------|
| 超级管理员 | 13800000000 | 完整权限 |
| 普通用户 | 13900139002 | 基础权限 |

**登录方式**: 短信验证码登录

---

## 访问地址

- **前端**: http://localhost:3001
- **后端API**: http://localhost:8090/api
- **API文档**: http://localhost:8090/api/swagger-ui.html
- **MySQL**: localhost:3307 (root/root)
- **Redis**: localhost:6380

---

**最后更新**: 2026-03-04
