<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuth } from '../stores/auth'
import { useCatalog } from '../catalog'
import { send, message } from '../api'
const auth = useAuth(),
  cat = useCatalog(),
  name = ref(''),
  remark = ref(''),
  busy = ref(false),
  error = ref(''),
  loading = ref(true)
async function load() {
  loading.value = true
  try {
    await auth.refresh()
    await cat.load()
    name.value = auth.user!.realName
    remark.value = auth.user!.remark || ''
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)
async function save() {
  if (busy.value) return
  error.value = ''
  if (!name.value.trim()) {
    error.value = '姓名不能为空'
    return
  }
  busy.value = true
  try {
    await auth.refresh()
    const u = auth.user!
    await send('put', `/sys-user/update/${u.id}`, {
      realName: name.value.trim(),
      remark: remark.value,
      classId: u.classId,
      status: u.status,
    })
    await auth.refresh()
    ElMessage.success('资料已保存')
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div>
    <div class="page-heading">
      <div>
        <p class="eyebrow">ACCOUNT</p>
        <h1>个人资料</h1>
        <p class="muted">保持信息准确，让每一份荣誉有所归属。</p>
      </div>
    </div>
    <section class="panel profile" v-loading="loading">
      <dl class="details">
        <dt>用户编号</dt>
        <dd>{{ auth.user?.userNo }}</dd>
        <dt>身份</dt>
        <dd>
          {{
            auth.user?.role === 'ADMIN' ? '管理员' : auth.user?.role === 'TEACHER' ? '教师' : '学生'
          }}
        </dd>
        <dt>组织</dt>
        <dd>{{ cat.orgName(auth.user?.orgId || 0) }}</dd>
        <dt>班级</dt>
        <dd>{{ cat.className(auth.user?.classId || null) }}</dd>
        <dt>状态</dt>
        <dd>{{ auth.user?.status === 1 ? '启用' : '停用' }}</dd>
      </dl>
      <form @submit.prevent="save">
        <label for="profile-name">姓名 *</label
        ><input id="profile-name" v-model="name" maxlength="50" /><label for="profile-remark"
          >备注</label
        ><textarea id="profile-remark" v-model="remark" maxlength="500" rows="4" /><el-alert
          v-if="error"
          :title="error"
          type="error"
          :closable="false"
        /><el-button type="primary" native-type="submit" :loading="busy" :disabled="loading"
          >保存资料</el-button
        >
      </form>
    </section>
  </div>
</template>
