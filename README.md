# 高校宿舍智能报修管理系统

Spring Boot + Vue 前后端分离报修系统，支持学生报修、管理员审核派单、维修人员处理、学生确认评价，以及用户、宿舍、公告、日志和统计管理。本次补充针对课程个人实验一、二、四；未加入实验三 Git/CI 或实验五 LLM/RAG。

## 环境

- JDK 17（须确认 `java -version` 为17或更高的兼容版本）、Maven 3.8+。
- MySQL 8+；本次真实验证版本见 `docs/验证结果.md`。
- Node.js 20.19+ 或22.12+、npm；前端依赖版本由 package-lock.json 锁定。
- 前后端请统一使用 `localhost`，避免与127.0.0.1混用导致 Cookie 跨站问题。

## 数据库

首次启动时，对独立空数据库执行：

```bash
mysql -u root -p < database/init.sql
```

**init.sql 会删除并重建同名表，不能用于保留业务数据的升级。** 已有旧系统数据库，只需执行：

```bash
mysql -u root -p dorm_repair < database/migrations/002_permissions.sql
```

迁移添加 permission、role_permission 及初始授权，可重复执行。

## 后端

应用从环境读取密码和JWT密钥，不会自动加载 `.env`；`.env.example` 仅列出变量示例。

macOS/Linux：

```bash
export JAVA_HOME=/your/jdk17/home
export PATH="$JAVA_HOME/bin:$PATH"
export DB_USER=root
export DB_PASSWORD='你的本机数据库密码'
export JWT_SECRET="$(openssl rand -base64 48)"
# 默认 DB_URL 连接 localhost:3306/dorm_repair；端口不同时设置完整连接串
bash scripts/start-backend.sh
```

Windows PowerShell：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_USER = 'root'
$env:DB_PASSWORD = '你的本机数据库密码'
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
.\scripts\start-backend.ps1
```

也可在 backend 目录手动执行 `mvn verify`，然后 `java -jar target/dorm-repair-backend-1.0.0.jar`。默认监听8080；可用 `SERVER_PORT` 更改端口。JWT_SECRET 必须至少32字节，同一部署重启时沿用原密钥才能保留现有登录。

## 前端

```bash
cd frontend
npm ci
npm run build
npm run dev
```

打开 `http://localhost:5173`。默认 API 为 `http://localhost:8080/api`，可在登录页修改。复制自其他操作系统的 node_modules 不作为可移植依赖使用，应执行 npm ci 重装。

## 演示账号

初始化的四个账号密码均为 `password`，数据库只存 BCrypt 哈希。它们用于本地课程演示。

| 角色 | 账号 |
|---|---|
| 学生 | student |
| 宿舍管理员 | admin |
| 维修人员 | repairer |
| 系统管理员 | sysadmin |

## 接口与认证

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`。
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`；已验证版本快照见 `docs/openapi.json`。
- 先 GET `/api/auth/csrf`，随后每次写请求携带 `X-XSRF-TOKEN` 和对应 Cookie。
- POST `/api/auth/login` 接收 username、password；成功返回安全用户信息，并签发 HttpOnly Cookie，不返回JWT字符串。
- GET `/api/auth/me` 恢复当前用户；POST `/api/auth/logout` 清除当前浏览器登录 Cookie。
- 前端 Axios 使用 withCredentials，身份只保存在内存，JWT 不写入 localStorage/sessionStorage。
- Spring Security 实施权限与角色校验，Service 实施工单归属和状态校验；账号禁用和角色变更在下一请求重新核对。
- HTTPS 部署设置 `COOKIE_SECURE=true`，并把 `CORS_ALLOWED_ORIGINS` 配置为实际前端来源。当前 Cookie 的 SameSite=Lax 适合相同站点部署；不同站点部署需另行设计 Cookie 与 CSRF 策略。

## 测试与报告

```bash
cd backend
mvn test
```

以下HTTP测试会创建实验用户、工单和日志，只对独立测试数据库执行：

```bash
python3 scripts/verify_api.py --base http://localhost:8080/api --allow-write
```

- [验证结果](docs/验证结果.md)：自动测试、真实数据库联调与浏览器检查。
- [实验一Word报告](docs/个人实验/实验一_Web基础项目.docx)；[Markdown版](docs/个人实验/实验一_Web基础项目.md)。
- [实验二Word报告](docs/个人实验/实验二_前后端分离与RESTfulAPI.docx)；[Markdown版](docs/个人实验/实验二_前后端分离与RESTfulAPI.md)。
- [实验四Word报告](docs/个人实验/实验四_登录权限与安全.docx)；[Markdown版](docs/个人实验/实验四_登录权限与安全.md)。
- [功能说明与验收报告](docs/功能说明与验收报告.md)。

## AI 使用记录与已知边界

本次补充使用 Codex 辅助实现与整理，新增关键代码和测试标注 AI-assisted；验证记录公开具体检查结果。个人报告包含关键代码讲解要点，提交者须本人理解并核对。历史代码的原始AI使用情况无法从当前源码证明，不补造历史记录。

系统名称沿用原项目名称；本次未实现大模型或RAG能力。退出仅清除当前浏览器Cookie，未提供被窃取JWT的集中撤销。权限表当前覆盖用户管理、日志和统计，其他业务由角色注解和数据归属检查约束。

## 目录

```text
backend/           Spring Boot、Spring Security、JWT、OpenAPI及自动化测试
frontend/          Vue、Axios、Vite
database/         初始化SQL与保留数据的权限迁移
scripts/           启动和HTTP验证脚本
docs/              个人实验报告、接口快照、测试记录及真实截图
UML图/             原有项目图示
```
