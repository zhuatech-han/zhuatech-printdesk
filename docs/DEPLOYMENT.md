# 部署、升级及备份恢复 / Deployment and recovery

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

## 全新安装 / Fresh installation

Docker Engine/Desktop、Compose V2、Python3.10+；建议至少4GB可用内存。构建需访问官方Maven/npm/Docker源，后端测试不会跳过。

```sh
python3 scripts/init-env.py
docker compose -p printdesk config --quiet
docker compose -p printdesk up -d --build --wait --wait-timeout 240
```

本机端口8143，可在私有.env用WEB_PORT覆盖。默认仅绑定127.0.0.1；数据库不对外暴露。首次管理员admin，密码从私有.env的ADMIN_PASSWORD读取，没有共享口令。初始化只创建目录与管理员，无业务或测试数据；已有库重启不改管理员密码。

Local port8143 is configurable in private.env. Default binding is localhost; MySQL is not exposed. First administrator is admin with the generated private ADMIN_PASSWORD, no shared password. Initialization creates identity/catalogs only. Restarting an existing database does not reset credentials.

公网或手机跨设备使用需可达域名/地址、HTTPS反向代理及COOKIE_SECURE=true。不要把后台明文HTTP直接暴露到公网。浏览器和客户登录使用同一部署地址，另一台手机不能访问服务器的127.0.0.1。仓库不包含公网托管服务。

Public/cross-device access needs a reachable hostname, HTTPS reverse proxy and COOKIE_SECURE=true. Do not expose the administration over plain HTTP. A phone cannot reach another machine's localhost. Public hosting is not included.

.env.example列出DATABASE_PASSWORD、MYSQL_ROOT_PASSWORD、ADMIN_USERNAME/PASSWORD、WEB_PORT、BIND_ADDRESS和COOKIE_SECURE。可配置外部MySQL的DATABASE_URL/USER；部署方配置受信任CA、VERIFY_IDENTITY和迁移权限，内置备份脚本不支持外部数据库。

Optional external MySQL requires trusted CA/VERIFY_IDENTITY and migration privileges. Bundled backup/restore scripts only support the built-in database.

## 升级和持久化 / Upgrade and persistence

Flyway执行V1__identity.sql、V2__print_business.sql和V3__review_and_receipts.sql、V3__review_and_receipts.sql；Hibernate validate不自动改表。已有迁移不改校验和；新增升级应提供新版本迁移和恢复演练。业务、文件、账号与流水保存在mysql-data卷；替换前后端容器不删除数据。`down -v`会删数据，不能用于已有业务环境。

Flyway versions schema changes; Hibernate only validates. Add migrations rather than modifying applied checksums. The mysql-data volume holds business records and binary files. Never use down -v against real business data.

```sh
python3 scripts/backup.py --project printdesk --output private-backups/printdesk.zip
```

备份暂停指定项目后端写入，mysqldump使用事务快照与hex-blob，保留二进制及散列清单；最后恢复后端运行。它不暂停其他项目。SQL包含答案、文件、账号密码散列，必须私有保存并制定保留策略。只恢复受信任的本地备份，散列校验不等于恶意SQL隔离。

Backup stops only the named backend, dumps a consistent transaction snapshot with hex blobs, then restarts it. Dumps contain business data/files/password hashes and must stay private. Only restore trusted backups; hash validation does not sandbox malicious SQL.

先准备私有.env.restore：独立数据库密码、BIND_ADDRESS=127.0.0.1、新WEB_PORT（如28143），ADMIN_PASSWORD保留备份中已有管理员的真实登录密码。恢复不会按新的环境变量重置已有账号。恢复脚本拒绝覆盖已有项目、卷和网络，解压单成员上限512MB。

Prepare private.env.restore with unique database credentials, localhost binding and a separate port. Keep the backed-up administrator's existing password for login; environment changes do not reset it. Restore refuses existing project/volume/network and caps each decompressed archive member at512MB.

```sh
python3 scripts/restore.py private-backups/printdesk.zip --project printdesk-recovery --env-file .env.restore
```

运行后核对订单、报价版本、数量、金额、目录及附件散列。公网、实机手机、容量、灾备时间和外部服务验收需部署方另行完成。

## 源码调试 / Native source development

JDK21、Maven3.9+、Node24.19.0+。开发覆盖文件仅暴露本机MySQL33143，不同时启动容器版前后端。

```sh
docker compose -p printdesk-dev -f compose.yaml -f compose.dev.yaml up -d mysql
python3 scripts/dev-backend.py
# Another terminal / 另一终端
cd frontend
npm ci --no-audit --no-fund
npm run dev
```

后端8080，Vite5173。正式交付验收以完整Compose为准；当前主机未安装原生JDK，未单独验证此原生调试路径。

## 排障 / Troubleshooting

健康失败先检查mysql/backend日志与端口占用，不删除其他容器。登录失败核对私有密码和账号/组织启用状态；版本冲突刷新；客户看不到订单核对绑定账号和报价是否发布；无法开工核对客户报价、最新稿件与派工人；报工失败核对实际耗材及产出；质检失败核对独立人员和委外；结清失败核对交货与净收款。参数变更不会伪造完成任何业务。
