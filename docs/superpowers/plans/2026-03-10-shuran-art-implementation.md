# 画室小程序实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建画室小程序，包含活动推广和分享抽奖裂变功能

**Architecture:** 前后端分离架构。后端 Spring Boot 提供 RESTful API，前端微信小程序原生开发，通过 Docker Compose 部署到腾讯云轻量服务器

**Tech Stack:** Spring Boot 3.x, MyBatis-Plus, MySQL 8, 微信小程序原生, Docker Compose, Nginx

---

## 文件结构

### 后端 backend/
```
backend/
├── pom.xml
├── Dockerfile
├── src/main/java/com/shuran/art/
│   ├── ArtApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   ├── WxConfig.java
│   │   └── WebMvcConfig.java
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── StudioController.java
│   │   ├── TeacherController.java
│   │   ├── ActivityController.java
│   │   ├── ShareController.java
│   │   ├── LotteryController.java
│   │   └── PointsController.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── TeacherService.java
│   │   ├── ActivityService.java
│   │   ├── ShareService.java
│   │   ├── LotteryService.java
│   │   └── PointsService.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── TeacherMapper.java
│   │   ├── ActivityMapper.java
│   │   ├── PrizeMapper.java
│   │   ├── ShareRecordMapper.java
│   │   ├── LotteryRecordMapper.java
│   │   ├── ExchangeItemMapper.java
│   │   └── ExchangeRecordMapper.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Teacher.java
│   │   ├── Activity.java
│   │   ├── Prize.java
│   │   ├── ShareRecord.java
│   │   ├── LotteryRecord.java
│   │   ├── ExchangeItem.java
│   │   └── ExchangeRecord.java
│   ├── dto/
│   │   ├── Result.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── ShareRequest.java
│   │   └── LotteryResponse.java
│   └── util/
│       ├── JwtUtil.java
│       └── CodeGenerator.java
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── src/test/java/com/shuran/art/
    └── service/
        ├── LotteryServiceTest.java
        └── ShareServiceTest.java
```

### 前端 frontend/
```
frontend/
├── app.js
├── app.json
├── app.wxss
├── project.config.json
├── sitemap.json
├── api/
│   ├── request.js
│   ├── user.js
│   ├── activity.js
│   └── lottery.js
├── utils/
│   ├── auth.js
│   └── util.js
├── components/
│   ├── lottery-grid/
│   └── prize-popup/
└── pages/
    ├── index/
    ├── teacher/list/
    ├── activity/list/
    ├── activity/detail/
    ├── lottery/index/
    └── user/index/
```

### 部署配置
```
shuranArt/
├── docker-compose.yml
├── nginx/
│   └── nginx.conf
└── mysql/
    └── init.sql
```

---

## Chunk 1: 项目初始化

### Task 1.1: 初始化 Git 仓库

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: 初始化 Git**

```bash
cd /Users/fengzhongjincao/Documents/aidemo/shuranArt
git init
```

- [ ] **Step 2: 创建 .gitignore**

```gitignore
# IDE
.idea/
*.iml
.vscode/
.DS_Store

# Java
target/
*.class
*.jar
*.log

# Node
node_modules/

# 小程序
miniprogram_npm/

# 环境配置
*.local
.env
application-prod.yml
```

- [ ] **Step 3: 首次提交**

```bash
git add .
git commit -m "chore: init project"
```

---

### Task 1.2: 创建后端 Spring Boot 项目骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/shuran/art/ArtApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.shuran</groupId>
    <artifactId>art-backend</artifactId>
    <version>1.0.0</version>
    <name>art-backend</name>
    <description>画室小程序后端</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jjwt.version>0.12.3</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

```java
// backend/src/main/java/com/shuran/art/ArtApplication.java
package com.shuran.art;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArtApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArtApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
# backend/src/main/resources/application.yml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/shuran_art?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 微信小程序配置
wx:
  appid: ${WX_APPID:your_appid}
  secret: ${WX_SECRET:your_secret}

# JWT配置
jwt:
  secret: ${JWT_SECRET:shuran-art-jwt-secret-key-2026}
  expiration: 604800000  # 7天
```

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: init spring boot project skeleton"
```

---

### Task 1.3: 创建通用响应类和配置

**Files:**
- Create: `backend/src/main/java/com/shuran/art/dto/Result.java`
- Create: `backend/src/main/java/com/shuran/art/config/CorsConfig.java`

- [ ] **Step 1: 创建统一响应类**

```java
// backend/src/main/java/com/shuran/art/dto/Result.java
package com.shuran.art.dto;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(-1);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
```

- [ ] **Step 2: 创建 CORS 配置**

```java
// backend/src/main/java/com/shuran/art/config/CorsConfig.java
package com.shuran.art.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/shuran/art/dto/
git add backend/src/main/java/com/shuran/art/config/
git commit -m "feat: add Result class and CORS config"
```

---

### Task 1.4: 创建数据库初始化脚本

**Files:**
- Create: `mysql/init.sql`

- [ ] **Step 1: 创建 SQL 初始化脚本**

```sql
-- mysql/init.sql
CREATE DATABASE IF NOT EXISTS shuran_art DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shuran_art;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) UNIQUE NOT NULL,
    `nick_name` VARCHAR(64),
    `avatar_url` VARCHAR(512),
    `phone` VARCHAR(20),
    `points` INT DEFAULT 0,
    `lottery_chances` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 老师表
CREATE TABLE IF NOT EXISTS `teacher` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,
    `title` VARCHAR(32),
    `intro` TEXT,
    `avatar` VARCHAR(512),
    `works` JSON,
    `sort_order` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `cover_img` VARCHAR(512),
    `start_time` DATETIME,
    `end_time` DATETIME,
    `daily_share_limit` INT DEFAULT 5,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 奖品表
CREATE TABLE IF NOT EXISTS `prize` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `value` INT DEFAULT 0,
    `probability` INT DEFAULT 0,
    `stock` INT DEFAULT -1,
    `icon` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分享记录表
CREATE TABLE IF NOT EXISTS `share_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sharer_id` BIGINT NOT NULL,
    `visitor_id` BIGINT NOT NULL,
    `activity_id` BIGINT NOT NULL,
    `lottery_granted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_share` (`sharer_id`, `visitor_id`, `activity_id`),
    INDEX `idx_sharer` (`sharer_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `prize_id` BIGINT NOT NULL,
    `prize_name` VARCHAR(64),
    `prize_type` VARCHAR(20),
    `prize_value` INT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `claim_code` VARCHAR(16),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` DATETIME,
    `expire_at` DATETIME,
    INDEX `idx_user` (`user_id`, `created_at`),
    INDEX `idx_claim_code` (`claim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换商品表
CREATE TABLE IF NOT EXISTS `exchange_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `points_cost` INT NOT NULL,
    `stock` INT DEFAULT 0,
    `description` VARCHAR(256),
    `image` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 1,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换记录表
CREATE TABLE IF NOT EXISTS `exchange_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `item_name` VARCHAR(64),
    `points_cost` INT,
    `claim_code` VARCHAR(16),
    `status` VARCHAR(20) DEFAULT 'pending',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` DATETIME,
    `expire_at` DATETIME,
    INDEX `idx_user` (`user_id`, `created_at`),
    INDEX `idx_claim_code` (`claim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认奖品数据
INSERT INTO `prize` (`name`, `type`, `value`, `probability`, `stock`, `icon`, `need_claim`) VALUES
('5积分', 'points', 5, 40, -1, '/images/prize-5.png', 0),
('20积分', 'points', 20, 25, -1, '/images/prize-20.png', 0),
('50积分', 'points', 50, 15, -1, '/images/prize-50.png', 0),
('100积分', 'points', 100, 5, -1, '/images/prize-100.png', 0),
('体验课', 'experience', 1, 10, 50, '/images/prize-exp.png', 1),
('画材礼包', 'gift', 1, 5, 20, '/images/prize-gift.png', 1);

-- 插入测试活动
INSERT INTO `activity` (`title`, `description`, `cover_img`, `start_time`, `end_time`, `daily_share_limit`) VALUES
('暑期班报名优惠', '分享活动，抽取丰厚奖品！', '/images/activity-summer.jpg', '2026-06-01 00:00:00', '2026-08-31 23:59:59', 5);
```

- [ ] **Step 2: 提交**

```bash
git add mysql/
git commit -m "feat: add database init script"
```

---

### Task 1.5: 创建 Docker 配置

**Files:**
- Create: `backend/Dockerfile`
- Create: `docker-compose.yml`
- Create: `nginx/nginx.conf`

- [ ] **Step 1: 创建后端 Dockerfile**

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/art-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 创建 docker-compose.yml**

```yaml
# docker-compose.yml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
    restart: always

  backend:
    build: ./backend
    expose:
      - "8080"
    environment:
      - DB_HOST=mysql
      - DB_USER=root
      - DB_PASSWORD=${DB_PASSWORD:-shuran123}
      - WX_APPID=${WX_APPID}
      - WX_SECRET=${WX_SECRET}
      - JWT_SECRET=${JWT_SECRET:-shuran-art-jwt-secret}
    depends_on:
      mysql:
        condition: service_healthy
    restart: always

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD:-shuran123}
      - MYSQL_DATABASE=shuran_art
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: always

volumes:
  mysql_data:
```

- [ ] **Step 3: 创建 nginx.conf**

```nginx
# nginx/nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        server_name _;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/Dockerfile docker-compose.yml nginx/
git commit -m "feat: add Docker deployment config"
```

---

## Chunk 2: 后端实体和基础 API

### Task 2.1: 创建实体类

**Files:**
- Create: `backend/src/main/java/com/shuran/art/entity/User.java`
- Create: `backend/src/main/java/com/shuran/art/entity/Teacher.java`
- Create: `backend/src/main/java/com/shuran/art/entity/Activity.java`
- Create: `backend/src/main/java/com/shuran/art/entity/Prize.java`

- [ ] **Step 1: 创建 User 实体**

```java
// backend/src/main/java/com/shuran/art/entity/User.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickName;
    private String avatarUrl;
    private String phone;
    private Integer points;
    private Integer lotteryChances;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 Teacher 实体**

```java
// backend/src/main/java/com/shuran/art/entity/Teacher.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "teacher", autoResultMap = true)
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String title;
    private String intro;
    private String avatar;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> works;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Activity 实体**

```java
// backend/src/main/java/com/shuran/art/entity/Activity.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer dailyShareLimit;
    private Integer status;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 Prize 实体**

```java
// backend/src/main/java/com/shuran/art/entity/Prize.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prize")
public class Prize {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer value;
    private Integer probability;
    private Integer stock;
    private String icon;
    private Integer needClaim;
    private Integer status;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/shuran/art/entity/
git commit -m "feat: add entity classes"
```

---

### Task 2.2: 创建分享和抽奖相关实体

**Files:**
- Create: `backend/src/main/java/com/shuran/art/entity/ShareRecord.java`
- Create: `backend/src/main/java/com/shuran/art/entity/LotteryRecord.java`
- Create: `backend/src/main/java/com/shuran/art/entity/ExchangeItem.java`
- Create: `backend/src/main/java/com/shuran/art/entity/ExchangeRecord.java`

- [ ] **Step 1: 创建 ShareRecord 实体**

```java
// backend/src/main/java/com/shuran/art/entity/ShareRecord.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share_record")
public class ShareRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sharerId;
    private Long visitorId;
    private Long activityId;
    private Integer lotteryGranted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 LotteryRecord 实体**

```java
// backend/src/main/java/com/shuran/art/entity/LotteryRecord.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("lottery_record")
public class LotteryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer prizeValue;
    private String status;
    private String claimCode;
    private LocalDateTime createdAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expireAt;
}
```

- [ ] **Step 3: 创建 ExchangeItem 实体**

```java
// backend/src/main/java/com/shuran/art/entity/ExchangeItem.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exchange_item")
public class ExchangeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer pointsCost;
    private Integer stock;
    private String description;
    private String image;
    private Integer needClaim;
    private Integer status;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 ExchangeRecord 实体**

```java
// backend/src/main/java/com/shuran/art/entity/ExchangeRecord.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exchange_record")
public class ExchangeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private Integer pointsCost;
    private String claimCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expireAt;
}
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/shuran/art/entity/
git commit -m "feat: add share and lottery entities"
```

---

### Task 2.3: 创建 Mapper 接口

**Files:**
- Create: `backend/src/main/java/com/shuran/art/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/TeacherMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/ActivityMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/PrizeMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/ShareRecordMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/LotteryRecordMapper.java`

- [ ] **Step 1: 创建所有 Mapper**

```java
// backend/src/main/java/com/shuran/art/mapper/UserMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/TeacherMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/ActivityMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/PrizeMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.Prize;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrizeMapper extends BaseMapper<Prize> {
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/ShareRecordMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.ShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;

@Mapper
public interface ShareRecordMapper extends BaseMapper<ShareRecord> {
    @Select("SELECT COUNT(*) FROM share_record WHERE sharer_id = #{sharerId} AND created_at >= #{startTime}")
    int countTodayShares(@Param("sharerId") Long sharerId, @Param("startTime") LocalDateTime startTime);
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/LotteryRecordMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.LotteryRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LotteryRecordMapper extends BaseMapper<LotteryRecord> {
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/shuran/art/mapper/
git commit -m "feat: add mapper interfaces"
```

---

---

## Chunk 3: 后端核心业务逻辑

### Task 3.1: 创建 JWT 工具类和用户服务

**Files:**
- Create: `backend/src/main/java/com/shuran/art/util/JwtUtil.java`
- Create: `backend/src/main/java/com/shuran/art/service/UserService.java`
- Create: `backend/src/main/java/com/shuran/art/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/shuran/art/dto/LoginResponse.java`

- [ ] **Step 1: 创建 JwtUtil**

```java
// backend/src/main/java/com/shuran/art/util/JwtUtil.java
package com.shuran.art.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: 创建登录 DTO**

```java
// backend/src/main/java/com/shuran/art/dto/LoginRequest.java
package com.shuran.art.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "code不能为空")
    private String code;
}
```

```java
// backend/src/main/java/com/shuran/art/dto/LoginResponse.java
package com.shuran.art.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Integer points;
    private Integer lotteryChances;
}
```

- [ ] **Step 3: 创建 UserService**

```java
// backend/src/main/java/com/shuran/art/service/UserService.java
package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.LoginRequest;
import com.shuran.art.dto.LoginResponse;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.UserMapper;
import com.shuran.art.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    public LoginResponse login(LoginRequest request) {
        // 调用微信接口获取 openid
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, request.getCode()
        );

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> wxResult = restTemplate.getForObject(url, Map.class);

        if (wxResult == null || wxResult.containsKey("errcode")) {
            throw new RuntimeException("微信登录失败");
        }

        String openid = (String) wxResult.get("openid");

        // 查找或创建用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setPoints(0);
            user.setLotteryChances(0);
            userMapper.insert(user);
        }

        // 生成 token
        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickName(user.getNickName())
                .avatarUrl(user.getAvatarUrl())
                .points(user.getPoints())
                .lotteryChances(user.getLotteryChances())
                .build();
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/shuran/art/util/
git add backend/src/main/java/com/shuran/art/dto/
git add backend/src/main/java/com/shuran/art/service/UserService.java
git commit -m "feat: add JWT util and UserService"
```

---

### Task 3.2: 创建抽奖服务

**Files:**
- Create: `backend/src/main/java/com/shuran/art/util/CodeGenerator.java`
- Create: `backend/src/main/java/com/shuran/art/service/LotteryService.java`
- Create: `backend/src/main/java/com/shuran/art/dto/LotteryResponse.java`

- [ ] **Step 1: 创建领取码生成器**

```java
// backend/src/main/java/com/shuran/art/util/CodeGenerator.java
package com.shuran.art.util;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String generateClaimCode() {
        return generate(8);
    }
}
```

- [ ] **Step 2: 创建 LotteryResponse**

```java
// backend/src/main/java/com/shuran/art/dto/LotteryResponse.java
package com.shuran.art.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotteryResponse {
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer prizeValue;
    private String icon;
    private Boolean needClaim;
    private String claimCode;
}
```

- [ ] **Step 3: 创建 LotteryService**

```java
// backend/src/main/java/com/shuran/art/service/LotteryService.java
package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.LotteryRecordMapper;
import com.shuran.art.mapper.PrizeMapper;
import com.shuran.art.mapper.UserMapper;
import com.shuran.art.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final UserMapper userMapper;
    private final PrizeMapper prizeMapper;
    private final LotteryRecordMapper lotteryRecordMapper;
    private final Random random = new Random();

    @Transactional
    public LotteryResponse draw(Long userId) {
        // 1. 检查抽奖机会
        User user = userMapper.selectById(userId);
        if (user == null || user.getLotteryChances() <= 0) {
            throw new RuntimeException("没有抽奖机会");
        }

        // 2. 扣减抽奖机会
        user.setLotteryChances(user.getLotteryChances() - 1);
        userMapper.updateById(user);

        // 3. 获取奖品池
        List<Prize> prizes = prizeMapper.selectList(
            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );

        // 4. 按概率抽奖
        Prize selectedPrize = selectPrize(prizes);

        // 5. 检查并扣减库存
        if (selectedPrize.getStock() != -1) {
            if (selectedPrize.getStock() <= 0) {
                // 库存不足，降级到积分奖品
                selectedPrize = prizes.stream()
                    .filter(p -> "points".equals(p.getType()))
                    .findFirst()
                    .orElse(prizes.get(0));
            } else {
                selectedPrize.setStock(selectedPrize.getStock() - 1);
                prizeMapper.updateById(selectedPrize);
            }
        }

        // 6. 创建抽奖记录
        LotteryRecord record = new LotteryRecord();
        record.setUserId(userId);
        record.setPrizeId(selectedPrize.getId());
        record.setPrizeName(selectedPrize.getName());
        record.setPrizeType(selectedPrize.getType());
        record.setPrizeValue(selectedPrize.getValue());

        boolean needClaim = selectedPrize.getNeedClaim() == 1;
        if (needClaim) {
            record.setStatus("pending");
            record.setClaimCode(CodeGenerator.generateClaimCode());
            record.setExpireAt(LocalDateTime.now().plusDays(30));
        } else {
            record.setStatus("claimed");
            record.setClaimedAt(LocalDateTime.now());
        }

        lotteryRecordMapper.insert(record);

        // 7. 如果是积分，直接发放
        if ("points".equals(selectedPrize.getType())) {
            user.setPoints(user.getPoints() + selectedPrize.getValue());
            userMapper.updateById(user);
        }

        return LotteryResponse.builder()
                .prizeId(selectedPrize.getId())
                .prizeName(selectedPrize.getName())
                .prizeType(selectedPrize.getType())
                .prizeValue(selectedPrize.getValue())
                .icon(selectedPrize.getIcon())
                .needClaim(needClaim)
                .claimCode(record.getClaimCode())
                .build();
    }

    private Prize selectPrize(List<Prize> prizes) {
        int randomValue = random.nextInt(100);
        int cumulative = 0;

        for (Prize prize : prizes) {
            cumulative += prize.getProbability();
            if (randomValue < cumulative) {
                return prize;
            }
        }
        return prizes.get(0);
    }

    public List<Prize> getPrizes() {
        return prizeMapper.selectList(
            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );
    }

    public List<LotteryRecord> getUserRecords(Long userId) {
        return lotteryRecordMapper.selectList(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .orderByDesc(LotteryRecord::getCreatedAt)
        );
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/shuran/art/
git commit -m "feat: add LotteryService with probability draw"
```

---

### Task 3.3: 创建分享服务

**Files:**
- Create: `backend/src/main/java/com/shuran/art/service/ShareService.java`
- Create: `backend/src/main/java/com/shuran/art/dto/ShareRequest.java`

- [ ] **Step 1: 创建 ShareRequest**

```java
// backend/src/main/java/com/shuran/art/dto/ShareRequest.java
package com.shuran.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareRequest {
    @NotNull
    private Long sharerId;
    @NotNull
    private Long activityId;
}
```

- [ ] **Step 2: 创建 ShareService**

```java
// backend/src/main/java/com/shuran/art/service/ShareService.java
package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.ShareRecord;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.ShareRecordMapper;
import com.shuran.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordMapper shareRecordMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

    @Transactional
    public Map<String, Object> recordShare(Long visitorId, ShareRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long sharerId = request.getSharerId();
        Long activityId = request.getActivityId();

        // 1. 不能给自己加抽奖机会
        if (sharerId.equals(visitorId)) {
            result.put("success", false);
            result.put("msg", "不能给自己助力");
            return result;
        }

        // 2. 检查是否已记录过
        ShareRecord existing = shareRecordMapper.selectOne(
            new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getSharerId, sharerId)
                .eq(ShareRecord::getVisitorId, visitorId)
                .eq(ShareRecord::getActivityId, activityId)
        );

        if (existing != null) {
            result.put("success", false);
            result.put("msg", "已助力过");
            return result;
        }

        // 3. 检查今日是否达到上限
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int todayCount = shareRecordMapper.countTodayShares(sharerId, todayStart);

        Activity activity = activityMapper.selectById(activityId);
        int dailyLimit = activity != null ? activity.getDailyShareLimit() : 5;

        ShareRecord record = new ShareRecord();
        record.setSharerId(sharerId);
        record.setVisitorId(visitorId);
        record.setActivityId(activityId);

        if (todayCount >= dailyLimit) {
            record.setLotteryGranted(0);
            shareRecordMapper.insert(record);
            result.put("success", true);
            result.put("msg", "今日抽奖机会已达上限");
            result.put("lotteryAdded", false);
            return result;
        }

        // 4. 发放抽奖机会
        record.setLotteryGranted(1);
        shareRecordMapper.insert(record);

        User sharer = userMapper.selectById(sharerId);
        sharer.setLotteryChances(sharer.getLotteryChances() + 1);
        userMapper.updateById(sharer);

        result.put("success", true);
        result.put("msg", "获得1次抽奖机会");
        result.put("lotteryAdded", true);
        return result;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/shuran/art/
git commit -m "feat: add ShareService with daily limit"
```

---

### Task 3.4: 创建 Controller 层

**Files:**
- Create: `backend/src/main/java/com/shuran/art/controller/UserController.java`
- Create: `backend/src/main/java/com/shuran/art/controller/TeacherController.java`
- Create: `backend/src/main/java/com/shuran/art/controller/ActivityController.java`
- Create: `backend/src/main/java/com/shuran/art/controller/LotteryController.java`
- Create: `backend/src/main/java/com/shuran/art/controller/ShareController.java`

- [ ] **Step 1: 创建 UserController**

```java
// backend/src/main/java/com/shuran/art/controller/UserController.java
package com.shuran.art.controller;

import com.shuran.art.dto.LoginRequest;
import com.shuran.art.dto.LoginResponse;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.User;
import com.shuran.art.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestAttribute Long userId) {
        return Result.success(userService.getUserById(userId));
    }
}
```

- [ ] **Step 2: 创建 TeacherController**

```java
// backend/src/main/java/com/shuran/art/controller/TeacherController.java
package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Teacher;
import com.shuran.art.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherMapper teacherMapper;

    @GetMapping("/list")
    public Result<List<Teacher>> list() {
        List<Teacher> teachers = teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>().orderByAsc(Teacher::getSortOrder)
        );
        return Result.success(teachers);
    }
}
```

- [ ] **Step 3: 创建 ActivityController**

```java
// backend/src/main/java/com/shuran/art/controller/ActivityController.java
package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Activity;
import com.shuran.art.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityMapper activityMapper;

    @GetMapping("/list")
    public Result<List<Activity>> list() {
        List<Activity> activities = activityMapper.selectList(
            new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 1)
                .orderByDesc(Activity::getCreatedAt)
        );
        return Result.success(activities);
    }

    @GetMapping("/{id}")
    public Result<Activity> detail(@PathVariable Long id) {
        return Result.success(activityMapper.selectById(id));
    }
}
```

- [ ] **Step 4: 创建 LotteryController**

```java
// backend/src/main/java/com/shuran/art/controller/LotteryController.java
package com.shuran.art.controller;

import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.service.LotteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    @PostMapping("/draw")
    public Result<LotteryResponse> draw(@RequestAttribute Long userId) {
        return Result.success(lotteryService.draw(userId));
    }

    @GetMapping("/prizes")
    public Result<List<Prize>> prizes() {
        return Result.success(lotteryService.getPrizes());
    }

    @GetMapping("/records")
    public Result<List<LotteryRecord>> records(@RequestAttribute Long userId) {
        return Result.success(lotteryService.getUserRecords(userId));
    }
}
```

- [ ] **Step 5: 创建 ShareController**

```java
// backend/src/main/java/com/shuran/art/controller/ShareController.java
package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/record")
    public Result<Map<String, Object>> record(
            @RequestAttribute Long userId,
            @Valid @RequestBody ShareRequest request) {
        return Result.success(shareService.recordShare(userId, request));
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/shuran/art/controller/
git commit -m "feat: add REST controllers"
```

---

### Task 3.5: 创建认证拦截器

**Files:**
- Create: `backend/src/main/java/com/shuran/art/config/AuthInterceptor.java`
- Create: `backend/src/main/java/com/shuran/art/config/WebMvcConfig.java`

- [ ] **Step 1: 创建拦截器**

```java
// backend/src/main/java/com/shuran/art/config/AuthInterceptor.java
package com.shuran.art.config;

import com.shuran.art.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        token = token.substring(7);

        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
```

- [ ] **Step 2: 创建 WebMvcConfig**

```java
// backend/src/main/java/com/shuran/art/config/WebMvcConfig.java
package com.shuran.art.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/user/login",
                    "/teacher/**",
                    "/activity/**",
                    "/studio/**"
                );
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/shuran/art/config/
git commit -m "feat: add JWT auth interceptor"
```

---

## Chunk 4: 微信小程序前端

### Task 4.1: 创建小程序基础结构

**Files:**
- Create: `frontend/app.js`
- Create: `frontend/app.json`
- Create: `frontend/app.wxss`
- Create: `frontend/project.config.json`

- [ ] **Step 1: 创建 app.json**

```json
{
  "pages": [
    "pages/index/index",
    "pages/teacher/list/index",
    "pages/activity/list/index",
    "pages/activity/detail/index",
    "pages/lottery/index/index",
    "pages/user/index/index"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarBackgroundColor": "#FF6B6B",
    "navigationBarTitleText": "舒然画室",
    "navigationBarTextStyle": "white"
  },
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#FF6B6B",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "images/tab-home.png",
        "selectedIconPath": "images/tab-home-active.png"
      },
      {
        "pagePath": "pages/activity/list/index",
        "text": "活动",
        "iconPath": "images/tab-activity.png",
        "selectedIconPath": "images/tab-activity-active.png"
      },
      {
        "pagePath": "pages/user/index/index",
        "text": "我的",
        "iconPath": "images/tab-user.png",
        "selectedIconPath": "images/tab-user-active.png"
      }
    ]
  },
  "sitemapLocation": "sitemap.json"
}
```

- [ ] **Step 2: 创建 app.js**

```javascript
// frontend/app.js
App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'https://your-domain.com/api/v1'
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = wx.getStorageSync('userInfo');
    }
  },

  // 登录方法
  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            wx.request({
              url: `${this.globalData.baseUrl}/user/login`,
              method: 'POST',
              data: { code: res.code },
              success: (response) => {
                if (response.data.code === 0) {
                  const data = response.data.data;
                  this.globalData.token = data.token;
                  this.globalData.userInfo = data;
                  wx.setStorageSync('token', data.token);
                  wx.setStorageSync('userInfo', data);
                  resolve(data);
                } else {
                  reject(response.data.msg);
                }
              },
              fail: reject
            });
          } else {
            reject('登录失败');
          }
        },
        fail: reject
      });
    });
  }
});
```

- [ ] **Step 3: 创建 app.wxss**

```css
/* frontend/app.wxss */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
}

.container {
  padding: 30rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.btn-primary {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  color: #fff;
  border-radius: 40rpx;
  padding: 24rpx 48rpx;
  text-align: center;
  font-weight: bold;
}

.btn-primary:active {
  opacity: 0.9;
}
```

- [ ] **Step 4: 提交**

```bash
git add frontend/
git commit -m "feat: init miniprogram structure"
```

---

### Task 4.2: 创建 API 请求封装

**Files:**
- Create: `frontend/api/request.js`
- Create: `frontend/api/user.js`
- Create: `frontend/api/activity.js`
- Create: `frontend/api/lottery.js`

- [ ] **Step 1: 创建 request.js**

```javascript
// frontend/api/request.js
const app = getApp();

const request = (options) => {
  return new Promise((resolve, reject) => {
    const header = {
      'Content-Type': 'application/json'
    };

    if (app.globalData.token) {
      header['Authorization'] = `Bearer ${app.globalData.token}`;
    }

    wx.request({
      url: `${app.globalData.baseUrl}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        if (res.data.code === 0) {
          resolve(res.data.data);
        } else if (res.statusCode === 401) {
          // token 过期，重新登录
          app.login().then(() => {
            request(options).then(resolve).catch(reject);
          }).catch(reject);
        } else {
          wx.showToast({
            title: res.data.msg || '请求失败',
            icon: 'none'
          });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
};

module.exports = { request };
```

- [ ] **Step 2: 创建 API 模块**

```javascript
// frontend/api/user.js
const { request } = require('./request');

module.exports = {
  getUserInfo: () => request({ url: '/user/info' })
};
```

```javascript
// frontend/api/activity.js
const { request } = require('./request');

module.exports = {
  getList: () => request({ url: '/activity/list' }),
  getDetail: (id) => request({ url: `/activity/${id}` })
};
```

```javascript
// frontend/api/lottery.js
const { request } = require('./request');

module.exports = {
  draw: () => request({ url: '/lottery/draw', method: 'POST' }),
  getPrizes: () => request({ url: '/lottery/prizes' }),
  getRecords: () => request({ url: '/lottery/records' }),
  recordShare: (data) => request({ url: '/share/record', method: 'POST', data })
};
```

- [ ] **Step 3: 提交**

```bash
git add frontend/api/
git commit -m "feat: add API request modules"
```

---

### Task 4.3: 创建首页

**Files:**
- Create: `frontend/pages/index/index.js`
- Create: `frontend/pages/index/index.wxml`
- Create: `frontend/pages/index/index.wxss`
- Create: `frontend/pages/index/index.json`

- [ ] **Step 1: 创建首页文件**

```json
// frontend/pages/index/index.json
{
  "navigationBarTitleText": "舒然画室"
}
```

```javascript
// frontend/pages/index/index.js
const activityApi = require('../../api/activity');

Page({
  data: {
    studioInfo: {
      name: '舒然画室',
      slogan: '用艺术点亮生活',
      description: '专注美术教育10年，培养学员超过2000人',
      images: ['/images/studio-1.jpg', '/images/studio-2.jpg']
    },
    currentActivity: null
  },

  onLoad() {
    this.loadActivity();
  },

  async loadActivity() {
    try {
      const list = await activityApi.getList();
      if (list && list.length > 0) {
        this.setData({ currentActivity: list[0] });
      }
    } catch (e) {
      console.error(e);
    }
  },

  goToActivity() {
    if (this.data.currentActivity) {
      wx.navigateTo({
        url: `/pages/activity/detail/index?id=${this.data.currentActivity.id}`
      });
    }
  },

  goToTeachers() {
    wx.navigateTo({
      url: '/pages/teacher/list/index'
    });
  }
});
```

```html
<!-- frontend/pages/index/index.wxml -->
<view class="container">
  <!-- 画室介绍 -->
  <view class="card studio-card">
    <view class="studio-name">{{studioInfo.name}}</view>
    <view class="studio-slogan">{{studioInfo.slogan}}</view>
    <view class="studio-desc">{{studioInfo.description}}</view>
  </view>

  <!-- 当前活动入口 -->
  <view class="card activity-card" wx:if="{{currentActivity}}" bindtap="goToActivity">
    <image class="activity-cover" src="{{currentActivity.coverImg}}" mode="aspectFill"></image>
    <view class="activity-info">
      <view class="activity-title">{{currentActivity.title}}</view>
      <view class="activity-btn">立即参与 ></view>
    </view>
  </view>

  <!-- 老师入口 -->
  <view class="card menu-card" bindtap="goToTeachers">
    <text class="menu-icon">👨‍🏫</text>
    <text class="menu-text">师资力量</text>
    <text class="menu-arrow">></text>
  </view>
</view>
```

```css
/* frontend/pages/index/index.wxss */
.studio-card {
  text-align: center;
  padding: 60rpx 30rpx;
}

.studio-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #FF6B6B;
  margin-bottom: 16rpx;
}

.studio-slogan {
  font-size: 32rpx;
  color: #666;
  margin-bottom: 24rpx;
}

.studio-desc {
  font-size: 26rpx;
  color: #999;
}

.activity-card {
  padding: 0;
  overflow: hidden;
}

.activity-cover {
  width: 100%;
  height: 300rpx;
}

.activity-info {
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-title {
  font-size: 32rpx;
  font-weight: bold;
}

.activity-btn {
  color: #FF6B6B;
  font-size: 28rpx;
}

.menu-card {
  display: flex;
  align-items: center;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
}

.menu-arrow {
  color: #999;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/index/
git commit -m "feat: add index page"
```

---

### Task 4.4: 创建活动详情页（核心分享页）

**Files:**
- Create: `frontend/pages/activity/detail/index.js`
- Create: `frontend/pages/activity/detail/index.wxml`
- Create: `frontend/pages/activity/detail/index.wxss`
- Create: `frontend/pages/activity/detail/index.json`

- [ ] **Step 1: 创建活动详情页**

```json
// frontend/pages/activity/detail/index.json
{
  "navigationBarTitleText": "活动详情"
}
```

```javascript
// frontend/pages/activity/detail/index.js
const app = getApp();
const activityApi = require('../../../api/activity');
const lotteryApi = require('../../../api/lottery');

Page({
  data: {
    activity: null,
    shareFrom: null
  },

  onLoad(options) {
    const { id, shareFrom } = options;
    this.loadActivity(id);

    // 处理分享进入
    if (shareFrom && app.globalData.userInfo) {
      this.recordShare(shareFrom, id);
    }
    this.setData({ shareFrom });
  },

  async loadActivity(id) {
    try {
      const activity = await activityApi.getDetail(id);
      this.setData({ activity });
    } catch (e) {
      console.error(e);
    }
  },

  async recordShare(sharerId, activityId) {
    try {
      await app.login();
      await lotteryApi.recordShare({
        sharerId: parseInt(sharerId),
        activityId: parseInt(activityId)
      });
    } catch (e) {
      console.error(e);
    }
  },

  goToLottery() {
    wx.navigateTo({
      url: '/pages/lottery/index/index'
    });
  },

  // 分享给好友
  onShareAppMessage() {
    const { activity } = this.data;
    const userId = app.globalData.userInfo?.userId;
    return {
      title: `【分享抽奖】${activity.title}`,
      path: `/pages/activity/detail/index?id=${activity.id}&shareFrom=${userId}`,
      imageUrl: activity.coverImg
    };
  },

  // 分享到朋友圈
  onShareTimeline() {
    const { activity } = this.data;
    const userId = app.globalData.userInfo?.userId;
    return {
      title: activity.title,
      query: `id=${activity.id}&shareFrom=${userId}`,
      imageUrl: activity.coverImg
    };
  }
});
```

```html
<!-- frontend/pages/activity/detail/index.wxml -->
<view class="container" wx:if="{{activity}}">
  <image class="cover" src="{{activity.coverImg}}" mode="aspectFill"></image>

  <view class="card">
    <view class="title">{{activity.title}}</view>
    <view class="time">
      活动时间：{{activity.startTime}} - {{activity.endTime}}
    </view>
    <view class="desc">{{activity.description}}</view>
  </view>

  <view class="card rule-card">
    <view class="rule-title">🎁 分享抽奖规则</view>
    <view class="rule-item">1. 分享活动给好友</view>
    <view class="rule-item">2. 好友点击进入，你获得1次抽奖机会</view>
    <view class="rule-item">3. 每天最多获得{{activity.dailyShareLimit}}次机会</view>
    <view class="rule-item">4. 100%中奖，奖品丰厚！</view>
  </view>

  <view class="btn-group">
    <button class="btn-primary" bindtap="goToLottery">去抽奖</button>
    <button class="btn-share" open-type="share">分享给好友</button>
  </view>
</view>
```

```css
/* frontend/pages/activity/detail/index.wxss */
.cover {
  width: 100%;
  height: 400rpx;
  margin-bottom: 20rpx;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.time {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 20rpx;
}

.desc {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.rule-card {
  background: linear-gradient(135deg, #FFF5F5, #FFF);
}

.rule-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  color: #FF6B6B;
}

.rule-item {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.btn-group {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  background: #fff;
  display: flex;
  gap: 20rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.btn-group .btn-primary {
  flex: 1;
}

.btn-share {
  flex: 1;
  background: #fff;
  color: #FF6B6B;
  border: 2rpx solid #FF6B6B;
  border-radius: 40rpx;
  font-size: 28rpx;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/activity/
git commit -m "feat: add activity detail page with share"
```

---

### Task 4.5: 创建九宫格抽奖页

**Files:**
- Create: `frontend/pages/lottery/index/index.js`
- Create: `frontend/pages/lottery/index/index.wxml`
- Create: `frontend/pages/lottery/index/index.wxss`
- Create: `frontend/pages/lottery/index/index.json`

- [ ] **Step 1: 创建抽奖页**

```json
// frontend/pages/lottery/index/index.json
{
  "navigationBarTitleText": "幸运抽奖"
}
```

```javascript
// frontend/pages/lottery/index/index.js
const app = getApp();
const lotteryApi = require('../../../api/lottery');
const userApi = require('../../../api/user');

Page({
  data: {
    prizes: [],
    userInfo: null,
    isDrawing: false,
    currentIndex: -1,
    result: null
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadUserInfo();
  },

  async loadData() {
    try {
      const prizes = await lotteryApi.getPrizes();
      // 构建九宫格（8个奖品 + 1个抽奖按钮）
      const grid = this.buildGrid(prizes);
      this.setData({ prizes: grid });
    } catch (e) {
      console.error(e);
    }
  },

  async loadUserInfo() {
    try {
      if (!app.globalData.token) {
        await app.login();
      }
      const userInfo = await userApi.getUserInfo();
      this.setData({ userInfo });
    } catch (e) {
      console.error(e);
    }
  },

  buildGrid(prizes) {
    // 九宫格顺序：0,1,2,3,4,5,6,7 围绕中心
    const grid = [];
    for (let i = 0; i < 8 && i < prizes.length; i++) {
      grid.push({ ...prizes[i], index: i });
    }
    // 填充空位
    while (grid.length < 8) {
      grid.push({ name: '谢谢参与', type: 'empty', index: grid.length });
    }
    // 中间插入抽奖按钮
    grid.splice(4, 0, { type: 'button', index: 4 });
    return grid;
  },

  async startDraw() {
    if (this.data.isDrawing) return;

    if (!this.data.userInfo || this.data.userInfo.lotteryChances <= 0) {
      wx.showToast({ title: '没有抽奖机会', icon: 'none' });
      return;
    }

    this.setData({ isDrawing: true, result: null });

    try {
      // 开始动画
      const result = await lotteryApi.draw();

      // 找到中奖奖品的位置
      let targetIndex = this.data.prizes.findIndex(
        p => p.id === result.prizeId
      );
      if (targetIndex === -1 || targetIndex === 4) targetIndex = 0;

      // 执行转动动画
      await this.runAnimation(targetIndex);

      // 显示结果
      this.setData({ result });
      this.loadUserInfo();
    } catch (e) {
      wx.showToast({ title: e.msg || '抽奖失败', icon: 'none' });
    } finally {
      this.setData({ isDrawing: false });
    }
  },

  runAnimation(targetIndex) {
    return new Promise((resolve) => {
      const sequence = [0, 1, 2, 5, 8, 7, 6, 3]; // 顺时针顺序
      let rounds = 3;
      let totalSteps = rounds * 8 + sequence.indexOf(targetIndex);
      let step = 0;
      let speed = 100;

      const animate = () => {
        const gridIndex = sequence[step % 8];
        this.setData({ currentIndex: gridIndex });

        step++;
        if (step >= totalSteps) {
          setTimeout(resolve, 500);
          return;
        }

        // 逐渐减速
        if (step > totalSteps - 8) {
          speed = Math.min(speed + 30, 300);
        }

        setTimeout(animate, speed);
      };

      animate();
    });
  },

  closeResult() {
    this.setData({ result: null, currentIndex: -1 });
  },

  onShareAppMessage() {
    return {
      title: '我在舒然画室抽到了好礼，你也来试试！',
      path: '/pages/activity/list/index'
    };
  }
});
```

```html
<!-- frontend/pages/lottery/index/index.wxml -->
<view class="container">
  <view class="header">
    <view class="chances">
      剩余抽奖次数：<text class="num">{{userInfo.lotteryChances || 0}}</text>
    </view>
    <view class="points">
      我的积分：<text class="num">{{userInfo.points || 0}}</text>
    </view>
  </view>

  <view class="lottery-grid">
    <view
      wx:for="{{prizes}}"
      wx:key="index"
      class="grid-item {{item.type === 'button' ? 'draw-btn' : ''}} {{currentIndex === index ? 'active' : ''}}"
      bindtap="{{item.type === 'button' ? 'startDraw' : ''}}"
    >
      <block wx:if="{{item.type === 'button'}}">
        <view class="btn-text">{{isDrawing ? '抽奖中' : '开始抽奖'}}</view>
      </block>
      <block wx:else>
        <image wx:if="{{item.icon}}" class="prize-icon" src="{{item.icon}}"></image>
        <view class="prize-name">{{item.name}}</view>
      </block>
    </view>
  </view>

  <view class="tip">分享活动给好友，好友点击即可获得抽奖机会</view>
  <button class="btn-share" open-type="share">分享获取更多机会</button>
</view>

<!-- 中奖弹窗 -->
<view class="popup-mask" wx:if="{{result}}" bindtap="closeResult">
  <view class="popup-content" catchtap="">
    <view class="popup-title">🎉 恭喜中奖</view>
    <image wx:if="{{result.icon}}" class="popup-icon" src="{{result.icon}}"></image>
    <view class="popup-prize">{{result.prizeName}}</view>
    <view class="popup-code" wx:if="{{result.claimCode}}">
      领取码：{{result.claimCode}}
    </view>
    <view class="popup-tip" wx:if="{{result.needClaim}}">
      请到店出示领取码领取奖品
    </view>
    <view class="popup-tip" wx:else>
      积分已自动发放到账户
    </view>
    <view class="popup-btn" bindtap="closeResult">知道了</view>
  </view>
</view>
```

```css
/* frontend/pages/lottery/index/index.wxss */
.header {
  display: flex;
  justify-content: space-between;
  padding: 30rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  color: #fff;
  border-radius: 16rpx;
  margin-bottom: 30rpx;
}

.num {
  font-size: 36rpx;
  font-weight: bold;
}

.lottery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  padding: 20rpx;
  background: #fff;
  border-radius: 16rpx;
}

.grid-item {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #FFF5F5;
  border-radius: 12rpx;
  transition: all 0.1s;
}

.grid-item.active {
  background: #FF6B6B;
  transform: scale(1.05);
}

.grid-item.active .prize-name {
  color: #fff;
}

.draw-btn {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
}

.btn-text {
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
}

.prize-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 10rpx;
}

.prize-name {
  font-size: 24rpx;
  color: #666;
}

.tip {
  text-align: center;
  color: #999;
  font-size: 24rpx;
  margin: 30rpx 0;
}

.btn-share {
  background: #fff;
  color: #FF6B6B;
  border: 2rpx solid #FF6B6B;
  border-radius: 40rpx;
  font-size: 28rpx;
}

/* 弹窗 */
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.popup-content {
  width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 60rpx 40rpx;
  text-align: center;
}

.popup-title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 30rpx;
}

.popup-icon {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 20rpx;
}

.popup-prize {
  font-size: 36rpx;
  font-weight: bold;
  color: #FF6B6B;
  margin-bottom: 20rpx;
}

.popup-code {
  font-size: 32rpx;
  color: #333;
  background: #FFF5F5;
  padding: 16rpx 24rpx;
  border-radius: 8rpx;
  margin-bottom: 16rpx;
}

.popup-tip {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 30rpx;
}

.popup-btn {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  color: #fff;
  padding: 20rpx 60rpx;
  border-radius: 40rpx;
  display: inline-block;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/lottery/
git commit -m "feat: add lottery page with animation"
```

---

### Task 4.6: 创建个人中心页

**Files:**
- Create: `frontend/pages/user/index/index.js`
- Create: `frontend/pages/user/index/index.wxml`
- Create: `frontend/pages/user/index/index.wxss`
- Create: `frontend/pages/user/index/index.json`

- [ ] **Step 1: 创建个人中心**

```json
// frontend/pages/user/index/index.json
{
  "navigationBarTitleText": "我的"
}
```

```javascript
// frontend/pages/user/index/index.js
const app = getApp();
const userApi = require('../../../api/user');
const lotteryApi = require('../../../api/lottery');

Page({
  data: {
    userInfo: null,
    prizes: [],
    logged: false
  },

  onShow() {
    this.loadData();
  },

  async loadData() {
    try {
      if (!app.globalData.token) {
        this.setData({ logged: false });
        return;
      }

      const userInfo = await userApi.getUserInfo();
      const prizes = await lotteryApi.getRecords();

      this.setData({
        userInfo,
        prizes: prizes.filter(p => p.status === 'pending'),
        logged: true
      });
    } catch (e) {
      console.error(e);
    }
  },

  async doLogin() {
    try {
      await app.login();
      this.loadData();
    } catch (e) {
      wx.showToast({ title: '登录失败', icon: 'none' });
    }
  },

  goToLottery() {
    wx.navigateTo({ url: '/pages/lottery/index/index' });
  }
});
```

```html
<!-- frontend/pages/user/index/index.wxml -->
<view class="container">
  <!-- 未登录 -->
  <view class="login-card card" wx:if="{{!logged}}">
    <view class="login-tip">登录后查看抽奖记录和奖品</view>
    <button class="btn-primary" bindtap="doLogin">微信登录</button>
  </view>

  <!-- 已登录 -->
  <block wx:else>
    <view class="user-header">
      <image class="avatar" src="{{userInfo.avatarUrl || '/images/default-avatar.png'}}"></image>
      <view class="nickname">{{userInfo.nickName || '用户'}}</view>
    </view>

    <view class="stats card">
      <view class="stat-item" bindtap="goToLottery">
        <view class="stat-num">{{userInfo.lotteryChances || 0}}</view>
        <view class="stat-label">抽奖机会</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">{{userInfo.points || 0}}</view>
        <view class="stat-label">积分</view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">🎁 待领取奖品</view>
      <view class="prize-list" wx:if="{{prizes.length > 0}}">
        <view class="prize-item card" wx:for="{{prizes}}" wx:key="id">
          <view class="prize-info">
            <view class="prize-name">{{item.prizeName}}</view>
            <view class="prize-code">领取码：{{item.claimCode}}</view>
            <view class="prize-expire">{{item.expireAt}}前领取</view>
          </view>
          <view class="prize-status pending">待领取</view>
        </view>
      </view>
      <view class="empty" wx:else>
        <text>暂无待领取奖品</text>
      </view>
    </view>
  </block>
</view>
```

```css
/* frontend/pages/user/index/index.wxss */
.login-card {
  text-align: center;
  padding: 60rpx;
}

.login-tip {
  color: #999;
  margin-bottom: 30rpx;
}

.user-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid #fff;
  margin-bottom: 16rpx;
}

.nickname {
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
}

.stats {
  display: flex;
}

.stat-item {
  flex: 1;
  text-align: center;
  padding: 30rpx 0;
}

.stat-num {
  font-size: 48rpx;
  font-weight: bold;
  color: #FF6B6B;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.section {
  margin-top: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.prize-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.prize-name {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.prize-code {
  font-size: 26rpx;
  color: #FF6B6B;
}

.prize-expire {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.prize-status.pending {
  color: #FF6B6B;
  font-size: 26rpx;
}

.empty {
  text-align: center;
  color: #999;
  padding: 60rpx;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/user/
git commit -m "feat: add user center page"
```

---

## Chunk 5: 剩余页面和最终完善

### Task 5.1: 创建老师列表页和活动列表页

略，结构类似，按需实现

### Task 5.2: 测试和部署

- [ ] **Step 1: 本地测试后端**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- [ ] **Step 2: 构建并部署**

```bash
cd backend
mvn clean package -DskipTests
docker-compose up -d --build
```

---

## Chunk 6: 后台配置管理功能

### Task 6.1: 数据库扩展（管理员和配置表）

**Files:**
- Modify: `mysql/init.sql`

- [ ] **Step 1: 添加管理员白名单表**

```sql
-- 管理员白名单表（根据微信openid控制权限）
CREATE TABLE IF NOT EXISTS `admin_whitelist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) UNIQUE NOT NULL,
    `name` VARCHAR(32),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 画室配置表（存储画室介绍信息）
CREATE TABLE IF NOT EXISTS `studio_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(64) UNIQUE NOT NULL,
    `config_value` TEXT,
    `config_type` VARCHAR(20) DEFAULT 'text',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认画室配置
INSERT INTO `studio_config` (`config_key`, `config_value`, `config_type`) VALUES
('studio_name', '舒然画室', 'text'),
('studio_slogan', '用艺术点亮生活', 'text'),
('studio_description', '专注美术教育10年，培养学员超过2000人', 'text'),
('studio_images', '[]', 'json'),
('studio_video', '', 'text'),
('share_title_template', '【分享抽奖】{activity_title}', 'text'),
('share_desc_template', '分享活动，抽取丰厚奖品！', 'text');
```

- [ ] **Step 2: 提交**

```bash
git add mysql/init.sql
git commit -m "feat: add admin whitelist and studio config tables"
```

---

### Task 6.2: 创建管理员相关实体和 Mapper

**Files:**
- Create: `backend/src/main/java/com/shuran/art/entity/AdminWhitelist.java`
- Create: `backend/src/main/java/com/shuran/art/entity/StudioConfig.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/AdminWhitelistMapper.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/StudioConfigMapper.java`

- [ ] **Step 1: 创建 AdminWhitelist 实体**

```java
// backend/src/main/java/com/shuran/art/entity/AdminWhitelist.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_whitelist")
public class AdminWhitelist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String name;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 StudioConfig 实体**

```java
// backend/src/main/java/com/shuran/art/entity/StudioConfig.java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("studio_config")
public class StudioConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 Mapper**

```java
// backend/src/main/java/com/shuran/art/mapper/AdminWhitelistMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.AdminWhitelist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminWhitelistMapper extends BaseMapper<AdminWhitelist> {
}
```

```java
// backend/src/main/java/com/shuran/art/mapper/StudioConfigMapper.java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.StudioConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudioConfigMapper extends BaseMapper<StudioConfig> {
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/shuran/art/entity/
git add backend/src/main/java/com/shuran/art/mapper/
git commit -m "feat: add admin whitelist and studio config entities"
```

---

### Task 6.3: 创建管理员服务和拦截器

**Files:**
- Create: `backend/src/main/java/com/shuran/art/service/AdminService.java`
- Create: `backend/src/main/java/com/shuran/art/config/AdminInterceptor.java`
- Modify: `backend/src/main/java/com/shuran/art/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 AdminService**

```java
// backend/src/main/java/com/shuran/art/service/AdminService.java
package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.entity.*;
import com.shuran.art.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminWhitelistMapper adminWhitelistMapper;
    private final StudioConfigMapper studioConfigMapper;
    private final TeacherMapper teacherMapper;
    private final ActivityMapper activityMapper;
    private final PrizeMapper prizeMapper;

    public boolean isAdmin(String openid) {
        return adminWhitelistMapper.selectCount(
            new LambdaQueryWrapper<AdminWhitelist>().eq(AdminWhitelist::getOpenid, openid)
        ) > 0;
    }

    // 画室配置
    public Map<String, String> getStudioConfig() {
        List<StudioConfig> configs = studioConfigMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (StudioConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    public void updateStudioConfig(String key, String value) {
        StudioConfig config = studioConfigMapper.selectOne(
            new LambdaQueryWrapper<StudioConfig>().eq(StudioConfig::getConfigKey, key)
        );
        if (config != null) {
            config.setConfigValue(value);
            studioConfigMapper.updateById(config);
        } else {
            config = new StudioConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("text");
            studioConfigMapper.insert(config);
        }
    }

    // 老师管理
    public List<Teacher> getTeachers() {
        return teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>().orderByAsc(Teacher::getSortOrder)
        );
    }

    public void saveTeacher(Teacher teacher) {
        if (teacher.getId() == null) {
            teacherMapper.insert(teacher);
        } else {
            teacherMapper.updateById(teacher);
        }
    }

    public void deleteTeacher(Long id) {
        teacherMapper.deleteById(id);
    }

    // 活动管理
    public List<Activity> getActivities() {
        return activityMapper.selectList(
            new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreatedAt)
        );
    }

    public void saveActivity(Activity activity) {
        if (activity.getId() == null) {
            activityMapper.insert(activity);
        } else {
            activityMapper.updateById(activity);
        }
    }

    public void deleteActivity(Long id) {
        activityMapper.deleteById(id);
    }

    // 奖品管理
    public List<Prize> getPrizes() {
        return prizeMapper.selectList(null);
    }

    public void savePrize(Prize prize) {
        if (prize.getId() == null) {
            prizeMapper.insert(prize);
        } else {
            prizeMapper.updateById(prize);
        }
    }

    public void deletePrize(Long id) {
        prizeMapper.deleteById(id);
    }
}
```

- [ ] **Step 2: 创建管理员拦截器**

```java
// backend/src/main/java/com/shuran/art/config/AdminInterceptor.java
package com.shuran.art.config;

import com.shuran.art.service.AdminService;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final AdminService adminService;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            response.setStatus(401);
            return false;
        }

        User user = userMapper.selectById(userId);
        if (user == null || !adminService.isAdmin(user.getOpenid())) {
            response.setStatus(403);
            return false;
        }

        return true;
    }
}
```

- [ ] **Step 3: 更新 WebMvcConfig 添加管理员路径拦截**

```java
// 在 WebMvcConfig.java 的 addInterceptors 方法中添加：
registry.addInterceptor(adminInterceptor)
        .addPathPatterns("/admin/**");
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/shuran/art/service/AdminService.java
git add backend/src/main/java/com/shuran/art/config/AdminInterceptor.java
git add backend/src/main/java/com/shuran/art/config/WebMvcConfig.java
git commit -m "feat: add AdminService and AdminInterceptor"
```

---

### Task 6.4: 创建管理员 Controller

**Files:**
- Create: `backend/src/main/java/com/shuran/art/controller/AdminController.java`

- [ ] **Step 1: 创建 AdminController**

```java
// backend/src/main/java/com/shuran/art/controller/AdminController.java
package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.*;
import com.shuran.art.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 验证管理员身份
    @GetMapping("/check")
    public Result<Boolean> checkAdmin() {
        return Result.success(true);
    }

    // ===== 画室配置 =====
    @GetMapping("/studio/config")
    public Result<Map<String, String>> getStudioConfig() {
        return Result.success(adminService.getStudioConfig());
    }

    @PostMapping("/studio/config")
    public Result<Void> updateStudioConfig(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            adminService.updateStudioConfig(entry.getKey(), entry.getValue());
        }
        return Result.success();
    }

    // ===== 老师管理 =====
    @GetMapping("/teachers")
    public Result<List<Teacher>> getTeachers() {
        return Result.success(adminService.getTeachers());
    }

    @PostMapping("/teacher")
    public Result<Void> saveTeacher(@RequestBody Teacher teacher) {
        adminService.saveTeacher(teacher);
        return Result.success();
    }

    @DeleteMapping("/teacher/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        adminService.deleteTeacher(id);
        return Result.success();
    }

    // ===== 活动管理 =====
    @GetMapping("/activities")
    public Result<List<Activity>> getActivities() {
        return Result.success(adminService.getActivities());
    }

    @PostMapping("/activity")
    public Result<Void> saveActivity(@RequestBody Activity activity) {
        adminService.saveActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/activity/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        adminService.deleteActivity(id);
        return Result.success();
    }

    // ===== 奖品管理 =====
    @GetMapping("/prizes")
    public Result<List<Prize>> getPrizes() {
        return Result.success(adminService.getPrizes());
    }

    @PostMapping("/prize")
    public Result<Void> savePrize(@RequestBody Prize prize) {
        adminService.savePrize(prize);
        return Result.success();
    }

    @DeleteMapping("/prize/{id}")
    public Result<Void> deletePrize(@PathVariable Long id) {
        adminService.deletePrize(id);
        return Result.success();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/shuran/art/controller/AdminController.java
git commit -m "feat: add AdminController for backend management"
```

---

### Task 6.5: 创建管理端 API 封装

**Files:**
- Create: `frontend/api/admin.js`

- [ ] **Step 1: 创建 admin.js**

```javascript
// frontend/api/admin.js
const { request } = require('./request');

module.exports = {
  // 检查管理员权限
  checkAdmin: () => request({ url: '/admin/check' }),

  // 画室配置
  getStudioConfig: () => request({ url: '/admin/studio/config' }),
  updateStudioConfig: (data) => request({ url: '/admin/studio/config', method: 'POST', data }),

  // 老师管理
  getTeachers: () => request({ url: '/admin/teachers' }),
  saveTeacher: (data) => request({ url: '/admin/teacher', method: 'POST', data }),
  deleteTeacher: (id) => request({ url: `/admin/teacher/${id}`, method: 'DELETE' }),

  // 活动管理
  getActivities: () => request({ url: '/admin/activities' }),
  saveActivity: (data) => request({ url: '/admin/activity', method: 'POST', data }),
  deleteActivity: (id) => request({ url: `/admin/activity/${id}`, method: 'DELETE' }),

  // 奖品管理
  getPrizes: () => request({ url: '/admin/prizes' }),
  savePrize: (data) => request({ url: '/admin/prize', method: 'POST', data }),
  deletePrize: (id) => request({ url: `/admin/prize/${id}`, method: 'DELETE' })
};
```

- [ ] **Step 2: 提交**

```bash
git add frontend/api/admin.js
git commit -m "feat: add admin API module"
```

---

### Task 6.6: 创建管理入口页面

**Files:**
- Create: `frontend/pages/admin/index/index.js`
- Create: `frontend/pages/admin/index/index.wxml`
- Create: `frontend/pages/admin/index/index.wxss`
- Create: `frontend/pages/admin/index/index.json`
- Modify: `frontend/app.json`

- [ ] **Step 1: 在 app.json 中添加管理页面**

```json
{
  "pages": [
    "pages/index/index",
    "pages/teacher/list/index",
    "pages/activity/list/index",
    "pages/activity/detail/index",
    "pages/lottery/index/index",
    "pages/user/index/index",
    "pages/admin/index/index",
    "pages/admin/studio/index",
    "pages/admin/teacher/index",
    "pages/admin/activity/index",
    "pages/admin/prize/index"
  ]
}
```

- [ ] **Step 2: 创建管理入口页面**

```json
// frontend/pages/admin/index/index.json
{
  "navigationBarTitleText": "后台管理"
}
```

```javascript
// frontend/pages/admin/index/index.js
const app = getApp();
const adminApi = require('../../../api/admin');

Page({
  data: {
    isAdmin: false,
    loading: true
  },

  onLoad() {
    this.checkAdmin();
  },

  async checkAdmin() {
    try {
      if (!app.globalData.token) {
        await app.login();
      }
      await adminApi.checkAdmin();
      this.setData({ isAdmin: true, loading: false });
    } catch (e) {
      this.setData({ isAdmin: false, loading: false });
      wx.showToast({ title: '无管理权限', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  goToStudio() {
    wx.navigateTo({ url: '/pages/admin/studio/index' });
  },

  goToTeacher() {
    wx.navigateTo({ url: '/pages/admin/teacher/index' });
  },

  goToActivity() {
    wx.navigateTo({ url: '/pages/admin/activity/index' });
  },

  goToPrize() {
    wx.navigateTo({ url: '/pages/admin/prize/index' });
  }
});
```

```html
<!-- frontend/pages/admin/index/index.wxml -->
<view class="container" wx:if="{{!loading}}">
  <view class="admin-header">
    <text class="title">后台管理</text>
    <text class="subtitle">管理画室信息和活动配置</text>
  </view>

  <view class="menu-list" wx:if="{{isAdmin}}">
    <view class="menu-item" bindtap="goToStudio">
      <text class="menu-icon">🏠</text>
      <view class="menu-info">
        <text class="menu-title">画室配置</text>
        <text class="menu-desc">文字、图片、视频介绍</text>
      </view>
      <text class="menu-arrow">></text>
    </view>

    <view class="menu-item" bindtap="goToTeacher">
      <text class="menu-icon">👨‍🏫</text>
      <view class="menu-info">
        <text class="menu-title">老师管理</text>
        <text class="menu-desc">添加、编辑老师信息</text>
      </view>
      <text class="menu-arrow">></text>
    </view>

    <view class="menu-item" bindtap="goToActivity">
      <text class="menu-icon">🎉</text>
      <view class="menu-info">
        <text class="menu-title">活动管理</text>
        <text class="menu-desc">发布、编辑推广活动</text>
      </view>
      <text class="menu-arrow">></text>
    </view>

    <view class="menu-item" bindtap="goToPrize">
      <text class="menu-icon">🎁</text>
      <view class="menu-info">
        <text class="menu-title">奖品管理</text>
        <text class="menu-desc">配置抽奖奖品池</text>
      </view>
      <text class="menu-arrow">></text>
    </view>
  </view>

  <view class="no-permission" wx:else>
    <text>您没有管理权限</text>
  </view>
</view>

<view class="loading" wx:else>
  <text>验证权限中...</text>
</view>
```

```css
/* frontend/pages/admin/index/index.wxss */
.admin-header {
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 60rpx 30rpx;
  color: #fff;
  margin-bottom: 30rpx;
  border-radius: 0 0 30rpx 30rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 10rpx;
}

.subtitle {
  font-size: 26rpx;
  opacity: 0.8;
}

.menu-list {
  padding: 0 30rpx;
}

.menu-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.menu-icon {
  font-size: 48rpx;
  margin-right: 24rpx;
}

.menu-info {
  flex: 1;
}

.menu-title {
  font-size: 32rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 8rpx;
}

.menu-desc {
  font-size: 24rpx;
  color: #999;
}

.menu-arrow {
  color: #ccc;
  font-size: 32rpx;
}

.no-permission, .loading {
  text-align: center;
  padding: 100rpx;
  color: #999;
}
```

- [ ] **Step 3: 提交**

```bash
git add frontend/pages/admin/
git add frontend/app.json
git commit -m "feat: add admin entry page"
```

---

### Task 6.7: 创建画室配置页面

**Files:**
- Create: `frontend/pages/admin/studio/index.js`
- Create: `frontend/pages/admin/studio/index.wxml`
- Create: `frontend/pages/admin/studio/index.wxss`
- Create: `frontend/pages/admin/studio/index.json`

- [ ] **Step 1: 创建画室配置页面**

```json
// frontend/pages/admin/studio/index.json
{
  "navigationBarTitleText": "画室配置"
}
```

```javascript
// frontend/pages/admin/studio/index.js
const adminApi = require('../../../api/admin');

Page({
  data: {
    config: {
      studio_name: '',
      studio_slogan: '',
      studio_description: '',
      studio_images: [],
      studio_video: '',
      share_title_template: '',
      share_desc_template: ''
    },
    loading: true
  },

  onLoad() {
    this.loadConfig();
  },

  async loadConfig() {
    try {
      const config = await adminApi.getStudioConfig();
      // 解析 JSON 字段
      if (config.studio_images) {
        try {
          config.studio_images = JSON.parse(config.studio_images);
        } catch (e) {
          config.studio_images = [];
        }
      }
      this.setData({ config, loading: false });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({
      [`config.${field}`]: e.detail.value
    });
  },

  async chooseImage() {
    const res = await wx.chooseMedia({
      count: 9 - this.data.config.studio_images.length,
      mediaType: ['image']
    });

    // 这里需要上传图片到服务器，获取URL
    // 简化示例：直接使用本地路径
    const images = [...this.data.config.studio_images];
    res.tempFiles.forEach(file => {
      images.push(file.tempFilePath);
    });

    this.setData({ 'config.studio_images': images });
  },

  removeImage(e) {
    const { index } = e.currentTarget.dataset;
    const images = this.data.config.studio_images;
    images.splice(index, 1);
    this.setData({ 'config.studio_images': images });
  },

  async saveConfig() {
    wx.showLoading({ title: '保存中...' });
    try {
      const config = { ...this.data.config };
      config.studio_images = JSON.stringify(config.studio_images);
      await adminApi.updateStudioConfig(config);
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
  }
});
```

```html
<!-- frontend/pages/admin/studio/index.wxml -->
<view class="container" wx:if="{{!loading}}">
  <view class="form-section">
    <view class="section-title">基本信息</view>

    <view class="form-item">
      <text class="label">画室名称</text>
      <input class="input" value="{{config.studio_name}}"
             data-field="studio_name" bindinput="onInputChange" />
    </view>

    <view class="form-item">
      <text class="label">宣传语</text>
      <input class="input" value="{{config.studio_slogan}}"
             data-field="studio_slogan" bindinput="onInputChange" />
    </view>

    <view class="form-item">
      <text class="label">画室介绍</text>
      <textarea class="textarea" value="{{config.studio_description}}"
                data-field="studio_description" bindinput="onInputChange" />
    </view>
  </view>

  <view class="form-section">
    <view class="section-title">图片展示</view>
    <view class="image-list">
      <view class="image-item" wx:for="{{config.studio_images}}" wx:key="*this">
        <image src="{{item}}" mode="aspectFill"></image>
        <view class="remove-btn" data-index="{{index}}" bindtap="removeImage">×</view>
      </view>
      <view class="image-add" bindtap="chooseImage" wx:if="{{config.studio_images.length < 9}}">
        <text>+</text>
      </view>
    </view>
  </view>

  <view class="form-section">
    <view class="section-title">视频介绍</view>
    <view class="form-item">
      <text class="label">视频链接</text>
      <input class="input" value="{{config.studio_video}}"
             data-field="studio_video" bindinput="onInputChange"
             placeholder="支持腾讯视频/优酷等链接" />
    </view>
  </view>

  <view class="form-section">
    <view class="section-title">分享文案</view>

    <view class="form-item">
      <text class="label">分享标题模板</text>
      <input class="input" value="{{config.share_title_template}}"
             data-field="share_title_template" bindinput="onInputChange"
             placeholder="使用{activity_title}代表活动名" />
    </view>

    <view class="form-item">
      <text class="label">分享描述模板</text>
      <input class="input" value="{{config.share_desc_template}}"
             data-field="share_desc_template" bindinput="onInputChange" />
    </view>
  </view>

  <view class="btn-save" bindtap="saveConfig">保存配置</view>
</view>
```

```css
/* frontend/pages/admin/studio/index.wxss */
.form-section {
  background: #fff;
  margin: 20rpx;
  padding: 30rpx;
  border-radius: 16rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-bottom: 16rpx;
}

.input {
  background: #f5f5f5;
  padding: 20rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
}

.textarea {
  background: #f5f5f5;
  padding: 20rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  width: 100%;
  height: 200rpx;
  box-sizing: border-box;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.image-item {
  width: 200rpx;
  height: 200rpx;
  position: relative;
}

.image-item image {
  width: 100%;
  height: 100%;
  border-radius: 8rpx;
}

.remove-btn {
  position: absolute;
  top: -16rpx;
  right: -16rpx;
  width: 40rpx;
  height: 40rpx;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  text-align: center;
  line-height: 40rpx;
  font-size: 28rpx;
}

.image-add {
  width: 200rpx;
  height: 200rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  color: #999;
}

.btn-save {
  margin: 40rpx 20rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  text-align: center;
  padding: 30rpx;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/admin/studio/
git commit -m "feat: add studio config admin page"
```

---

### Task 6.8: 创建老师管理页面

**Files:**
- Create: `frontend/pages/admin/teacher/index.js`
- Create: `frontend/pages/admin/teacher/index.wxml`
- Create: `frontend/pages/admin/teacher/index.wxss`
- Create: `frontend/pages/admin/teacher/index.json`

- [ ] **Step 1: 创建老师管理页面**

```json
// frontend/pages/admin/teacher/index.json
{
  "navigationBarTitleText": "老师管理"
}
```

```javascript
// frontend/pages/admin/teacher/index.js
const adminApi = require('../../../api/admin');

Page({
  data: {
    teachers: [],
    showForm: false,
    currentTeacher: {
      id: null,
      name: '',
      title: '',
      intro: '',
      avatar: '',
      works: [],
      sortOrder: 0
    }
  },

  onLoad() {
    this.loadTeachers();
  },

  async loadTeachers() {
    try {
      const teachers = await adminApi.getTeachers();
      this.setData({ teachers });
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  showAddForm() {
    this.setData({
      showForm: true,
      currentTeacher: {
        id: null,
        name: '',
        title: '',
        intro: '',
        avatar: '',
        works: [],
        sortOrder: 0
      }
    });
  },

  editTeacher(e) {
    const teacher = e.currentTarget.dataset.teacher;
    this.setData({
      showForm: true,
      currentTeacher: { ...teacher }
    });
  },

  closeForm() {
    this.setData({ showForm: false });
  },

  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({
      [`currentTeacher.${field}`]: e.detail.value
    });
  },

  async chooseAvatar() {
    const res = await wx.chooseMedia({ count: 1, mediaType: ['image'] });
    this.setData({
      'currentTeacher.avatar': res.tempFiles[0].tempFilePath
    });
  },

  async saveTeacher() {
    const { currentTeacher } = this.data;
    if (!currentTeacher.name) {
      wx.showToast({ title: '请填写姓名', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });
    try {
      await adminApi.saveTeacher(currentTeacher);
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.setData({ showForm: false });
      this.loadTeachers();
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
  },

  async deleteTeacher(e) {
    const { id } = e.currentTarget.dataset;
    const res = await wx.showModal({
      title: '确认删除',
      content: '确定要删除该老师吗？'
    });

    if (res.confirm) {
      try {
        await adminApi.deleteTeacher(id);
        wx.showToast({ title: '删除成功', icon: 'success' });
        this.loadTeachers();
      } catch (e) {
        wx.showToast({ title: '删除失败', icon: 'none' });
      }
    }
  }
});
```

```html
<!-- frontend/pages/admin/teacher/index.wxml -->
<view class="container">
  <view class="header">
    <text class="title">老师列表</text>
    <view class="add-btn" bindtap="showAddForm">+ 添加老师</view>
  </view>

  <view class="list">
    <view class="item" wx:for="{{teachers}}" wx:key="id">
      <image class="avatar" src="{{item.avatar || '/images/default-avatar.png'}}"></image>
      <view class="info">
        <text class="name">{{item.name}}</text>
        <text class="title-text">{{item.title}}</text>
      </view>
      <view class="actions">
        <text class="edit" data-teacher="{{item}}" bindtap="editTeacher">编辑</text>
        <text class="delete" data-id="{{item.id}}" bindtap="deleteTeacher">删除</text>
      </view>
    </view>
  </view>

  <view class="empty" wx:if="{{teachers.length === 0}}">
    <text>暂无老师，点击上方添加</text>
  </view>
</view>

<!-- 编辑表单弹窗 -->
<view class="form-mask" wx:if="{{showForm}}" bindtap="closeForm">
  <view class="form-popup" catchtap="">
    <view class="form-title">{{currentTeacher.id ? '编辑老师' : '添加老师'}}</view>

    <view class="form-item">
      <text class="label">头像</text>
      <view class="avatar-picker" bindtap="chooseAvatar">
        <image wx:if="{{currentTeacher.avatar}}" src="{{currentTeacher.avatar}}"></image>
        <text wx:else>+</text>
      </view>
    </view>

    <view class="form-item">
      <text class="label">姓名 *</text>
      <input value="{{currentTeacher.name}}" data-field="name" bindinput="onInputChange" />
    </view>

    <view class="form-item">
      <text class="label">职称</text>
      <input value="{{currentTeacher.title}}" data-field="title" bindinput="onInputChange" />
    </view>

    <view class="form-item">
      <text class="label">简介</text>
      <textarea value="{{currentTeacher.intro}}" data-field="intro" bindinput="onInputChange"></textarea>
    </view>

    <view class="form-item">
      <text class="label">排序（数字越小越靠前）</text>
      <input type="number" value="{{currentTeacher.sortOrder}}" data-field="sortOrder" bindinput="onInputChange" />
    </view>

    <view class="form-actions">
      <view class="btn-cancel" bindtap="closeForm">取消</view>
      <view class="btn-save" bindtap="saveTeacher">保存</view>
    </view>
  </view>
</view>
```

```css
/* frontend/pages/admin/teacher/index.wxss */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background: #fff;
  margin-bottom: 20rpx;
}

.header .title {
  font-size: 36rpx;
  font-weight: bold;
}

.add-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 16rpx 32rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.list {
  padding: 0 20rpx;
}

.item {
  background: #fff;
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
}

.item .avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 24rpx;
}

.item .info {
  flex: 1;
}

.item .name {
  font-size: 32rpx;
  font-weight: bold;
  display: block;
}

.item .title-text {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.actions {
  display: flex;
  gap: 20rpx;
}

.actions .edit {
  color: #667eea;
  font-size: 26rpx;
}

.actions .delete {
  color: #ff4d4f;
  font-size: 26rpx;
}

.empty {
  text-align: center;
  padding: 100rpx;
  color: #999;
}

/* 弹窗样式 */
.form-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.form-popup {
  width: 90%;
  max-height: 80vh;
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  overflow-y: auto;
}

.form-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 40rpx;
  text-align: center;
}

.form-item {
  margin-bottom: 30rpx;
}

.form-item .label {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-bottom: 16rpx;
}

.form-item input, .form-item textarea {
  background: #f5f5f5;
  padding: 20rpx;
  border-radius: 8rpx;
  width: 100%;
  box-sizing: border-box;
}

.form-item textarea {
  height: 160rpx;
}

.avatar-picker {
  width: 150rpx;
  height: 150rpx;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-picker image {
  width: 100%;
  height: 100%;
}

.avatar-picker text {
  font-size: 60rpx;
  color: #999;
}

.form-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
}

.btn-cancel, .btn-save {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  border-radius: 40rpx;
  font-size: 30rpx;
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/admin/teacher/
git commit -m "feat: add teacher management admin page"
```

---

### Task 6.9: 创建活动管理页面

**Files:**
- Create: `frontend/pages/admin/activity/index.js`
- Create: `frontend/pages/admin/activity/index.wxml`
- Create: `frontend/pages/admin/activity/index.wxss`
- Create: `frontend/pages/admin/activity/index.json`

- [ ] **Step 1: 创建活动管理页面**

结构与老师管理类似，包含活动列表、添加/编辑表单，字段包括：
- title（标题）
- description（描述）
- coverImg（封面图）
- startTime（开始时间）
- endTime（结束时间）
- dailyShareLimit（每日分享上限）
- status（状态）

```javascript
// frontend/pages/admin/activity/index.js
// 结构与 teacher 管理页面类似，替换为活动字段
// 省略具体代码，实现方式相同
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/admin/activity/
git commit -m "feat: add activity management admin page"
```

---

### Task 6.10: 创建奖品管理页面

**Files:**
- Create: `frontend/pages/admin/prize/index.js`
- Create: `frontend/pages/admin/prize/index.wxml`
- Create: `frontend/pages/admin/prize/index.wxss`
- Create: `frontend/pages/admin/prize/index.json`

- [ ] **Step 1: 创建奖品管理页面**

管理奖品池，字段包括：
- name（奖品名）
- type（类型：points/experience/gift）
- value（数值）
- probability（中奖概率）
- stock（库存，-1表示无限）
- icon（图标）
- needClaim（是否需线下领取）
- status（状态）

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/admin/prize/
git commit -m "feat: add prize management admin page"
```

---

### Task 6.11: 在用户中心添加管理入口

**Files:**
- Modify: `frontend/pages/user/index/index.js`
- Modify: `frontend/pages/user/index/index.wxml`

- [ ] **Step 1: 添加管理员入口检测**

在 user/index 页面添加管理员身份检测，如果是管理员则显示"后台管理"入口。

```javascript
// 在 loadData 方法中添加
async checkAdmin() {
  try {
    await adminApi.checkAdmin();
    this.setData({ isAdmin: true });
  } catch (e) {
    this.setData({ isAdmin: false });
  }
}
```

```html
<!-- 在 user/index.wxml 中添加 -->
<view class="admin-entry card" wx:if="{{isAdmin}}" bindtap="goToAdmin">
  <text class="admin-icon">⚙️</text>
  <text class="admin-text">后台管理</text>
  <text class="admin-arrow">></text>
</view>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/user/
git commit -m "feat: add admin entry in user center"
```

---

**计划完成，共 6 个 Chunk，约 35 个 Task。**

Chunk 6 新增内容摘要：
- Task 6.1: 数据库扩展（管理员白名单表、画室配置表）
- Task 6.2: 管理员实体和 Mapper
- Task 6.3: AdminService 和 AdminInterceptor
- Task 6.4: AdminController
- Task 6.5: 前端 admin API 封装
- Task 6.6: 管理入口页面
- Task 6.7: 画室配置页面
- Task 6.8: 老师管理页面
- Task 6.9: 活动管理页面
- Task 6.10: 奖品管理页面
- Task 6.11: 用户中心添加管理入口
