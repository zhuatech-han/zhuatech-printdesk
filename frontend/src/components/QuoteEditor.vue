<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed } from "vue";
import { t, cloneRecord, ask } from "../ui.js";
const props = defineProps({
  order: { type: Object, default: null },
  options: { type: Object, required: true },
  pending: Boolean,
});
const emit = defineEmits(["save", "cancel"]);
const fresh = () => ({
  name: "",
  materialId: "",
  quantity: 1,
  widthMm: 0,
  heightMm: 0,
  usage: "1",
  printRate: "0",
  finishRate: "0",
  setup: "0",
  markupPercent: "0",
});
const form = ref(
  props.order
    ? cloneRecord(props.order)
    : {
        customerId: "",
        title: "",
        dueDate: "",
        currency: "CNY",
        category: "DIGITAL",
        instructions: "",
        discount: "0",
        taxPercent: "0",
        lines: [fresh()],
      },
);
const initial = JSON.stringify(form.value);
const customer = computed(() =>
  props.options.customers.find((c) => c.id == form.value.customerId),
);
const materials = computed(() =>
  props.options.materials.filter(
    (m) =>
      m.currency === form.value.currency &&
      m.departmentId === customer.value?.departmentId,
  ),
);
const material = (l) => materials.value.find((m) => m.id == l.materialId);
async function canLeave() {
  return (
    JSON.stringify(form.value) === initial ||
    (await ask(t("放弃未保存的报价修改？", "Discard unsaved quote changes?")))
  );
}
defineExpose({ canLeave });
function save() {
  emit("save", cloneRecord(form.value));
}
</script>
<template>
  <form class="quote-editor" @submit.prevent="save">
    <div class="section-heading">
      <h2>
        {{ order ? t("修订报价", "Revise quote") : t("新建报价", "New quote") }}
      </h2>
      <button type="button" @click="emit('cancel')">
        {{ t("取消", "Cancel") }}
      </button>
    </div>
    <div class="form-grid">
      <label
        >{{ t("客户", "Customer")
        }}<select v-model="form.customerId" required :disabled="!!order">
          <option value="">{{ t("选择客户", "Select customer") }}</option>
          <option v-for="c in options.customers" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select></label
      >
      <label
        >{{ t("订单名称", "Order title")
        }}<input v-model="form.title" required maxlength="160"
      /></label>
      <label
        >{{ t("交期", "Due date")
        }}<input v-model="form.dueDate" type="date" required
      /></label>
      <label
        >{{ t("币种", "Currency")
        }}<select v-model="form.currency">
          <option>CNY</option>
          <option>USD</option>
          <option>EUR</option>
          <option>GBP</option>
        </select></label
      >
      <label
        >{{ t("分类", "Category")
        }}<select v-model="form.category" required>
          <option v-for="c in options.categories" :key="c.id" :value="c.code">
            {{ t(c.name, c.nameEn) }}
          </option>
        </select></label
      >
      <label
        >{{ t("订单折扣金额", "Order discount")
        }}<input
          v-model="form.discount"
          type="number"
          min="0"
          step="0.01"
          required
      /></label>
      <label
        >{{ t("手工税率 %", "Manual tax %")
        }}<input
          v-model="form.taxPercent"
          type="number"
          min="0"
          max="100"
          step="0.0001"
          required
      /></label>
      <label class="span2"
        >{{ t("制作要求", "Production instructions")
        }}<textarea
          v-model="form.instructions"
          maxlength="2000"
          rows="2"
        ></textarea>
      </label>
    </div>
    <div v-for="(l, i) in form.lines" :key="i" class="line-editor">
      <div class="section-heading">
        <h3>{{ t("报价行", "Quote line") }} {{ i + 1 }}</h3>
        <button
          v-if="form.lines.length > 1"
          type="button"
          @click="form.lines.splice(i, 1)"
        >
          {{ t("移除", "Remove") }}
        </button>
      </div>
      <div class="form-grid four">
        <label class="span2"
          >{{ t("产品 / 制作内容", "Product / work")
          }}<input v-model="l.name" required maxlength="160"
        /></label>
        <label
          >{{ t("数量（件）", "Quantity (pieces)")
          }}<input
            v-model="l.quantity"
            type="number"
            min="1"
            max="1000000"
            step="1"
            required
        /></label>
        <label
          >{{ t("材料", "Material")
          }}<select v-model="l.materialId" required>
            <option value="">
              {{ t("选择同币种材料", "Select same-currency material") }}
            </option>
            <option v-for="m in materials" :key="m.id" :value="m.id">
              {{ m.name }} · {{ m.unit }}
            </option>
          </select></label
        >
        <template v-if="material(l)?.unit === 'M2'"
          ><label
            >{{ t("宽 mm", "Width mm")
            }}<input
              v-model="l.widthMm"
              type="number"
              min="1"
              max="100000"
              step="1"
              required /></label
          ><label
            >{{ t("高 mm", "Height mm")
            }}<input
              v-model="l.heightMm"
              type="number"
              min="1"
              max="100000"
              step="1"
              required /></label
        ></template>
        <label v-else
          >{{ t("每件耗材数量", "Material usage / piece")
          }}<input
            v-model="l.usage"
            type="number"
            min="0.0001"
            max="100000"
            step="0.0001"
            required
        /></label>
        <label
          >{{ t("每件制作费", "Print charge / piece")
          }}<input
            v-model="l.printRate"
            type="number"
            min="0"
            step="0.01"
            required
        /></label>
        <label
          >{{ t("每件后道费", "Finishing / piece")
          }}<input
            v-model="l.finishRate"
            type="number"
            min="0"
            step="0.01"
            required
        /></label>
        <label
          >{{ t("一次开机费", "Setup charge")
          }}<input v-model="l.setup" type="number" min="0" step="0.01" required
        /></label>
        <label
          >{{ t("成本加成 %", "Cost markup %")
          }}<input
            v-model="l.markupPercent"
            type="number"
            min="0"
            max="500"
            step="0.0001"
            required
        /></label>
      </div>
    </div>
    <div class="toolbar">
      <button
        v-if="form.lines.length < 20"
        type="button"
        @click="form.lines.push(fresh())"
      >
        {{ t("添加报价行", "Add line") }}</button
      ><span class="grow"></span
      ><button class="primary" :disabled="pending">
        {{ t("计算并保存报价", "Calculate & save quote") }}
      </button>
    </div>
  </form>
</template>
