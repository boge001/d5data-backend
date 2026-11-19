# 带身份认证的 Hello World 接口

使用 **Spring Boot 3.2.0** 实现用户名密码登录和受保护的 `GET /api/hello`。登录成功后，接口以纯文本返回精确字符串 `Hello World`。

## 运行

需要 JDK 17 或 21。Maven Wrapper 首次运行时自动下载 Maven 3.9.11。

```sh
# macOS / Linux
chmod +x mvnw
./mvnw spring-boot:run
```

```bat
:: Windows
mvnw.cmd spring-boot:run
```

默认地址为 `http://localhost:8080`，按 Ctrl+C 停止。可以设置环境变量 `SERVER_PORT` 修改端口，也可以打包后运行：

```sh
./mvnw clean package
java -jar target/secured-hello-world-1.0.0.jar --server.port=8081
```

Windows 中将 `./mvnw` 替换为 `mvnw.cmd`。

## 接口

| 方法 | 路径 | 请求与响应 |
| --- | --- | --- |
| GET | `/api/auth/csrf` | 获取 `token`、`headerName`、`parameterName`，并保留会话 Cookie。 |
| POST | `/api/auth/login` | 提交表单字段 `username=test&password=123456`，同时携带 CSRF 请求头；成功返回 `{"authenticated":true,"username":"test"}`，JSON 字段顺序不作要求。 |
| GET | `/api/hello` | 携带已登录的会话 Cookie，返回 HTTP 200 和 `Hello World`；未登录返回 HTTP 401 JSON。 |
| POST | `/api/auth/logout` | 携带会话 Cookie 和新的 CSRF 令牌；返回 HTTP 204，并使会话失效。 |

登录请求使用 `application/x-www-form-urlencoded` 表单格式。凭据错误返回 401；POST 请求缺少有效 CSRF 令牌返回 403。登录会更新会话并清除原 CSRF 令牌，因此注销前需要重新获取令牌。

启动服务后，在另一个终端运行以下 Python 标准库客户端，可执行完整登录流程：

```sh
python scripts/smoke_test.py
# 使用其他端口时：
python scripts/smoke_test.py --url http://127.0.0.1:8081
```

脚本自动保留 Cookie，依次验证未登录访问、错误密码、正确登录、精确响应、注销，以及注销后重新被拒绝访问。

## 实现说明

`SecurityConfig` 使用 Spring Security 的表单登录过滤器、内存用户存储、BCrypt 密码哈希和标准 Session 安全上下文，使登录状态在不同请求之间保持。浏览器会自动携带会话 Cookie，因此保留 CSRF 防护。登录失败及匿名访问返回 JSON，HTTP Basic 已关闭。

`HelloController` 提供受保护的接口；`CsrfController` 返回当前令牌。固定账号用于题目演示；生产服务需要更换用户存储，并按实际环境配置 HTTPS、安全 Cookie 和密码策略。

## 验证记录

```sh
./mvnw test
```

在 JDK 21、Java 编译目标 17 下，8 项 MockMvc 集成测试通过。另启动打包后的 JAR，真实 HTTP 客户端验证也通过。

- `verification/integration-tests.json`：8 项测试，失败数和错误数均为 0。
- `verification/http-smoke.txt`：未登录 401 → 登录 200 → `Hello World` → 注销 204 → 未登录 401。
