import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { useAuth, safeReturn } from './stores/auth'
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
window.addEventListener('rc:unauthorized', () => {
  const returnTo = safeReturn(router.currentRoute.value.fullPath)
  useAuth().clear()
  if (router.currentRoute.value.path != '/login')
    void router.replace({ path: '/login', query: { returnTo, expired: '1' } })
})
app.mount('#app')
