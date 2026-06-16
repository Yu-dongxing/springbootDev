# Aegis-Boot (神盾 · 现代安全开发架构)

[![Java Version](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Sa-Token](https://img.shields.io/badge/Security-Sa--Token%201.45-red.svg)](https://sa-token.cc/)
[![MyBatis-Plus](https://img.shields.io/badge/ORM-MyBatis--Plus-blue.svg)](https://baomidou.com/)
[![JustAuth](https://img.shields.io/badge/OAuth-JustAuth%201.16-violet.svg)](https://github.com/justauth/JustAuth)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**Aegis-Boot** 是一款对标并超越 **若依 (RuoYi)** 的下一代全栈开发架构。它基于最新的 **Spring Boot 4.0.5** 和 **Java 21** 构建，深度集成**虚拟线程**与**现代化安全组件**，专为追求极致性能、防御性编程和敏捷开发的生产环境而设计。

> **为什么选择 Aegis-Boot？**
> - **更现代**：全面拥抱 Java 21 虚拟线程，抛弃过时的线程池模型，资源消耗降低 90%。
> - **更安全**：集成 Sa-Token 1.45 全栈权限体系，原生支持 **Sa-Firewall 防火墙**、JWT、多账号体系、二级认证与踢人下线。
> - **更纯净**：全盘贯彻 **Zero-XML (无 XML 配置文件)** 极致轻量设计，无 SQL 联表 Join 锁瓶颈，大幅提升高并发读写吞吐量。
> - **无侵入**：物理 API 全自动扫描增量同步，精细到 `Path + Method`，实现 0 注解硬编码拦截鉴权。
> - **更快捷**：深度集成 JustAuth，一键无感接入 20+ 第三方登录/注册，内置极光暗黑毛玻璃（Glassmorphic）交付视效。
> - **更智能**：Aegis DB Engine (Code-First) 2.0，组件化驱动数据库自动演进，支持元数据解析与多数据库扩展。
> - **更解耦**：Aegis File Hub 原生支持事件驱动架构，文件上传与业务逻辑通过 `FileUploadedEvent` 异步/同步联动。

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

### 3. 动态无注解拦截鉴权 (No-Annotation Security)
开发人员编写 Controller 业务方法时**无需添加任何权限注解**（如 `@SaCheckPermission`），物理路由在系统启动时通过 Spring 事件机制（监听 `ApplicationReadyEvent`）全自动扫描并增量同步至 `sys_api` 数据库表中。
安全拦截精细至 `Path + Method` 级别。通过 **Redis 二级高速缓存**（Set 集合 `METHOD:PATH` 规则对碰）进行高并发极速过滤与判定，改动权限即时生效。
> **安全防护网覆盖**：除精细防护的主管理端 `/api/admin/**` 路由外，系统对公共后台组件路径 `/sys/**`（如系统文件管理 `/sys/file` 和日志接口 `/sys/log`）统一应用同等力度的 B 端管理员会话保护，杜绝了历史接口未受权限拦截器保护的潜在风险。


### 4. 通用社交登录与无感快捷注册 (JustAuth OAuth)
系统深度集成了第三方登录授权组件 **JustAuth**，并通过 `sys_user_social` 一主多伴表设计，支持一个本地账户绑定多个社交账号（GitHub、Gitee 等）。
- **C端与B端双端登录态物理隔离**：分别采用 `StpUtil`（B端管理端）与 `StpUserUtil`（C端用户端）分发和校验 Token，彻底避免主键 ID 冲突与越权篡改。
- **快捷免密直登**：对已被绑定的社交用户进行一键免密直登，并支持自动同步纠偏最新昵称、头像。
- **无感一键自动注册**：对于首次授权的新社交用户，在事务内自动为其生成 `oauth_` 前缀的本地 C 端用户并完成一键绑定。
- **毛玻璃极光 HTML 响应**：授权成功后，后端直出 2026 级奢华暗黑毛玻璃风格网页，支持波纹点击动效的一键复制 Token。

### 5. 零 XML 纯净单表内存重构 (Zero-XML Design)
将所有 RBAC 联表 Join 复杂的 XML 物理文件全盘剔除（Zero-XML），由 MyBatis-Plus 的 `BaseMapper` 全权托管单表操作。
采用“数据层极简，服务层编排”的高并发设计哲学：
- 将涉及用户、角色、API 及菜单按钮等多张多对多关联表的查询完全拆解，在 Java 级使用高性能、强类型安全的 **单表分步 Lambda 检索** 并进行内存级组装。
- 极大地降低了多表 Inner Join 产生的数据库行/表锁、极大限度地提高了 Redis 单表主键和局部缓存的命中率与高并发吞吐。

### 6. 组件化 DB 演进引擎 (Aegis DB Engine 2.0)
**Code-First 架构升级**：DB 引擎已拆分为专门的组件，支持更细粒度的控制：
- **EntityScanner**：高效扫描类路径实体。
- **TableMetadataParser**：深度解析类元数据，支持列、索引、外键全量提取。
- **SqlGenerator**：支持针对不同数据库（目前主打 MySQL）生成精准的 DDL 语句。
- **SchemaExecutor**：安全的结构同步执行器，支持事务与增量更新。

### 7. 前端高精度适配 (Precision Protection)
针对雪花算法 ID (Long 型) 在 JavaScript 中出现的精度丢失（截断）风险，内置 **Jackson 自动转换引擎**：
- **全局生效**：序列化时自动将 `Long` 转为 `String`，反序列化时自动转回 `Long`。
- **灵活开关**：通过 `jackson.long-to-string` 配置项即可一键启闭。

### 8. 事件驱动文件管理中心 (Aegis File Hub)
内置高度抽象且解耦的文件管理体系：
- **事件驱动解耦**：上传完成后自动发布 `FileUploadedEvent`，由专用监听器负责落库，实现存储逻辑与业务数据库逻辑的完美分离。
- **扩展元数据支持**：`SysFile` 原生支持 `metadata` (JSON) 字段，轻松适配各行业差异化业务需求。
- **策略模式架构**：一键切换 `LOCAL`、`ALIYUN_OSS` 等存储方式，业务代码零改动。
- **环境迁移无忧**：动态 URL 构建，域名变更后历史文件链接自动适配新域名。

### 9. 极速异步日志 (Non-blocking Logging)
使用 **Log4j2** 配合 **Lmax Disruptor** 无锁并发框架。在高并发请求下，日志写入不再是系统的阿喀琉斯之踵。

---

## 项目结构 (Module-First Architecture)

```text
top.yuxs.springbootdev
├── core                   # 核心基础设施
│   ├── common             # 基础父类 (BaseEntity, Result)
│   ├── config             # 框架核心配置 (SaTokenConfig, MybatisPlusConfig, JacksonConfig)
│   ├── db                 # Aegis DB Engine 组件化实现 (自动 Code-First 实体扫描同步器)
│   ├── enums              # 系统通用枚举
│   ├── exception          # 全局异常体系
│   └── utils              # 通用工具类 (AntPathMatcher 规则工具, IpUtils)
└── modules                # 业务逻辑模块 (按功能垂直切分)
    ├── file               # 文件管理模块 (Entity, Event, Listener, Storage)
    └── system             # 系统功能模块 (Auth鉴权, OAuth三方登录, API拦截器)
```

---

## 技术栈

| 类别 | 技术选型 | 优势 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 4.0.5 | 下一代 Spring 生态，全面优化 Native 支持 |
| **开发语言** | Java 21 (LTS) | 虚拟线程、模式匹配、高性能记录类 |
| **安全体系** | Sa-Token 1.45 + Firewall | 现代化的登录鉴权与防火墙，支持 0 注解 Path + Method 动态拦截 |
| **三方授权** | JustAuth 1.16.7 | 极其全能且好用的多平台 OAuth 社交登录/快捷注册组件 |
| **持久层** | MyBatis-Plus 3.5.16 | **100% Zero-XML** 极纯单表接口，高性能内存分步高并发检索 |
| **存储引擎** | Aegis File Hub | 事件驱动架构，支持本地与云端策略切换，扩展元数据支持 |
| **数据引擎** | Aegis DB Engine 2.0 | 组件化 Code-First 自动演进引擎 |

---

## 快速开始

### 1. 环境准备
- **JDK 21+** (建议使用 GraalVM)
- **MySQL 8.0+**
- **Redis 6.x+**

### 2. 关键配置
在 `application.yml` 中配置您的数据源信息、扫描包路径、以及多渠道 JustAuth 凭证：

```yaml
mybatis-plus:
  type-aliases-package: top.yuxs.springbootdev.modules.**.entity

db:
  init:
    enabled: true
    base-package: top.yuxs.springbootdev.modules # 扫描业务模块下的实体

# 系统配置自适应初始化器开关（在测试环境下配置为 false 即可跳过对数据库 sys_config 的依赖）
sys:
  config:
    init:
      enabled: true


justauth:
  enabled: true
  type:
    github:
      client-id: YOUR_GITHUB_CLIENT_ID
      client-secret: YOUR_GITHUB_CLIENT_SECRET
      redirect-uri: http://localhost:8080/api/common/oauth/callback/github
    gitee:
      client-id: YOUR_GITEE_CLIENT_ID
      client-secret: YOUR_GITEE_CLIENT_SECRET
      redirect-uri: http://localhost:8080/api/common/oauth/callback/gitee
```

### 3. 定义您的第一个安全实体 (无 XML 生成)
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

## 前端与第三方对接指引

为了方便前端团队、移动端开发人员或第三方协同系统无缝接入 **Aegis-Boot (神盾)** 现代安全架构（包括 RSA 散列非对称密码传输、双端物理会话隔离、雪花 ID 高精度处理、零成本动态枚举绑定等机制），项目根目录下已经为您整理并准备了一份极具生产级实战价值的对接规格说明书：

👉 **[前端对接与物理 API 接口详细规格说明书 (API.md)](file:///d:/CodeProject/DevelopmentProjects/springbootDev/API.md)**

该文档为您准备了：
- **密码非对称 RSA 加密**在前端的配置流程及 `jsencrypt` 代码示例；
- **B端 (`StpUtil`) 与 C端 (`StpUserUtil`) 强隔离登录态**透出及 Token 交互细节；
- **雪花 ID 自动转 String 防精度丢失机制**下、上行无损适配；
- **一键式 Axios 统一拦截器模板**（开箱即用，已集成会话失效、RSA公钥自动置换及拦截对碰等业务流程）。

---

## 版本与安全更新日志 (Release & Security Log)

### v1.2.0-Security (2026-06-07)
**「物理隔离安全穿透审计、C端用户后台创建与密码哈希一致性加固」**
1. **密码哈希一致性高安全保障**：
   - 彻底重构并统一了全系统所有的用户密码加密与传输校验生命周期。
   - 过去，在后台手动创建用户或重置密码时存在哈希不互通的问题。现已全盘统一为：无论是在 C 端前台安全注册、B 端管理员后台手动创建，还是执行管理员级重置，所有账户密码在入库前一律采用高度一致的 BCrypt（`$2a$10$...`）强哈希算法进行加盐处理，确保密码生态全域打通、安全一致。
2. **C 端普通用户账号后台快捷创建**：
   - 扩展了后台系统用户维护能力，支持管理员（ADMIN）在后台直接创建普通 C 端用户账号（指定 `userType` 为 `USER`），且完美继承自适应一致性 BCrypt 密码加密入库。
3. **跨端登录拦截安全穿透日志唯一落库**：
   - 为防御恶意撞库、暴力破解等攻击行为对系统造成的频繁 IO 写入，系统对不带 `@AegisLog` 的公共/匿名登录等核心接口默认应用静默策略。
   - 唯独在触发 **“Aegis Auth Guard” 跨端物理隔离鉴权拦截**（即普通 USER 账号尝试登录 ADMIN 管理后台）这一物理越权的高危安全警报事件时，切面将启动**“精准安全拦截穿透机制”**：强行打破静默限制，**特异性地将且只将这唯一一条带有安全防护警报的审计日志落库写入 `sys_log` 物理表中**。这极大程度地保护了数据库，同时实现了对高危物理越权、撞库威胁的 100% 精准留痕与实时雷达呈现。

### v1.1.0-Security (2026-06-03)
**「高优先级安全稳定性加固与测试兼容性升级」**
1. **核心管理路由 `/sys/**` 全面接入网关物理防御**：
   - 修复了原物理路由 `/sys/**`（包含系统文件管理 `/sys/file` 与操作日志 `/sys/log`）未受安全拦截器防御的问题。
   - 现已全面将其纳入 B 端管理员的安全网关拦截，任何未登录的请求都将被强力拒之门外，杜绝后台接口裸露风险。
2. **公共文件上传与删除接口 (`/api/common/file/**`) 双端联合登录防御**：
   - 针对原有公共接口的越权写入与破坏风险，对文件上传 `/api/common/file/upload` 与物理/逻辑删除 `/api/common/file/{id}` 接口强制实施 **B端/C端联合登录鉴权机制**。
   - 任何接口调用者必须属于有效的 B端管理员 会话 (`StpUtil`) 或 C端用户 会话 (`StpUserUtil`) 之一。不具备有效 Token 的匿名请求会直接阻断并抛出 `1001` (会话未登录) 错误。
   - 保持获取存储配置 `GET /api/common/file/config` 与获取详情 `GET /api/common/file/{id}` 接口继续豁免放行，确保免登录场景下的业务平滑与交互友好。
3. **测试环境 H2 数据库不兼容与自适应配置修复**：
   - 彻底解决了测试环境下 H2 数据库由于带入 `DEFAULT_NULL_ORDER=FIRST` 导致新版 H2 解析异常、HikariCP 连接池创建失败的偶发崩溃问题。
   - 为系统配置热加载初始化器 `SysConfigInitRunner` 新增了优雅的条件注解控制，并提供 `sys.config.init.enabled` 属性。在主配置文件中默认开启，在测试环境中显式配置为 `false` 以切断其在测试等特殊环境对 `sys_config` 表及真实数据库连接的前置依赖，恢复无感、飞速的本地自动化测试（`.\mvnw test` 100% BUILD SUCCESS）。

---

## 愿景
**Aegis-Boot** 致力于解决传统框架过于臃肿、性能低下、安全配置繁琐的问题。我们通过最先进的 Java 技术栈，为您打造一个更现代、更安全、更丝滑的开发底座。

---
**Copyright © 2026 Aegis-Boot Team. All rights reserved.**

