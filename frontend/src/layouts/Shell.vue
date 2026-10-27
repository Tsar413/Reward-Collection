<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { message } from '../api'
import { ElMessage } from 'element-plus'
const props = defineProps<{ role: 'admin' | 'teacher' | 'student' }>()
const auth = useAuth(),
  route = useRoute(),
  router = useRouter(),
  busy = ref(false)
const labels: Record<string, string> = {
  admin: '管理中心',
  teacher: '教师工作台',
  student: '我的荣誉册',
}
const nav = computed(() =>
  props.role === 'admin'
    ? [
        ['orgs', '组织管理'],
        ['teachers', '教师管理'],
        ['classes', '班级管理'],
        ['students', '学生管理'],
        ['awards', '个人奖状'],
        ['class-awards', '班级奖状'],
        ['profile', '个人资料'],
      ]
    : props.role === 'teacher'
      ? [
          ['classes', '我的班级'],
          ['students', '学生管理'],
          ['awards', '学生个人奖状'],
          ['class-awards', '班级奖状'],
          ['profile', '个人资料'],
        ]
      : [
          ['awards', '我的奖状'],
          ['class-awards', '班级奖状'],
          ['profile', '个人资料'],
        ],
)
async function logout() {
  if (busy.value) return
  busy.value = true
  try {
    await auth.logout()
  } catch (e) {
    ElMessage.warning(message(e))
  } finally {
    busy.value = false
    await router.replace('/login')
  }
}
</script>
<template>
  <div class="shell" :class="role">
    <aside>
      <div class="brand">
        <span class="brand-mark">荣</span>
        <div>荣誉册<small>REWARD COLLECTION</small></div>
      </div>
      <div class="workspace-label">{{ labels[role] }}</div>
      <nav aria-label="主导航">
        <router-link
          v-for="[path, label] in nav"
          :key="path"
          :to="`/${role}/${path}`"
          :class="{ active: route.path === `/${role}/${path}` }"
          >{{ label }}</router-link
        >
      </nav>
      <div class="sidebar-note">记录成长的每一份荣誉</div>
    </aside>
    <div class="workspace">
      <header>
        <span>{{ labels[role] }}</span>
        <div class="user-actions">
          <span>{{ auth.user?.realName }}</span
          ><el-button text :loading="busy" @click="logout">退出登录</el-button>
        </div>
      </header>
      <main><router-view :key="route.path" /></main>
      <footer>荣誉册 · 成长有迹可循</footer>
    </div>
  </div>
</template>
