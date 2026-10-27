<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth, safeReturn } from '../stores/auth'
import { message } from '../api'
const userNo = ref(''),
  password = ref(''),
  busy = ref(false),
  error = ref('')
const auth = useAuth(),
  route = useRoute(),
  router = useRouter()
async function submit() {
  if (busy.value) return
  error.value = ''
  if (!userNo.value.trim() || !password.value) {
    error.value = '请填写用户编号和密码'
    return
  }
  busy.value = true
  try {
    await auth.login(userNo.value.trim(), password.value)
    password.value = ''
    await router.replace(safeReturn(route.query.returnTo, auth.user?.role))
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div class="login-page">
    <section class="login-intro">
      <div class="brand"><span class="brand-mark">荣</span>荣誉册</div>
      <p class="eyebrow">EVERY ACHIEVEMENT MATTERS</p>
      <h1>每一份努力，<br />都值得被珍藏。</h1>
      <p>从个人成长到集体荣誉，<br />在这里记录、整理与回顾。</p>
      <div class="login-art" aria-hidden="true">
        <span>✦</span>
        <div>成长 · 荣誉 · 记忆</div>
      </div>
    </section>
    <section class="login-panel">
      <div class="login-form">
        <span class="eyebrow">欢迎回来</span>
        <h2>登录荣誉册</h2>
        <p class="muted">使用你的学号或工号登录</p>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon /><el-alert
          v-else-if="route.query.expired"
          title="登录已失效，请重新登录"
          type="warning"
          :closable="false"
        />
        <form @submit.prevent="submit">
          <label for="login-no">用户编号</label
          ><el-input
            id="login-no"
            v-model="userNo"
            maxlength="50"
            autocomplete="username"
            placeholder="请输入学号或工号"
          /><label for="login-password">密码</label
          ><el-input
            id="login-password"
            v-model="password"
            type="password"
            show-password
            maxlength="64"
            autocomplete="current-password"
            placeholder="请输入密码"
          /><el-button type="primary" native-type="submit" :loading="busy" class="login-submit"
            >登录</el-button
          >
        </form>
        <p class="muted small">账号与所属组织由管理员维护。</p>
      </div>
    </section>
  </div>
</template>
