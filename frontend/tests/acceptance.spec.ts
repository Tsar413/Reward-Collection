import {
  test,
  expect,
  login,
  nav,
  accounts,
  seed,
  fixture,
  unique,
  adminApi,
  probe,
  imageState,
  fillAward,
  saveAward,
  openStudentAward,
  browserImageHash,
  confirmDelete,
  publicDir,
} from './helpers'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'

for (const [alias, role] of [
  ['admin', 'admin'],
  ['teacher', 'teacher'],
  ['student1', 'student'],
] as const) {
  test(`A01-${role} 三角色登录与刷新身份`, async ({ page }) => {
    await login(page, alias)
    await expect(page).toHaveURL(`/${role}/awards`)
    await page.reload()
    await expect(page.locator('.page-heading h1')).toBeVisible()
    await expect(page.locator('header')).toContainText(
      role === 'admin' ? '验收管理员' : role === 'teacher' ? '林老师' : '叶知秋',
    )
    if (role !== 'admin') await expect(page.getByRole('link', { name: '组织管理' })).toHaveCount(0)
  })
}
test('A02 错误密码与未登录跳转', async ({ page }) => {
  await page.goto('/admin/students')
  await expect(page).toHaveURL(/\/login\?returnTo=/)
  await page.locator('#login-no').fill(accounts.admin.userNo)
  await page.locator('#login-password').fill('wrong-password')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.locator('.el-alert')).toContainText('用户编号或密码错误')
})
for (const alias of ['teacher', 'student1'])
  test(`A03-${alias} 跨角色地址被阻止`, async ({ page }) => {
    await login(page, alias)
    await page.goto('/admin/students')
    await expect(page.getByText('权限不足', { exact: true })).toBeVisible()
    expect(await page.evaluate(() => !!sessionStorage.getItem('rc.token'))).toBe(true)
  })
test('A04 登出调用后端并清理状态', async ({ page, network }) => {
  await login(page, 'student1')
  await page.getByRole('button', { name: '退出登录' }).click()
  await expect(page).toHaveURL('/login')
  expect(network.some((r) => r.path === '/api/sys-login/logout' && r.status === 200)).toBe(true)
  expect(await page.evaluate(() => sessionStorage.getItem('rc.token'))).toBeNull()
  await page.goto('/student/awards')
  await expect(page).toHaveURL(/login/)
})
test('A05 真实Token失效与安全返回地址', async ({ page }) => {
  await login(page, 'student1')
  await fetch('http://127.0.0.1:18127/api/sys-login/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(accounts.student1),
  })
  await page.getByRole('button', { name: '刷新', exact: true }).click()
  await expect(page).toHaveURL(/login.*expired=1/)
  await expect(page.locator('.el-alert')).toContainText('登录已失效')
  await page.goto('/login?returnTo=https://example.com')
  await page.locator('#login-no').fill(accounts.student1.userNo)
  await page.locator('#login-password').fill(accounts.student1.password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL('/student/awards')
})

test('B01 管理员跨组织筛选与组织详情', async ({ page }) => {
  await login(page, 'admin')
  await nav(page, '组织管理')
  await expect(page.getByRole('button', { name: '新增组织' })).toHaveCount(0)
  const row = page.getByRole('row').filter({ hasText: '远山第二学校' })
  await row.getByRole('button', { name: '详情' }).click()
  await expect(page.getByRole('dialog')).toContainText('远山第二学校')
  await page.getByRole('dialog').locator('.el-dialog__headerbtn').click()
  for (const label of ['教师管理', '班级管理', '学生管理', '个人奖状']) {
    await nav(page, label)
    await page.locator('#org-filter').selectOption(String(seed.orgB))
    await expect(page.locator('.el-table__body')).toContainText('远山')
    await expect(page.locator('.el-table__body')).not.toContainText('青禾')
  }
})
test('B02 管理员新增教师班级学生并按依赖删除', async ({ page }) => {
  const tag = unique('A')
  await login(page, 'admin')
  await nav(page, '教师管理')
  await page.getByRole('button', { name: '新增教师' }).click()
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.locator('.el-alert')).toContainText('必填')
  await page.locator('#f-org').selectOption(String(seed.orgB))
  await page.locator('#f-code').fill(tag + 'T')
  await page.locator('#f-name').fill(tag + '教师')
  await page.locator('#f-password').fill(accounts.teacher.password)
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await nav(page, '班级管理')
  await page.getByRole('button', { name: '新增班级' }).click()
  await page.locator('#f-org').selectOption(String(seed.orgA))
  await page.locator('#f-teacher').selectOption(String(seed.users.teacher.id))
  await page.locator('#f-org').selectOption(String(seed.orgB))
  await expect(page.locator('#f-teacher')).toHaveValue('')
  await expect(page.locator('#f-teacher')).not.toContainText('林老师')
  await page.locator('#f-teacher').selectOption({ label: tag + '教师（' + tag + 'T）' })
  await page.locator('#f-code').fill(tag + 'C')
  await page.locator('#f-name').fill(tag + '班级')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await nav(page, '学生管理')
  await page.getByRole('button', { name: '新增学生' }).click()
  await page.locator('#f-org').selectOption(String(seed.orgB))
  await page.locator('#f-class').selectOption({ label: tag + '班级' })
  await page.locator('#f-code').fill(tag + 'S')
  await page.locator('#f-name').fill(tag + '学生')
  await page.locator('#f-password').fill(accounts.student1.password)
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.locator('#search').fill(tag)
  let row = page.getByRole('row').filter({ hasText: tag + '学生' })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.locator('#f-name').fill(tag + '改名')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: tag + '改名' })).toBeVisible()
  expect(
    probe(
      'query',
      `SELECT COUNT(*) FROM sys_user WHERE user_no='${tag}S' AND real_name=username AND real_name='${tag}改名';`,
    ),
  ).toBe('1')
  for (const [label, name] of [
    ['学生管理', tag + '改名'],
    ['班级管理', tag + '班级'],
    ['教师管理', tag + '教师'],
  ]) {
    await nav(page, label!)
    await page.locator('#search').fill(tag)
    await page
      .getByRole('row')
      .filter({ hasText: name! })
      .getByRole('button', { name: '删除' })
      .click()
    await confirmDelete(page)
    await expect(page.getByText('删除成功', { exact: true }).last()).toBeVisible()
  }
})
test('B03 删除关联班级返回400及组织编辑', async ({ page, network }) => {
  await login(page, 'admin')
  await nav(page, '班级管理')
  await page
    .getByRole('row')
    .filter({ hasText: '青禾一班' })
    .getByRole('button', { name: '删除' })
    .click()
  await confirmDelete(page)
  await expect(page.getByText('班级中存在学生，无法删除', { exact: true })).toBeVisible()
  expect(network.some((r) => r.status === 400)).toBe(true)
  await nav(page, '组织管理')
  await page
    .getByRole('row')
    .filter({ hasText: '青禾实验学校' })
    .getByRole('button', { name: '编辑' })
    .click()
  await expect(page.locator('#f-status')).toHaveCount(0)
  await page.locator('#f-remark').fill('组织备注通过浏览器更新')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
})
test('B04 重复编号409与输入保留', async ({ page, network }) => {
  await login(page, 'admin')
  await nav(page, '教师管理')
  await page.getByRole('button', { name: '新增教师' }).click()
  await page.locator('#f-org').selectOption(String(seed.orgA))
  await page.locator('#f-code').fill(accounts.teacher.userNo)
  await page.locator('#f-name').fill('重复工号测试')
  await page.locator('#f-password').fill(accounts.teacher.password)
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.locator('.el-alert')).toContainText('用户编号已存在')
  await expect(page.locator('#f-name')).toHaveValue('重复工号测试')
  expect(network.some((r) => r.status === 409)).toBe(true)
})

test('C01 教师切换两个班级与管理入口范围', async ({ page }) => {
  await login(page, 'teacher')
  await nav(page, '我的班级')
  await expect(page.locator('#class-filter option')).toHaveCount(3)
  await expect(page.getByRole('button', { name: '新增班级' })).toHaveCount(0)
  for (const key of ['A1', 'A2']) {
    await page.locator('#class-filter').selectOption(String(seed.classes[key].id))
    await expect(page.locator('.el-table__body')).toContainText(seed.classes[key].className)
    await expect(page.locator('.el-table__body .el-button')).toHaveCount(1)
  }
  await expect(page.locator('#class-filter')).not.toContainText('青禾三班')
  await expect(page.getByRole('link', { name: '教师管理' })).toHaveCount(0)
})
test('C02 教师新增修改停用学生并调整自己班级', async ({ page }) => {
  const tag = unique('T')
  await login(page, 'teacher')
  await nav(page, '学生管理')
  await page.getByRole('button', { name: '新增学生' }).click()
  await expect(page.locator('#f-class option')).toHaveCount(3)
  await page.locator('#f-class').selectOption(String(seed.classes.A1.id))
  await page.locator('#f-code').fill(tag)
  await page.locator('#f-name').fill('教师创建的学生')
  await page.locator('#f-password').fill(accounts.student1.password)
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.locator('#search').fill(tag)
  const row = page.getByRole('row').filter({ hasText: tag })
  await row.getByRole('button', { name: '编辑' }).click()
  await page.locator('#f-class').selectOption(String(seed.classes.A2.id))
  await page.locator('#f-status').selectOption('0')
  await page.locator('#f-name').fill('教师修改后的学生')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(row).toContainText('青禾二班')
  await expect(row).toContainText('停用')
  expect(
    probe(
      'query',
      `SELECT COUNT(*) FROM sys_user WHERE user_no='${tag}' AND status=0 AND class_id=${seed.classes.A2.id} AND real_name=username;`,
    ),
  ).toBe('1')
  await row.getByRole('button', { name: '删除' }).click()
  await confirmDelete(page)
  await expect(page.getByText('删除成功', { exact: true }).last()).toBeVisible()
})
test('C03 Excel混合导入与真实MySQL行级结果', async ({ page }) => {
  test.setTimeout(120000)
  const api = await adminApi()
  for (const u of await api('GET', '/sys-user/all'))
    if (seed.importIds.includes(u.userNo)) await api('DELETE', `/sys-user/delete/${u.id}`)
  await login(page, 'teacher')
  await nav(page, '学生管理')
  await page.getByRole('button', { name: 'Excel 导入' }).click()
  await page.locator('#excel-file').setInputFiles(fixture('students_mixed.xlsx'))
  await page.getByRole('button', { name: '开始导入' }).click()
  await expect(page.getByTestId('import-report')).toContainText('成功 3，跳过 1，失败 8', {
    timeout: 90000,
  })
  await expect(page.getByTestId('import-report')).toContainText('总计 12 行')
  for (const id of seed.importIds)
    expect(
      probe(
        'query',
        `SELECT COUNT(*) FROM sys_user WHERE user_no='${id}' AND class_id=${seed.classes.A1.id};`,
      ),
    ).toBe('1')
  fs.writeFileSync(
    path.join(publicDir, 'C03-mysql.json'),
    JSON.stringify({
      expected: 3,
      persisted: seed.importIds.length,
      rows: 12,
      success: 3,
      skipped: 1,
      failed: 8,
    }),
  )
})
for (const type of ['PERSONAL', 'CLASS'])
  test(`C04-${type} 教师创建奖状`, async ({ page }) => {
    const name = unique(type)
    await login(page, 'teacher')
    if (type === 'CLASS') await nav(page, '班级奖状')
    await page.getByRole('button', { name: '新增奖状' }).click()
    await page.locator('#award-class').selectOption(String(seed.classes.A2.id))
    if (type === 'PERSONAL')
      await page.locator('#award-student').selectOption(String(seed.users.student3.id))
    await fillAward(page, name)
    await page.locator('#award-file').setInputFiles(fixture('award.png'))
    await saveAward(page)
    await page.locator('#search').fill(name)
    await expect(page.locator('.el-table__body')).toContainText(name)
    expect(
      probe(
        'query',
        `SELECT COUNT(*) FROM award_record WHERE award_name='${name}' AND award_type='${type}' AND class_id=${seed.classes.A2.id} AND is_deleted=0;`,
      ),
    ).toBe('1')
    await page
      .getByRole('row')
      .filter({ hasText: name })
      .getByRole('button', { name: '删除' })
      .click()
    await confirmDelete(page)
    await expect(page.getByText('奖状已删除', { exact: true })).toBeVisible()
  })
test('C05 权限变更后真实403保留登录与表单', async ({ page, network }) => {
  const api = await adminApi(),
    cl = seed.classes.A2
  await login(page, 'teacher')
  await nav(page, '学生管理')
  await page
    .getByRole('row')
    .filter({ hasText: seed.users.student3.userNo })
    .getByRole('button', { name: '编辑' })
    .click()
  await page.locator('#f-name').fill('权限错误保留输入')
  try {
    await api('PUT', `/sys-class/update/${cl.id}`, { ...cl, teacherUserId: seed.users.teacher2.id })
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await expect(page.locator('.el-alert')).toContainText('权限不足')
    await expect(page.locator('#f-name')).toHaveValue('权限错误保留输入')
    expect(network.some((r) => r.status === 403)).toBe(true)
    expect(await page.evaluate(() => !!sessionStorage.getItem('rc.token'))).toBe(true)
  } finally {
    await api('PUT', `/sys-class/update/${cl.id}`, cl)
  }
})

test('D01 学生新增修改图片替换并删除完整链路', async ({ page, network }) => {
  await login(page, 'student1')
  const name = unique('S')
  await page.getByRole('button', { name: '新增奖状' }).click()
  await expect(page.locator('#award-student')).toHaveCount(0)
  await expect(page.locator('#award-class')).toHaveCount(0)
  await fillAward(page, name)
  await page.locator('#award-file').setInputFiles(fixture('award.png'))
  await saveAward(page)
  await page.locator('#search').fill(name)
  await openStudentAward(page, name)
  const id = Number(new URL(page.url()).searchParams.get('award'))
  const before = imageState(id)
  expect(await browserImageHash(page)).toBe(before.sha256)
  await page.getByRole('button', { name: '修改文字', exact: true }).click()
  await page.locator('#award-awardName').fill(name + '更新')
  await page.locator('#award-awardDate').fill('2026-02-28')
  await saveAward(page)
  await expect(page.getByTestId('detail-title')).toHaveText(name + '更新')
  await page.locator('#replace-file').setInputFiles(fixture('replacement.jpg'))
  const expected = crypto
    .createHash('sha256')
    .update(fs.readFileSync(fixture('replacement.jpg')))
    .digest('hex')
  await expect.poll(() => browserImageHash(page)).toBe(expected)
  const after = imageState(id)
  expect(after.sha256).toBe(expected)
  expect(after.storage_key).not.toBe(before.storage_key)
  expect(
    fs.existsSync(path.join('../../frontend_run_20261027_153619/uploads', before.storage_key)),
  ).toBe(false)
  await page.screenshot({ path: path.join(publicDir, 'D01-replaced.png'), fullPage: true })
  await page.getByRole('button', { name: '关闭详情' }).click()
  await openStudentAward(page, name + '更新')
  expect(await browserImageHash(page)).toBe(expected)
  await page.locator('#replace-file').setInputFiles(fixture('disguised.png'))
  await expect(page.locator('.el-alert')).toContainText('文件不是支持的图片')
  expect(await browserImageHash(page)).toBe(expected)
  expect(imageState(id).sha256).toBe(expected)
  await page.getByRole('button', { name: '关闭详情' }).click()
  await page
    .locator('.award-card')
    .filter({ hasText: name + '更新' })
    .getByRole('button', { name: '删除', exact: true })
    .click()
  await confirmDelete(page)
  await expect(page.locator('.award-card')).toHaveCount(0)
  expect(probe('query', `SELECT is_deleted FROM award_record WHERE id=${id};`)).toBe('1')
  expect(probe('query', `SELECT COUNT(*) FROM award_image WHERE award_id=${id};`)).toBe('0')
  expect(
    fs.existsSync(path.join('../../frontend_run_20261027_153619/uploads', after.storage_key)),
  ).toBe(false)
  fs.writeFileSync(
    path.join(publicDir, 'D01-mysql-files.json'),
    JSON.stringify(
      {
        before,
        after,
        deleted: true,
        activeImageRows: 0,
        requests: network.filter((r) => r.path.includes('award-image')),
      },
      null,
      2,
    ),
  )
})
test('D02 学生班级奖状只读与数据范围', async ({ page }) => {
  await login(page, 'student1')
  await expect(page.locator('.award-cards')).toContainText('数学探索之星')
  await expect(page.locator('.award-cards')).not.toContainText('阅读小达人')
  await nav(page, '班级奖状')
  await expect(page.locator('.award-cards')).toContainText('青禾一班文明班级')
  await expect(page.getByRole('button', { name: '新增奖状' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '删除', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '查看详情' }).click()
  await expect(page.getByTestId('award-image')).toBeVisible()
  await expect(page.locator('#replace-file')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '修改文字' })).toHaveCount(0)
})
for (const alias of ['teacher', 'student1'])
  test(`D03-${alias} 本人资料只修改姓名备注`, async ({ page }) => {
    await login(page, alias)
    await nav(page, '个人资料')
    await expect(page.locator('select')).toHaveCount(0)
    await page.locator('#profile-remark').fill('浏览器修改本人备注')
    await page.getByRole('button', { name: '保存资料' }).click()
    await expect(page.getByText('资料已保存', { exact: true })).toBeVisible()
    const id = seed.users[alias === 'teacher' ? 'teacher' : 'student1'].id
    expect(
      probe(
        'query',
        `SELECT COUNT(*) FROM sys_user WHERE id=${id} AND remark='浏览器修改本人备注' AND status=1;`,
      ),
    ).toBe('1')
  })
test('D04 手机视口无横向溢出主要操作可用', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await login(page, 'student1')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('button', { name: '新增奖状' }).click()
  await expect(page.locator('#award-awardName')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await nav(page, '班级奖状')
  await page.getByRole('button', { name: '查看详情' }).click()
  await expect(page.getByTestId('award-image')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

test('E01 列表与筛选零图片请求详情按需加载', async ({ page, network }) => {
  await login(page, 'student1')
  await page.locator('#search').fill('数学')
  await page.locator('#search').fill('')
  await page.waitForTimeout(300)
  expect(
    network.filter((r) => r.event === 'request' && r.path.startsWith('/api/award-image/one/')),
  ).toHaveLength(0)
  await openStudentAward(page, '数学探索之星')
  const id = Number(new URL(page.url()).searchParams.get('award'))
  const text = network.find(
    (r) => r.event === 'response' && r.path === `/api/award-record/one/${id}`,
  )
  const image = network.find(
    (r) => r.event === 'request' && r.path === `/api/award-image/one/${id}`,
  )
  expect(text).toBeTruthy()
  expect(image!.time).toBeGreaterThanOrEqual(text!.time)
  expect(await browserImageHash(page)).toBe(imageState(id).sha256)
})
test('E02 快速切换及关闭详情不会串图且释放URL', async ({ page, network }) => {
  await page.addInitScript(() => {
    const original = URL.createObjectURL.bind(URL),
      revoke = URL.revokeObjectURL.bind(URL)
    ;(window as any).objectUrls = { created: [], revoked: [] }
    URL.createObjectURL = (b: Blob | MediaSource) => {
      const u = original(b)
      ;(window as any).objectUrls.created.push(u)
      return u
    }
    URL.revokeObjectURL = (u: string) => {
      ;(window as any).objectUrls.revoked.push(u)
      revoke(u)
    }
  })
  await login(page, 'student1')
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Network.enable')
  await cdp.send('Network.emulateNetworkConditions', {
    offline: false,
    latency: 300,
    downloadThroughput: 300000,
    uploadThroughput: 300000,
  })
  const firstId = seed.awards.find((a: any) => a.awardName === '数学探索之星').id
  const firstImageRequest = page.waitForRequest((r) =>
    r.url().endsWith('/api/award-image/one/' + firstId),
  )
  await page
    .locator('.award-card')
    .filter({ hasText: '数学探索之星' })
    .getByRole('button', { name: '查看详情' })
    .click()
  await firstImageRequest
  await page.getByRole('button', { name: '下一张奖状' }).click()
  await expect(page.getByTestId('detail-title')).toHaveText('春日创作奖')
  await expect(page.getByTestId('award-image')).toBeVisible()
  await page.waitForTimeout(900)
  const id = Number(new URL(page.url()).searchParams.get('award'))
  expect(await browserImageHash(page)).toBe(imageState(id).sha256)
  await page.getByRole('button', { name: '关闭详情' }).click()
  expect(
    await page.evaluate(() => {
      const v = (window as any).objectUrls
      return v.created.every((u: string) => v.revoked.includes(u))
    }),
  ).toBe(true)
  await cdp.send('Network.emulateNetworkConditions', {
    offline: false,
    latency: 0,
    downloadThroughput: -1,
    uploadThroughput: -1,
  })
  fs.writeFileSync(
    path.join(publicDir, 'E02-race.json'),
    JSON.stringify(
      { finalAwardId: id, correctSHA256: true, allObjectUrlsRevoked: true, network },
      null,
      2,
    ),
  )
})
test('E03 图片404错误解析与重新加载恢复', async ({ page }) => {
  const aid = seed.awards.find((a: any) => a.awardName === '数学探索之星').id
  await login(page, 'student1')
  probe('hide', aid)
  try {
    await page.goto(`/student/awards?award=${aid}`)
    await expect(page.locator('.el-alert')).toContainText('图片文件不存在')
    await expect(page.getByTestId('award-image')).toHaveCount(0)
  } finally {
    probe('restore', aid)
  }
  await page.getByRole('button', { name: '重新加载图片' }).click()
  await expect(page.getByTestId('award-image')).toBeVisible()
  expect(await browserImageHash(page)).toBe(imageState(aid).sha256)
})
test('E04 真实图片IO异常500合理呈现并恢复', async ({ page, network }) => {
  const aid = seed.awards.find((a: any) => a.awardName === '数学探索之星').id
  await login(page, 'student1')
  probe('directory', aid)
  try {
    await page.goto(`/student/awards?award=${aid}`)
    await expect(page.locator('.el-alert')).toContainText('图片读取失败')
    expect(network.some((r) => r.status === 500)).toBe(true)
    await expect(page).toHaveURL(/student\/awards/)
  } finally {
    probe('restore', aid)
  }
  await page.getByRole('button', { name: '重新加载图片' }).click()
  await expect(page.getByTestId('award-image')).toBeVisible()
})

test('F01 奖状必填长度日期大小格式校验', async ({ page, network }) => {
  await login(page, 'student1')
  await page.getByRole('button', { name: '新增奖状' }).click()
  await page.getByRole('button', { name: '保存奖状' }).click()
  await expect(page.locator('.el-alert')).toContainText('请填写')
  await page.locator('#award-awardName').fill('奖'.repeat(250))
  expect((await page.locator('#award-awardName').inputValue()).length).toBe(200)
  await page.locator('#award-issuingOrganization').fill('校级')
  await page.locator('#award-awardDate').fill('2026-02-30')
  await page.getByRole('button', { name: '保存奖状' }).click()
  await expect(page.locator('.el-alert')).toContainText('有效日期')
  await page.locator('#award-file').setInputFiles(fixture('oversized.png'))
  await expect(page.locator('.el-alert')).toContainText('5MiB')
  await page.locator('#award-file').setInputFiles(fixture('invalid.txt'))
  await expect(page.locator('.el-alert')).toContainText('只支持')
  expect(
    network.filter((r) => r.event === 'request' && r.path === '/api/award-record/add'),
  ).toHaveLength(0)
})
test('F02 双击保存只创建一条记录', async ({ page, network }) => {
  const name = unique('D')
  await login(page, 'student1')
  await page.getByRole('button', { name: '新增奖状' }).click()
  await fillAward(page, name)
  await page.locator('#award-file').setInputFiles(fixture('award.png'))
  await page.getByRole('button', { name: '保存奖状' }).dblclick()
  await expect(page.getByText('奖状已保存', { exact: true })).toBeVisible()
  expect(
    network.filter((r) => r.event === 'request' && r.path === '/api/award-record/add'),
  ).toHaveLength(1)
  expect(
    probe(
      'query',
      `SELECT COUNT(*) FROM award_record WHERE award_name='${name}' AND is_deleted=0;`,
    ),
  ).toBe('1')
  await page.locator('#search').fill(name)
  await page.getByRole('button', { name: '删除', exact: true }).click()
  await confirmDelete(page)
  await expect(page.getByText('奖状已删除', { exact: true })).toBeVisible()
})
test('F03 网络失败提示重试与无数据状态', async ({ page }) => {
  await login(page, 'student1')
  await page.context().setOffline(true)
  await page.getByRole('button', { name: '刷新', exact: true }).click()
  await expect(page.locator('.el-alert')).toContainText('网络连接失败')
  await page.context().setOffline(false)
  await page.getByRole('button', { name: '刷新', exact: true }).click()
  await expect(page.locator('.el-alert')).toHaveCount(0)
  await page.locator('#search').fill('没有此奖状XYZ')
  await expect(page.getByText('还没有奖状，期待你的精彩时刻')).toBeVisible()
})
test('F04 200加null详情禁止继续图片请求', async ({ page, network }) => {
  await login(page, 'student1')
  await page.goto('/student/awards?award=99999999')
  await expect(page.getByText('奖状不存在或不可访问')).toBeVisible()
  expect(network.filter((r) => r.path === '/api/award-image/one/99999999')).toHaveLength(0)
})

test('B05 管理员跨组织奖状新增文字编辑换图与删除', async ({ page }) => {
  const name = unique('B')
  await login(page, 'admin')
  await nav(page, '班级奖状')
  await page.getByRole('button', { name: '新增奖状' }).click()
  await page.locator('#award-org').selectOption(String(seed.orgA))
  await page.locator('#award-class').selectOption(String(seed.classes.A1.id))
  await page.locator('#award-org').selectOption(String(seed.orgB))
  await expect(page.locator('#award-class')).toHaveValue('')
  await expect(page.locator('#award-class')).not.toContainText('青禾')
  await page.locator('#award-class').selectOption(String(seed.classes.B1.id))
  await fillAward(page, name)
  await page.locator('#award-file').setInputFiles(fixture('alternate.webp'))
  await saveAward(page)
  await page.locator('#search').fill(name)
  await page
    .getByRole('row')
    .filter({ hasText: name })
    .getByRole('button', { name: '详情', exact: true })
    .click()
  await expect(page.getByTestId('award-image')).toBeVisible()
  const id = Number(new URL(page.url()).searchParams.get('award'))
  expect(
    probe(
      'query',
      `SELECT COUNT(*) FROM award_record WHERE id=${id} AND org_id=${seed.orgB} AND class_id=${seed.classes.B1.id} AND student_user_id IS NULL;`,
    ),
  ).toBe('1')
  await page.getByRole('button', { name: '修改文字' }).click()
  await page.locator('#award-description').fill('跨组织修改说明')
  await saveAward(page)
  await expect(page.getByRole('dialog').filter({ hasText: '奖状详情' })).toContainText(
    '跨组织修改说明',
  )
  await page.locator('#replace-file').setInputFiles(fixture('award.png'))
  const expected = crypto
    .createHash('sha256')
    .update(fs.readFileSync(fixture('award.png')))
    .digest('hex')
  await expect.poll(() => browserImageHash(page)).toBe(expected)
  await page.getByRole('button', { name: '关闭详情' }).click()
  await page
    .getByRole('row')
    .filter({ hasText: name })
    .getByRole('button', { name: '删除', exact: true })
    .click()
  await confirmDelete(page)
  await expect(page.getByText('奖状已删除', { exact: true })).toBeVisible()
})
test('B06 管理员修改教师与班级保持归属', async ({ page }) => {
  await login(page, 'admin')
  await nav(page, '教师管理')
  await page
    .getByRole('row')
    .filter({ hasText: accounts.teacherB.userNo })
    .getByRole('button', { name: '编辑' })
    .click()
  await expect(page.locator('#f-org')).toHaveCount(0)
  await expect(page.locator('#f-code')).toHaveCount(0)
  await page.locator('#f-remark').fill('管理员跨组织维护教师')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await nav(page, '班级管理')
  await page
    .getByRole('row')
    .filter({ hasText: '远山一班' })
    .getByRole('button', { name: '编辑' })
    .click()
  await expect(page.locator('#f-teacher')).not.toContainText('林老师')
  await page.locator('#f-remark').fill('管理员维护班级')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText('保存成功', { exact: true }).last()).toBeVisible()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  expect(
    probe(
      'query',
      `SELECT COUNT(*) FROM sys_class WHERE id=${seed.classes.B1.id} AND org_id=${seed.orgB} AND remark='管理员维护班级';`,
    ),
  ).toBe('1')
})
for (const alias of ['admin', 'teacher'])
  test(`E05-${alias} 管理列表筛选同样不预取图片`, async ({ page, network }) => {
    await login(page, alias)
    await page.locator('#class-filter').selectOption(String(seed.classes.A1.id))
    await page.locator('#search').fill('数学')
    await page.waitForTimeout(300)
    expect(
      network.filter((r) => r.event === 'request' && r.path.startsWith('/api/award-image/one/')),
    ).toHaveLength(0)
    await page
      .getByRole('row')
      .filter({ hasText: '数学探索之星' })
      .getByRole('button', { name: '详情' })
      .click()
    await expect(page.getByTestId('award-image')).toBeVisible()
    expect(
      network.some((r) => r.event === 'request' && r.path.startsWith('/api/award-image/one/')),
    ).toBe(true)
  })
test('F05 访问其他学生奖状URL显示不可访问且不加载图', async ({ page, network }) => {
  await login(page, 'student1')
  const other = seed.awards.find((a: any) => a.studentUserId === seed.users.student6.id)
  await page.goto(`/student/awards?award=${other.id}`)
  await expect(page.getByText('奖状不存在或不可访问')).toBeVisible()
  expect(network.filter((r) => r.path === `/api/award-image/one/${other.id}`)).toHaveLength(0)
})
