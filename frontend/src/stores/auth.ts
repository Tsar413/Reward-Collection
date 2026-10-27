import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get, send } from '../api'
import type { User, Role } from '../types'
export const home = (role?: Role) => `/${(role || 'STUDENT').toLowerCase()}/awards`
export function safeReturn(value: unknown, role?: Role) {
  return typeof value === 'string' &&
    /^\/(admin|teacher|student)\/[a-z-]+(?:\?[^#]*)?$/.test(value) &&
    (!role || value.startsWith('/' + role.toLowerCase() + '/'))
    ? value
    : home(role)
}
export const useAuth = defineStore('auth', () => {
  const user = ref<User | null>(null),
    validated = ref(false)
  function clear() {
    sessionStorage.removeItem('rc.token')
    sessionStorage.removeItem('rc.userId')
    user.value = null
    validated.value = false
  }
  async function refresh() {
    await get('/sys-login/check')
    const id = sessionStorage.getItem('rc.userId')
    if (!id || !/^\d+$/.test(id)) {
      clear()
      throw new Error('会话资料缺失，请重新登录')
    }
    const u = await get<User | null>(`/sys-user/one/${id}`)
    if (!u || !['ADMIN', 'TEACHER', 'STUDENT'].includes(u.role)) {
      clear()
      throw new Error('账号不存在或不可访问')
    }
    user.value = u
    validated.value = true
  }
  async function login(userNo: string, password: string) {
    const d = await send<{ token: string; user: User }>('post', '/sys-login/login', {
      userNo,
      password,
    })
    sessionStorage.setItem('rc.token', d.token)
    sessionStorage.setItem('rc.userId', String(d.user.id))
    user.value = d.user
    validated.value = true
    await refresh()
  }
  async function logout() {
    try {
      await send('post', '/sys-login/logout')
    } finally {
      clear()
    }
  }
  return { user, validated, clear, refresh, login, logout }
})
