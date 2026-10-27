import { defineConfig } from '@playwright/test'
const run = process.env.E2E_RUN || new Date().toISOString().replace(/[:.]/g, '-')
process.env.E2E_RUN = run
const root = `evidence/private/${run}`
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60000,
  expect: { timeout: 10000 },
  outputDir: `${root}/artifacts`,
  reporter: [
    ['line'],
    ['html', { outputFolder: `${root}/html`, open: 'never' }],
    ['json', { outputFile: `${root}/results.json` }],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:15173',
    browserName: 'chromium',
    channel: process.env.PW_CHANNEL || undefined,
    headless: !process.env.HEADED,
    viewport: { width: 1440, height: 1000 },
    trace: 'on',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
})
