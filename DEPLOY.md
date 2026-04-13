# 舒然画室小程序部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 腾讯云轻量应用服务器 (4核4G推荐)
- 域名 + SSL证书
- 微信小程序 AppID 和 AppSecret

## 部署步骤

### 1. 准备服务器

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 2. 克隆项目

```bash
git clone <项目地址>
cd shuranArt
```

### 3. 配置环境变量

创建 `.env` 文件：

```bash
# 数据库密码
DB_PASSWORD=your_secure_password

# 微信小程序配置
WX_APPID=your_appid
WX_SECRET=your_secret

# JWT密钥
JWT_SECRET=your_jwt_secret_at_least_32_characters
```

### 4. 配置SSL证书

将SSL证书放到 `nginx/ssl/` 目录：

```bash
mkdir -p nginx/ssl
# 将证书文件放到该目录
# cert.pem - 证书文件
# key.pem - 私钥文件
```

### 5. 构建并启动服务

```bash
# 构建后端
cd backend
mvn clean package -DskipTests
cd ..

# 启动所有服务
docker-compose up -d
```

### 6. 验证部署

```bash
# 检查容器状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

## 微信小程序配置

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入「开发」-「开发管理」-「开发设置」
3. 配置服务器域名：
   - request合法域名: `https://tianma.chat`

4. 更新小程序中的 `app.js`:
```javascript
globalData: {
  baseUrl: 'https://tianma.chat/api'
}
```

5. 更新 `project.config.json` 中的 AppID

## 常用命令

```bash
# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 更新部署
git pull
cd backend && mvn clean package -DskipTests && cd ..
docker-compose up -d --build
```

## 数据备份

```bash
# 备份数据库
docker exec shuranart_mysql_1 mysqldump -u root -p$DB_PASSWORD shuran_art > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i shuranart_mysql_1 mysql -u root -p$DB_PASSWORD shuran_art < backup.sql
```

## 添加管理员

```sql
-- 连接数据库
docker exec -it shuranart_mysql_1 mysql -u root -p

-- 添加管理员(需要先让用户登录一次获取openid)
INSERT INTO admin_whitelist (openid, name) VALUES ('用户的openid', '管理员姓名');
```

## 故障排查

### 后端无法启动
```bash
# 查看日志
docker-compose logs backend

# 检查数据库连接
docker exec -it shuranart_mysql_1 mysql -u root -p -e "SELECT 1"
```

### 小程序请求失败
1. 检查服务器域名是否配置正确
2. 检查SSL证书是否有效
3. 检查API路径是否正确
