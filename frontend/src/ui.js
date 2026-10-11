// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 双语界面，业务数据保持原文。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function t(zh, en) {
  return language.value === "zh" ? zh : en;
}
export const states = {
  DRAFT: ["报价草稿", "Draft quote"],
  QUOTED: ["待客户确认", "Awaiting customer"],
  CHANGES: ["客户要求修改", "Changes requested"],
  ACCEPTED: ["已接单", "Accepted"],
  IN_PROGRESS: ["制作中", "In production"],
  READY: ["待交货", "Ready"],
  PART_DELIVERED: ["部分交货", "Part delivered"],
  DELIVERED: ["已交货", "Delivered"],
  CLOSED: ["已结清", "Closed"],
  CANCELLED: ["已取消", "Cancelled"],
  PENDING: ["待确认", "Pending"],
  APPROVED: ["已确认", "Approved"],
  REJECTED: ["要求修改", "Rejected"],
  REQUESTED: ["委外待确认", "Requested"],
  CONFIRMED: ["供应商已确认", "Confirmed"],
  RECEIVED: ["全部收回", "Received"],
};
/** 服务端状态准确显示。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function stateName(c) {
  return states[c] ? t(...states[c]) : c;
}
export const actions = {
  REVISE: ["撤回并修订报价", "Withdraw & revise quote"],
  QUOTE: ["发送报价", "Issue quote"],
  ACCEPT: ["确认报价", "Accept quote"],
  REJECT: ["要求修改报价", "Request quote changes"],
  PROOF_ACCEPT: ["确认当前稿件", "Approve current proof"],
  PROOF_REJECT: ["要求修改稿件", "Request proof changes"],
  ASSIGN: ["派工", "Assign operator"],
  START: ["开始制作", "Start production"],
  ISSUE: ["领料", "Issue material"],
  RETURN: ["退料", "Return material"],
  WORK: ["登记报工", "Record production"],
  QUALITY_PASS: ["质检通过", "Pass quality check"],
  QUALITY_FAIL: ["质检返工", "Require rework"],
  DELIVER: ["登记交货", "Record delivery"],
  RECEIVE: ["登记外部收款", "Record external payment"],
  REFUND: ["登记原款退款", "Record original refund"],
  CLOSE: ["结清订单", "Close order"],
  CANCEL: ["取消订单", "Cancel order"],
  VENDOR_CREATE: ["建立委外任务", "Create outsourced task"],
  VENDOR_CONFIRM: ["登记供应商确认", "Record supplier confirmation"],
  VENDOR_RECEIVE: ["登记委外收回", "Record outsourced receipt"],
  VENDOR_CANCEL: ["取消委外任务", "Cancel outsourced task"],
  NOTE: ["内部备注", "Internal note"],
  SAVE: ["保存报价", "Save quote"],
  UPLOAD: ["上传文件", "Upload file"],
};
/** 操作标签对应真实接口。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function actionName(c) {
  return actions[c] ? t(...actions[c]) : c;
}
const errors = {
  REVISION_COMMITMENT_EXISTS: [
    "已有收款或委外约定，不能直接修订报价。请先处理原约定。",
    "Payments or outsourced commitments exist. Resolve them before revising.",
  ],
  USED_MATERIAL_LOCKED: [
    "已报工消耗的材料不能退回。",
    "Material consumed by recorded production cannot be returned.",
  ],
  RECORD_REFERENCED: [
    "已发布或已有文件，不能删除草稿。",
    "Draft has a published quote or files and cannot be deleted.",
  ],
  OUTPUT_LIMIT: [
    "累计制作数量超过该订单行的返工上限。",
    "Cumulative output exceeds this line’s rework limit.",
  ],
  UNAUTHENTICATED: [
    "会话失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号、密码错误或账号已停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "尝试过多，五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: ["没有操作权限。", "Permission denied."],
  OUT_OF_SCOPE: [
    "记录不在访问范围内或尚未向客户发布。",
    "Record outside your scope or not published to the customer.",
  ],
  VERSION_CONFLICT: [
    "记录已经更新，请刷新核对。",
    "Record changed. Refresh and verify.",
  ],
  REQUEST_REUSED: [
    "该请求编号已用于另一项操作。请刷新核对。",
    "Request ID already used for different input. Refresh and verify.",
  ],
  INVALID_INPUT: ["字段格式不正确。", "Invalid field format."],
  DECIMAL_INVALID: [
    "金额或数量超出精度和范围，请核对。",
    "Amount or quantity exceeds precision or range.",
  ],
  STATE_INVALID: [
    "当前状态不能执行此操作。",
    "Action not allowed in this state.",
  ],
  CUSTOMER_ONLY: [
    "此确认只能由绑定客户账号完成。",
    "Only the bound customer may confirm.",
  ],
  PROOF_REQUIRED: [
    "开工前需客户确认最新稿件。",
    "The customer must approve the latest proof before production.",
  ],
  PROOF_STALE: [
    "稿件已经更新或已处理，请刷新。",
    "Proof changed or already reviewed. Refresh.",
  ],
  ASSIGNEE_ONLY: [
    "只有当前派工人员可以开工和报工。",
    "Only the assigned operator can start or report work.",
  ],
  ASSIGNEE_INVALID: [
    "请选择同组织、启用且有制作权限的员工。",
    "Choose an enabled production operator in this organization.",
  ],
  INDEPENDENT_REVIEW_REQUIRED: [
    "质检人员须独立于该单制作人员。",
    "Quality review must be independent of this order's operators.",
  ],
  PRODUCTION_INCOMPLETE: [
    "尚有订单行未完成足量良品。",
    "Some order lines have insufficient finished goods.",
  ],
  MATERIAL_NOT_ISSUED: [
    "请先登记该订单行实际领料。",
    "Record actual material issue for this line first.",
  ],
  STOCK_SHORTAGE: [
    "库存不足或物料已停用。",
    "Insufficient stock or disabled material.",
  ],
  RETURN_EXCEEDS_ISSUE: [
    "退料数量不能超过原领料剩余量。",
    "Return exceeds the remaining original issue.",
  ],
  VENDOR_PENDING: ["还有未完成的委外任务。", "Outsourced tasks remain open."],
  PAYMENT_INCOMPLETE: [
    "净收款尚未达到订单应收。",
    "Net payments do not match the order total.",
  ],
  OVERPAYMENT: [
    "收款不能超过剩余应收。",
    "Payment exceeds the remaining balance.",
  ],
  REFUND_EXCEEDS_PAYMENT: [
    "退款不能超过原收款剩余金额。",
    "Refund exceeds the remaining original payment.",
  ],
  REFUND_REQUIRED: [
    "取消前须处理已收款。",
    "Refund existing payments before cancellation.",
  ],
  REFERENCE_DUPLICATE: [
    "该凭据已登记，请核对已有流水。",
    "This reference is already recorded. Check existing entries.",
  ],
  IDENTITY_LOCKED: [
    "组织、客户、编码、单位或币种不能更换。",
    "Organization, customer, code, unit or currency cannot be changed.",
  ],
  LAST_ADMIN: [
    "必须保留一个启用的完整管理员。",
    "Keep one enabled full administrator.",
  ],
  PASSWORD_WEAK: [
    "密码需12–72字节，含大小写字母和数字。",
    "Use 12–72 bytes with uppercase, lowercase and a digit.",
  ],
  OLD_PASSWORD_INVALID: ["原密码错误。", "Current password incorrect."],
  FILE_INVALID: [
    "支持PDF、PNG、JPEG；文件名不能包含路径。",
    "Use PDF, PNG or JPEG with no path in the filename.",
  ],
  FILE_TOO_LARGE: [
    "文件不能为空且不能超过5MB。",
    "File must be nonempty and at most 5MB.",
  ],
  FILE_LIMIT: ["该订单文件数量达到上限。", "Order file limit reached."],
  MATERIAL_INVALID: [
    "物料停用、组织或币种不匹配。",
    "Material disabled, wrong organization or currency.",
  ],
  CUSTOMER_ACCOUNT_INVALID: [
    "请选择同组织的专用客户账号。",
    "Choose a dedicated customer account in this organization.",
  ],
  CUSTOMER_ROLE_INVALID: [
    "客户角色只能具有订单查阅权限。",
    "Customer role must contain only the order permission.",
  ],
  CUSTOMER_DISABLED: ["客户已经停用。", "Customer disabled."],
  DATE_INVALID: [
    "请填写有效日期；新报价交期不能早于今天。",
    "Use a valid date; new quotes cannot be due before today.",
  ],
  DISCOUNT_EXCEEDS_TOTAL: [
    "折扣不能超过报价小计。",
    "Discount cannot exceed the subtotal.",
  ],
  RESULT_UNKNOWN: [
    "网络中断，结果未知。请刷新核对，勿直接重复提交。",
    "Connection interrupted; result unknown. Refresh and verify before resubmitting.",
  ],
  NETWORK_ERROR: [
    "无法连接服务器，请检查网络。",
    "Server unavailable. Check your connection.",
  ],
  NOT_FOUND: ["记录不存在。", "Record not found."],
  CONFLICT: ["记录重复或仍被引用。", "Duplicate or referenced record."],
  RESOURCE_LIMIT: [
    "达到学习版数据上限。",
    "Learning edition data limit reached.",
  ],
};
/** 不显示内部异常或敏感配置。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function errorMessage(e) {
  return errors[e.message]
    ? t(...errors[e.message])
    : `${t("操作未完成", "Action failed")} (${e.message})`;
}
export const confirmation = ref(null);
/** 页内明确确认，不自动重放未知结果操作。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function ask(message) {
  if (confirmation.value) return Promise.resolve(false);
  return new Promise((resolve) => (confirmation.value = { message, resolve }));
}
/** 结束当前确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function answerConfirmation(ok) {
  const p = confirmation.value;
  confirmation.value = null;
  p?.resolve(ok);
}
/** 编辑副本与列表分离。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function cloneRecord(r) {
  return JSON.parse(JSON.stringify(r));
}
/** 时间按浏览器时区展示，服务端统一UTC。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function dateTime(v) {
  return v
    ? new Intl.DateTimeFormat(language.value === "zh" ? "zh-CN" : "en", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(v))
    : "—";
}
/** 制作岗位仅显示流程数，不计算未获授权金额。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function money(v, currency = "CNY") {
  return v == null
    ? "—"
    : new Intl.NumberFormat(language.value === "zh" ? "zh-CN" : "en", {
        style: "currency",
        currency,
      }).format(Number(v));
}
