# 测试与验收 / Testing and acceptance

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

46项后端：12报价/面积/金额/CSV规则单元测试，34真实MockMvc/Security/JPA/Flyway集成测试。9项前端测试校验真实CSRF头、失败请求不重放、授权下载、过期会话和受保护POST导出。H2隔离测试之外必须另做全新MySQL8.4验收；不能以H2通过代替MySQL迁移检查。Docker Maven和前端镜像都执行测试及构建，不跳过。

46 backend tests:12 unit pricing/area/amount/CSV checks and34 real MockMvc/Security/JPA/Flyway checks.9 frontend tests verify CSRF, non-replay, authorized download, expired sessions and POST exports. H2 tests do not replace fresh MySQL8.4 migration/workflow verification. Both Docker build stages run applicable tests and checks.

```sh
mvn -B -f backend/pom.xml spotless:check clean package
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm test
npm run build
cd ..
python3 scripts/release-check.py
git diff --check
```

专用空库、本机127.0.0.1才允许quality脚本；它明确拒绝已有订单/客户数据，建立TEST账号和业务，私有快照保存在忽略的output/quality-state.json，不打印凭据。不要在现有业务实例执行，不要为了重跑清理其他项目。

The QA suite only accepts localhost and an empty business database. It creates TEST accounts/data, stores private snapshots under ignored output/, and never prints credentials. Never execute it against existing business data or remove another project's resources to rerun.

```sh
python3 scripts/quality.py --base http://127.0.0.1:8143
docker compose -p printdesk restart backend
python3 scripts/verify-persistence.py --base http://127.0.0.1:8143
```

覆盖：首次登录/目录/菜单、十进制报价、报价修订、客户本人确认、旧稿失效、实际二进制下载、客户和组织越权、制作成本剔除、足量领料、超退拒绝、报工/返工成本、委外部分收回、独立质检、分批交货、过量拒绝、并发重复收款幂等、超收/超退保护、结清、CSV、空草稿删除。重启和独立恢复比对完整订单、七类系统目录、三类业务目录、报告及附件SHA-256。

Coverage: initialization/auth/catalogs, decimal quotes/revisions, customer confirmation/current proof, actual binary downloads, customer/organization boundaries and operator redaction, material conservation, rework costs, partial outsourcing/delivery, independent quality, concurrent identical payments, overpayment/refund guards, closure, CSV and empty-draft deletion. Restart/restore compares complete orders/catalogs/reports and binary hashes.

浏览器另检查真实登录、报价编辑/保存、发布、客户审稿、菜单、后台、统计、中英文及窄屏操作；截图取自此运行实例。检查控制台错误、溢出、确认窗口、必填反馈和未保存修改保护。仍未覆盖实机手机、公网、多实例、压力/大容量、真实支付/物流/邮件/供应商服务，以及真实客户采购验证。
