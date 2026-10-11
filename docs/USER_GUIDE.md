# PrintDesk 操作手册 / User guide

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

## 先建立真实目录 / Set up real master data

管理员在登录账号建立销售、制作、独立质检和客户账号；客户角色的数据范围为CUSTOMER，权限仅为orders。随后在“客户与物料”建立客户，绑定同组织的客户账号。客户本人登录后只看自己的已发布订单，不能看其他客户或内部成本。一个账号只能绑定一个客户。

Create sales, production, independent quality and customer accounts. Customer scope is CUSTOMER with only orders permission. Bind an enabled customer account to one customer in the same organization. Customers only see their published orders, not other customers or internal costs.

建立物料，确定组织、编码、单位（张/平方米/件）和币种；创建后不能更换这些身份。库存从零开始，通过真实入库凭据登记数量与单位成本，重复凭据拒绝入库。系统按移动平均计算物料成本。停用目录保留历史；不会自动向供应商下单。

Create materials with immutable organization, code, unit (SHEET/M2/PIECE) and currency. Stock starts at zero. Record actual receipts with unique reference, quantity and unit cost; moving average determines material cost. Disable records to retain history. No supplier order is sent automatically.

## 报价和准确稿件版本 / Quote and exact proof revision

销售建立报价，选择客户、交期、币种和制作内容。每行填写数量、材料、单件耗材或毫米尺寸、制作/后道费、一次开机费和成本加成。保存后由服务端重新计算；发布产生不可变报价快照。每笔订单最多20行。平方米耗材按外包络面积向上取4位，并非优化排样；其他单位的单件用量显式填写。

Sales creates a quote with customer, due date, currency and up to20 lines. Each line has quantity, material usage or millimetre dimensions, per-piece print/finish charge, one-time setup and markup. Server calculation is authoritative. Publication creates immutable snapshots. M2 usage rounds bounding area up to4 decimals; this is not cutting/nesting optimization.

报价计算：`（单件材料成本 × 单件耗材 + 单件制作费 + 单件后道费）× 数量 + 一次开机费`，再按成本加成算行价、取2位；全部行小计减订单折扣后计算手工税率。币种必须与材料一致，没有汇率换算。所有成本和收款输入须采用同一币种。

Pricing: material per unit × material usage plus per-piece printing/finishing, multiplied by quantity, plus setup; apply markup and round each line to2 decimals. Discount precedes manual tax. Currency must match materials; no exchange conversion. Cost and payment inputs use the order currency.

客户登录确认或要求修改报价。修改后重新发布新版本；旧版本不覆盖，员工不能代替客户确认。原始文件与待确认稿件分开上传，PDF/PNG/JPEG每个不超过5MB；每单文件默认最多10、可配置到20。二进制在数据库内持久化；下载始终验权。文件签名检查不等于印前内容核验或病毒扫描。

Customers accept or request quote changes; revised drafts must be published again. Staff cannot approve for a customer. Source files and approval proofs are separate. PDF/PNG/JPEG up to5MB each, default10/max20 per order, are privately stored in the database. Download authorization is checked every time. Signature checks are not prepress or malware inspection.

销售/设计上传新稿件后，该新版本为待确认；旧稿件的确认不能用于开工。客户下载核对后，确认准确文件或填写修改意见。客户可上传原始资料，不能把自己的文件冒充员工发出的待审稿件。

A new staff proof is pending even when an older proof was approved. The customer downloads/checks and approves that exact file or requests changes. Customers may upload source material, not a staff-issued proof.

## 材料、制作、委外及独立质检 / Production and quality

派工给同组织启用且具制作权限的账号。只有当前制作人员可开工和报工；开工须已确认报价且最新稿件已批准。制作员工只看本人分派（或本人负责）的订单，不获得订单售价、收款和利润。

Assign an enabled operator in the same organization. Only the current operator can start/report. Start requires customer quote acceptance and approval of the latest proof. Assigned operators do not receive selling prices, payments or margin.

领料检查实际库存；每行净领料不超过计划耗材的3倍，额外领料须填写原因。退料引用原领料，按原成本返回；超过剩余量或已被报工消耗的材料不能退回。报工登记实际良品、报废、分钟和约定小时成本；不得超过订单良品数量，累计产出（含返工）最多订单量3倍，耗材必须足量。工时是人员登记，并非考勤或设备自动采集。

Issues check actual stock and are capped at3× planned material per line, with a reason. Returns reference the original issue/cost and cannot exceed remaining or unconsumed material. Reports record good/scrap output, minutes and agreed hourly cost. Cumulative output including rework is capped at3× ordered pieces; materials must cover it. These are manual reports, not verified attendance or machine measurements.

委外任务记录供应商、加工内容、件数、约定费用及收回日期；外部确认凭据、分批收回分别登记。已收回比例决定确认成本，全部收回为完整约定金额。它不是供应商下单接口，收回也不自动生成生产良品。质检前任务必须已完成或取消。

Outsourced tasks record supplier, work, pieces, agreed charge and due date. Record external confirmation and partial receipts. Recognized cost follows received proportion, with exact full amount at completion. No supplier API is called; receipts do not automatically report finished goods. Tasks must complete/cancel before quality approval.

质检人员不能是当前派工人员，也不能曾为该单报工。全部订单行足量良品后才能质检；不通过须写理由，良品计数回到零等待返工，但已发生工时、物料和委外成本保持不变。不得用虚拟第二个管理员代替真实岗位分工。

Quality review must be independent of the assigned and all reporting operators. All lines need full good quantities. Failure requires a reason, resets current good counts for rework and retains incurred material/labor/outsource costs. Use actual separate roles, not a fictitious reviewer.

## 交货、收退款及结清 / Delivery and settlement

质检通过后可以分批交货，每行不得超过尚未交出的合格数量；自提、送货、发运均保存地点和实际凭据。没有物流跟踪或运单接口，单据中填写的参考号须人工核实。

After quality approval, record partial deliveries no greater than accepted remaining quantities. Pickup, local delivery and shipment retain location and actual reference. There is no carrier tracking/API; references are manually verified.

财务只登记在系统外真实完成的收款和退款，不进行支付。收款不超过剩余应收；退款必须引用原收款，不能超过原款剩余金额。重复完全相同的请求不会重复记账；重复不同内容使用同一请求ID会拒绝。已交完且净收款等于应收后，销售结清订单。结清后本版不提供售后退货/财务冲销重开流程。

Finance records payments/refunds completed elsewhere; no transaction is initiated. Payments cannot exceed remaining due, refunds must reference and not exceed the original payment. Identical request retries are deduplicated; reused IDs with different input are rejected. Sales closes only after full delivery and net payment. Post-close returns/accounting reversals are not implemented.

制作前或未交货阶段可有理由取消，先退清已收款、完成/取消委外。取消不凭空回滚真实材料或成本。未发布且无文件的草稿可删除；其他历史不删除。更新冲突或网络中断时先刷新核对，不自动重复提交。

Cancellation requires a reason, refunded net payments and resolved outsourced tasks before any delivery. It does not fictitiously reverse materials/costs. Unpublished drafts without files may be deleted; other history is retained. Refresh and reconcile version conflicts/unknown network results before retrying.

## 确认与收回凭据 / Approval and receipt evidence

报价版本及稿件记录保存确认账号与时间。每次委外收回保存独立数量、成本增量、凭据和人员，重复同任务凭据拒绝；不是只覆盖累计数。已发报价可在尚未开工、没有收款及委外约定时有理由撤回修订，重新发布后须客户重新确认。

Quote/proof records retain the confirming account and timestamp. Each outsourced receipt retains separate quantity, incremental cost, reference and actor; duplicate task references are rejected. Before production, payment or outsourced commitments, sales may withdraw/revise a quote with a reason and require new customer acceptance after publication.

V3迁移不伪造旧记录的确认账号/时间或逐次委外收回。旧确认信息可为空，旧累计数量保留；新增操作保存完整凭据。

V3 does not fabricate historic approval actors/timestamps or partial receipts. Legacy receipt fields can be empty while previous cumulative amounts remain; new operations retain the full evidence.
