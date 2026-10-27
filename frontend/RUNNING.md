# 本机运行中的演示环境

- 前端：http://127.0.0.1:15173/login，Vite PID 32206（npm 启动进程 32189）。
- 后端：http://127.0.0.1:18127，Java PID 31249。
- 专用 MySQL 8.0.44：PID 30866，端口 13327。
- 日志目录：`/home/tsar413/桌面/Java_Project/reward-collection/frontend_run_20261027_153619/logs/`。
- 本机私有演示账号：`evidence/private/DEMO_ACCOUNTS.md`（不在公开交付包中）。
- 公开验收：`evidence/REPORT.md`、`evidence/index.html`；36 项最终均通过，首轮失败和复测均保留。

停止本轮全部服务（不会删除数据库和文件）：

```bash
python3 /home/tsar413/桌面/Java_Project/reward-collection/frontend_run_20261027_153619/stop.py
```

当前保留 2 个组织、3 位教师、4 个班级、9 名学生（6 名基线 + 3 名有效导入）、11 张演示奖状。请不要把本机私有账号、trace 或录像上传到公共仓库。
