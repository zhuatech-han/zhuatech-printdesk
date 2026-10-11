# API 与权限 / API and permissions

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

同源服务端会话、HttpOnly/SameSite Strict Cookie和CSRF。`GET /api/auth/csrf`获取header/token；写入携带该头。`POST /api/auth/login`使用username/password。账号/组织停用及改密撤销旧会话，角色实时读取。客户的scope为CUSTOMER，仅orders权限，必须绑定当前组织的Customer.accountId。ALL/DEPARTMENT/ASSIGNED适用于员工；ASSIGNED订单归属ownerId/assigneeId。角色/组织是单企业范围控制，不是运营型多租户隔离。

Same-origin sessions, HttpOnly/SameSite Strict cookies and CSRF. Get header/token from `/api/auth/csrf` and send it on mutations. Account/organization disable or password changes revoke sessions; roles are read live. CUSTOMER requires a bound Customer.accountId and only orders permission. Staff ALL/DEPARTMENT/ASSIGNED scopes control organization/owner/assignee access. This is single-company access control, not managed multi-tenant SaaS isolation.

| Endpoint | Capability / 权限 |
|---|---|
| GET /api/auth/me; POST /api/auth/logout,password | 本人身份/退出/改密 / identity/logout/password |
| GET /api/options | orders，最小目录，客户不读员工或其他客户 / minimized options |
| GET /api/orders?q=&state=&sort=&page=&size= | orders；先范围过滤，最多50/页 / scoped pagination |
| GET /api/orders/:id | orders；客户/制作员的成本与内部信息剔除 / role-redacted detail |
| POST /api/orders; PUT /api/orders/:id | sales；草稿计算和保存 / draft quote |
| POST /api/orders/:id/:ACTION | 对应业务权限，version和requestId / workflow mutation |
| POST /api/orders/:id/files | multipart kind,version,requestId,file；artwork或客户SOURCE / upload |
| GET /api/files/:id | orders及订单归属，强制下载 / authorized download |
| POST /api/orders/:id/delete | sales；version；仅未发布无文件草稿 / empty draft delete |
| GET/POST/PUT /api/masters/:kind[/:id] | masters；customers/materials/suppliers / master data |
| POST /api/materials/:id/receive | masters；version,quantity,cost,reference,note / receipt |
| GET /api/reports | reports；按币种统计 / currency-separated reports |
| POST /api/orders/export | export；CSV与审计 / audited CSV |
| GET /api/audit | audit，组织范围 / scoped audit |
| GET /api/admin/options,:kind; POST/PUT /api/admin/:kind[/:id] | users/roles/settings且ALL范围 / identity and system catalogs |
| GET /actuator/health | 无配置详情的匿名健康 / anonymous health |

ACTION：QUOTE/REVISE/ASSIGN/CLOSE/CANCEL/NOTE/VENDOR_*需要sales；ACCEPT/REJECT/PROOF_ACCEPT/PROOF_REJECT只允许绑定客户；START/ISSUE/RETURN/WORK需要production，开工/报工另检查当前派工人；QUALITY_PASS/QUALITY_FAIL需要quality且人员独立；DELIVER需要delivery；RECEIVE/REFUND需要finance。全部检查合法状态和实体归属。详情中的客户看不到内部事件、材料成本、工时成本及委外费用；制作员无售价/付款/总成本。

Actions require matching permission plus state and ownership checks. Customer quote/proof confirmation cannot be performed by staff. Start/work require the current operator; quality requires a separate actor. Cost/payment/internal data are removed before JSON serialization for unauthorized roles.

报价body：customerId,title,dueDate(YYYY-MM-DD),currency,category,instructions,discount,taxPercent,lines[{name,materialId,quantity,widthMm,heightMm,usage,printRate,finishRate,setup,markupPercent}],requestId；更新另需version。金额和材料数量建议用十进制字符串，拒绝超精度。新建和订单动作requestId为小写UUID；完全相同请求幂等，指纹包含原输入的字段顺序，刷新核对后新的操作使用新ID。入库通过物料+实际凭据去重。来源退款/退料使用sourceId。

Quote inputs are typed/validated, amounts use decimal strings. Writes carry UUID requestId, and updates carry current version. Identical requests are deduplicated; fingerprints include original input field order. New reconciled actions use new IDs. Receipts are deduplicated by material/reference; returns/refunds reference sourceId.

错误仅code：400参数，401身份，403权限/范围，404不存在，409业务状态/版本/数量，413资源或大小，429登录限流。网络失败不自动重放写入。除本系统浏览器会话外，没有已完成外部Bearer API、Webhook、SSO或CRM连接器。

Errors expose codes only; mutations are never automatically replayed. External bearer APIs, webhooks, SSO and CRM connectors are not implemented.
