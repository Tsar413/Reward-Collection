<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, send, pick, message } from '../api'
import { useAuth } from '../stores/auth'
import { useCatalog } from '../catalog'
import type { Entity } from '../types'
import ImportDialog from '../components/ImportDialog.vue'
const route = useRoute(),
  auth = useAuth(),
  cat = useCatalog()
const { orgs, classes, users, orgName, className, userName } = cat
const kind = computed(() => String(route.meta.kind)),
  admin = computed(() => auth.user?.role === 'ADMIN')
const titles: Record<string, string> = {
  orgs: '组织管理',
  teachers: '教师管理',
  classes: admin.value ? '班级管理' : '我的班级',
  students: '学生管理',
}
const title = computed(() => titles[kind.value]),
  endpoint = computed(() =>
    kind.value === 'orgs' ? 'sys-org' : kind.value === 'classes' ? 'sys-class' : 'sys-user',
  )
const orgFilter = ref<number | ''>(''),
  classFilter = ref<number | ''>(''),
  search = ref(''),
  page = ref(1),
  loading = ref(false),
  error = ref(''),
  busy = ref(false),
  dialog = ref(false),
  editing = ref<number>(),
  detail = ref<Entity | null>(),
  showDetail = ref(false),
  detailLoading = ref(false),
  detailError = ref(''),
  importOpen = ref(false),
  formError = ref(''),
  form = ref<Record<string, any>>({})
const canWrite = computed(() => kind.value !== 'classes' || admin.value)
const records = computed(() =>
  kind.value === 'orgs'
    ? orgs.value
    : kind.value === 'classes'
      ? classes.value
      : users.value.filter((u) => u.role === (kind.value === 'teachers' ? 'TEACHER' : 'STUDENT')),
)
const filtered = computed(() =>
  records.value.filter(
    (r) =>
      (!orgFilter.value ||
        r.orgId === orgFilter.value ||
        (kind.value === 'orgs' && r.id === orgFilter.value)) &&
      (!classFilter.value ||
        r.classId === classFilter.value ||
        (kind.value === 'classes' && r.id === classFilter.value)) &&
      JSON.stringify([r.orgName, r.className, r.realName, r.userNo, r.classCode])
        .toLowerCase()
        .includes(search.value.toLowerCase()),
  ),
)
const visible = computed(() => filtered.value.slice((page.value - 1) * 10, page.value * 10))
const filterClasses = computed(() =>
  classes.value.filter((c) => !orgFilter.value || c.orgId === orgFilter.value),
)
const formClasses = computed(() =>
  classes.value.filter(
    (c) => c.orgId === Number(form.value.orgId) && (c.status === 1 || c.id === form.value.classId),
  ),
)
const formTeachers = computed(() =>
  users.value.filter(
    (u) =>
      u.role === 'TEACHER' &&
      u.orgId === Number(form.value.orgId) &&
      (u.status === 1 || u.id === form.value.teacherUserId),
  ),
)
watch([orgFilter, classFilter, search], () => (page.value = 1))
watch(orgFilter, () => (classFilter.value = ''))
async function load() {
  loading.value = true
  error.value = ''
  try {
    await cat.load()
    page.value = Math.min(page.value, Math.max(1, Math.ceil(filtered.value.length / 10)))
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
function open(row?: Entity) {
  editing.value = row?.id
  form.value = row
    ? { ...row }
    : {
        orgId: admin.value ? orgFilter.value || '' : auth.user!.orgId,
        realName: '',
        userNo: '',
        password: '',
        classId: '',
        teacherUserId: '',
        orgName: '',
        className: '',
        classCode: '',
        admissionYear: new Date().getFullYear(),
        status: 1,
        remark: '',
      }
  formError.value = ''
  dialog.value = true
}
function changeOrg() {
  form.value.classId = ''
  form.value.teacherUserId = ''
}
function validate() {
  const f = form.value
  const required =
    kind.value === 'orgs'
      ? ['orgName']
      : kind.value === 'classes'
        ? [
            'className',
            'admissionYear',
            'teacherUserId',
            ...(!editing.value ? ['orgId', 'classCode'] : []),
          ]
        : [
            'realName',
            ...(!editing.value ? ['orgId', 'userNo', 'password'] : []),
            ...(kind.value === 'students' ? ['classId'] : []),
          ]
  if (required.some((k) => f[k] === null || f[k] === undefined || !String(f[k]).trim()))
    return '请填写所有必填项'
  if (
    !editing.value &&
    kind.value !== 'classes' &&
    kind.value !== 'orgs' &&
    (f.password.length < 8 || f.password.length > 64)
  )
    return '初始密码长度应为 8–64 位'
  if (
    kind.value === 'classes' &&
    (!Number.isInteger(Number(f.admissionYear)) || f.admissionYear < 2020 || f.admissionYear > 3000)
  )
    return '入学年份应为 2020–3000 的整数'
  return ''
}
async function save() {
  if (busy.value) return
  formError.value = validate()
  if (formError.value) return
  busy.value = true
  try {
    let keys: string[]
    if (kind.value === 'orgs') keys = ['orgName', 'remark']
    else if (kind.value === 'classes')
      keys = editing.value
        ? ['className', 'admissionYear', 'teacherUserId', 'status', 'remark']
        : ['orgId', 'classCode', 'className', 'admissionYear', 'teacherUserId', 'remark']
    else
      keys = editing.value
        ? ['realName', 'classId', 'status', 'remark']
        : ['orgId', 'realName', 'userNo', 'password', 'classId', 'remark', 'role']
    const f: Record<string, any> = {
      ...form.value,
      role: kind.value === 'teachers' ? 'TEACHER' : 'STUDENT',
    }
    if (kind.value === 'teachers') f.classId = null
    await send(
      editing.value ? 'put' : 'post',
      `/${endpoint.value}/${editing.value ? 'update/' + editing.value : kind.value === 'classes' ? 'add' : 'registration-one'}`,
      pick(f, keys),
    )
    dialog.value = false
    ElMessage.success('保存成功')
    await load()
  } catch (e) {
    formError.value = message(e)
  } finally {
    busy.value = false
  }
}
async function remove(row: Entity) {
  if (busy.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.realName || row.className || row.orgName}」？存在关联数据时后端会阻止删除。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
    )
    busy.value = true
    await send('delete', `/${endpoint.value}/delete/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}
async function view(row: Entity) {
  showDetail.value = true
  detail.value = null
  detailLoading.value = true
  detailError.value = ''
  try {
    detail.value = await get<Entity | null>(`/${endpoint.value}/one/${row.id}`)
  } catch (e) {
    detailError.value = message(e)
  } finally {
    detailLoading.value = false
  }
}
onMounted(load)
</script>
<template>
  <div>
    <div class="page-heading">
      <div>
        <p class="eyebrow">WORKSPACE / {{ admin ? '管理' : '教学' }}</p>
        <h1>{{ title }}</h1>
        <p class="muted">
          {{
            kind === 'classes' && !admin
              ? '切换并查看你负责的班级。'
              : '查看与维护当前权限范围内的资料。'
          }}
        </p>
      </div>
      <div class="actions">
        <el-button v-if="kind === 'students'" @click="importOpen = true">Excel 导入</el-button
        ><el-button v-if="kind !== 'orgs' && canWrite" type="primary" @click="open()"
          >新增{{ kind === 'teachers' ? '教师' : kind === 'classes' ? '班级' : '学生' }}</el-button
        >
      </div>
    </div>
    <section class="panel">
      <div class="toolbar">
        <div v-if="admin && kind !== 'orgs'">
          <label for="org-filter">组织筛选</label
          ><select id="org-filter" v-model="orgFilter">
            <option value="">全部组织</option>
            <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
          </select>
        </div>
        <div v-if="kind === 'students' || kind === 'classes'">
          <label for="class-filter">班级筛选</label
          ><select id="class-filter" v-model="classFilter">
            <option value="">全部班级</option>
            <option v-for="c in filterClasses" :key="c.id" :value="c.id">{{ c.className }}</option>
          </select>
        </div>
        <div>
          <label for="search">搜索</label
          ><input id="search" v-model="search" placeholder="姓名、编号或名称" />
        </div>
        <el-button @click="load" :loading="loading">刷新</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon /><el-table
        :data="visible"
        v-loading="loading"
        empty-text="暂无符合条件的数据"
        row-key="id"
        ><el-table-column
          :prop="kind === 'orgs' ? 'orgName' : kind === 'classes' ? 'className' : 'realName'"
          label="名称"
          min-width="140"
        /><el-table-column
          :prop="kind === 'orgs' ? 'orgCode' : kind === 'classes' ? 'classCode' : 'userNo'"
          label="编号"
          min-width="160"
        /><el-table-column v-if="kind !== 'orgs'" label="组织" min-width="110"
          ><template #default="{ row }">{{ orgName(row.orgId) }}</template></el-table-column
        ><el-table-column v-if="kind === 'students'" label="班级" min-width="110"
          ><template #default="{ row }">{{ className(row.classId) }}</template></el-table-column
        ><el-table-column
          v-if="kind === 'classes'"
          prop="admissionYear"
          label="入学年份"
          width="95"
        /><el-table-column v-if="kind === 'classes'" label="教师" min-width="100"
          ><template #default="{ row }">{{
            userName(row.teacherUserId)
          }}</template></el-table-column
        ><el-table-column label="状态" width="80"
          ><template #default="{ row }"
            ><el-tag :type="row.status === 1 ? 'success' : 'info'">{{
              row.status === 1 ? '启用' : '停用'
            }}</el-tag></template
          ></el-table-column
        ><el-table-column label="操作" :width="canWrite ? 195 : 75" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="view(row)">详情</el-button
            ><el-button v-if="canWrite" link type="primary" @click="open(row)">编辑</el-button
            ><el-button v-if="canWrite" link type="danger" :disabled="busy" @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      ><el-pagination
        v-model:current-page="page"
        :total="filtered.length"
        :page-size="10"
        layout="total, prev, pager, next"
      />
    </section>
    <el-dialog
      v-model="dialog"
      :title="`${editing ? '编辑' : '新增'}${kind === 'orgs' ? '组织' : kind === 'classes' ? '班级' : kind === 'teachers' ? '教师' : '学生'}`"
      width="560px"
      :close-on-click-modal="false"
      :before-close="
        () => {
          if (!busy) dialog = false
        }
      "
      ><form id="resource-form" @submit.prevent="save">
        <div class="form-grid">
          <template v-if="!editing && kind !== 'orgs'"
            ><div>
              <label for="f-org">组织 *</label
              ><select id="f-org" v-model="form.orgId" :disabled="!admin" @change="changeOrg">
                <option value="">请选择组织</option>
                <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
              </select>
            </div>
            <div>
              <label for="f-code"
                >{{
                  kind === 'classes' ? '班级编号' : kind === 'teachers' ? '工号' : '学号'
                }}
                *</label
              ><input
                id="f-code"
                v-model="form[kind === 'classes' ? 'classCode' : 'userNo']"
                maxlength="50"
              /></div
          ></template>
          <div>
            <label for="f-name"
              >{{
                kind === 'orgs' ? '组织名称' : kind === 'classes' ? '班级名称' : '姓名'
              }}
              *</label
            ><input
              id="f-name"
              v-model="
                form[kind === 'orgs' ? 'orgName' : kind === 'classes' ? 'className' : 'realName']
              "
              :maxlength="kind === 'orgs' || kind === 'classes' ? 100 : 50"
            />
          </div>
          <div v-if="!editing && (kind === 'teachers' || kind === 'students')">
            <label for="f-password">初始密码 *</label
            ><input
              id="f-password"
              v-model="form.password"
              type="password"
              minlength="8"
              maxlength="64"
              autocomplete="new-password"
            />
          </div>
          <div v-if="kind === 'classes'">
            <label for="f-year">入学年份 *</label
            ><input
              id="f-year"
              v-model.number="form.admissionYear"
              type="number"
              min="2020"
              max="3000"
            />
          </div>
          <div v-if="kind === 'classes'">
            <label for="f-teacher">负责教师 *</label
            ><select id="f-teacher" v-model="form.teacherUserId">
              <option value="">请选择教师</option>
              <option v-for="u in formTeachers" :key="u.id" :value="u.id">
                {{ u.realName }}（{{ u.userNo }}）
              </option>
            </select>
          </div>
          <div v-if="kind === 'students'">
            <label for="f-class">所属班级 *</label
            ><select id="f-class" v-model="form.classId">
              <option value="">请选择班级</option>
              <option v-for="c in formClasses" :key="c.id" :value="c.id">{{ c.className }}</option>
            </select>
          </div>
          <div v-if="editing && kind !== 'orgs'">
            <label for="f-status">状态 *</label
            ><select id="f-status" v-model="form.status">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="span-2">
            <label for="f-remark">备注</label
            ><textarea id="f-remark" v-model="form.remark" maxlength="500" rows="3" />
          </div>
        </div>
        <el-alert v-if="formError" :title="formError" type="error" :closable="false" />
      </form>
      <template #footer
        ><el-button :disabled="busy" @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="busy" @click="save">保存</el-button></template
      ></el-dialog
    >
    <el-dialog v-model="showDetail" title="资料详情" width="560px"
      ><p v-if="detailLoading" role="status">正在读取资料…</p>
      <el-alert
        v-else-if="detailError"
        :title="detailError"
        type="error"
        :closable="false"
      /><el-empty v-else-if="!detail" description="不存在或不可访问" />
      <dl v-else class="details">
        <dt>名称</dt>
        <dd>{{ detail.orgName || detail.className || detail.realName }}</dd>
        <dt>编号</dt>
        <dd>{{ detail.orgCode || detail.classCode || detail.userNo }}</dd>
        <template v-if="detail.orgId"
          ><dt>组织</dt>
          <dd>{{ orgName(detail.orgId) }}</dd></template
        ><template v-if="detail.classId"
          ><dt>班级</dt>
          <dd>{{ className(detail.classId) }}</dd></template
        ><template v-if="detail.teacherUserId"
          ><dt>负责教师</dt>
          <dd>{{ userName(detail.teacherUserId) }}</dd>
          <dt>入学年份</dt>
          <dd>{{ detail.admissionYear }}</dd></template
        >
        <dt>状态</dt>
        <dd>{{ detail.status === 1 ? '启用' : '停用' }}</dd>
        <dt>备注</dt>
        <dd>{{ detail.remark || '无' }}</dd>
      </dl></el-dialog
    ><ImportDialog
      v-if="importOpen"
      :orgs="orgs"
      :org-id="Number(orgFilter) || auth.user?.orgId"
      :admin="admin"
      @close="importOpen = false"
      @saved="load"
    />
  </div>
</template>
