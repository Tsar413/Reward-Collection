<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useAuth } from '../stores/auth'
import { send, pick, message } from '../api'
import { awardFields, validDate, imageError } from '../validation'
import type { Entity, User } from '../types'
const props = defineProps<{
    record?: Entity
    type: 'PERSONAL' | 'CLASS'
    orgs: Entity[]
    classes: Entity[]
    users: User[]
  }>(),
  emit = defineEmits<{ close: []; saved: [] }>()
const auth = useAuth(),
  form = ref<Record<string, any>>(
    props.record
      ? { ...props.record }
      : {
          awardName: '',
          issuingOrganization: '',
          awardDate: '',
          activityName: '',
          awardCategory: '',
          awardLevel: '',
          awardGrade: '',
          certificateNo: '',
          description: '',
          orgId: auth.user?.role === 'ADMIN' ? '' : auth.user?.orgId,
          classId: '',
          studentUserId: '',
        },
  ),
  busy = ref(false),
  error = ref(''),
  file = ref<File>(),
  preview = ref('')
const own = auth.user?.role === 'STUDENT',
  formClasses = computed(() =>
    props.classes.filter((c) => c.orgId === Number(form.value.orgId) && c.status === 1),
  ),
  students = computed(() =>
    props.users.filter(
      (u) =>
        u.role === 'STUDENT' &&
        u.orgId === Number(form.value.orgId) &&
        u.classId === Number(form.value.classId),
    ),
  )
function selected(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  const err = imageError(f)
  if (preview.value) URL.revokeObjectURL(preview.value)
  preview.value = ''
  file.value = undefined
  error.value = err
  if (!err && f) {
    file.value = f
    preview.value = URL.createObjectURL(f)
  }
}
onBeforeUnmount(() => {
  if (preview.value) URL.revokeObjectURL(preview.value)
})
async function save() {
  if (busy.value) return
  error.value = ''
  const f = form.value
  if (!f.awardName.trim() || !f.issuingOrganization.trim()) {
    error.value = '请填写奖状名称和颁发单位'
    return
  }
  if (!validDate(f.awardDate)) {
    error.value = '请输入有效日期，格式为 yyyy-MM-dd'
    return
  }
  if (!props.record) {
    if (!own && (props.type === 'PERSONAL' ? !f.studentUserId : !f.classId)) {
      error.value = '请选择获奖学生或班级'
      return
    }
    error.value = imageError(file.value)
    if (error.value) return
  }
  busy.value = true
  try {
    const text = pick(
      f,
      awardFields.map(([k]) => k),
    )
    if (props.record) await send('put', `/award-record/update/${props.record.id}`, text)
    else {
      const data = new FormData()
      const dto = {
        ...text,
        awardType: props.type,
        ...(!own
          ? props.type === 'CLASS'
            ? { classId: Number(f.classId) }
            : { studentUserId: Number(f.studentUserId) }
          : {}),
      }
      data.append('data', new Blob([JSON.stringify(dto)], { type: 'application/json' }))
      data.append('file', file.value!)
      await send('post', '/award-record/add', data)
    }
    emit('saved')
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}
function changeOrganization() {
  form.value.classId = ''
  form.value.studentUserId = ''
}
</script>
<template>
  <el-dialog
    :model-value="true"
    :title="record ? '修改奖状文字' : '新增' + (type === 'CLASS' ? '班级' : '个人') + '奖状'"
    width="660px"
    :close-on-click-modal="false"
    :before-close="
      () => {
        if (!busy) emit('close')
      }
    "
    ><form @submit.prevent="save">
      <p v-if="record" class="muted">
        修改文字资料，奖状类型与归属保持不变。图片请在详情中单独替换。
      </p>
      <div class="form-grid">
        <template v-if="!record && !own"
          ><div v-if="auth.user?.role === 'ADMIN'">
            <label for="award-org">所属组织 *</label
            ><select id="award-org" v-model="form.orgId" @change="changeOrganization">
              <option value="">请选择组织</option>
              <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
            </select>
          </div>
          <div>
            <label for="award-class">所属班级 *</label
            ><select id="award-class" v-model="form.classId" @change="form.studentUserId = ''">
              <option value="">请选择班级</option>
              <option v-for="c in formClasses" :key="c.id" :value="c.id">{{ c.className }}</option>
            </select>
          </div>
          <div v-if="type === 'PERSONAL'">
            <label for="award-student">获奖学生 *</label
            ><select id="award-student" v-model="form.studentUserId">
              <option value="">请选择学生</option>
              <option v-for="u in students" :key="u.id" :value="u.id">
                {{ u.realName }}（{{ u.userNo }}）
              </option>
            </select>
          </div></template
        >
        <div
          v-for="[key, label, max] in awardFields"
          :key="key"
          :class="{ 'span-2': key === 'description' }"
        >
          <label :for="'award-' + key"
            >{{ label
            }}{{
              ['awardName', 'issuingOrganization', 'awardDate'].includes(key) ? ' *' : ''
            }}</label
          ><textarea
            v-if="key === 'description'"
            :id="'award-' + key"
            v-model="form[key]"
            :maxlength="max"
            rows="3"
          /><input
            v-else
            :id="'award-' + key"
            v-model="form[key]"
            :maxlength="max"
            :placeholder="key === 'awardDate' ? 'yyyy-MM-dd' : ''"
          />
        </div>
        <div v-if="!record" class="span-2">
          <label for="award-file">奖状图片 *</label
          ><input
            id="award-file"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="selected"
          />
          <p class="muted small">JPEG / PNG / WebP，不超过 5MiB</p>
          <img v-if="preview" :src="preview" class="local-preview" alt="已选择图片预览" />
        </div>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    </form>
    <template #footer
      ><el-button :disabled="busy" @click="emit('close')">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save">保存奖状</el-button></template
    ></el-dialog
  >
</template>
