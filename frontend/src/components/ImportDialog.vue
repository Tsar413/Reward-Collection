<script setup lang="ts">
import { ref } from 'vue'
import { send, message } from '../api'
import type { Entity, ImportReport } from '../types'
const props = defineProps<{ orgs: Entity[]; orgId?: number; admin: boolean }>(),
  emit = defineEmits<{ saved: []; close: [] }>()
const selected = ref(props.orgId || ''),
  file = ref<File>(),
  busy = ref(false),
  error = ref(''),
  report = ref<ImportReport>()
async function submit() {
  if (busy.value) return
  error.value = ''
  if (!selected.value || !file.value) {
    error.value = '请选择组织和 Excel 文件'
    return
  }
  if (!/\.xlsx?$/i.test(file.value.name) || file.value.size > 5 * 1024 * 1024) {
    error.value = '请选择不超过 5MiB 的 Excel 文件'
    return
  }
  busy.value = true
  try {
    const data = new FormData()
    data.append('orgId', String(selected.value))
    data.append('file', file.value)
    report.value = await send<ImportReport>('post', '/sys-user/import', data, { timeout: 120000 })
    emit('saved')
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}
function selectFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0]
  report.value = undefined
}
</script>
<template>
  <el-dialog
    :model-value="true"
    title="Excel 导入学生"
    width="760px"
    :close-on-click-modal="false"
    :before-close="
      () => {
        if (!busy) emit('close')
      }
    "
    ><p class="muted">
      表头依次为「学号」「姓名」「班级编号」，每行独立处理，最多 1000
      行。班级编号须属于所选组织及你的管理范围。
    </p>
    <label for="import-org">导入组织</label
    ><select id="import-org" v-model="selected" :disabled="!admin || busy">
      <option value="">请选择组织</option>
      <option v-for="org in orgs" :value="org.id" :key="org.id">{{ org.orgName }}</option></select
    ><label for="excel-file">Excel 文件</label
    ><input
      id="excel-file"
      type="file"
      accept=".xlsx,.xls"
      :disabled="busy"
      @change="selectFile"
    /><el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-if="report" data-testid="import-report">
      <h3>
        处理完成：成功 {{ report.successCount }}，跳过 {{ report.skippedCount }}，失败
        {{ report.failedCount }}
      </h3>
      <p>总计 {{ report.total }} 行。有效行已经保存，失败行可修正后重新导入。</p>
      <el-table :data="report.details" max-height="320"
        ><el-table-column prop="rowNumber" label="行号" width="65" /><el-table-column
          prop="userNo"
          label="学号"
          min-width="100" /><el-table-column label="结果" width="80"
          ><template #default="{ row }">{{
            row.status === 'SKIPPED' ? '跳过' : '失败'
          }}</template></el-table-column
        ><el-table-column prop="message" label="原因" min-width="180"
      /></el-table>
    </div>
    <template #footer
      ><el-button :disabled="busy" @click="emit('close')">关闭</el-button
      ><el-button type="primary" :loading="busy" :disabled="!!report" @click="submit"
        >开始导入</el-button
      ></template
    ></el-dialog
  >
</template>
