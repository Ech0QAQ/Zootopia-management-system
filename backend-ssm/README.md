# 动物城管理系统 - SSM后端

## 技术栈
- Spring Boot 3.1.5
- MyBatis Plus 3.5.4
- MySQL 8.0+
- JWT认证
- Java 17

## 配置说明
1. 修改 `src/main/resources/application.yml` 中的数据库连接信息
2. 确保MySQL数据库已创建，并执行 `zootopia.sql` 初始化表结构

## 运行方式
```bash
mvn spring-boot:run
```

后端服务将运行在 `http://localhost:3000/api`

## API接口说明
所有接口前缀为 `/api`，需要JWT认证（除登录、注册、健康检查外）

