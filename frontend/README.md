# 荣誉册 Vue 前端

面向 Reward-Collection 后端的中文单页应用。后端基准 `59c4d3d8df5d13e8748c2eb83601b6473bb8ca62`，本轮未修改后端源码及 pom。

## 启动与构建

本轮使用 Node 24.22.1、npm 11.19.1；依赖精确版本写入 package.json 和 package-lock.json。要求 Node >=22.12。

```bash
cd frontend
npm ci
cp .env.example .env.local
npm run dev
npm run typecheck
npm run build
```

Vite 默认仅监听 `127.0.0.1:15173`。`.env.local` 中的 `API_PROXY_TARGET` 指向后端，如 `http://127.0.0.1:18127`；变更后重启 Vite。所有业务请求使用 `/api`，由开发代理连接后端，图片目录不公开。

`dist/` 是生产构建产物。生产环境需要 Web 服务器设置 history 回退到 index.html，并将 `/api` 反向代理到受保护后端。`vite preview` 只适合检查静态构建，不能替代生产反向代理。未部署公网。

## 页面清单

| 角色布局                                   | 页面     | 功能                                                  |
| ------------------------------------------ | -------- | ----------------------------------------------------- |
| `/login`                                   | 登录     | 用户编号、密码；自动根据后端角色进入工作台            |
| `/admin/orgs`                              | 组织     | 列表、详情、名称/备注编辑、依赖保护删除               |
| `/admin/teachers`                          | 教师     | 跨组织筛选、新增、编辑、状态与删除                    |
| `/admin/classes`                           | 班级     | 组织筛选、新增、详情、编辑、状态、删除；教师联动      |
| `/admin/students`                          | 学生     | 组织/班级筛选、注册、编辑、调班、停用、删除、Excel    |
| `/admin/awards`、`/admin/class-awards`     | 奖状     | 跨组织个人/班级奖状，新增、详情、文字编辑、换图、删除 |
| `/teacher/classes`                         | 我的班级 | 只读本人负责班级，可切换多个班级                      |
| `/teacher/students`                        | 学生     | 只管理本人负责班级，调班范围同样受限                  |
| `/teacher/awards`、`/teacher/class-awards` | 奖状     | 负责班级的个人/集体奖状管理                           |
| `/student/awards`                          | 我的奖状 | 文字卡片，新增本人奖状、编辑、换图、删除              |
| `/student/class-awards`                    | 班级荣誉 | 本班奖状只读，图片按需加载                            |
| `/{role}/profile`                          | 个人资料 | 修改姓名与备注，保留原状态、原班级                    |

三个独立布局组件共用 Shell、认证 Store、API 封装和表单/图片组件。前端按角色隐藏入口；服务端 DataScopeService 始终决定实际数据权限。教师/学生修改资料前重新读取本人，提交后端要求的完整更新 DTO。

后端暂未提供服务端分页：列表从可见范围接口加载，筛选与每页 10 条分页在前端完成。名称只从当前账号可见的组织、班级、用户解析，无法读取时显示 `#编号`。

## 认证与图片

- Token 存在当前标签页 sessionStorage。刷新调用 `/sys-login/check`，再查询本人资料；不解析 Token 或用前端时间判断过期。
- 401 清理会话并保存同角色站内返回地址；403 显示权限错误，不清理登录。登出先调用后端。
- 奖状列表没有图片标签或预取。详情文字存在后才请求 `/award-image/one/{awardId}`，Bearer + Blob + Object URL。
- AbortController 和请求序号共同防止迟到响应覆盖；关闭、切换、卸载释放 Object URL。旧图片在非法替换失败后继续可见。
- 新增的 multipart `data` 是 application/json Blob；不设置 boundary。更新文字仅发送允许修改字段，换图使用专用 PUT。
- 图片先校验 MIME、扩展名和 5MiB；最终由后端解码验证。日期保留 yyyy-MM-dd 字符串，没有 UTC 转换。
- 不存在记录的 `200 + data:null` 会显示不可访问；图片 JSON 错误从 Blob 解码，保留后端 message。

## 真实浏览器验收

本机私有合成账号清单：`evidence/private/accounts.json`；前置数据 ID：`evidence/private/seed.json`。二者均不入 Git，不能公开。已有环境运行时：

```bash
npm ci
npx playwright install chromium
# 每次使用全新 E2E_RUN，保留前次证据
E2E_RUN=regression-$(date +%Y%m%d-%H%M%S) npm run test:e2e
# Linux 桌面有界面关键流程
HEADED=1 E2E_RUN=headed-$(date +%Y%m%d-%H%M%S) npx playwright test --grep 'A01|D01'
```

`E2E_BASE_URL` 可改访问地址，`RUN_DIR` 指向本轮独立后端测试目录（包含 runtime.py、probe.py）。本机默认目录位于仓库外的 `frontend_run_20261027_153619`，里面的 prepare.py、seed.py 和 Bootstrap.java 保存完整建库、启动与初始化步骤。后续新建环境可参考 scripts/create-test-environment.py 和 scripts/environment/README.md。新建环境时换时间戳库名及上传目录；禁止在原业务库执行。

用例使用真实 Chromium 页面操作及真实 API 准备数据，不使用 Mock、route.fulfill 或跳过 Token。图片异常测试只短暂移动独立测试目录内的图片，并在 finally 恢复。它们不能与演示数据修改、另一轮验收并行运行（同一账号再次登录会使旧 Token 失效）。

原始 Playwright HTML、JSON、trace、失败视频含请求认证信息，全部保存在 `evidence/private/<run>/`，父目录权限 700 且被 Git 忽略。公开脱敏网络时序、截图和摘要在 `evidence/public/<run>/`。HTML 原始报告可用 `npx playwright show-report evidence/private/<run>/html` 本机查看，不应上传共享服务。

参阅 `docs/API_CONTRACT.md`、`docs/BACKEND_ISSUES.md` 和最终 `evidence/REPORT.md`。
