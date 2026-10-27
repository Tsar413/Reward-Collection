# Reward-Collection｜奖状管理系统

面向学校、教师和学生的奖状管理系统。统一管理组织、班级、人员、个人奖状和班级奖状，为每份奖状保存文字资料与一张图片。

项目最初服务于个人和班级自用，同时保留多组织管理与后续推广的业务结构。当前阶段支持手工填写奖状信息、图片上传与管理；OCR 为第二阶段目标。

- 项目仓库：[Tsar413/Reward-Collection](https://github.com/Tsar413/Reward-Collection)
- 后端核对基准：`59c4d3d8df5d13e8748c2eb83601b6473bb8ca62`
- 应用形态：Spring Boot 后端 + Vue 单页前端 + MySQL + 独立图片目录
- 界面语言：中文

> 本文前端启动约定与验收摘要依据随项目交付的前端 README 和 RUNNING.md。更新版本后，应以实际源码、`frontend/.env.example`、`frontend/package-lock.json` 及本轮验收报告为准。验收记录不代表已经部署公网。

## 目录

- [主要功能](#主要功能)
- [角色与权限](#角色与权限)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [数据与业务关系](#数据与业务关系)
- [快速开始](#快速开始)
- [首次初始化](#首次初始化)
- [认证与接口约定](#认证与接口约定)
- [图片管理](#图片管理)
- [Excel 批量导入](#excel-批量导入)
- [前端页面](#前端页面)
- [测试与验收](#测试与验收)
- [生产部署参考](#生产部署参考)
- [当前限制与后续计划](#当前限制与后续计划)
- [常见问题](#常见问题)

## 主要功能

- 管理组织、教师、班级、学生及其关联关系。
- 支持管理员跨组织查看和维护资料。
- 支持一位教师负责多个班级。
- 支持学生单个注册及 Excel 批量导入。
- 支持个人奖状、班级奖状的创建、详情、文字修改与删除。
- 支持奖状图片上传、按需查看和独立替换。
- 按管理员、教师、学生提供独立布局、导航和操作入口。
- 学生端采用适配手机的文字卡片或列表。
- 提供加载、空数据、错误、提交中、删除确认及导入部分失败提示。

## 角色与权限

| 功能 | 管理员 | 教师 | 学生 |
| --- | --- | --- | --- |
| 组织 | 跨组织查看、修改名称和备注、删除 | 读取授权范围资料 | 读取授权范围资料 |
| 教师 | 新增、查询、修改、状态管理、删除 | 本人资料 | 无管理入口 |
| 班级 | 新增、查询、修改、删除 | 只读本人负责班级 | 读取本人所属班级资料 |
| 学生 | 跨组织管理、调班、停用、删除、导入 | 管理本人负责班级内学生 | 仅本人资料 |
| 个人奖状 | 跨组织管理 | 管理负责班级学生奖状 | 管理本人奖状 |
| 班级奖状 | 跨组织管理 | 管理本人负责班级奖状 | 只读当前班级奖状 |
| 本人资料 | 修改姓名与备注 | 修改姓名与备注 | 修改姓名与备注 |

权限不仅由前端菜单控制。后端 `DataScopeService` 决定数据可见范围和写入权限；直接访问接口同样需要通过认证与数据范围检查。

当前组织新增接口主动返回 **403**，前端不提供可用的“新增组织”按钮。组织修改 DTO 没有 `status` 字段，因此不提供组织停用功能。

教师、学生修改本人资料时，保留后端完整更新 DTO 所需的原班级和原状态，不允许借此更改身份或归属。

## 技术栈

### 后端

| 组件 | 说明 |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.1 |
| Maven | 后端依赖与构建 |
| MySQL | 8.x，实际验收使用真实 MySQL |
| MyBatis-Plus | 3.5.16，业务数据访问与数据范围查询 |
| Spring Data JPA / Hibernate | 实体映射与当前开发环境表结构维护 |
| Apache POI | 5.5.1，Excel 读取 |
| TwelveMonkeys ImageIO WebP | 3.12.0，WebP 解码支持 |
| PBKDF2WithHmacSHA256 | 密码派生与校验 |

后端包含 Thymeleaf、邮件及 Redis 等依赖；本阶段核心页面由 Vue 承担，不将这些依赖的存在等同于已实现对应业务功能。

### 前端

- Vue 3、Vite、TypeScript。
- Vue Router、Pinia、Axios。
- Element Plus。
- Playwright + Chromium。

前端依赖以交付版本的 `package.json` 和 `package-lock.json` 为准，使用 `npm ci` 安装。本次随附前端说明要求 **Node.js ≥22.12**，记录的构建环境为 Node.js 24.22.1。

## 项目结构

```text
Reward-Collection/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/study/rewardCollection/
│   │   │   ├── controller/       HTTP 接口
│   │   │   ├── dto/              请求和响应对象
│   │   │   ├── entity/           数据实体
│   │   │   ├── mapper/           MyBatis 数据访问
│   │   │   ├── service/          业务接口与实现
│   │   │   ├── security/         数据范围控制
│   │   │   ├── interceptor/      登录拦截
│   │   │   ├── exception/        全局异常处理
│   │   │   └── util/             密码、Excel、图片工具
│   │   └── resources/
│   │       └── application.yml
│   └── test/                    后端测试
├── frontend/
│   ├── src/                     Vue 页面、布局、组件与状态
│   ├── tests/                   Playwright 用例与素材
│   ├── docs/                    前端接口和问题说明
│   ├── scripts/                 测试环境辅助程序
│   ├── evidence/                验收摘要及公开/私有证据
│   ├── .env.example
│   ├── package.json
│   ├── package-lock.json
│   └── README.md
└── README.md
```

图片文件保存在通过配置指定的独立目录中，不以 Base64 存入奖状列表，也不放在前端 `public/` 中公开访问。

## 数据与业务关系

| 表 | 主要用途 |
| --- | --- |
| `sys_org` | 组织编号、名称、状态与备注 |
| `sys_class` | 所属组织、班级编号、名称、入学年份、负责教师、状态 |
| `sys_user` | 学号/工号、姓名、角色、所属组织/班级、密码派生资料、状态 |
| `sys_login_token` | 登录 Token 的哈希、所属用户、过期与撤销时间 |
| `award_record` | 奖状类型、归属、文字信息、上传人与修改人 |
| `award_image` | 奖状对应图片的存储键、原文件名、类型、尺寸、大小与修改人 |

业务约束：

1. 一个组织包含多个班级和用户。
2. 一个班级有一位负责教师；同一教师可以负责多个班级。
3. 学生归属于一个组织和班级；教师、管理员不使用学生班级归属。
4. 奖状分为 `PERSONAL` 和 `CLASS`。
5. 个人奖状关联学生，班级和组织由后端根据学生归属确定。
6. 班级奖状关联班级，不设置获奖学生。
7. 一份奖状对应一张图片。
8. 奖状文字更新不能改变类型、组织、班级或获奖学生。
9. 删除组织、班级或用户时，后端会检查相关业务数据；存在依赖时可能拒绝删除。

`userNo` 用于登录。项目使用场景要求学号/工号全局不重复，但当前注册判重仍存在组织范围规则，见[当前限制与后续计划](#当前限制与后续计划)。

## 快速开始

### 1. 准备环境

- Java 21，包含 `java` 和 `javac`。
- Maven。
- MySQL 8.x。
- Node.js ≥22.12 和 npm。
- 可写的独立图片目录。
- 运行浏览器验收时，安装 Chromium 及 Playwright 所需 Linux 依赖。

```bash
java -version
mvn -version
mysql --version
node --version
npm --version
```

### 2. 获取项目

```bash
git clone https://github.com/Tsar413/Reward-Collection.git
cd Reward-Collection
git rev-parse HEAD
```

前后端交付包应放在同一项目目录中；若当前 GitHub 分支尚未包含 `frontend/`，先合入对应前端交付文件，再执行前端命令。不要将本机私有验收数据一起提交。

### 3. 创建独立数据库

以下以开发库为例，在 MySQL 管理终端执行：

```sql
CREATE DATABASE reward_collection_dev
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

创建仅可访问该库的应用账号，并设置自己的强密码。不要在 README、源码或提交记录中写入真实数据库口令。

当前 `application.yml` 使用 Hibernate `ddl-auto: update`，启动时会依据实体维护表结构；数据库及应用账号仍需提前建立。正式上线前应改为经过审查的数据库迁移流程，避免让应用自动变更生产表结构。

### 4. 配置并启动后端

支持通过 Spring Boot 环境变量覆盖仓库配置：

| 变量 | 含义 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC 地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库应用账号 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库口令 |
| `AWARD_IMAGE_ROOT` | 奖状图片目录，建议使用绝对路径 |
| `SERVER_PORT` | 后端监听端口 |

Linux 示例，数据库账号和口令通过终端输入：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/reward_collection_dev?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia%2FShanghai'
read -r -p '数据库账号: ' SPRING_DATASOURCE_USERNAME
export SPRING_DATASOURCE_USERNAME
read -r -s -p '数据库密码: ' SPRING_DATASOURCE_PASSWORD
printf '\n'
export SPRING_DATASOURCE_PASSWORD

export AWARD_IMAGE_ROOT="$HOME/reward-collection-data/dev/awards"
export SERVER_PORT=18127
mkdir -p "$AWARD_IMAGE_ROOT"

mvn -DskipTests package
java -jar target/reward-collection.jar
```

`-DskipTests` 仅用于此处打包启动，不代表测试已经通过。测试需要连接专用测试数据库，见后文。

仓库后端原始默认端口为 **8101**；上述示例覆盖为 **18127**，便于与随附演示环境说明保持一致。端口被占用时应选择其他空闲端口，不要停止不属于本项目的服务。

首次访问受保护接口返回 401 是未登录时的正常行为，不代表后端未启动。

### 5. 配置并启动前端

另开终端：

```bash
cd frontend
npm ci
cp .env.example .env.local
```

按随附前端版本的约定，在 `.env.local` 中配置：

```dotenv
API_PROXY_TARGET=http://127.0.0.1:18127
```

然后启动：

```bash
npm run dev
```

随附前端说明记录的默认地址：

```text
http://127.0.0.1:15173/login
```

所有业务请求使用相对路径 `/api`，由 Vite 代理到后端。若后端端口发生变化，应同步修改代理配置并重启 Vite。若使用其他前端交付版本，以它的 `.env.example` 中实际变量名和终端显示的监听地址为准。

### 6. 类型检查与生产构建

```bash
npm run typecheck
npm run build
```

生产静态文件输出到 `frontend/dist/`。开发服务器能够打开，不等于类型检查或生产构建已经通过。

## 首次初始化

一个全新的数据库没有可用于登录的管理员，也没有组织。当前组织新增接口又主动返回 403，因此首次初始化必须通过受控的一次性初始化程序完成。

推荐顺序：

1. 在目标环境建立组织。
2. 创建首位管理员，并关联组织。
3. 使用管理员登录。
4. 创建教师。
5. 创建班级并分配负责教师。
6. 注册或导入学生。
7. 创建个人奖状和班级奖状。

初始化管理员密码必须调用项目的 `PasswordUtil`，不能直接把明文或普通 SHA256 写入 `password_hash`：

```java
String salt = PasswordUtil.generateSalt();
String passwordHash = PasswordUtil.hashPassword(initialPassword, salt);
String algorithm = PasswordUtil.ALGORITHM;
```

持久化时分别保存盐、派生哈希与算法标识。当前算法为 PBKDF2WithHmacSHA256，默认迭代 600,000 次，输出 256 位。

测试交付中的 `Bootstrap.java` 和环境准备脚本用于合成数据验收；正式初始化时应独立配置，不复用演示密码或测试组织。

## 认证与接口约定

### 登录

```http
POST /api/sys-login/login
Content-Type: application/json
```

```json
{
  "userNo": "<学号或工号>",
  "password": "<用户输入的密码>"
}
```

不要求组织编号或角色选择。有效登录响应位于 `data` 中，包含 `token` 和 `user`，前端根据 `data.user.role` 进入对应工作台。

后续受保护请求携带：

```http
Authorization: Bearer <TOKEN>
```

Token 是由后端校验的登录凭证，不按 JWT 解析。随附前端将会话保存在当前标签页 `sessionStorage`；刷新后调用 `check` 并重新读取本人资料，不依靠浏览器时间判断过期。

| 接口 | 用途 |
| --- | --- |
| `POST /api/sys-login/login` | 登录 |
| `GET /api/sys-login/check` | 校验 Token |
| `POST /api/sys-login/logout` | 登出并撤销当前登录凭证 |

当前后端设置登录有效期为三天，并为每个用户维护一条登录记录；同一账号再次登录会使旧 Token 失效，测试与演示时应避免并行使用同一账号。

### 主要业务接口

| 模块 | 方法与路径 | 用途 |
| --- | --- | --- |
| 组织 | `GET /api/sys-org/all` | 可见组织列表 |
| 组织 | `GET /api/sys-org/one/{id}` | 组织详情 |
| 组织 | `PUT /api/sys-org/update/{id}` | 修改名称与备注 |
| 组织 | `DELETE /api/sys-org/delete/{id}` | 依赖检查后删除 |
| 班级 | `GET /api/sys-class/all` | 可见班级列表 |
| 班级 | `GET /api/sys-class/all/{orgId}` | 按组织查询班级 |
| 班级 | `GET /api/sys-class/one/{id}` | 班级详情 |
| 班级 | `POST /api/sys-class/add` | 新增班级 |
| 班级 | `PUT /api/sys-class/update/{id}` | 修改班级 |
| 班级 | `DELETE /api/sys-class/delete/{id}` | 依赖检查后删除 |
| 用户 | `GET /api/sys-user/all` | 可见用户列表 |
| 用户 | `GET /api/sys-user/one/{id}` | 用户详情 |
| 用户 | `GET /api/sys-user/org/{orgId}` | 按组织查询用户 |
| 用户 | `GET /api/sys-user/class/{classId}` | 按班级查询用户 |
| 用户 | `POST /api/sys-user/registration-one` | 单个注册 |
| 用户 | `POST /api/sys-user/import` | Excel 批量导入学生 |
| 用户 | `PUT /api/sys-user/update/{id}` | 修改资料、状态或允许调整的班级 |
| 用户 | `DELETE /api/sys-user/delete/{id}` | 依赖检查后删除 |
| 奖状 | `GET /api/award-record/all` | 可见奖状文字列表 |
| 奖状 | `GET /api/award-record/classes/{classId}` | 按班级查询奖状 |
| 奖状 | `GET /api/award-record/students/{studentUserId}` | 按学生查询奖状 |
| 奖状 | `GET /api/award-record/one/{id}` | 奖状文字详情 |
| 奖状 | `POST /api/award-record/add` | 同时新增文字与图片 |
| 奖状 | `PUT /api/award-record/update/{id}` | 仅修改文字资料 |
| 奖状 | `DELETE /api/award-record/delete/{id}` | 删除奖状及关联图片 |
| 图片 | `GET /api/award-image/one/{awardId}` | 获取图片二进制 |
| 图片 | `PUT /api/award-image/upload/{awardId}` | 替换图片 |

DTO 字段、校验规则与实际数据范围以对应 Controller、DTO、Service 和 `DataScopeService` 为准。

### 响应与错误

普通接口通过 `Result` 返回结果，有效内容在 `data` 层。新增用户、班级和奖状通常返回整数 `1`，前端保存成功后重新查询列表，不将其作为新记录 ID。

- 用户、班级和奖状详情缺失或不可见时，可能返回 `200 + data:null`。
- 组织详情缺失返回 404。
- 图片成功返回二进制，失败可能返回 JSON，需要从 Blob 中解析错误消息。
- 400、401、403、404、409、500 应分别呈现合适的错误提示。
- 401 清理登录状态并跳转登录，保留安全的站内返回地址。
- 403 表示无权执行操作，不应误判为未登录。
- Excel 导入的 HTTP 200 不代表所有行都成功。

## 图片管理

### 加载策略

奖状列表只加载文字资料，不显示缩略图，不预取图片，也不将图片转为 Base64 混入列表。

用户打开详情后：

1. 获取奖状文字详情。
2. 确认记录存在且可访问。
3. 携带 Bearer Token 单独请求图片。
4. 以 Blob 接收，通过 Object URL 展示。
5. 关闭、切换详情或卸载组件时取消旧请求并释放 URL。

前端通过 `AbortController` 和请求序号共同避免迟到响应覆盖当前图片。图片有独立加载、失败和重试状态。

### 新增与替换

新增奖状使用 `multipart/form-data`：

- `data`：`application/json` 类型的 Blob。
- `file`：图片文件。

由浏览器自动生成 multipart boundary，不手工设置。文字修改与图片替换分开操作，替换失败时保留原图；替换成功后重新获取当前图片。

支持 JPEG、PNG、WebP，单张上限 **5MiB**。前端检查扩展名、MIME 和大小，后端继续校验真实文件内容、解码结果与图片尺寸。

获奖日期始终提交 `yyyy-MM-dd` 字符串，避免转换成 UTC 时间戳造成日期偏移。

图片根目录不直接映射为公开静态目录。查看图片必须通过受保护的后端接口。

## Excel 批量导入

仅用于批量导入学生，由管理员或具有相应班级权限的教师操作。

第一行表头按以下顺序填写：

| 学号 | 姓名 | 班级编号 |
| --- | --- | --- |
| SYNTH_STUDENT_001 | 示例同学 | SYNTH_CLASS_01 |

- 支持 `.xls`、`.xlsx`。
- 文件最大 5MiB，每次最多处理 1000 行学生数据。
- 班级必须事先建立，且处于操作人允许管理的范围内。
- 学号、班级编号建议设置为 Excel 文本，避免前导零丢失或长数字精度变化。
- 学生采用后端统一初始密码策略，具体账号交接信息放在私有说明中。
- 导入页面展示总数、成功数、跳过数、失败数及逐行原因。
- 部分失败后先修正失败行，不要将整份文件重复导入的结果当作首次导入统计。

## 前端页面

| 布局 | 路径 | 页面 |
| --- | --- | --- |
| 共用 | `/login` | 登录 |
| 管理员 | `/admin/orgs` | 组织管理 |
| 管理员 | `/admin/teachers` | 教师管理 |
| 管理员 | `/admin/classes` | 班级管理 |
| 管理员 | `/admin/students` | 学生管理 |
| 管理员 | `/admin/awards`、`/admin/class-awards` | 个人与班级奖状管理 |
| 教师 | `/teacher/classes` | 我的班级 |
| 教师 | `/teacher/students` | 负责班级的学生管理 |
| 教师 | `/teacher/awards`、`/teacher/class-awards` | 学生个人奖状、班级奖状 |
| 学生 | `/student/awards` | 我的奖状 |
| 学生 | `/student/class-awards` | 本班班级奖状，只读 |
| 各角色 | `/{role}/profile` | 个人资料 |

`{role}` 为小写 `admin`、`teacher` 或 `student`。同一前端工程使用三个独立布局，共用认证状态、API 封装和通用组件。

管理员按组织筛选班级、教师、学生与奖状；选择组织后，关联下拉只展示对应范围的资料。修改组织选择时清除不匹配的旧班级或教师。

后端尚未提供服务端分页，当前筛选与分页在前端完成。名称仅使用当前角色可见的资料解析，无法读取名称时显示编号，不伪造名称。

## 测试与验收

### 后端测试

先配置专用测试数据库和绝对图片目录，再执行：

```bash
mvn test
```

测试不得连接原业务数据库或使用原图片目录。原有 JUnit 测试不能替代完整项目的接口和浏览器验收。

### Playwright 真实浏览器

随附前端验收说明的复跑方式：

```bash
cd frontend
npm ci
npx playwright install chromium
E2E_RUN=regression-$(date +%Y%m%d-%H%M%S) npm run test:e2e
```

Linux 桌面关键流程可有界面运行：

```bash
HEADED=1 E2E_RUN=headed-$(date +%Y%m%d-%H%M%S) \
  npx playwright test --grep 'A01|D01'
```

- `E2E_BASE_URL`：测试访问的前端地址。
- `RUN_DIR`：独立测试环境目录，包含本轮环境准备和核对脚本。
- 每轮使用不同 `E2E_RUN`，保留首次失败和后续复测证据。
- 本机账号及前置数据位于 `frontend/evidence/private/`，不写入公开文档。
- 新环境准备参考交付的 `frontend/scripts/create-test-environment.py` 和 `frontend/scripts/environment/README.md`；具体参数以该交付版本说明为准。

核心验收要求包括：三角色登录与权限、真实 CRUD、Excel 部分成功与 MySQL 一致性、奖状图片按需加载、换图前后摘要或像素核对、快速切换防串图、移动端布局、错误处理及提交保护。

测试使用真实 Chromium 页面、真实 API 和真实 MySQL，不以 Mock、`route.fulfill` 或关闭 Token 校验代替联调。

### 已有验收记录

随附 `RUNNING.md` 记录的虚拟机验收环境为 MySQL 8.0.44，**36 项最终通过**，并保留首轮失败与复测记录。该数字属于已有交付记录；新增代码或环境变更后应重新运行，不能沿用旧结果证明新版本通过。

详细结果以随交付提供的下列文件为准：

- [公开验收报告](frontend/evidence/REPORT.md)。
- [公开证据索引](frontend/evidence/index.html)。
- [前端运行与测试说明](frontend/README.md)。
- [接口约定](frontend/docs/API_CONTRACT.md)。
- [后端问题记录](frontend/docs/BACKEND_ISSUES.md)。

原始 HTML 报告、JSON、trace、录像及 storageState 可能含认证信息，只在 `evidence/private/` 私有保存并排除 Git。公开报告、截图和网络时序需要脱敏，不能输出真实凭据、完整 Token、密码派生字段或私有账号文件。

本机运行端口、PID、日志路径和停止方法放在独立 `RUNNING.md` 中，避免将某次临时环境信息固定为长期项目配置。

## 生产部署参考

以下是部署准备说明，本项目不因执行开发启动命令而自动部署公网。

1. 在目标环境配置 MySQL 和独立图片目录。
2. 使用受控数据库迁移及一次性管理员初始化流程。
3. 构建并启动后端 JAR。
4. 执行 `npm ci`、类型检查和 `npm run build`。
5. 使用 Nginx 等 Web 服务器托管 `frontend/dist/`。
6. 将 `/api` 反向代理到后端，保留 Authorization 请求头。
7. 为 Vue Router history 模式配置回退到 `index.html`。
8. 根据环境配置 HTTPS、访问控制、备份与日志策略。

Nginx 路由示例，需替换实际路径和后端端口：

```nginx
server {
    listen 80;
    server_name example.invalid;

    root /srv/reward-collection/frontend/dist;
    index index.html;
    client_max_body_size 6m;

    location /api/ {
        proxy_pass http://127.0.0.1:18127;
        proxy_set_header Host $host;
        proxy_set_header Authorization $http_authorization;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`proxy_pass` 保留原请求的 `/api` 前缀。上例没有公开图片目录；图片仍由后端授权读取。

`vite preview` 仅适合检查构建产物，不替代正式 Web 服务和生产 API 反向代理。生产环境还应关闭不必要的 SQL 参数输出，防止认证派生资料进入共享日志；数据库与图片需要配套备份和恢复验证。

## 当前限制与后续计划

### 当前限制

- 组织新增 API 暂不对页面开放，组织初始化由受控程序完成。
- 当前组织更新不支持状态修改。
- 注册的 `userNo` 判重范围与不带组织号的登录方式存在后置一致性问题；现阶段由使用规则保证全局唯一，尚未在本轮擅自修改后端规则。
- 列表没有服务端分页，大规模数据场景需补充分页、筛选和排序接口。
- 不提供后端尚不存在的修改密码、密码重置等页面。
- 数据归属字段不通过奖状修改表单开放调整。
- 当前不是具有完整租户隔离、组织自助开通和运营体系的公开 SaaS 产品；对外推广前仍需完善组织接入、全局账号规则、运维与审计。

### OCR 二期

计划在现有图片和手工录入流程上增加：

- OCR 训练数据准备与模型训练。
- 异步识别任务及状态查询。
- 候选字段、置信度与人工确认。
- 模型版本、失败原因及必要的识别记录。
- 同样受组织、班级和用户数据范围约束的识别接口。

识别结果应由用户确认后写入正式奖状资料。当前不提供假 OCR 结果，也不将识别能力描述为已经完成。

## 常见问题

### 为什么登录不选择组织和角色？

登录只使用学号/工号与密码。角色由后端登录响应确定；组织是用户资料的一部分。

### 为什么管理员看不到新增组织或组织停用按钮？

当前新增组织接口主动返回 403，组织更新 DTO 没有状态字段。前端按实际接口能力提供操作。

### 为什么一个教师能出现在多个班级？

业务允许一位教师负责多个班级。班级保存自己的 `teacherUserId`，不将教师强制限制为只能管理一个班。

### 为什么列表没有奖状图片？

这是按需加载策略。打开详情后才通过带 Token 的图片接口取图，减少列表流量并保护图片访问范围。

### 图片替换失败，原图是否会丢失？

前端在失败时保留原图；实际数据库和文件替换的一致性由后端处理。查看错误提示，修正图片格式、大小或权限后重试。

### 为什么导入接口返回 200，仍显示失败行？

HTTP 请求成功和每一行导入成功是两层结果。页面会展示成功、跳过、失败数量及逐行原因。

### 为什么刷新页面后需要重新校验 Token？

Token 是否有效、是否到期或撤销由后端判断。前端不能用本地时间或缓存资料代替校验。

### 为什么其他终端再次登录后，原页面被要求重新登录？

当前后端按用户维护登录记录，新登录会替换旧凭证。演示、自动化验收和日常操作应使用不同合成账号，避免相互影响。

### 可以把测试证据全部上传 GitHub 吗？

不能直接上传原始私有证据。trace、录像、storageState、账号清单和日志可能包含认证信息。仅提交已脱敏的报告、截图、公开索引和可复跑测试代码。

### 开源协议是什么？

本说明不擅自指定开源许可证。公开仓库可访问不代表已授予任意使用、修改或再分发许可；具体以仓库实际 LICENSE 文件为准。
