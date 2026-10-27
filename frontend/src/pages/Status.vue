<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuth, home } from '../stores/auth'
const route = useRoute(),
  router = useRouter(),
  auth = useAuth()
function returnToLogin() {
  auth.clear()
  void router.replace('/login')
}
</script>
<template>
  <div class="status-page">
    <el-result
      :icon="route.meta.status === 'forbidden' ? 'warning' : 'info'"
      :title="
        route.meta.status === 'forbidden'
          ? '权限不足'
          : route.meta.status === 'session'
            ? '暂时无法验证登录状态'
            : '页面不存在'
      "
      :sub-title="
        route.meta.status === 'session'
          ? '请检查网络连接后重试，你的会话仍被保留。'
          : '请返回自己的工作台继续操作。'
      "
      ><template #extra
        ><el-button type="primary" @click="router.replace(home(auth.user?.role))"
          >返回工作台 / 重试</el-button
        ><el-button @click="returnToLogin">返回登录</el-button></template
      ></el-result
    >
  </div>
</template>
