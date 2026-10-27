import { test as base, expect, type Page } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
export const accounts = JSON.parse(fs.readFileSync('evidence/private/accounts.json', 'utf8'))
export const seed = JSON.parse(fs.readFileSync('evidence/private/seed.json', 'utf8'))
export const fixture = (name: string) => path.resolve('tests/fixtures', name)
export const run = process.env.E2E_RUN || 'local'
export const publicDir = path.resolve('evidence/public', run)
fs.mkdirSync(publicDir, { recursive: true })
export const runDir = process.env.RUN_DIR || path.resolve('../../frontend_run_20261027_153619')
export function probe(mode: string, arg: string | number) {
  return execFileSync('python3', [path.join(runDir, 'probe.py'), mode, String(arg)], {
    encoding: 'utf8',
  }).trim()
}
export const imageState = (id: number) => JSON.parse(probe('image', id))
export const unique = (tag: string) => `UI${Date.now().toString().slice(-10)}${tag}`
export const test = base.extend<{
  network: { time: number; method: string; path: string; status?: number; event: string }[]
}>({
  network: [
    async ({ page }, use, testInfo) => {
      const network: any[] = [],
        errors: any[] = []
      const start = Date.now()
      page.on('request', (r) => {
        if (r.url().includes('/api/'))
          network.push({
            time: Date.now() - start,
            event: 'request',
            method: r.method(),
            path: new URL(r.url()).pathname,
          })
      })
      page.on('response', (r) => {
        if (r.url().includes('/api/'))
          network.push({
            time: Date.now() - start,
            event: 'response',
            method: r.request().method(),
            path: new URL(r.url()).pathname,
            status: r.status(),
          })
      })
      page.on('pageerror', (e) => errors.push({ type: 'pageerror', message: e.message }))
      page.on('console', (m) => {
        if (m.type() === 'error') errors.push({ type: 'console', message: m.text() })
      })
      page.on('requestfailed', (r) =>
        errors.push({
          type: 'requestfailed',
          path: new URL(r.url()).pathname,
          message: r.failure()?.errorText,
        }),
      )
      await use(network)
      const name = testInfo.title.split(' ')[0]
      fs.writeFileSync(
        path.join(publicDir, name + '-network.json'),
        JSON.stringify(network, null, 2),
      )
      fs.writeFileSync(
        path.join(publicDir, name + '-browser-errors.json'),
        JSON.stringify(errors, null, 2),
      )
      await page
        .screenshot({ path: path.join(publicDir, name + '.png'), fullPage: true })
        .catch(() => {})
      expect(
        errors.filter((e) => e.type === 'pageerror'),
        'Uncaught page exceptions',
      ).toEqual([])
    },
    { auto: true },
  ],
})
export { expect }
export async function login(page: Page, alias: string) {
  await page.goto('/login')
  await page.getByLabel('用户编号', { exact: true }).fill(accounts[alias].userNo)
  await page.getByLabel('密码', { exact: true }).fill(accounts[alias].password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(/\/(admin|teacher|student)\/awards$/)
  await expect(page.locator('.page-heading h1')).toBeVisible()
  await expect(page.locator('.el-loading-mask')).toHaveCount(0)
}
export async function nav(page: Page, label: string) {
  await page.getByRole('link', { name: label, exact: true }).click()
  await expect(page.locator('.el-loading-mask')).toHaveCount(0)
}
export async function adminApi() {
  const r = await fetch('http://127.0.0.1:18127/api/sys-login/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(accounts.admin),
  })
  const d = await r.json()
  if (!r.ok) throw new Error('Admin setup login failed')
  return async (method: string, endpoint: string, body?: unknown) => {
    const r = await fetch('http://127.0.0.1:18127/api' + endpoint, {
      method,
      headers: {
        Authorization: `Bearer ${d.data.token}`,
        ...(body ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    })
    const response = await r.json()
    if (!r.ok) throw new Error(`Setup ${endpoint} ${r.status}: ${response.message}`)
    return response.data
  }
}
export async function fillAward(page: Page, name: string) {
  await page.locator('#award-awardName').fill(name)
  await page.locator('#award-issuingOrganization').fill('浏览器验收委员会')
  await page.locator('#award-awardDate').fill('2024-02-29')
}
export async function saveAward(page: Page) {
  await page.getByRole('button', { name: '保存奖状', exact: true }).click()
  await expect(page.getByText('奖状已保存', { exact: true })).toBeVisible()
}
export async function openStudentAward(page: Page, name: string) {
  await page
    .locator('.award-card')
    .filter({ has: page.getByRole('heading', { name, exact: true }) })
    .getByRole('button', { name: '查看详情' })
    .click()
  await expect(page.getByTestId('award-image')).toBeVisible()
}
export async function browserImageHash(page: Page) {
  return page.getByTestId('award-image').evaluate(async (img: HTMLImageElement) => {
    const bytes = await (await fetch(img.src)).arrayBuffer()
    return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)))
      .map((x) => x.toString(16).padStart(2, '0'))
      .join('')
  })
}
export async function confirmDelete(page: Page) {
  await page.getByRole('button', { name: '确认删除', exact: true }).click()
}
