import axios from 'axios'
export const http = axios.create({ baseURL: '/api', timeout: 20000 })
export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
  ) {
    super(message)
  }
}
http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('rc.token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
http.interceptors.response.use(
  (r) => r,
  async (error) => {
    if (axios.isCancel(error)) throw error
    let data = error.response?.data
    if (data instanceof Blob) {
      try {
        data = JSON.parse(await data.text())
      } catch {
        data = null
      }
    }
    const status = error.response?.status || 0
    const fallback: Record<number, string> = {
      400: '提交内容不符合要求',
      401: '登录已失效，请重新登录',
      403: '权限不足，无法执行此操作',
      404: '不存在或不可访问',
      409: '记录冲突，请检查编号',
      413: '文件超过大小限制',
      500: '服务暂时异常，请稍后重试',
    }
    const message =
      data?.message ||
      fallback[status] ||
      (status ? '请求失败，请稍后重试' : '网络连接失败，请检查连接后重试')
    if (status === 401 && !error.config?.url?.endsWith('/login'))
      window.dispatchEvent(new CustomEvent('rc:unauthorized'))
    throw new ApiError(status === 403 ? `权限不足：${message}` : message, status)
  },
)
export async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  return (await http.get(path, { signal })).data.data
}
export async function send<T = number>(
  method: 'post' | 'put' | 'delete',
  path: string,
  data?: unknown,
  options: { timeout?: number } = {},
): Promise<T> {
  return (await http.request({ method, url: path, data, ...options })).data.data
}
export const message = (e: unknown) => (e instanceof Error ? e.message : '操作失败，请重试')
export const isCancelled = (e: unknown) => axios.isCancel(e)
export const pick = (item: Record<string, any>, keys: string[]) =>
  Object.fromEntries(keys.map((k) => [k, item[k] ?? null]))
