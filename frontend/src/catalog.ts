import { ref } from 'vue'
import { get } from './api'
import type { Entity, User } from './types'
export function useCatalog() {
  const orgs = ref<Entity[]>([]),
    classes = ref<Entity[]>([]),
    users = ref<User[]>([])
  async function load() {
    const [o, c, u] = await Promise.all([
      get<Entity[]>('/sys-org/all'),
      get<Entity[]>('/sys-class/all'),
      get<User[]>('/sys-user/all'),
    ])
    orgs.value = o
    classes.value = c
    users.value = u
  }
  const orgName = (id: number) => orgs.value.find((r) => r.id === id)?.orgName || `组织 #${id}`
  const className = (id: number | null) =>
    id ? classes.value.find((r) => r.id === id)?.className || `班级 #${id}` : '未分配'
  const userName = (id: number | null) =>
    id ? users.value.find((r) => r.id === id)?.realName || `用户 #${id}` : '—'
  return { orgs, classes, users, load, orgName, className, userName }
}
