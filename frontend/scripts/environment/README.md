# 本机真实 MySQL 测试环境脚本

`python3 scripts/create-test-environment.py` 生成全新时间戳目录、数据库和绝对上传路径，运行原后端测试及构建，调用 PasswordUtil 初始化，再用 HTTP 创建演示数据。账号密码随机生成，仅写本机 private/accounts.json。

脚本复用本机已定位的 JDK、Maven 缓存与原专用 MySQL 数据目录，路径见 runtime.py。凭据来源可用 RC_CREDENTIAL_CONFIG 指向本机 Spring 配置，不能把密码写进命令行。首次组织/管理员由 Bootstrap.java 初始化，剩余资料通过真实 API。

安全约束：已有 MySQL socket、已有数据库、非空用户库会拒绝运行，不主动停止其他服务。当前演示环境保持运行时，不要重复运行建库脚本。若将来有意结束当前环境，先使用当前 run 目录中的 stop.py，之后再创建新环境。已有报告与旧库不删除。

新环境创建后设置 RUN_DIR 为输出路径运行 Playwright。生成器会在 run 目录留下可直接执行的 probe.py 和 stop.py 包装脚本。
