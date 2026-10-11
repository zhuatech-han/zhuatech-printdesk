<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  ClipboardList,
  Boxes,
  ChartColumn,
  Users,
  Shield,
  Settings,
  History,
  Menu,
  X,
  LogOut,
  ArrowLeft,
  RefreshCw,
  Plus,
  Printer,
  FileDown,
} from "@lucide/vue";
import { api, resetApi, download } from "./api.js";
import {
  language,
  t,
  stateName,
  actionName,
  errorMessage,
  dateTime,
  money,
  confirmation,
  answerConfirmation,
  ask,
} from "./ui.js";
import AdminPanel from "./components/AdminPanel.vue";
import MasterPanel from "./components/MasterPanel.vue";
import QuoteEditor from "./components/QuoteEditor.vue";
import ActionDialog from "./components/ActionDialog.vue";
const profile = ref(null),
  ready = ref(false),
  pending = ref(false),
  notice = ref(null),
  view = ref("orders"),
  mobile = ref(false),
  about = ref(false),
  passwordForm = ref(null),
  login = ref({ username: "", password: "" });
const options = ref({
    customers: [],
    materials: [],
    suppliers: [],
    departments: [],
    categories: [],
    accounts: [],
  }),
  orders = ref({ items: [], total: 0 }),
  selected = ref(null),
  editing = ref(false),
  detailTab = ref("summary"),
  action = ref(null),
  report = ref(null),
  audit = ref([]),
  q = ref(""),
  filter = ref(""),
  page = ref(1),
  sort = ref("newest"),
  fileKind = ref("PROOF");
const editorRef = ref(null),
  adminRef = ref(null),
  masterRef = ref(null);
const has = (code) => profile.value?.permissions.includes(code);
const isCustomer = computed(() => profile.value?.scope === "CUSTOMER");
const icons = {
  orders: ClipboardList,
  masters: Boxes,
  reports: ChartColumn,
  users: Users,
  roles: Shield,
  settings: Settings,
  audit: History,
};
const menuName = (code) =>
  ({
    orders: t("订单工作台", "Orders"),
    masters: t("客户与物料", "Customers & materials"),
    reports: t("经营统计", "Reports"),
    users: t("登录账号", "Accounts"),
    roles: t("角色与权限", "Roles & permissions"),
    settings: t("组织与设置", "Organizations & settings"),
    audit: t("操作记录", "Audit trail"),
  })[code] || code;
const menus = computed(
  () => profile.value?.menus.filter((m) => icons[m.code]) || [],
);
const tabNames = computed(() => ({
  summary: t("报价与要求", "Quote & requirements"),
  files: t("文件与审稿", "Files & approvals"),
  production: t("制作记录", "Production"),
  vendor: t("委外加工", "Outsourced work"),
  delivery: t("交货记录", "Deliveries"),
  finance: t("收款与成本", "Payments & costs"),
  history: t("订单履历", "History"),
}));
const visibleTabs = computed(() =>
  Object.keys(tabNames.value).filter(
    (k) =>
      !["production", "vendor", "finance", "history"].includes(k) ||
      (!isCustomer.value &&
        ((k !== "vendor" && k !== "finance") || selected.value?.cost != null)),
  ),
);
const actorName = (id) =>
  options.value.accounts.find((a) => a.id === id)?.displayName ||
  (id ? "#" + id : t("尚未派工", "Unassigned"));
const reviewerName = (id) =>
  id === profile.value?.id ? profile.value.displayName : actorName(id);
const maxPage = computed(() => Math.max(1, Math.ceil(orders.value.total / 15)));
const canEdit = computed(
  () =>
    !isCustomer.value &&
    has("sales") &&
    ["DRAFT", "CHANGES"].includes(selected.value?.state),
);
const canUpload = computed(
  () =>
    ["DRAFT", "CHANGES", "QUOTED", "ACCEPTED"].includes(
      selected.value?.state,
    ) &&
    (has("artwork") || isCustomer.value),
);
const available = computed(() => {
  const o = selected.value;
  if (!o) return [];
  const s = o.state;
  let list = [];
  if (isCustomer.value) {
    if (s === "QUOTED") list.push("ACCEPT", "REJECT");
    const p = o.files.find((f) => f.id === o.latestProofId);
    if (["QUOTED", "ACCEPTED"].includes(s) && p?.status === "PENDING")
      list.push("PROOF_ACCEPT", "PROOF_REJECT");
    return list;
  }
  if (has("sales")) {
    if (["DRAFT", "CHANGES"].includes(s)) list.push("QUOTE");
    if (["QUOTED", "ACCEPTED"].includes(s)) list.push("REVISE");
    if (["DRAFT", "CHANGES", "QUOTED", "ACCEPTED", "IN_PROGRESS"].includes(s))
      list.push("ASSIGN");
    if (["ACCEPTED", "IN_PROGRESS"].includes(s))
      list.push(
        "VENDOR_CREATE",
        "VENDOR_CONFIRM",
        "VENDOR_RECEIVE",
        "VENDOR_CANCEL",
      );
    if (s === "DELIVERED") list.push("CLOSE");
    if (
      [
        "DRAFT",
        "CHANGES",
        "QUOTED",
        "ACCEPTED",
        "IN_PROGRESS",
        "READY",
      ].includes(s)
    )
      list.push("CANCEL");
    list.push("NOTE");
  }
  if (has("production")) {
    if (s === "ACCEPTED" && o.assigneeId === profile.value.id)
      list.push("START");
    if (s === "IN_PROGRESS") {
      list.push("ISSUE", "RETURN");
      if (o.assigneeId === profile.value.id) list.push("WORK");
    }
  }
  if (has("quality") && s === "IN_PROGRESS")
    list.push("QUALITY_PASS", "QUALITY_FAIL");
  if (has("delivery") && ["READY", "PART_DELIVERED"].includes(s))
    list.push("DELIVER");
  if (
    has("finance") &&
    !["DRAFT", "CHANGES", "QUOTED", "CLOSED", "CANCELLED"].includes(s)
  )
    list.push("RECEIVE", "REFUND");
  return list;
});
const visibleActions = computed(() =>
  available.value.filter((a) => {
    const groups = {
      PROOF_ACCEPT: "files",
      PROOF_REJECT: "files",
      ISSUE: "production",
      RETURN: "production",
      WORK: "production",
      QUALITY_PASS: "production",
      QUALITY_FAIL: "production",
      VENDOR_CREATE: "vendor",
      VENDOR_CONFIRM: "vendor",
      VENDOR_RECEIVE: "vendor",
      VENDOR_CANCEL: "vendor",
      DELIVER: "delivery",
      RECEIVE: "finance",
      REFUND: "finance",
      NOTE: "history",
      CANCEL: "history",
    };
    return !groups[a] || groups[a] === detailTab.value;
  }),
);
function fail(e) {
  notice.value = { type: "error", text: errorMessage(e) };
  if (e.status === 401) {
    profile.value = null;
    selected.value = null;
    editing.value = false;
    resetApi();
  }
}
/** 所有写入串行，失败不重放未知请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(url, method, body) {
  if (pending.value) return null;
  pending.value = true;
  notice.value = null;
  try {
    const v = await api(url, { method, body });
    notice.value = { type: "success", text: t("已保存。", "Saved.") };
    return v;
  } catch (e) {
    fail(e);
    return null;
  } finally {
    pending.value = false;
  }
}
async function read(fn) {
  try {
    await fn();
  } catch (e) {
    fail(e);
  }
}
async function loadOptions() {
  if (has("orders")) options.value = await api("/options");
}
async function loadOrders() {
  orders.value = await api(
    "/orders?" +
      new URLSearchParams({
        q: q.value,
        state: filter.value,
        page: page.value,
        sort: sort.value,
      }),
  );
}
async function initialize() {
  view.value = menus.value[0]?.code || "orders";
  fileKind.value = isCustomer.value ? "SOURCE" : "PROOF";
  await loadOptions();
  await refresh();
}
onMounted(async () => {
  try {
    profile.value = await api("/auth/me");
    await initialize();
  } catch (e) {
    if (e.status !== 401) fail(e);
  } finally {
    ready.value = true;
  }
});
async function signIn() {
  const p = await perform("/auth/login", "POST", login.value);
  if (p) {
    profile.value = p;
    login.value.password = "";
    resetApi();
    await read(initialize);
  }
}
async function canLeave() {
  if (pending.value) return false;
  if (editing.value && editorRef.value && !(await editorRef.value.canLeave()))
    return false;
  if (adminRef.value && !(await adminRef.value.canLeave())) return false;
  if (masterRef.value && !(await masterRef.value.canLeave())) return false;
  return true;
}
async function navigate(code) {
  if (!(await canLeave())) return;
  view.value = code;
  selected.value = null;
  editing.value = false;
  mobile.value = false;
  notice.value = null;
  await read(refresh);
}
async function refresh() {
  if (view.value === "orders") await loadOrders();
  if (view.value === "reports") report.value = await api("/reports");
  if (view.value === "audit") audit.value = await api("/audit");
}
async function search() {
  page.value = 1;
  await read(loadOrders);
}
async function movePage(n) {
  page.value += n;
  await read(loadOrders);
}
async function openOrder(id) {
  if (!(await canLeave())) return;
  await read(async () => {
    selected.value = await api("/orders/" + id);
    detailTab.value = "summary";
    editing.value = false;
  });
}
async function back() {
  if (await canLeave()) {
    selected.value = null;
    editing.value = false;
    await read(loadOrders);
  }
}
async function saveQuote(body) {
  body.requestId = crypto.randomUUID();
  const out = await perform(
    "/orders" + (selected.value ? "/" + selected.value.id : ""),
    selected.value ? "PUT" : "POST",
    body,
  );
  if (out) {
    selected.value = out;
    editing.value = false;
    detailTab.value = "summary";
    await read(loadOrders);
  }
}
async function submitAction(body) {
  const out = await perform(
    "/orders/" + selected.value.id + "/" + action.value,
    "POST",
    {
      ...body,
      version: selected.value.version,
      requestId: crypto.randomUUID(),
    },
  );
  if (out) {
    action.value = null;
    if (isCustomer.value && out.state === "CHANGES") {
      selected.value = null;
      await read(loadOrders);
    } else {
      selected.value = out;
      await read(async () => {
        await loadOptions();
        await loadOrders();
      });
    }
  }
}
async function deleteDraft() {
  if (
    !(await ask(
      t(
        "删除这笔未发布、无文件的报价草稿？",
        "Delete this unpublished quote draft without files?",
      ),
    ))
  )
    return;
  const r = await perform("/orders/" + selected.value.id + "/delete", "POST", {
    version: selected.value.version,
  });
  if (r) {
    selected.value = null;
    await read(loadOrders);
  }
}
async function uploadFile(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  const body = new FormData();
  body.append("file", file);
  body.append("kind", fileKind.value);
  body.append("version", selected.value.version);
  body.append("requestId", crypto.randomUUID());
  const out = await perform(
    "/orders/" + selected.value.id + "/files",
    "POST",
    body,
  );
  if (out) selected.value = out;
  e.target.value = "";
}
async function downloadFile(f) {
  await read(() => download("/files/" + f.id, f.filename));
}
async function exportOrders() {
  await read(() =>
    download("/orders/export", "orders.csv", { method: "POST", body: {} }),
  );
}
async function logout() {
  if (!(await canLeave()) || !(await ask(t("退出当前账号？", "Sign out?"))))
    return;
  const r = await perform("/auth/logout", "POST", {});
  if (r) {
    profile.value = null;
    selected.value = null;
    editing.value = false;
    resetApi();
  }
}
async function changePassword() {
  const r = await perform("/auth/password", "POST", passwordForm.value);
  if (r) {
    passwordForm.value = null;
    profile.value = null;
    selected.value = null;
    resetApi();
  }
}
function printOrder() {
  window.print();
}
const eventFieldName = (k) =>
  ({
    quoteRevision: t("报价版本", "Quote revision"),
    fileId: t("文件编号", "File number"),
    kind: t("文件类型", "File kind"),
    filename: t("文件", "File"),
    assigneeId: t("制作员工", "Operator"),
    proofId: t("稿件编号", "Proof number"),
    lineId: t("订单行", "Order line"),
    quantity: t("数量", "Quantity"),
    sourceId: t("原流水", "Original entry"),
    good: t("良品", "Good"),
    scrap: t("报废", "Scrap"),
    minutes: t("分钟", "Minutes"),
    hourlyRate: t("小时成本", "Hourly cost"),
    amount: t("金额", "Amount"),
    taskId: t("委外任务", "Outsourced task"),
    supplierId: t("供应商", "Supplier"),
    reference: t("实际凭据", "Actual reference"),
    method: t("交货方式", "Delivery method"),
    address: t("地点", "Location"),
    description: t("加工说明", "Work description"),
    dueDate: t("日期", "Date"),
  })[k] || k;
const auditName = (code) =>
  code.startsWith("ORDER_")
    ? actionName(code.slice(6))
    : {
        LOGIN: t("登录", "Signed in"),
        PASSWORD_CHANGE: t("修改密码", "Changed password"),
        CUSTOMER_SAVE: t("维护客户", "Updated customer"),
        MATERIAL_SAVE: t("维护物料", "Updated material"),
        MATERIAL_RECEIVE: t("材料入库", "Received material"),
        SUPPLIER_SAVE: t("维护供应商", "Updated supplier"),
      }[code] || code;
</script>
<template>
  <div v-if="!ready" class="loading">{{ t("正在加载…", "Loading…") }}</div>
  <div v-else-if="!profile" class="login-page">
    <button
      class="language-toggle"
      @click="language = language === 'zh' ? 'en' : 'zh'"
    >
      {{ language === "zh" ? "English" : "中文" }}
    </button>
    <form class="login-card" @submit.prevent="signIn">
      <img class="login-logo" src="/brand/logo.jpg" alt="知华科技" />
      <h1>PrintDesk</h1>
      <p>
        {{
          t("印刷与广告制作订单经营", "Print & custom production management")
        }}
      </p>
      <div v-if="notice" class="notice" :class="notice.type" role="alert">
        {{ notice.text }}
      </div>
      <label
        >{{ t("登录账号", "Username")
        }}<input
          v-model="login.username"
          required
          autocomplete="username"
          maxlength="60" /></label
      ><label
        >{{ t("密码", "Password")
        }}<input
          v-model="login.password"
          type="password"
          required
          autocomplete="current-password" /></label
      ><button class="primary" :disabled="pending">
        {{ t("登录", "Sign in") }}</button
      ><small>{{
        t(
          "客户和员工使用各自账号。",
          "Customers and staff use their own accounts.",
        )
      }}</small>
    </form>
    <footer class="login-footer">
      {{
        t(
          "知华科技 · 公开源码学习版，商用需授权",
          "ZhiHua Technology · Non-commercial source edition",
        )
      }}
      ·
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
        t("官网", "Website")
      }}</a>
    </footer>
  </div>
  <div v-else class="app-shell">
    <aside class="sidebar" :class="{ open: mobile }">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" /><span
          >PrintDesk<small>{{
            t("知华印刷经营", "ZhiHua print operations")
          }}</small></span
        ><button
          class="mobile-close icon-button"
          :aria-label="t('关闭菜单', 'Close menu')"
          @click="mobile = false"
        >
          <X :size="20" />
        </button>
      </div>
      <nav>
        <button
          v-for="m in menus"
          :key="m.code"
          :class="{ active: view === m.code }"
          @click="navigate(m.code)"
        >
          <component :is="icons[m.code]" :size="18" />{{ t(m.name, m.nameEn) }}
        </button>
      </nav>
      <div class="sidebar-footer">
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
          t("知华科技官网", "ZhiHua website")
        }}</a
        ><button @click="about = true">
          {{ t("关于与商业咨询", "About & contact") }}</button
        ><small>{{
          t("公开源码学习版", "Non-commercial source edition")
        }}</small>
      </div>
    </aside>
    <main class="workspace">
      <header class="topbar">
        <button
          class="mobile-menu icon-button"
          :aria-label="t('打开菜单', 'Open menu')"
          @click="mobile = true"
        >
          <Menu :size="20" /></button
        ><span>{{
          isCustomer
            ? t("客户工作台", "Customer workspace")
            : t("制作经营工作台", "Production workspace")
        }}</span
        ><span class="grow"></span
        ><button @click="language = language === 'zh' ? 'en' : 'zh'">
          {{ language === "zh" ? "English" : "中文" }}</button
        ><button @click="passwordForm = { oldPassword: '', newPassword: '' }">
          {{ profile.displayName }}</button
        ><button
          class="icon-button"
          :aria-label="t('退出', 'Sign out')"
          @click="logout"
        >
          <LogOut :size="18" />
        </button>
      </header>
      <div class="content">
        <div v-if="notice" class="notice" :class="notice.type" role="status">
          {{ notice.text
          }}<button
            :aria-label="t('关闭提示', 'Dismiss')"
            @click="notice = null"
          >
            ×
          </button>
        </div>
        <template v-if="view === 'orders'">
          <div class="page-heading">
            <div>
              <h1>{{ selected ? selected.number : menuName(view) }}</h1>
              <p v-if="selected">
                {{ selected.customerName }} · {{ selected.title }}
              </p>
              <p v-else>
                {{
                  isCustomer
                    ? t(
                        "报价、稿件确认及交货进度",
                        "Quotes, proof approval and deliveries",
                      )
                    : t(
                        "从报价到制作、交货与结清",
                        "From quote to production, delivery and settlement",
                      )
                }}
              </p>
            </div>
            <div class="actions">
              <button v-if="selected || editing" @click="back">
                <ArrowLeft :size="16" />{{ t("订单列表", "Orders") }}</button
              ><button v-else @click="read(refresh)">
                <RefreshCw :size="16" />{{ t("刷新", "Refresh") }}</button
              ><button
                v-if="!selected && !editing && has('sales')"
                class="primary"
                @click="editing = true"
              >
                <Plus :size="16" />{{ t("新建报价", "New quote") }}</button
              ><button v-if="selected && !editing" @click="printOrder">
                <Printer :size="16" />{{ t("打印", "Print") }}
              </button>
            </div>
          </div>
          <QuoteEditor
            v-if="editing"
            ref="editorRef"
            :key="selected?.id || 'new'"
            :order="selected"
            :options="options"
            :pending="pending"
            @save="saveQuote"
            @cancel="back"
          />
          <template v-else-if="!selected"
            ><div class="toolbar">
              <label class="search-label"
                >{{ t("搜索订单", "Search orders")
                }}<input
                  v-model="q"
                  type="search"
                  :placeholder="
                    t('客户、名称或单号', 'Customer, title or number')
                  "
                  @keyup.enter="search" /></label
              ><label
                >{{ t("状态", "State")
                }}<select v-model="filter" @change="search">
                  <option value="">{{ t("全部状态", "All states") }}</option>
                  <option
                    v-for="s in [
                      'DRAFT',
                      'QUOTED',
                      'CHANGES',
                      'ACCEPTED',
                      'IN_PROGRESS',
                      'READY',
                      'PART_DELIVERED',
                      'DELIVERED',
                      'CLOSED',
                      'CANCELLED',
                    ]"
                    :key="s"
                    :value="s"
                  >
                    {{ stateName(s) }}
                  </option>
                </select></label
              ><label
                >{{ t("排序", "Sort")
                }}<select v-model="sort" @change="search">
                  <option value="newest">{{ t("最新订单", "Newest") }}</option>
                  <option value="due">{{ t("交期优先", "Due date") }}</option>
                  <option value="customer">{{ t("客户", "Customer") }}</option>
                </select></label
              ><button @click="search">{{ t("查询", "Search") }}</button
              ><span class="grow"></span
              ><button v-if="has('export')" @click="exportOrders">
                <FileDown :size="16" />CSV
              </button>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{{ t("订单", "Order") }}</th>
                    <th>{{ t("客户", "Customer") }}</th>
                    <th>{{ t("状态", "State") }}</th>
                    <th>{{ t("交期", "Due") }}</th>
                    <th v-if="!isCustomer">{{ t("制作员工", "Operator") }}</th>
                    <th v-if="orders.items.some((o) => o.total != null)">
                      {{ t("报价 / 净收款", "Quote / net payments") }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="o in orders.items"
                    :key="o.id"
                    class="click-row"
                    @click="openOrder(o.id)"
                  >
                    <td>
                      <button class="link" @click.stop="openOrder(o.id)">
                        {{ o.number }}</button
                      ><small>{{ o.title }}</small>
                    </td>
                    <td>{{ o.customerName }}</td>
                    <td>
                      <span class="badge" :class="o.state">{{
                        stateName(o.state)
                      }}</span>
                    </td>
                    <td class="nowrap">{{ o.dueDate }}</td>
                    <td v-if="!isCustomer">{{ actorName(o.assigneeId) }}</td>
                    <td v-if="o.total != null" class="numeric">
                      {{ money(o.total, o.currency)
                      }}<small>{{ money(o.paid, o.currency) }}</small>
                    </td>
                  </tr>
                  <tr v-if="!orders.items.length">
                    <td colspan="6" class="empty">
                      {{ t("没有符合条件的订单。", "No matching orders.") }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pagination">
              <span>{{ orders.total }} {{ t("笔订单", "orders") }}</span
              ><button :disabled="page === 1" @click="movePage(-1)">
                {{ t("上一页", "Previous") }}</button
              ><span>{{ page }} / {{ maxPage }}</span
              ><button :disabled="page >= maxPage" @click="movePage(1)">
                {{ t("下一页", "Next") }}
              </button>
            </div></template
          >
          <template v-else
            ><div class="order-meta">
              <span class="badge" :class="selected.state">{{
                stateName(selected.state)
              }}</span
              ><span
                >{{ t("报价版本", "Quote revision") }}
                {{ selected.revision }}</span
              ><span>{{ t("交期", "Due") }} {{ selected.dueDate }}</span
              ><span v-if="!isCustomer">{{
                actorName(selected.assigneeId)
              }}</span
              ><span class="grow"></span
              ><strong v-if="selected.total != null">{{
                money(selected.total, selected.currency)
              }}</strong>
            </div>
            <div class="order-actions">
              <button v-if="canEdit" class="primary" @click="editing = true">
                {{ t("编辑报价", "Edit quote") }}</button
              ><button
                v-if="
                  canEdit && selected.revision === 0 && !selected.files.length
                "
                @click="deleteDraft"
              >
                {{ t("删除草稿", "Delete draft") }}</button
              ><button
                v-for="a in visibleActions"
                :key="a"
                :disabled="pending"
                @click="action = a"
              >
                {{ actionName(a) }}
              </button>
            </div>
            <div class="tabs">
              <button
                v-for="k in visibleTabs"
                :key="k"
                :class="{ active: detailTab === k }"
                @click="detailTab = k"
              >
                {{ tabNames[k] }}
              </button>
            </div>
            <section v-if="detailTab === 'summary'" class="panel">
              <div class="section-heading">
                <h2>{{ t("订单明细", "Order lines") }}</h2>
                <span>{{ selected.currency }}</span>
              </div>
              <p v-if="selected.instructions" class="instructions">
                {{ selected.instructions }}
              </p>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("制作内容", "Work") }}</th>
                      <th>{{ t("规格 / 材料", "Size / material") }}</th>
                      <th>{{ t("数量", "Quantity") }}</th>
                      <th>
                        {{
                          t("良品 / 质检 / 已交", "Good / accepted / delivered")
                        }}
                      </th>
                      <th v-if="selected.total != null">
                        {{ t("行报价", "Line quote") }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="l in selected.lines" :key="l.id">
                      <td>{{ l.name }}</td>
                      <td>
                        {{
                          l.widthMm && l.heightMm
                            ? l.widthMm + " × " + l.heightMm + " mm"
                            : ""
                        }}<small>{{ l.materialName }} · {{ l.unit }}</small>
                      </td>
                      <td class="numeric">{{ l.quantity }}</td>
                      <td class="numeric">
                        {{ l.good }} / {{ l.accepted }} / {{ l.shipped }}
                      </td>
                      <td v-if="l.quoted != null" class="numeric">
                        {{ money(l.quoted, selected.currency) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <dl v-if="selected.total != null" class="totals">
                <dt>{{ t("小计", "Subtotal") }}</dt>
                <dd>{{ money(selected.subtotal, selected.currency) }}</dd>
                <dt>{{ t("折扣", "Discount") }}</dt>
                <dd>−{{ money(selected.discount, selected.currency) }}</dd>
                <dt>{{ t("手工税率", "Manual tax") }}</dt>
                <dd>{{ selected.taxPercent }}%</dd>
                <dt>{{ t("应收", "Total due") }}</dt>
                <dd>
                  <strong>{{
                    money(selected.total, selected.currency)
                  }}</strong>
                </dd>
                <dt>{{ t("净收款", "Net payments") }}</dt>
                <dd>{{ money(selected.paid, selected.currency) }}</dd>
              </dl>
              <details v-if="selected.quotes?.length">
                <summary>
                  {{ t("已发布报价版本", "Published quote revisions") }}
                </summary>
                <div
                  v-for="r in selected.quotes"
                  :key="r.id"
                  class="revision-row"
                >
                  <strong>V{{ r.number }}</strong
                  ><span>{{ dateTime(r.createdAt) }}</span
                  ><span>{{ JSON.parse(r.snapshot).title }}</span
                  ><span>{{
                    money(
                      JSON.parse(r.snapshot).total,
                      JSON.parse(r.snapshot).currency,
                    )
                  }}</span
                  ><span
                    >{{
                      r.decision
                        ? stateName(r.decision)
                        : t(
                            "未记录确认结果",
                            "Legacy confirmation unavailable",
                          )
                    }}<small v-if="r.reviewedAt"
                      >{{ reviewerName(r.reviewerId) }} ·
                      {{ dateTime(r.reviewedAt) }}</small
                    ></span
                  >
                </div>
              </details>
            </section>
            <section v-if="detailTab === 'files'" class="panel">
              <div class="section-heading">
                <h2>{{ t("文件与稿件版本", "Files & proof versions") }}</h2>
                <div v-if="canUpload" class="file-toolbar">
                  <select
                    v-model="fileKind"
                    :aria-label="t('上传类型', 'Upload type')"
                  >
                    <option value="SOURCE">
                      {{ t("原始资料", "Source material") }}
                    </option>
                    <option v-if="!isCustomer" value="PROOF">
                      {{ t("待确认稿件", "Proof for approval") }}
                    </option></select
                  ><label class="file-input"
                    >{{ t("上传文件", "Upload file")
                    }}<input
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg"
                      :disabled="pending"
                      @change="uploadFile"
                  /></label>
                </div>
              </div>
              <p class="muted">
                PDF / PNG / JPEG ·
                {{
                  t(
                    "单文件不超过5MB；旧版本保留。",
                    "Maximum 5MB per file; earlier versions are retained.",
                  )
                }}
              </p>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("文件", "File") }}</th>
                      <th>{{ t("类型", "Kind") }}</th>
                      <th>{{ t("状态", "Status") }}</th>
                      <th>{{ t("上传时间", "Uploaded") }}</th>
                      <th>{{ t("客户反馈", "Customer feedback") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="f in selected.files" :key="f.id">
                      <td>
                        <button class="link" @click="downloadFile(f)">
                          {{ f.filename }}</button
                        ><small
                          >{{ Math.ceil(f.size / 1024) }} KB · #{{ f.id }}
                          {{
                            f.id === selected.latestProofId
                              ? t("当前稿件", "Current proof")
                              : ""
                          }}</small
                        >
                      </td>
                      <td>
                        {{
                          f.kind === "PROOF"
                            ? t("稿件", "Proof")
                            : t("原始资料", "Source")
                        }}
                      </td>
                      <td>
                        {{ f.kind === "PROOF" ? stateName(f.status) : "—"
                        }}<small v-if="f.reviewedAt"
                          >{{ reviewerName(f.reviewerId) }} ·
                          {{ dateTime(f.reviewedAt) }}</small
                        >
                      </td>
                      <td>{{ dateTime(f.createdAt) }}</td>
                      <td>{{ f.feedback || "—" }}</td>
                    </tr>
                    <tr v-if="!selected.files.length">
                      <td colspan="5" class="empty">
                        {{ t("尚未上传文件。", "No uploaded files.") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section v-if="detailTab === 'production'" class="panel">
              <h2>{{ t("领退料流水", "Material entries") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("原流水", "Entry") }}</th>
                      <th>{{ t("订单行", "Line") }}</th>
                      <th>{{ t("类型", "Kind") }}</th>
                      <th>{{ t("数量", "Quantity") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="s in selected.stock" :key="s.id">
                      <td>#{{ s.id }}</td>
                      <td>
                        {{
                          selected.lines.find((l) => l.id === s.lineId)?.name ||
                          s.lineId
                        }}
                      </td>
                      <td>
                        {{
                          s.kind === "ISSUE"
                            ? t("领料", "Issue")
                            : t("退料", "Return")
                        }}
                      </td>
                      <td>{{ s.quantity }}</td>
                    </tr>
                    <tr v-if="!selected.stock.length">
                      <td colspan="4" class="empty">
                        {{ t("尚无领退料。", "No material entries.") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <h2>{{ t("实际报工", "Actual work reports") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("订单行", "Line") }}</th>
                      <th>{{ t("人员", "Operator") }}</th>
                      <th>{{ t("良品 / 报废", "Good / scrap") }}</th>
                      <th>{{ t("分钟", "Minutes") }}</th>
                      <th>{{ t("说明", "Note") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(w, i) in selected.work" :key="i">
                      <td>
                        {{
                          selected.lines.find((l) => l.id === w.lineId)?.name ||
                          w.lineId
                        }}
                      </td>
                      <td>{{ actorName(w.actorId) }}</td>
                      <td>{{ w.good }} / {{ w.scrap }}</td>
                      <td>{{ w.minutes }}</td>
                      <td>{{ w.note || "—" }}</td>
                    </tr>
                    <tr v-if="!selected.work.length">
                      <td colspan="5" class="empty">
                        {{ t("尚无报工。", "No work reports.") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section v-if="detailTab === 'vendor'" class="panel">
              <h2>{{ t("委外任务", "Outsourced tasks") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("供应商 / 内容", "Supplier / work") }}</th>
                      <th>{{ t("状态", "State") }}</th>
                      <th>{{ t("收回 / 约定数量", "Received / ordered") }}</th>
                      <th>
                        {{ t("约定 / 已确认成本", "Agreed / recognized cost") }}
                      </th>
                      <th>{{ t("收回日期 / 凭据", "Date / reference") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="v in selected.vendorTasks" :key="v.id">
                      <td>
                        #{{ v.id }} · {{ v.supplierName
                        }}<small>{{ v.description }}</small>
                      </td>
                      <td>{{ stateName(v.state) }}</td>
                      <td>{{ v.received }} / {{ v.quantity }}</td>
                      <td>
                        {{ money(v.amount, selected.currency)
                        }}<small>{{
                          money(v.recognized, selected.currency)
                        }}</small>
                      </td>
                      <td>
                        {{ v.dueDate }}<small>{{ v.reference }}</small>
                      </td>
                    </tr>
                    <tr v-if="!selected.vendorTasks.length">
                      <td colspan="5" class="empty">
                        {{ t("尚无委外任务。", "No outsourced tasks.") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section v-if="detailTab === 'vendor'" class="panel">
              <h2>{{ t("分批收回流水", "Partial receipt entries") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("时间 / 凭据", "Time / reference") }}</th>
                      <th>{{ t("委外任务", "Outsourced task") }}</th>
                      <th>{{ t("数量", "Quantity") }}</th>
                      <th>{{ t("本次确认成本", "Cost recognized now") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="r in selected.vendorReceipts" :key="r.id">
                      <td>
                        {{ r.reference
                        }}<small>{{ dateTime(r.createdAt) }}</small>
                      </td>
                      <td>#{{ r.taskId }}</td>
                      <td>{{ r.quantity }}</td>
                      <td>{{ money(r.amount, selected.currency) }}</td>
                    </tr>
                    <tr v-if="!selected.vendorReceipts?.length">
                      <td colspan="4" class="empty">
                        {{
                          t(
                            "尚无逐次收回流水。",
                            "No separate receipt entries.",
                          )
                        }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section v-if="detailTab === 'delivery'" class="panel">
              <h2>{{ t("分批交货记录", "Delivery records") }}</h2>
              <div
                v-for="d in selected.dispatches"
                :key="d.id"
                class="delivery-row"
              >
                <div>
                  <strong>{{ d.reference }}</strong> · {{ d.method }} ·
                  {{ dateTime(d.createdAt) }}
                </div>
                <p>{{ d.address }}</p>
                <ul>
                  <li v-for="(l, i) in JSON.parse(d.lines)" :key="i">
                    {{ l.name }} × {{ l.quantity }}
                  </li>
                </ul>
              </div>
              <p v-if="!selected.dispatches.length" class="empty">
                {{ t("尚无交货记录。", "No delivery records.") }}
              </p>
            </section>
            <section v-if="detailTab === 'finance'" class="panel">
              <h2>
                {{
                  t("订单成本与外部收退款", "Order costs & external payments")
                }}
              </h2>
              <dl class="cost-grid">
                <div
                  v-for="(label, k) in {
                    materialCost: t('材料净成本', 'Net material cost'),
                    laborCost: t('实际工时成本', 'Recorded labor cost'),
                    vendorCost: t('已收回委外成本', 'Received outsource cost'),
                    cost: t('已登记总成本', 'Total recorded cost'),
                  }"
                  :key="k"
                >
                  <dt>{{ label }}</dt>
                  <dd>{{ money(selected[k], selected.currency) }}</dd>
                </div>
              </dl>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("流水", "Entry") }}</th>
                      <th>{{ t("类型", "Kind") }}</th>
                      <th>{{ t("金额", "Amount") }}</th>
                      <th>{{ t("原收款", "Original payment") }}</th>
                      <th>{{ t("凭据 / 说明", "Reference / note") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="p in selected.payments" :key="p.id">
                      <td>
                        #{{ p.id }}<small>{{ dateTime(p.createdAt) }}</small>
                      </td>
                      <td>
                        {{
                          p.kind === "RECEIVE"
                            ? t("收款", "Payment")
                            : t("退款", "Refund")
                        }}
                      </td>
                      <td class="numeric">
                        {{ money(p.amount, selected.currency) }}
                      </td>
                      <td>{{ p.sourceId ? "#" + p.sourceId : "—" }}</td>
                      <td>
                        {{ p.reference }}<small>{{ p.note }}</small>
                      </td>
                    </tr>
                    <tr v-if="!selected.payments.length">
                      <td colspan="5" class="empty">
                        {{ t("尚无收退款记录。", "No payment records.") }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section v-if="detailTab === 'history'" class="panel">
              <h2>{{ t("订单履历", "Order history") }}</h2>
              <ol class="timeline">
                <li v-for="(e, i) in selected.events" :key="i">
                  <time>{{ dateTime(e.createdAt) }}</time
                  ><strong>{{ actionName(e.action) }}</strong
                  ><span>{{ actorName(e.actorId) }}</span>
                  <p v-if="e.note">{{ e.note }}</p>
                  <dl
                    v-if="e.details && Object.keys(e.details).length"
                    class="event-details"
                  >
                    <template v-for="(v, k) in e.details" :key="k"
                      ><dt>{{ eventFieldName(k) }}</dt>
                      <dd>
                        {{ k === "assigneeId" ? actorName(Number(v)) : v }}
                      </dd></template
                    >
                  </dl>
                </li>
              </ol>
            </section>
          </template>
        </template>
        <template v-else-if="view === 'masters'"
          ><div class="page-heading">
            <h1>{{ menuName(view) }}</h1>
          </div>
          <MasterPanel
            ref="masterRef"
            :options="options"
            :pending="pending"
            :perform="perform"
            @error="fail"
            @refresh="read(loadOptions)"
        /></template>
        <template v-else-if="['users', 'roles', 'settings'].includes(view)">
          <AdminPanel
            :key="view"
            ref="adminRef"
            :section="view"
            :pending="pending"
            :message="notice?.type === 'error' ? notice.text : ''"
            :perform="perform"
            @error="fail"
        /></template>
        <template v-else-if="view === 'reports' && report"
          ><div class="page-heading">
            <div>
              <h1>{{ menuName(view) }}</h1>
              <p>
                {{
                  t(
                    "仅汇总当前数据范围；不同币种分别统计。",
                    "Current data scope only; currencies are reported separately.",
                  )
                }}
              </p>
            </div>
            <button @click="read(refresh)">{{ t("刷新", "Refresh") }}</button>
          </div>
          <div class="stats-strip">
            <div>
              <span>{{ t("订单总数", "Orders") }}</span
              ><strong>{{ report.total }}</strong>
            </div>
            <div>
              <span>{{ t("未交付超期", "Overdue open orders") }}</span
              ><strong>{{ report.late }}</strong>
            </div>
            <div v-for="(count, state) in report.states" :key="state">
              <span>{{ stateName(state) }}</span
              ><strong>{{ count }}</strong>
            </div>
          </div>
          <div class="panel">
            <h2>{{ t("经营金额", "Business amounts") }}</h2>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{{ t("币种", "Currency") }}</th>
                    <th>{{ t("已发报价", "Issued quotes") }}</th>
                    <th>{{ t("净收款", "Net payments") }}</th>
                    <th>{{ t("已登记成本", "Recorded cost") }}</th>
                    <th>
                      {{ t("结清单不含税差额", "Closed-order pre-tax margin") }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(v, c) in report.currencies" :key="c">
                    <td>{{ c }}</td>
                    <td>{{ money(v.quoted || 0, c) }}</td>
                    <td>{{ money(v.received || 0, c) }}</td>
                    <td>{{ money(v.cost || 0, c) }}</td>
                    <td>{{ money(v.closedMargin || 0, c) }}</td>
                  </tr>
                  <tr v-if="!Object.keys(report.currencies).length">
                    <td colspan="5" class="empty">
                      {{ t("尚无订单数据。", "No order data.") }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div></template
        >
        <template v-else-if="view === 'audit'"
          ><div class="page-heading">
            <h1>{{ menuName(view) }}</h1>
            <button @click="read(refresh)">{{ t("刷新", "Refresh") }}</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("账号", "Account") }}</th>
                  <th>{{ t("动作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in audit.slice(0, 200)" :key="e.id">
                  <td>{{ dateTime(e.createdAt) }}</td>
                  <td>{{ e.actor }}</td>
                  <td>{{ auditName(e.action) }}</td>
                  <td>{{ e.objectId }}</td>
                </tr>
                <tr v-if="!audit.length">
                  <td colspan="4" class="empty">
                    {{ t("暂无记录。", "No records.") }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div></template
        >
      </div>
    </main>
  </div>
  <ActionDialog
    v-if="action && selected"
    :key="action"
    :action="action"
    :order="selected"
    :options="options"
    :pending="pending"
    @cancel="action = null"
    @submit="submitAction"
  />
  <div v-if="confirmation" class="modal-backdrop">
    <div class="dialog small-dialog" role="dialog" aria-modal="true">
      <h2>{{ t("确认操作", "Confirm action") }}</h2>
      <p>{{ confirmation.message }}</p>
      <div class="dialog-actions">
        <button @click="answerConfirmation(false)">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" @click="answerConfirmation(true)">
          {{ t("确认", "Confirm") }}
        </button>
      </div>
    </div>
  </div>
  <div v-if="passwordForm" class="modal-backdrop">
    <form class="dialog small-dialog" @submit.prevent="changePassword">
      <h2>{{ t("修改本人密码", "Change your password") }}</h2>
      <label
        >{{ t("原密码", "Current password")
        }}<input
          v-model="passwordForm.oldPassword"
          type="password"
          required
          autocomplete="current-password" /></label
      ><label
        >{{ t("新密码", "New password")
        }}<input
          v-model="passwordForm.newPassword"
          type="password"
          required
          minlength="12"
          autocomplete="new-password"
      /></label>
      <div class="dialog-actions">
        <button type="button" @click="passwordForm = null">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" :disabled="pending">
          {{ t("保存并重新登录", "Save & sign in again") }}
        </button>
      </div>
    </form>
  </div>
  <div v-if="about" class="modal-backdrop">
    <div class="dialog about-dialog">
      <div class="section-heading">
        <h2>PrintDesk 1.0.0</h2>
        <button @click="about = false">×</button>
      </div>
      <p>
        {{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}
      </p>
      <p>
        {{
          t(
            "公开源码学习版，未经书面授权不得商用。",
            "Source-available for non-commercial learning. Commercial use requires written authorization.",
          )
        }}
      </p>
      <p>
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >www.zhuatech.cn</a
        >
      </p>
      <template v-if="language === 'zh'"
        ><p>商业授权或深度定制开发请联系知华科技。</p>
        <div class="contact-grid">
          <figure>
            <img src="/brand/wechat-zhuatech.png" alt="微信 zhuatech" />
            <figcaption>zhuatech</figcaption>
          </figure>
          <figure>
            <img src="/brand/wechat-zhuatech2.png" alt="微信 zhuatech2" />
            <figcaption>zhuatech2</figcaption>
          </figure>
        </div></template
      ><template v-else
        ><p>
          <a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a> ·
          <a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a>
        </p>
        <p>
          <a href="https://wa.me/8617521234993" target="_blank" rel="noopener"
            >WhatsApp +86 17521234993</a
          >
        </p></template
      >
    </div>
  </div>
</template>
