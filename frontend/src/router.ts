import { createRouter, createWebHistory } from 'vue-router'
import { useAuth, home } from './stores/auth'
import { ApiError } from './api'
import Login from './pages/Login.vue'
import Resource from './pages/Resource.vue'
import Awards from './pages/Awards.vue'
import Profile from './pages/Profile.vue'
import Status from './pages/Status.vue'
import AdminLayout from './layouts/AdminLayout.vue'
import TeacherLayout from './layouts/TeacherLayout.vue'
import StudentLayout from './layouts/StudentLayout.vue'
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    { path: '/forbidden', component: Status, meta: { status: 'forbidden' } },
    { path: '/session-error', component: Status, meta: { status: 'session' } },
    ...(
      [
        [
          'admin',
          AdminLayout,
          ['orgs', 'teachers', 'classes', 'students', 'awards', 'class-awards', 'profile'],
        ],
        ['teacher', TeacherLayout, ['classes', 'students', 'awards', 'class-awards', 'profile']],
        ['student', StudentLayout, ['awards', 'class-awards', 'profile']],
      ] as const
    ).map(([role, layout, paths]) => ({
      path: `/${role}`,
      component: layout,
      meta: { role: role.toUpperCase() },
      children: [
        { path: '', redirect: `/${role}/awards` },
        ...paths.map((path) => ({
          path,
          component: path === 'profile' ? Profile : path.includes('awards') ? Awards : Resource,
          meta: { kind: path },
        })),
      ],
    })),
    { path: '/:pathMatch(.*)*', component: Status, meta: { status: 'missing' } },
  ],
})
router.beforeEach(async (to) => {
  const auth = useAuth()
  if (to.path === '/login' || to.path === '/session-error') return true
  if (!sessionStorage.getItem('rc.token'))
    return { path: '/login', query: { returnTo: to.fullPath } }
  if (!auth.validated) {
    try {
      await auth.refresh()
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) return { path: '/session-error' }
      return { path: '/login', query: { returnTo: to.fullPath } }
    }
  }
  if (to.meta.role && auth.user?.role !== to.meta.role) return '/forbidden'
  if (to.path === '/') return home(auth.user?.role)
  return true
})
