/**
 * "Zero browser console warnings" check (PLAN §3).
 *
 * Opens every public page in headless Chromium at phone (375 px) and desktop (1280 px) widths and reports every
 * console warning/error, page error and failed request; also checks scroll restoration and visible keyboard
 * focus. Screenshots go to test-results/screens/.
 *
 * Needs the backend and the frontend running (e.g. `docker compose up`, or the backend + `npm run dev`):
 *   npx playwright install chromium     # once
 *   BASE_URL=http://localhost:5173 npm run check:browser
 *
 * Expected output: only Chrome's own network-log line for /pokedex/99999, a deliberately unknown Pokémon (the
 * API correctly answers 404 and Chrome logs every non-2xx response; no page code can suppress that line).
 */
import { chromium } from 'playwright'
import fs from 'node:fs'

const base = process.env.BASE_URL ?? 'http://localhost:5173'
const outDir = 'test-results/screens'
const routes = ['/', '/pokedex', '/pokedex?page=2', '/pokedex/133', '/pokedex/2', '/pokedex/25', '/pokedex/99999', '/pokedex/abc', '/nowhere']
const viewports = { mobile: { width: 375, height: 812 }, desktop: { width: 1280, height: 900 } }
const expected = (line) => line.includes('/pokedex/99999') && line.includes('404')
const findings = []
fs.mkdirSync(outDir, { recursive: true })

// Optional, for sandboxed environments only: BROWSER_PROXY routes external requests (sprites) through a proxy.
const proxy = process.env.BROWSER_PROXY ? { server: process.env.BROWSER_PROXY, bypass: 'localhost,127.0.0.1' } : undefined
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined, proxy })
for (const [name, viewport] of Object.entries(viewports)) {
  const context = await browser.newContext({ viewport, ignoreHTTPSErrors: Boolean(proxy) })
  const page = await context.newPage()
  page.on('console', (msg) => {
    if (['warning', 'error'].includes(msg.type())) findings.push(`[${name}] console.${msg.type()} on ${page.url()}: ${msg.text()}`)
  })
  page.on('pageerror', (err) => findings.push(`[${name}] page error on ${page.url()}: ${err.message}`))

  for (const route of routes) {
    await page.goto(base + route, { waitUntil: 'networkidle' })
    await page.screenshot({ path: `${outDir}/${name}${route.replace(/[/?=]/g, '_') || '_root'}.png`, fullPage: true })
  }

  // Scroll: a detail page opens at the top; the browser's Back button returns to the same place in the list.
  await page.goto(`${base}/pokedex?page=2`, { waitUntil: 'networkidle' })
  const last = page.getByRole('link', { name: /Venonat/ })
  await last.scrollIntoViewIfNeeded()
  const listScroll = await page.evaluate(() => window.scrollY)
  await last.click()
  await page.getByRole('heading', { level: 1, name: 'Venonat' }).waitFor()
  const detailScroll = await page.evaluate(() => window.scrollY)
  await page.goBack()
  await page.getByRole('link', { name: /Venonat/ }).waitFor()
  await page.waitForTimeout(300)
  const backScroll = await page.evaluate(() => window.scrollY)
  if (detailScroll !== 0) findings.push(`[${name}] detail page opened at scroll ${detailScroll}, expected 0`)
  if (Math.abs(backScroll - listScroll) > 5) findings.push(`[${name}] Back restored scroll ${backScroll}, expected ${listScroll}`)

  // Keyboard: the focused element must show the focus outline.
  await page.goto(`${base}/pokedex`, { waitUntil: 'networkidle' })
  for (let i = 0; i < 4; i++) await page.keyboard.press('Tab')
  const outline = await page.evaluate(() => getComputedStyle(document.activeElement).outlineStyle)
  if (outline !== 'solid') findings.push(`[${name}] focused element has no visible outline (${outline})`)

  await context.close()
}
await browser.close()

const unexpected = findings.filter((line) => !expected(line))
findings.filter(expected).forEach((line) => console.log(`expected: ${line}`))
if (unexpected.length > 0) {
  console.error(unexpected.join('\n'))
  process.exit(1)
}
console.log(`OK: no unexpected console warnings or errors on ${routes.length} routes at 375 and 1280 px; scroll and focus checks passed.`)
