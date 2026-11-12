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
  // 위젯 4x1·2x1·1x1: 라이트·다크, 위젯 단독(모서리 투명) + 홈 화면 배경 포함 한 장
  for (const t of ['light', 'dark']) {
    await p.goto(url('widget.html'));
    await p.evaluate(t => document.documentElement.setAttribute('data-theme', t), t);
    await p.waitForTimeout(300);
    await p.locator('.wall').screenshot({ path: `${out}/widget-wall-${t}.png` });
    await p.addStyleTag({ content: 'html,body,.wall{background:transparent!important} .widget{box-shadow:none!important} *{transition:none!important}' });
    for (const [sel, size] of [['.widget:not(.w1):not(.w2)', '4x1'], ['.w2', '2x1'], ['.w1', '1x1']]) {
      await p.locator(sel).screenshot({ path: `${out}/widget-${size}-${t}.png`, omitBackground: true });
    }
  }
  // 제어 센터 이미지는 Stageshow 예시(design/examples/shiftwork-cc)에서 찍는다. design/HANDOFF.md 참고.
  await b.close();
})();
