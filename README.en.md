[中文](README.md) | [English](README.en.md)

# PrintDesk · Print and Custom Production Order Management · Java / Spring Boot / Vue / MySQL

<img src="frontend/public/brand/logo.jpg" width="56" alt="ZhiHua Technology">

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)** · [Official website](https://www.zhuatech.cn/)

Connect a made-to-order job from quoting and customer proof approval through materials, production, outsourced work, independent quality, deliveries and settlement. Intended for small digital-print, signage and custom-production teams with separate sales, operators and quality reviewers, and implementation teams evaluating private deployment and customization.

**1.0.0 · Source-available, non-commercial learning edition.** Personal learning, research and non-commercial exchange only. Commercial use requires written authorization. This is not an OSI license or free commercial permission. The project's own code follows [LICENSE](LICENSE); third-party components retain their own licenses.

## One order, from quote to settlement

Draft quote → immutable published revision → bound customer acceptance → approval of the latest proof → operator assignment/start → material issues/returns and actual work → outsourced receipts → independent quality/rework → partial deliveries → external payment/refund records → close.

Customers and staff use separate accounts. Customers only see their own published orders, exact proofs and deliveries. Staff follow organization/assigned scope. Staff cannot confirm for customers, operators cannot review their own quality, and revised quotes/new proofs require new approval while retaining history.

## Implemented modules

| Module | Actual capabilities |
|---|---|
| Customer workspace | Login, own quote acceptance/change request, current proof approval/rejection, source-file upload, authorized downloads, delivery progress |
| Quoting | Up to20 lines, material-cost snapshots, per-piece/M2 bounding usage, printing/finishing/setup, cost markup, discounts/manual tax and immutable published versions |
| Files and proofing | Actual PDF/PNG/JPEG uploads, 5MB limit, signature checks, SHA-256, source/proof separation, retained versions and current-proof production gate |
| Materials | Sheet/M2/piece units and currency, actual receipts/reference deduplication, moving-average costs, order issues and original-cost returns, consumed-material protection |
| Production | Organization assignment, current-operator start/report, good/scrap/minutes/agreed hourly cost, sufficient materials, independent quality and retained rework costs |
| Outsourcing | Supplier catalog, tasks, actual external confirmation, partial receipts/proportional cost, cancellations and quality gate |
| Deliveries and money | Pickup/local delivery/shipment, partial line deliveries, quantity limits, external payment/original refund records, overpayment/refund guards and full delivery/payment closure |
| Workspace | Search/filter/sort/page, details, current-page printing, formula-safe CSV, real currency-separated statistics |
| Administration | Account enablement/password reset/change, 6 initial/custom roles, ALL/organization/assigned/bound-customer scopes, organizations/menus/categories/parameters/audit |
| Maintenance | CSRF sessions, live permissions, version conflicts, request idempotency, migrations, private backup/fresh isolated restore, Chinese/English responsive UI |

Unauthorized cost/payment/internal data are removed by the backend, not merely hidden by the UI. Uploads and downloads validate order ownership. Password hashes, binary files and idempotency fingerprints are not exposed as business JSON.

## Actual running screens

Screenshots show TEST synthetic acceptance records, not real customers or paying cases. Fresh installation does not load those records/accounts.

| Screen | Actual function |
|---|---|
| ![Login](docs/screenshots/01-login.jpg) | Authentication and language selection |
| ![Orders](docs/screenshots/02-orders.jpg) | Search, state and order workspace |
| ![Quote](docs/screenshots/03-order.jpg) | Quote revision, specifications and quantities |
| ![Proofs](docs/screenshots/04-proof.jpg) | Actual file versions and approval state |
| ![Customer](docs/screenshots/05-customer.jpg) | Own quote and current proof confirmation |
| ![Materials](docs/screenshots/06-materials.jpg) | Receipts, inventory and costs |
| ![Accounts](docs/screenshots/07-accounts.jpg) | Staff/customer account administration |
| ![Reports](docs/screenshots/08-reports.jpg) | Actual orders, payments, costs and currencies |
| ![Roles](docs/screenshots/09-roles.jpg) | Custom role permissions and data scope |
| ![Mobile](docs/screenshots/10-mobile.jpg) | Responsive customer page |
| ![English](docs/screenshots/11-english.jpg) | English workspace |

## Architecture, database and directories

Java21, Spring Boot4.0.7, Security, JPA/Hibernate and Flyway; Vue3.5.43, Vite8.1.5, Node24.19.0 and Lucide; MySQL8.4, Nginx and Docker Compose. Same-origin Nginx proxies /api. Sessions use HttpOnly/SameSite Strict cookies and mutations require CSRF. BCrypt12 rounds, live permissions and revoked sessions after account/organization disable or password change.

```text
backend/src/main/java/cn/zhuatech/printdesk/   Business, access and identity services
backend/src/main/resources/db/migration/     V1 identity, V2 print business, V3 approvals/receipts
backend/src/test/java/                       Pricing and HTTP/security/JPA tests
frontend/src/components/                     Quotes, actions, masters and administration
frontend/public/brand/                       Official brand assets
docs/                                       User/API/deployment/testing guides and screenshots
scripts/                                    Environment, QA, backup and restore
compose.yaml / compose.dev.yaml             Full deployment / native development database
```

23 application tables plus Flyway history. Empty installation creates one organization/admin, 6roles, 14permissions, 7menus, 3categories and 2parameters, without business samples. Flyway applies V1__identity.sql then V2__print_business.sql then V3__review_and_receipts.sql; Hibernate only validates. New upgrades require new migrations, not altered applied checksums. Back up before upgrading. Business records and binary files persist in mysql-data.

## Installation and configuration

Docker Engine/Desktop, Compose V2 and Python3.10+; allow at least4GB available memory. Native development additionally requires JDK21, Maven3.9+, Node24.19.0+ and npm. Builds need official package registries. Core workflows require no paid external account.

```sh
python3 scripts/init-env.py
docker compose -p printdesk config --quiet
docker compose -p printdesk up -d --build --wait --wait-timeout 240
```

Staff/customer login: [http://127.0.0.1:8143/](http://127.0.0.1:8143/); health: [http://127.0.0.1:8143/actuator/health](http://127.0.0.1:8143/actuator/health). First username admin, password in private.env ADMIN_PASSWORD. The generator uses unique random credentials, refuses overwrite and provides no shared demo password. Environment changes do not reset an existing account.

| Variable | Purpose |
|---|---|
| MYSQL_ROOT_PASSWORD / DATABASE_PASSWORD | Required unique strong credentials |
| ADMIN_USERNAME / ADMIN_PASSWORD | Empty-database admin; password12–72 bytes with uppercase/lowercase/digit |
| WEB_PORT / BIND_ADDRESS | 8143 / 127.0.0.1 by default; privately override |
| COOKIE_SECURE | false for local HTTP; true for HTTPS deployment |
| DATABASE_URL / DATABASE_USER | Optional external MySQL with trusted CA/VERIFY_IDENTITY |
| MYSQL_DEV_PORT | Local development MySQL port33143 |

See [.env.example](.env.example). Public/cross-device access requires your reachable domain, HTTPS and network configuration; a phone cannot access another machine's localhost. Native development, upgrades, troubleshooting and backup/restore are in [Deployment](docs/DEPLOYMENT.md). The current verification host lacks a native JDK, so native debugging has not been separately verified; delivery verification uses full Compose.

## Operation and testing

Create actual catalogs/accounts first and follow one complete order. Stock and payments change through entries, not editable balances. Original-return/refund references preserve evidence. Refresh and reconcile conflicts or unknown network results; do not automatically replay. See [User guide](docs/USER_GUIDE.md), [API and permissions](docs/API.md), and [Business scope](docs/BUSINESS.md).

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

46 backend tests (12rules plus34 real HTTP/Security/JPA/Flyway) and9 frontend API tests. Deployment acceptance additionally uses fresh dedicated MySQL; never run it against an existing business instance. Docker Maven packaging does not skip tests. See [Testing](docs/TESTING.md) for coverage and boundaries.

```sh
python3 scripts/quality.py --base http://127.0.0.1:8143
docker compose -p printdesk restart backend
python3 scripts/verify-persistence.py --base http://127.0.0.1:8143
python3 scripts/backup.py --project printdesk --output private-backups/printdesk.zip
```

## Limits, security and unimplemented integrations

- Single-company, single-instance learning edition; not a full print ERP or public multi-tenant service. Queries cap each entity at10000, some filtering/statistics run in memory, and writes use a global database serialization lock. No large-volume/concurrency commitment.
- One explicit cost-markup pricing formula; no offset imposition/plate calculations, optimal nesting, separation/color control, device control, automatic scheduling, piece-rate payroll, vendor garment catalogs or online design studio. M2 bounding usage does not certify machine feasibility.
- No SMTP/SMS/WeChat delivery, mailbox synchronization, AI, payment transactions, tax invoices, carrier labels/tracking, SSO, webhooks, external bearer APIs or CRM/accounting connectors. External confirmations/payments/delivery references are manually verified. Purchasing a commercial license does not complete these integrations.
- PDF/PNG/JPEG only; signature checks are not prepress verification or malware scanning. No native AI/PSD/EPS/RAW, inline PDF annotations or certified electronic signatures. Backups contain files/password hashes and require private storage; restore caps each decompressed member at512MB.
- Material quantities/costs use4 decimals, money2. One order currency, no FX conversion. Cumulative output and net issues cap at3× plan; excess needs separate handling, not fabricated records. Unrecorded overheads are excluded; statistics are not accounting books.
- Closed-order returns/reopening/reversals are absent. Quotes/proofs cannot be directly changed after production starts, and deliveries cannot be reversed. Roles/organizations do not establish cross-company hosted isolation.
- Public-network, physical-phone, load/capacity and actual-customer validation are not completed. Tests do not establish production fitness, commercial results or certifications. Software is provided as-is under LICENSE.

Report reproducible issues in the repository. Contributions retain brand/license/third-party notices and exclude credentials, private backups and customer files. Report vulnerabilities privately through the contacts below. [Third-party notices](docs/THIRD_PARTY.md) preserve each component's copyright/license.

## License and contact ZhiHua Technology

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) provides this source-available edition for personal learning, research and non-commercial exchange. Commercial use requires written authorization. For source licensing, customization, private deployment, migration and systems integration, visit [www.zhuatech.cn](https://www.zhuatech.cn/).

- Email: [han@zhuatech.cn](mailto:han@zhuatech.cn)
- Email: [jack@zhuatech.cn](mailto:jack@zhuatech.cn)
- WhatsApp: [+86 17521234993](https://wa.me/8617521234993)
