<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuth } from '../stores/auth'
import { get, send, message } from '../api'
import { useCatalog } from '../catalog'
import type { Entity } from '../types'
import AwardForm from '../components/AwardForm.vue'
import AwardDetail from '../components/AwardDetail.vue'
const route = useRoute(),
  router = useRouter(),
  auth = useAuth(),
  cat = useCatalog()
const { orgs, classes, users, orgName, className, userName } = cat
const isStudent = computed(() => auth.user?.role === 'STUDENT'),
  isAdmin = computed(() => auth.user?.role === 'ADMIN'),
  type = computed<'CLASS' | 'PERSONAL'>(() =>
    route.meta.kind === 'class-awards' ? 'CLASS' : 'PERSONAL',
  ),
  canWrite = computed(() => !isStudent.value || type.value === 'PERSONAL')
const awards = ref<Entity[]>([]),
  loading = ref(false),
  error = ref(''),
  orgFilter = ref<number | ''>(''),
  classFilter = ref<number | ''>(''),
  search = ref(''),
  page = ref(1),
  formOpen = ref(false),
  editRecord = ref<Entity>(),
  selectedId = computed(() => Number(route.query.award) || undefined),
  busy = ref(false),
  detailKey = ref(0)
const filtered = computed(() =>
  awards.value.filter(
    (a) =>
      a.awardType === type.value &&
      (!orgFilter.value || a.orgId === orgFilter.value) &&
      (!classFilter.value || a.classId === classFilter.value) &&
      [a.awardName, a.issuingOrganization, a.activityName].some((v) =>
        String(v).toLowerCase().includes(search.value.toLowerCase()),
      ),
  ),
)
const visible = computed(() => filtered.value.slice((page.value - 1) * 10, page.value * 10)),
  filterClasses = computed(() =>
    classes.value.filter((c) => !orgFilter.value || c.orgId === orgFilter.value),
  )
const nextId = computed(() => {
  const i = filtered.value.findIndex((a) => a.id === selectedId.value)
  return filtered.value.length > 1 ? filtered.value[(i + 1) % filtered.value.length]?.id : undefined
})
watch([search, orgFilter, classFilter], () => (page.value = 1))
watch(orgFilter, () => (classFilter.value = ''))
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [a] = await Promise.all([get<Entity[]>('/award-record/all'), cat.load()])
    awards.value = a
    page.value = Math.min(page.value, Math.max(1, Math.ceil(filtered.value.length / 10)))
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
function detail(id?: number) {
  void router.replace({ path: route.path, query: id ? { award: id } : {} })
}
function edit(row?: Entity) {
  editRecord.value = row
  formOpen.value = true
}
async function saved() {
  formOpen.value = false
  ElMessage.success('奖状已保存')
  await load()
  detailKey.value++
}
async function remove(row: Entity) {
  if (busy.value) return
  try {
    await ElMessageBox.confirm(`确定删除「${row.awardName}」及其图片？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    busy.value = true
    await send('delete', `/award-record/delete/${row.id}`)
    if (selectedId.value === row.id) detail()
    ElMessage.success('奖状已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <div>
    <div class="page-heading">
      <div>
        <p class="eyebrow">HONORS / {{ type === 'CLASS' ? 'COLLECTIVE' : 'PERSONAL' }}</p>
        <h1>{{ type === 'CLASS' ? '班级奖状' : isStudent ? '我的奖状' : '学生个人奖状' }}</h1>
        <p class="muted">
          {{
            type === 'CLASS' ? '共同努力，留下集体的精彩时刻。' : '整理每一份荣誉，见证每一步成长。'
          }}
        </p>
      </div>
      <el-button v-if="canWrite" type="primary" @click="edit()">新增奖状</el-button>
    </div>
    <section class="panel">
      <div class="toolbar">
        <div v-if="isAdmin">
          <label for="org-filter">组织筛选</label
          ><select id="org-filter" v-model="orgFilter">
            <option value="">全部组织</option>
            <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
          </select>
        </div>
        <div v-if="!isStudent">
          <label for="class-filter">班级筛选</label
          ><select id="class-filter" v-model="classFilter">
            <option value="">全部班级</option>
            <option v-for="c in filterClasses" :key="c.id" :value="c.id">{{ c.className }}</option>
          </select>
        </div>
        <div>
          <label for="search">搜索奖状</label
          ><input id="search" v-model="search" placeholder="奖状、活动或颁发单位" />
        </div>
        <el-button @click="load" :loading="loading">刷新</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <div v-if="isStudent" v-loading="loading" class="award-cards">
        <el-empty v-if="!visible.length && !loading" description="还没有奖状，期待你的精彩时刻" />
        <article
          v-for="row in visible"
          :key="row.id"
          class="award-card"
          :data-testid="`award-${row.id}`"
        >
          <div class="award-card-top">
            <span class="award-emblem">✦</span><span class="muted small">{{ row.awardDate }}</span>
          </div>
          <h2>{{ row.awardName }}</h2>
          <p class="muted">{{ row.issuingOrganization }}</p>
          <p class="small">{{ row.awardLevel || '荣誉记录' }} · {{ className(row.classId) }}</p>
          <div class="card-actions">
            <el-button type="primary" plain @click="detail(row.id)">查看详情</el-button
            ><el-button v-if="canWrite" text @click="edit(row)">修改</el-button
            ><el-button v-if="canWrite" text type="danger" :disabled="busy" @click="remove(row)"
              >删除</el-button
            >
          </div>
        </article>
      </div>
      <el-table v-else :data="visible" v-loading="loading" empty-text="暂无符合条件的奖状"
        ><el-table-column prop="awardName" label="奖状名称" min-width="180" /><el-table-column
          label="组织 / 班级"
          min-width="160"
          ><template #default="{ row }"
            >{{ orgName(row.orgId) }} / {{ className(row.classId) }}</template
          ></el-table-column
        ><el-table-column v-if="type === 'PERSONAL'" label="获奖学生" min-width="110"
          ><template #default="{ row }">{{
            userName(row.studentUserId)
          }}</template></el-table-column
        ><el-table-column prop="awardDate" label="获奖日期" width="115" /><el-table-column
          prop="issuingOrganization"
          label="颁发单位"
          min-width="140"
        /><el-table-column label="操作" width="195" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="detail(row.id)">详情</el-button
            ><el-button link @click="edit(row)">修改</el-button
            ><el-button link type="danger" :disabled="busy" @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      ><el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="filtered.length"
        layout="total, prev, pager, next"
      />
    </section>
    <AwardDetail
      v-if="selectedId"
      :key="detailKey"
      :id="selectedId"
      :editable="
        canWrite &&
        !!awards.find(
          (a) =>
            a.id === selectedId &&
            (auth.user?.role !== 'STUDENT' ||
              (a.awardType === 'PERSONAL' && a.studentUserId === auth.user.id)),
        )
      "
      :next-id="nextId"
      :class-name="className"
      :user-name="userName"
      @close="detail()"
      @next="detail"
      @edit="edit"
      @changed="load"
    /><AwardForm
      v-if="formOpen"
      :record="editRecord"
      :type="type"
      :orgs="orgs"
      :classes="classes"
      :users="users"
      @close="formOpen = false"
      @saved="saved"
    />
  </div>
</template>
