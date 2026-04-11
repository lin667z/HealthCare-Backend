# 🏥 HealthCare-Backend (医疗健康大模型系统后端)

> 基于 Spring Boot 3 和大语言模型（Spring AI Alibaba）构建的医疗健康管理与智能报告生成系统后端。

## 🌟 项目简介

本项目是一个结合了 AI 能力的医疗健康演示系统后端服务。系统提供了完善的患者管理、门诊记录、临床数据管理等基础医疗功能，并深度集成了 **阿里云 DashScope（通义千问）** 大模型，能够根据患者的临床病历和检查检验结果，智能生成规范的 **AI 临床报告**。

## ✨ 核心特性

- **👨‍⚕️ 患者与门诊管理**：患者基础信息管理、门急诊就诊记录追踪。
- **📋 临床数据管理**：结构化的入院记录、病案首页、护理记录、检验结果和检查报告管理。
- **🤖 智能 AI 报告**：基于 Spring AI Alibaba 接入通义千问大模型，结合患者上下文自动生成专业的 AI 临床总结报告。
- **🔐 权限与安全**：基于 JWT 的身份认证体系与拦截器设计。
- **📄 规范化异常处理**：全局统一的异常拦截与标准化的数据返回结构（Result / PageResult）。
- **📊 慢日志拦截**：配置了慢 SQL 拦截器，便于系统性能优化与分析。

## 🛠️ 技术栈

- **核心框架**：Java 17, Spring Boot 3.2.5
- **持久层框架**：MyBatis-Plus 3.5.5
- **数据库**：MySQL 8.0+
- **AI 框架**：Spring AI Alibaba (接入 DashScope 通义千问 `qwen-flash`)
- **安全与工具**：JWT (jjwt 0.12.6), Hutool, Lombok, DozerMapper, Fastjson

## 🚀 快速开始

### 1. 环境准备

- JDK 17 或以上版本
- Maven 3.6+
- MySQL 8.0+
- 阿里云 DashScope API Key（用于 AI 报告生成功能）

### 2. 数据库配置

1. 在本地 MySQL 中创建名为 `chronic_disease_db` 的数据库：
   ```sql
   CREATE DATABASE chronic_disease_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. *注意：请自行导入项目相关的 SQL 结构脚本至该数据库。*

### 3. 配置环境变量

项目已对敏感信息进行了脱敏处理。在运行项目之前，请在你的 IDE 或操作系统中配置以下环境变量（或使用 `.env` 插件）：

| 环境变量名 | 说明 | 示例值 / 默认值 |
| --- | --- | --- |
| `DB_PASSWORD` | 数据库 root 用户的密码 | `root` |
| `JWT_SECRET_KEY` | JWT 签名的密钥 (至少32位) | `your_jwt_secret_key_here` |
| `DASHSCOPE_API_KEY` | 阿里云通义千问 API Key | `sk-xxxxxxxxx` |

> 💡 **提示**：如果在 IDEA 中运行，可以在 `Run/Debug Configurations` 的 `Environment variables` 选项中添加这些配置。

### 4. 运行项目

使用 Maven 编译并启动 Spring Boot 应用：

```bash
mvn clean install
mvn spring-boot:run
```

项目默认运行在 `8088` 端口。

## 📁 项目结构

```text
src/main/java/com/nlpai4h/healthydemobacked/
├── aspect/         # AOP 切面（日志记录等）
├── common/         # 公共组件（常量、异常处理、统一返回结果等）
├── config/         # 核心配置类（WebMvc、MybatisPlus、线程池、跨域等）
├── controller/     # RESTful API 控制器（处理 HTTP 请求）
├── filter/         # Web 过滤器（TraceId 等）
├── interceptor/    # 拦截器（JWT 校验、慢 SQL 监控）
├── mapper/         # MyBatis Mapper 接口
├── model/          # 数据模型 (DTO, Entity, Event, VO)
├── service/        # 业务逻辑层 (包括 AI 大模型调用服务)
│   ├── ai/         # AI 核心实现 (提示词构建、报告解析、大模型编排)
│   ├── helper/     # 业务辅助类
│   └── impl/       # 业务逻辑实现类
├── utils/          # 工具类 (JWT, Bean 拷贝等)
└── HealthyDemoBackedApplication.java # 项目启动类
```

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request 来完善这个项目！

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

---
*Powered by [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) & DashScope*
