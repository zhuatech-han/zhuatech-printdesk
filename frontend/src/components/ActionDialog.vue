<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed } from "vue";
import { t, actionName, money } from "../ui.js";
const props = defineProps({
  action: { type: String, required: true },
  order: { type: Object, required: true },
  options: { type: Object, required: true },
  pending: Boolean,
});
const emit = defineEmits(["submit", "cancel"]);
const form = ref({
  note: "",
  assigneeId: "",
  lineId: props.order.lines[0]?.id || "",
  quantity: "",
  sourceId: "",
  good: "",
  scrap: 0,
  minutes: "",
  hourlyRate: "",
  supplierId: "",
  description: "",
  dueDate: props.order.dueDate,
  amount: "",
  reference: "",
  method: "PICKUP",
  address: "",
  taskId: "",
  proofId: props.order.latestProofId,
  lines: props.order.lines.map((l) => ({
    lineId: l.id,
    name: l.name,
    quantity: l.accepted - l.shipped,
  })),
});
const include = (a) => a.includes(props.action);
const sources = computed(() =>
  props.action === "RETURN"
    ? props.order.stock.filter(
        (s) => s.kind === "ISSUE" && s.lineId == form.value.lineId,
      )
    : props.order.payments?.filter((p) => p.kind === "RECEIVE") || [],
);
const tasks = computed(
  () =>
    props.order.vendorTasks?.filter((v) =>
      props.action === "VENDOR_CONFIRM"
        ? v.state === "REQUESTED"
        : props.action === "VENDOR_RECEIVE"
          ? v.state === "CONFIRMED"
          : ["REQUESTED", "CONFIRMED"].includes(v.state),
    ) || [],
);
const operators = computed(() =>
  props.options.accounts.filter(
    (a) => a.production && a.departmentId === props.order.departmentId,
  ),
);
const noteRequired = computed(() =>
  include([
    "REVISE",
    "REJECT",
    "PROOF_REJECT",
    "QUALITY_FAIL",
    "CANCEL",
    "VENDOR_CANCEL",
    "NOTE",
    "ISSUE",
    "RETURN",
    "RECEIVE",
    "REFUND",
  ]),
);
function submit() {
  const b = { ...form.value };
  if (props.action === "DELIVER")
    b.lines = b.lines
      .filter((l) => Number(l.quantity) > 0)
      .map((l) => ({ lineId: l.lineId, quantity: l.quantity }));
  emit("submit", b);
}
</script>
<template>
  <div class="modal-backdrop" @click.self="emit('cancel')">
    <form
      class="dialog action-dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="actionName(action)"
      @submit.prevent="submit"
    >
      <div class="section-heading">
        <h2>{{ actionName(action) }}</h2>
        <button
          type="button"
          :aria-label="t('关闭', 'Close')"
          @click="emit('cancel')"
        >
          ×
        </button>
      </div>
      <p class="muted">{{ order.number }} · {{ order.title }}</p>
      <p v-if="include(['RECEIVE', 'REFUND'])" class="notice info">
        {{
          t(
            "只登记在系统外实际完成的收款或退款，不会发起扣款。",
            "Record a payment or refund completed outside this system. No charge is initiated.",
          )
        }}
      </p>
      <p v-if="include(['PROOF_ACCEPT', 'PROOF_REJECT'])" class="notice info">
        {{
          t(
            "请先下载并核对当前稿件；确认只针对这个文件版本。",
            "Download and check the current proof first. Approval applies to this file version only.",
          )
        }}
      </p>
      <div class="form-grid">
        <label v-if="action === 'ASSIGN'" class="span2"
          >{{ t("制作员工", "Operator")
          }}<select v-model="form.assigneeId" required>
            <option value="">{{ t("请选择", "Select") }}</option>
            <option v-for="a in operators" :key="a.id" :value="a.id">
              {{ a.displayName }}
            </option>
          </select></label
        >
        <label
          v-if="include(['ISSUE', 'RETURN', 'WORK', 'VENDOR_CREATE'])"
          class="span2"
          >{{ t("订单行", "Order line")
          }}<select v-model="form.lineId" required>
            <option v-for="l in order.lines" :key="l.id" :value="l.id">
              {{ l.name }} · {{ l.quantity }}
            </option>
          </select></label
        >
        <label v-if="include(['RETURN', 'REFUND'])" class="span2"
          >{{ t("原始流水", "Original entry")
          }}<select v-model="form.sourceId" required>
            <option value="">
              {{ t("选择原始记录", "Select original entry") }}
            </option>
            <option v-for="p in sources" :key="p.id" :value="p.id">
              #{{ p.id }} · {{ p.reference }} ·
              {{
                action === "RETURN"
                  ? p.quantity
                  : money(p.amount, order.currency)
              }}
            </option>
          </select></label
        >
        <label
          v-if="include(['ISSUE', 'RETURN', 'VENDOR_CREATE', 'VENDOR_RECEIVE'])"
          >{{ t("数量", "Quantity")
          }}<input
            v-model="form.quantity"
            required
            type="number"
            :min="include(['VENDOR_CREATE', 'VENDOR_RECEIVE']) ? 1 : 0.0001"
            :step="
              include(['VENDOR_CREATE', 'VENDOR_RECEIVE']) ? '1' : '0.0001'
            "
        /></label>
        <template v-if="action === 'WORK'"
          ><label
            >{{ t("本次良品（件）", "Good output (pieces)")
            }}<input
              v-model="form.good"
              required
              type="number"
              min="0"
              step="1" /></label
          ><label
            >{{ t("本次报废（件）", "Scrap (pieces)")
            }}<input
              v-model="form.scrap"
              required
              type="number"
              min="0"
              step="1" /></label
          ><label
            >{{ t("实际工时（分钟）", "Actual minutes")
            }}<input
              v-model="form.minutes"
              required
              type="number"
              min="1"
              step="1" /></label
          ><label
            >{{ t("约定小时成本", "Agreed hourly cost") }} · {{ order.currency
            }}<input
              v-model="form.hourlyRate"
              required
              type="number"
              min="0"
              step="0.01" /></label
        ></template>
        <template v-if="action === 'VENDOR_CREATE'"
          ><label
            >{{ t("委外供应商", "Supplier")
            }}<select v-model="form.supplierId" required>
              <option value="">{{ t("请选择", "Select") }}</option>
              <option
                v-for="s in options.suppliers.filter(
                  (s) => s.departmentId === order.departmentId,
                )"
                :key="s.id"
                :value="s.id"
              >
                {{ s.name }}
              </option>
            </select></label
          ><label class="span2"
            >{{ t("加工内容", "Outsourced work")
            }}<input
              v-model="form.description"
              maxlength="600"
              required /></label
          ><label
            >{{ t("约定收回日期", "Expected receipt date")
            }}<input v-model="form.dueDate" type="date" required /></label
        ></template>
        <label
          v-if="include(['VENDOR_CONFIRM', 'VENDOR_RECEIVE', 'VENDOR_CANCEL'])"
          class="span2"
          >{{ t("委外任务", "Outsourced task")
          }}<select v-model="form.taskId" required>
            <option value="">{{ t("请选择", "Select") }}</option>
            <option v-for="v in tasks" :key="v.id" :value="v.id">
              #{{ v.id }} · {{ v.description }} · {{ v.received }}/{{
                v.quantity
              }}
            </option>
          </select></label
        >
        <label v-if="include(['RECEIVE', 'REFUND', 'VENDOR_CREATE'])"
          >{{ t("金额", "Amount") }} · {{ order.currency
          }}<input
            v-model="form.amount"
            required
            type="number"
            :min="action === 'VENDOR_CREATE' ? 0 : 0.01"
            step="0.01"
        /></label>
        <label
          v-if="
            include([
              'RECEIVE',
              'REFUND',
              'VENDOR_CONFIRM',
              'VENDOR_RECEIVE',
              'DELIVER',
            ])
          "
          :class="{ span2: action !== 'RECEIVE' && action !== 'REFUND' }"
          >{{ t("实际凭据 / 参考号", "Actual reference")
          }}<input v-model="form.reference" maxlength="160" required
        /></label>
        <template v-if="action === 'DELIVER'"
          ><label
            >{{ t("交货方式", "Delivery method")
            }}<select v-model="form.method">
              <option value="PICKUP">{{ t("自提", "Pickup") }}</option>
              <option value="SHIP">{{ t("发运", "Shipment") }}</option>
              <option value="DELIVERY">
                {{ t("送货", "Local delivery") }}
              </option>
            </select></label
          ><label
            >{{ t("交货地点 / 收件地址", "Handover location / address")
            }}<input v-model="form.address" maxlength="600" required /></label
          ><label v-for="l in form.lines" :key="l.lineId"
            >{{ l.name
            }}<input
              v-model="l.quantity"
              type="number"
              min="0"
              step="1"
              :max="
                order.lines.find((x) => x.id === l.lineId).accepted -
                order.lines.find((x) => x.id === l.lineId).shipped
              " /></label
        ></template>
        <label class="span2"
          >{{
            noteRequired
              ? t("原因 / 凭据说明（必填）", "Reason / evidence (required)")
              : t("备注", "Note")
          }}<textarea
            v-model="form.note"
            :required="noteRequired"
            maxlength="1000"
            rows="3"
          ></textarea>
        </label>
      </div>
      <div class="dialog-actions">
        <button type="button" @click="emit('cancel')">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" :disabled="pending">
          {{ t("确认登记", "Confirm") }}
        </button>
      </div>
    </form>
  </div>
</template>
