# Aegis-Boot (神盾 · 现代安全开发架构)

[![Java Version](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Sa-Token](https://img.shields.io/badge/Security-Sa--Token%201.44-red.svg)](https://sa-token.cc/)
[![MyBatis-Plus](https://img.shields.io/badge/ORM-MyBatis--Plus-blue.svg)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**Aegis-Boot** 是一款对标并超越 **若依 (RuoYi)** 的下一代全栈开发架构。它基于最新的 **Spring Boot 4.0.3** 和 **Java 21** 构建，深度集成**虚拟线程**与**现代化安全组件**，专为追求极致性能、防御性编程和敏捷开发的生产环境而设计。

> **为什么选择 Aegis-Boot？**
> - **更现代**：全面拥抱 Java 21 虚拟线程，抛弃过时的线程池模型，资源消耗降低 90%。
> - **更安全**：集成 Sa-Token 全栈权限体系，原生支持 JWT、多账号体系、二级认证与踢人下线。
> - **更智能**：Aegis DB Engine (Code-First)，让数据库随实体类自动演进，彻底告别手动维护 SQL 的时代。

---

## 核心特性

### 1. 虚拟线程巅峰性能 (Next-Gen Concurrency)
全面拥抱 **Java 21 Virtual Threads**。通过轻量级线程模型，**Aegis-Boot** 能够以极低的内存开销支撑 **每秒万级 (10k+ RPS)** 的并发请求，彻底解决传统框架在 IO 密集型场景下的瓶颈。

### 2. 工业级全栈安全 (Aegis Security)
基于 **Sa-Token** 构建的防御性安全体系，提供远超传统 Security/Shiro 的开发体验：
- **无状态认证**：原生支持 JWT，适配前后端分离、移动端及微服务。
- **高阶权限控制**：支持注解鉴权、动态权限分配、角色布尔运算及二级认证。
- **分布式会话**：通过 Redis 托管 Session，实现 Token 自动续签与集群高可用。

### 3. 代码驱动数据库同步 (Code-First Evolution)
**拒绝繁琐 SQL**：通过 `@TableComment`, `@ColumnType`, `@Index`, `@ForeignKey` 等注解，**Aegis-Boot** 在系统启动时会自动扫描：
- **无感迁移**：自动检测字段变化，执行 `ADD`/`MODIFY`/`DROP` 操作。
- **约束维护**：自动创建物理/逻辑索引及外键，确保数据一致性。
- **循环依赖处理**：智能识别表间引用，确保建表顺序与约束创建的正确性。

### 4. 极速异步日志 (Non-blocking Logging)
使用 **Log4j2** 配合 **Lmax Disruptor** 无锁并发框架。在高并发请求下，日志写入不再是系统的阿喀琉斯之踵，确保业务吞吐量不受任何 IO 阻碍。

### 5. 现代化工具生态
- **一键项目定制**：内置 `ProjectRenameTool`，支持项目包名、名称的一键全局重命名。
- **高性能解析**：集成 **Fastjson2** (阿里最新一代) 与 **Hutool**，提供最前沿的库支持。

---

## 技术栈

| 类别 | 技术选型 | 优势 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 4.0.3 | 下一代 Spring 生态，全面优化 Native 支持 |
| **开发语言** | Java 21 (LTS) | 虚拟线程、模式匹配、高性能记录类 |
| **安全体系** | Sa-Token 1.44 + JWT | 现代化的登录鉴权，兼顾极简 API 与强大功能 |
| **持久层** | MyBatis-Plus 3.5.10.1 | 无需 XML，兼顾 SQL 灵活性与开发效率 |
| **日志组件** | Log4j2 + Disruptor | 生产级高性能异步日志，低延迟、高吞吐 |
| **数据引擎** | Aegis DB Engine | 独家 Code-First 自动演进引擎 |

---

## 项目结构

```text
src/main/java/top/yuxs/springbootdev/
├── annotation/db/      # 数据库自动同步自定义注解 (Aegis 核心)
├── common/             # 基础实体类、统一响应结果集
├── config/             # 全局配置 (Sa-Token, Redis, 线程池, 跨域)
├── db/                 # DatabaseInitService 数据库自动演进引擎
├── enums/              # 业务枚举与数据库约束策略
├── exception/          # 全局防御性异常拦截与处理
└── utils/              # 辅助工具 (含 Aegis 项目重命名工具)
```

---

## 快速开始

### 1. 环境准备
- **JDK 21+** (建议使用 GraalVM)
- **MySQL 8.0+**
- **Redis 6.x+**

### 2. 数据库自动初始化
在 `application.yml` 中配置您的数据源信息。**Aegis-Boot** 将在首次启动时自动为您创建所有表结构、字段注释、索引及外键约束。

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/aegis_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
    username: root
    password: your_password
```

### 3. 定义您的第一个安全实体
```java
@Data
@TableComment("系统用户")
@Index(name = "idx_username", columns = "username", type = IndexType.UNIQUE)
public class User extends BaseEntity {

    @ColumnComment("登录账号")
    private String username;

    @ColumnComment("加密密码")
    private String password;

    @DefaultValue("1")
    @ColumnComment("状态: 1-正常, 0-禁用")
    private Integer status;
}
```

---

## 愿景
**Aegis-Boot** 致力于解决传统框架过于臃肿、性能低下、安全配置繁琐的问题。我们通过最先进的 Java 技术栈，为您打造一个更现代、更安全、更丝滑的开发底座。

---
**Copyright © 2026 Aegis-Boot Team. All rights reserved.**
