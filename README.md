# Aegis-Boot (神盾 · 现代安全开发架构)

[![Java Version](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Sa-Token](https://img.shields.io/badge/Security-Sa--Token%201.45-red.svg)](https://sa-token.cc/)
[![MyBatis-Plus](https://img.shields.io/badge/ORM-MyBatis--Plus-blue.svg)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=flat&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/Yu-dongxing/springbootDev)

**Aegis-Boot** 是一款对标并超越 **若依 (RuoYi)** 的下一代全栈开发架构。它基于最新的 **Spring Boot 4.0.5** 和 **Java 21** 构建，深度集成**虚拟线程**与**现代化安全组件**，专为追求极致性能、防御性编程和敏捷开发的生产环境而设计。

> **为什么选择 Aegis-Boot？**
> - **更现代**：全面拥抱 Java 21 虚拟线程，抛弃过时的线程池模型，资源消耗降低 90%。
> - **更安全**：集成 Sa-Token 1.45 全栈权限体系，原生支持 **Sa-Firewall 防火墙**、JWT、多账号体系、二级认证与踢人下线。
> - **更智能**：Aegis DB Engine (Code-First)，让数据库随实体类自动演进，彻底告别手动维护 SQL 的时代。
> - **更丝滑**：内置 Aegis File Hub，支持灵活的多厂商存储切换、自动目录归档与资源智能回收。

---

## 核心特性

### 1. 虚拟线程巅峰性能 (Next-Gen Concurrency)
全面拥抱 **Java 21 Virtual Threads**。通过轻量级线程模型，**Aegis-Boot** 能够以极低的内存开销支撑 **每秒万级 (10k+ RPS)** 的并发请求，彻底解决传统框架在 IO 密集型场景下的瓶颈。

### 2. 工业级全栈安全 (Aegis Security)
基于 **Sa-Token 1.45** 构建的防御性安全体系，提供远超传统 Security/Shiro 的开发体验：
- **Sa-Firewall 防火墙**：原生拦截目录遍历 (..)、危险字符 (%2e) 及 Host 碰撞攻击。
- **无状态认证**：原生支持 JWT，适配前后端分离、移动端及微服务。
- **高阶权限控制**：支持注解鉴权、动态权限分配、角色布尔运算及二级认证。
- **分布式会话**：通过 Redis 托管 Session，实现 Token 自动续签与集群高可用。

### 3. 代码驱动数据库同步 (Code-First Evolution)
**拒绝繁琐 SQL**：通过 `@TableComment`, `@ColumnType`, `@Index`, `@ForeignKey` 等注解，**Aegis-Boot** 在系统启动时会自动扫描：
- **无感迁移**：自动检测字段变化，执行 `ADD`/`MODIFY`/`DROP` 操作。
- **约束维护**：自动创建物理/逻辑索引及外键，确保数据一致性。
- **循环依赖处理**：智能识别表间引用，确保建表顺序与约束创建的正确性。

### 4. 前端高精度适配 (Precision Protection)
针对雪花算法 ID (Long 型) 在 JavaScript 中出现的精度丢失（截断）风险，内置 **Jackson 自动转换引擎**：
- **全局生效**：序列化时自动将 `Long` 转为 `String`，反序列化时自动转回 `Long`。
- **灵活开关**：通过 `jackson.long-to-string` 配置项即可一键启闭。

### 5. 智能文件存储中心 (Aegis File Hub)
内置高度抽象的文件管理体系，超越简单的文件上传：
- **策略模式架构**：一键切换 `LOCAL`、`ALIYUN_OSS`、`MINIO` 等存储方式，业务代码零改动。
- **自动目录归档**：本地存储原生支持按 `yyyy/MM` 自动分库分表式存储。
- **智能资源回收**：删除文件时自动递归清理空的父文件夹。
- **环境迁移无忧**：数据库仅存储相对路径，URL 动态构建。当服务从测试环境迁移到生产环境（域名变更）时，所有历史文件链接自动失效并更新为新域名。


### 6. 极速异步日志 (Non-blocking Logging)
使用 **Log4j2** 配合 **Lmax Disruptor** 无锁并发框架。在高并发请求下，日志写入不再是系统的阿喀琉斯之踵。

### 7. 现代化工具生态
- **一键项目定制**：内置 `ProjectRenameTool`，支持项目包名、名称的一键全局重命名。
- **高性能解析**：集成 **Fastjson2** (阿里最新一代) 与 **Hutool**，提供最前沿的库支持。

---

## 技术栈

| 类别 | 技术选型 | 优势 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 4.0.5 | 下一代 Spring 生态，全面优化 Native 支持 |
| **开发语言** | Java 21 (LTS) | 虚拟线程、模式匹配、高性能记录类 |
| **安全体系** | Sa-Token 1.45 + Firewall | 现代化的登录鉴权与防火墙，兼顾极简 API 与强大功能 |
| **持久层** | MyBatis-Plus 3.5.16 | 无需 XML，兼顾 SQL 灵活性与开发效率 |
| **存储引擎** | Aegis File Hub | 支持本地与云端策略切换，智能清理与动态 URL |
| **日志组件** | Log4j2 + Disruptor | 生产级高性能异步日志，低延迟、高吞吐 |
| **数据引擎** | Aegis DB Engine | 独家 Code-First 自动演进引擎 |

---

## 快速开始

### 1. 环境准备
- **JDK 21+** (建议使用 GraalVM)
- **MySQL 8.0+**
- **Redis 6.x+**

### 2. 关键配置
在 `application.yml` 中根据需求调整以下自定义配置：

```yaml
# Jackson 配置：是否开启 Long 转 String（解决雪花算法 ID 截断问题）
jackson:
  long-to-string: true

# Sa-Token 防火墙配置
sa-token:
  firewall:
    check-path-character: true # 开启危险字符拦截
    allowed-method: GET, POST, PUT, DELETE # 限制请求方式
```
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

### 4. 文件上传示例
通过调用 `/common/file/upload` 接口，您可以轻松将文件存储在指定的子目录中，系统会自动处理年月归档。

---

## 愿景
**Aegis-Boot** 致力于解决传统框架过于臃肿、性能低下、安全配置繁琐的问题。我们通过最先进的 Java 技术栈，为您打造一个更现代、更安全、更丝滑的开发底座。

---
**Copyright © 2026 Aegis-Boot Team. All rights reserved.**
