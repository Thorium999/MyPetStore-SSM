# MyPetStore-SSM

《软件开发架构平台》实验一 · 用户端（服务端渲染）。管理端 `MyPetStore-admin` 另建仓库。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 编译目标 17（本机运行 JDK 25） | `pom.xml` 的 `java.version` |
| Spring Boot | 4.2.0 | Web 层 starter 在 4.x 里叫 `spring-boot-starter-webmvc` |
| MyBatis-Plus | 3.5.18 | `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`（分页） |
| 模板引擎 | Thymeleaf | 服务端渲染，不做前后端分离 |
| 数据库 | MySQL 8.4 | 库名 `MyPetStore`，字符集 utf8mb4 |
| 构建 | Maven Wrapper | 无需本机装 Maven，用 `mvnw` |

## 首次运行

1. **导数据库**（只做一次）：用 MySQL 客户端执行 `src/main/resources/database/02-schema-with-demo.sql`，
   脚本自带 `CREATE DATABASE`，完成后应有 7 张表。
2. **配本机密码**：把 `src/main/resources/application-local.yaml.example` 复制为
   `application-local.yaml`（该文件不进 git），填入自己的数据库密码；或设环境变量 `MYSQL_PASSWORD`。
3. **启动**：`./mvnw spring-boot:run`（或 IDEA 里直接运行 `MyPetStoreSsmApplication`）。
4. **验证**：浏览器打开 <http://localhost:8080/demo>，返回「Spring MVC 初始化成功」即环境正常。

## 目录

```
src/main/java/org/csu/mypetstore/
├── MyPetStoreSsmApplication.java   启动类
└── controller/                     控制器（DemoController 为环境自检，进业务前删除）
src/main/resources/
├── application.yaml                通用配置（激活 dev）
├── application-dev.yaml            开发环境：数据源、端口、MyBatis-Plus
├── application-local.yaml          本机私有（git 忽略）：数据库密码
├── database/                       建表与种子数据脚本（供 Agent 读表结构）
├── static/                         css / images / js（自绘占位图，非课程素材）
└── templates/                      Thymeleaf 模板
```

## 教学账号（来自种子数据）

| 用户名 | 密码 | 角色 | 状态 |
|---|---|---|---|
| alice / bob | `PetStore123!` | CUSTOMER | 启用 |
| disabled | `PetStore123!` | CUSTOMER | 禁用 |
| admin | `PetStore123!` | ADMIN | 启用 |

密码以 BCrypt 存储，校验用 `BCryptPasswordEncoder.matches()`。

## 重置演示数据

测试改乱了数据（加购、下单、删条目）想回到出厂状态时：

```bash
./mvnw spring-boot:run -Preset-db
```

`reset-db` profile 会附加 `--app.reset-db=true`，启动时依次执行
`database/00-reset-demo.sql`（删七张表）和 `database/02-schema-with-demo.sql`（重建 + 灌种子），
然后照常提供服务。IDEA 里也可以用：复制一份 Spring Boot 运行配置，在 *Program arguments* 加 `--app.reset-db=true`。

**这是破坏性操作**：七张表会被删掉重建，自己加的测试数据不会保留。平时启动不加这个参数就完全不受影响。

## 说明

- 所有写操作（POST）都要求带 CSRF 令牌：表单里有 `<input type="hidden" name="_csrf">`，
  服务端在 `CsrfInterceptor` 里与 Session 中的值做常量时间比较，不匹配直接 403。
  没引 Spring Security，这一小块是手写的（`security/CsrfToken`）；新增 POST 表单时别忘了带这个隐藏域。
- 商品图片为程序生成的示例占位图（`static/images/`），未使用课程提供的原型素材；
  换真图时保持 `images/products/<SKU>/<n>.jpg` 等原路径即可，数据库无需改动。
- 数据库只在首次导入和显式重置时初始化，日常启动不会建表或清库（`spring.sql.init.mode: never`）。
