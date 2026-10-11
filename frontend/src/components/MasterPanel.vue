<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, watch } from "vue";
import { api } from "../api.js";
import { t, cloneRecord, ask, money } from "../ui.js";
const props = defineProps({
  options: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["error", "refresh"]);
const kind = ref("customers"),
  rows = ref([]),
  q = ref(""),
  page = ref(1),
  editing = ref(null),
  receipt = ref(null),
  initial = ref("");
const labels = computed(() => ({
  customers: t("客户", "Customers"),
  materials: t("物料与库存", "Materials & stock"),
  suppliers: t("委外供应商", "Suppliers"),
}));
const filtered = computed(() =>
  rows.value.filter((r) =>
    JSON.stringify(r).toLowerCase().includes(q.value.toLowerCase()),
  ),
);
const visible = computed(() =>
  filtered.value.slice((page.value - 1) * 15, page.value * 15),
);
async function load() {
  try {
    rows.value = await api("/masters/" + kind.value);
  } catch (e) {
    emit("error", e);
  }
}
watch(
  kind,
  () => {
    q.value = "";
    page.value = 1;
    load();
  },
  { immediate: true },
);
watch(q, () => (page.value = 1));
async function canLeave() {
  return (
    !editing.value ||
    JSON.stringify(editing.value) === initial.value ||
    (await ask(t("放弃未保存的修改？", "Discard unsaved changes?")))
  );
}
defineExpose({ canLeave });
async function switchKind(k) {
  if (await canLeave()) {
    kind.value = k;
    editing.value = null;
    receipt.value = null;
  }
}
function edit(row) {
  editing.value = row
    ? cloneRecord(row)
    : {
        departmentId: props.options.departments[0]?.id || "",
        name: "",
        contact: "",
        email: "",
        address: "",
        accountId: "",
        enabled: true,
        code: "",
        unit: "PIECE",
        currency: "CNY",
      };
  initial.value = JSON.stringify(editing.value);
}
async function save() {
  const r = editing.value;
  const out = await props.perform(
    "/masters/" + kind.value + (r.id ? "/" + r.id : ""),
    r.id ? "PUT" : "POST",
    r,
  );
  if (out) {
    editing.value = null;
    await load();
    emit("refresh");
  }
}
async function saveReceipt() {
  const r = receipt.value;
  const out = await props.perform("/materials/" + r.id + "/receive", "POST", r);
  if (out) {
    receipt.value = null;
    await load();
    emit("refresh");
  }
}
</script>
<template>
  <section>
    <div class="tabs">
      <button
        v-for="(label, k) in labels"
        :key="k"
        :class="{ active: kind === k }"
        @click="switchKind(k)"
      >
        {{ label }}
      </button>
    </div>
    <div class="toolbar">
      <label class="search-label"
        >{{ t("搜索", "Search")
        }}<input
          v-model="q"
          type="search"
          :placeholder="t('名称或编码', 'Name or code')" /></label
      ><span class="grow"></span
      ><button class="primary" @click="edit(null)">
        {{ t("新增", "Add") }} {{ labels[kind] }}
      </button>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>{{ t("名称", "Name") }}</th>
            <th>{{ t("组织", "Organization") }}</th>
            <template v-if="kind === 'materials'"
              ><th>{{ t("单位 / 币种", "Unit / currency") }}</th>
              <th>{{ t("库存", "Stock") }}</th>
              <th>{{ t("平均成本", "Average cost") }}</th></template
            >
            <th>{{ t("状态", "Status") }}</th>
            <th>{{ t("操作", "Actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in visible" :key="r.id">
            <td>
              <strong>{{ r.name }}</strong
              ><small>{{ r.code || r.contact || "" }}</small>
            </td>
            <td>
              {{
                options.departments.find((d) => d.id === r.departmentId)
                  ?.name || r.departmentId
              }}
            </td>
            <template v-if="kind === 'materials'"
              ><td>{{ r.unit }} / {{ r.currency }}</td>
              <td class="numeric">{{ r.stock }}</td>
              <td class="numeric">{{ money(r.cost, r.currency) }}</td></template
            >
            <td>
              {{ r.enabled ? t("启用", "Enabled") : t("停用", "Disabled") }}
            </td>
            <td class="row-actions">
              <button @click="edit(r)">{{ t("编辑", "Edit") }}</button
              ><button
                v-if="kind === 'materials' && r.enabled"
                @click="
                  receipt = {
                    id: r.id,
                    version: r.version,
                    name: r.name,
                    currency: r.currency,
                    quantity: '',
                    cost: '',
                    reference: '',
                    note: '',
                  }
                "
              >
                {{ t("入库", "Receive") }}
              </button>
            </td>
          </tr>
          <tr v-if="!visible.length">
            <td colspan="7" class="empty">
              {{ t("没有符合条件的记录。", "No matching records.") }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination">
      <span>{{ filtered.length }} {{ t("条", "records") }}</span
      ><button :disabled="page === 1" @click="page--">
        {{ t("上一页", "Previous") }}</button
      ><span>{{ page }}</span
      ><button :disabled="page * 15 >= filtered.length" @click="page++">
        {{ t("下一页", "Next") }}
      </button>
    </div>
    <div v-if="editing" class="modal-backdrop">
      <form class="dialog" @submit.prevent="save">
        <div class="section-heading">
          <h2>{{ labels[kind] }}</h2>
          <button type="button" @click="editing = null">×</button>
        </div>
        <div class="form-grid">
          <label
            >{{ t("名称", "Name")
            }}<input v-model="editing.name" required maxlength="120" /></label
          ><label
            >{{ t("组织", "Organization")
            }}<select
              v-model="editing.departmentId"
              :disabled="!!editing.id"
              required
            >
              <option
                v-for="d in options.departments"
                :key="d.id"
                :value="d.id"
              >
                {{ d.name }}
              </option>
            </select></label
          >
          <template v-if="kind === 'materials'"
            ><label
              >{{ t("编码", "Code")
              }}<input
                v-model="editing.code"
                :disabled="!!editing.id"
                required
                pattern="[A-Z0-9_.-]{2,60}" /></label
            ><label
              >{{ t("单位", "Unit")
              }}<select v-model="editing.unit" :disabled="!!editing.id">
                <option value="PIECE">{{ t("件", "Piece") }}</option>
                <option value="SHEET">{{ t("张", "Sheet") }}</option>
                <option value="M2">{{ t("平方米", "Square metre") }}</option>
              </select></label
            ><label
              >{{ t("币种", "Currency")
              }}<select v-model="editing.currency" :disabled="!!editing.id">
                <option>CNY</option>
                <option>USD</option>
                <option>EUR</option>
                <option>GBP</option>
              </select></label
            ></template
          >
          <label v-else
            >{{ t("联系资料", "Contact")
            }}<input v-model="editing.contact" maxlength="120"
          /></label>
          <template v-if="kind === 'customers'"
            ><label
              >{{ t("邮箱", "Email")
              }}<input
                v-model="editing.email"
                type="email"
                maxlength="160" /></label
            ><label class="span2"
              >{{ t("地址", "Address")
              }}<input v-model="editing.address" maxlength="600" /></label
            ><label class="span2"
              >{{ t("客户登录账号", "Customer login")
              }}<select v-model="editing.accountId">
                <option value="">{{ t("暂不绑定", "Unbound") }}</option>
                <option
                  v-for="a in options.accounts.filter(
                    (a) =>
                      a.scope === 'CUSTOMER' &&
                      a.departmentId == editing.departmentId,
                  )"
                  :key="a.id"
                  :value="a.id"
                >
                  {{ a.displayName }}
                </option>
              </select></label
            ></template
          >
          <label class="check-label"
            ><input v-model="editing.enabled" type="checkbox" />{{
              t("启用", "Enabled")
            }}</label
          >
        </div>
        <div class="dialog-actions">
          <button type="button" @click="editing = null">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{ t("保存", "Save") }}
          </button>
        </div>
      </form>
    </div>
    <div v-if="receipt" class="modal-backdrop">
      <form class="dialog" @submit.prevent="saveReceipt">
        <div class="section-heading">
          <h2>{{ t("实际材料入库", "Actual material receipt") }}</h2>
          <button type="button" @click="receipt = null">×</button>
        </div>
        <p>{{ receipt.name }} · {{ receipt.currency }}</p>
        <div class="form-grid">
          <label
            >{{ t("数量", "Quantity")
            }}<input
              v-model="receipt.quantity"
              required
              type="number"
              min="0.0001"
              step="0.0001" /></label
          ><label
            >{{ t("实际单位成本", "Actual unit cost")
            }}<input
              v-model="receipt.cost"
              required
              type="number"
              min="0"
              step="0.0001" /></label
          ><label class="span2"
            >{{ t("入库凭据号", "Receipt reference")
            }}<input
              v-model="receipt.reference"
              required
              maxlength="160" /></label
          ><label class="span2"
            >{{ t("备注", "Note")
            }}<textarea v-model="receipt.note" maxlength="600"></textarea>
          </label>
        </div>
        <div class="dialog-actions">
          <button type="button" @click="receipt = null">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{ t("确认入库", "Receive stock") }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
