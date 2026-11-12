// 위젯·제어 센터 목업 PNG 내보내기 (사전 설치된 Playwright 사용, 추가 다운로드 없음)
// 사용: node design/scripts/export-mockups.js   (레포 루트에서)
const { chromium } = require('playwright');
const path = require('path');
const root = process.argv[2] || path.join(__dirname, '../pages');
const out = process.argv[3] || path.join(__dirname, '../images/export');
require('fs').mkdirSync(out, { recursive: true });
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 640, height: 900 }, deviceScaleFactor: 2, timezoneId: 'America/Toronto', locale: 'ko-KR' });
  const p = await ctx.newPage();
  const url = f => 'file://' + path.join(root, f);
  // 위젯 4x1: 라이트·다크, 위젯 단독 + 월페이퍼 포함
  for (const t of ['light', 'dark']) {
    await p.goto(url('widget.html'));
    await p.evaluate(t => document.documentElement.setAttribute('data-theme', t), t);
    await p.waitForTimeout(300);
    await p.locator('.wall').screenshot({ path: `${out}/widget-4x1-${t}-wall.png` });
    await p.addStyleTag({ content: 'html,body,.wall{background:transparent!important} .widget{box-shadow:none!important} *{transition:none!important}' });
    await p.locator('.widget').screenshot({ path: `${out}/widget-4x1-${t}.png`, omitBackground: true });
  }
  // 제어 센터: 중간 크기 타일, 시간 표시 타일(토론토 / 서울)
  await p.goto(url('cc.html'));
  const walls = p.locator('.wall');
  await walls.nth(0).screenshot({ path: `${out}/cc-medium.png` });
  for (const [tz, name] of [['America/Toronto', 'toronto'], ['Asia/Seoul', 'seoul']]) {
    await p.selectOption('#tz', tz);
    await walls.nth(1).screenshot({ path: `${out}/cc-times-${name}.png` });
  }
  await b.close();
})();
