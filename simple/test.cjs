// 교대온 간단(simple/index.html) 시험. 사용: node simple/test.cjs  (Playwright·python3 필요)
// 레포 루트를 작은 웹 서버로 띄우고, 시각을 2026-11-12 13:00(서울)로 고정해 CSV 불러오기와 화면을 확인한다.
// 이름은 모두 지어낸 것.
const { chromium } = require('playwright')
const { spawn, execFileSync } = require('child_process')
const path = require('path')
const fs = require('fs')
const os = require('os')

const ROOT = path.join(__dirname, '..')
const PORT = Number(process.env.SIMPLE_PORT || 8791)
const URL0 = `http://127.0.0.1:${PORT}/simple/`
const SHOTS = process.env.SHOTS || ''
const results = []
const check = (name, ok, detail = '') => { results.push(ok); console.log((ok ? 'PASS' : 'FAIL') + ' | ' + name + (ok ? '' : ' | ' + String(detail).slice(0, 160))) }
const wait = (ms) => new Promise((r) => setTimeout(r, ms))

// 통합관제실 근무표 엑셀을 CSV로 저장한 모양: 제목, 날짜 줄(31칸), 요일 줄, NC 4명
const cyc = ['일', '야', '조', 'O']
const WD = ['일', '월', '화', '수', '목', '금', '토']
function nqcCsv() {
  const head = ['', '', '2026년 11월', '']
  const week = ['', '', '', '']
  for (let d = 1; d <= 31; d++) { head.push(String(d)); week.push(WD[new Date(Date.UTC(2026, 10, d)).getUTCDay()]) }
  const people = [['김교대', 0], ['이야간', 1], ['박조근', 2], ['최비번', 3]]
  const rows = people.map(([n, k], i) => {
    const r = ['', '', i === 0 ? 'NC' : '', n]
    for (let d = 1; d <= 31; d++) r.push(d > 30 ? '' : cyc[(d - 1 + k) % 4])
    if (n === '이야간') { r[3 + 20] = '휴가'; r[3 + 21] = '대일'; r[3 + 25] = '교육' }
    return r
  })
  return '﻿' + ['11월  통합관제실 근무표', head.join(','), week.join(','), ...rows.map((r) => r.join(','))].join('\r\n') + '\r\n'
}
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'simple-'))
const f1 = path.join(tmp, 'nqc-11.csv')
fs.writeFileSync(f1, nqcCsv())
// 12월, 날짜·근무 두 칸, 한국어 윈도우 엑셀처럼 CP949(EUC-KR)로 저장
const dec = ['날짜,근무', ...Array.from({ length: 31 }, (_, i) => `2026-12-${String(i + 1).padStart(2, '0')},${['야', '조', '비번', '일'][i % 4]}`)].join('\n')
const f2 = path.join(tmp, 'dec-euckr.csv')
execFileSync('python3', ['-I', '-c', 'import sys;open(sys.argv[2],"wb").write(sys.argv[1].encode("euc-kr"))', dec, f2])
const f3 = path.join(tmp, 'table.xlsx')
fs.writeFileSync(f3, 'x')

;(async () => {
  const server = spawn('python3', ['-m', 'http.server', String(PORT), '--bind', '127.0.0.1'], { cwd: ROOT, stdio: 'ignore' })
  await wait(700)
  const b = await chromium.launch()
  const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: SHOTS ? 2 : 1, timezoneId: 'Asia/Seoul', locale: 'ko-KR', colorScheme: 'light' })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', (e) => errors.push(e.message))
  p.on('dialog', (d) => d.accept())
  await p.clock.install({ time: new Date('2026-11-12T13:00:00+09:00') })
  await p.goto(URL0)
  await wait(300)
  const txt = (s) => p.evaluate((s) => document.querySelector(s)?.textContent.trim(), s)
  const vis = (s) => p.evaluate((s) => { const e = document.querySelector(s); return !!e && !e.closest('[hidden]') }, s)
  const shot = async (n) => { if (!SHOTS) return; await p.evaluate(() => { document.querySelector('#toast').classList.remove('show'); window.scrollTo(0, 0) }); await wait(250); await p.screenshot({ path: path.join(SHOTS, n + '.png'), fullPage: true }) }

  check('처음: 홈에 빈 안내', (await vis('#homeEmpty')) && (await txt('#emptyTitle')) === '11월 근무표가 없어요', await txt('#emptyTitle'))
  check('탭은 홈·근무표·설정 셋', (await p.$$eval('#tabbar button', (bs) => bs.map((b) => b.textContent).join(','))) === '홈,근무표,설정')
  await shot('home-empty')
  await p.click('#tabbar button[data-tab="settings"]')
  check('이번 달·다음 달 이름', (await txt('#m0')) === '2026년 11월' && (await txt('#m1')) === '2026년 12월')

  // 1. 표 모양(여러 사람) CSV
  await p.setInputFiles('#csvFile', f1)
  await wait(300)
  check('미리보기 열림', await vis('#preview'))
  const names = await p.$$eval('#pvName option', (os) => os.map((o) => o.textContent))
  check('표에서 4명 찾음(NC 글자 말고 이름)', names.join(',') === '김교대,이야간,박조근,최비번', names)
  await p.selectOption('#pvName', { label: '이야간' })
  await wait(100)
  const notes = await p.$$eval('#pvNotes li', (ls) => ls.map((l) => l.textContent).join(' / '))
  check('31일 칸은 뺐다는 안내 없음(비어 있음)', !notes.includes('31일은 뺐어요'), notes)
  check('모르는 글 "교육" 안내', notes.includes('"교육"'), notes)
  check('미리보기 요약', (await txt('#pvSummary')).includes('휴가 1') && (await txt('#pvSummary')).includes('대근(일근) 1'), await txt('#pvSummary'))
  await shot('settings-preview')
  await p.click('#pvSave')
  await wait(200)
  check('저장 안내', (await txt('#toast')).includes('2026년 11월 근무표를 저장했어요'), await txt('#toast'))
  check('내 이름 기억', (await p.inputValue('#myName')) === '이야간')

  // 2. 홈
  await p.click('#tabbar button[data-tab="home"]')
  await wait(100)
  check('홈: 오늘 일근 · 근무 중', (await txt('#heroName')) === '일근' && (await txt('#heroPill')) === '근무 중', (await txt('#heroName')) + ' ' + (await txt('#heroPill')))
  check('홈: 남은 시간·퇴근', (await txt('#heroLeft')) === '4시간 30분 남음' && (await txt('#heroEnd')) === '17:30 퇴근', (await txt('#heroLeft')) + ' ' + (await txt('#heroEnd')))
  check('홈: 내일 야근 17:30 출근', (await txt('#nextName')) === '야근' && (await txt('#nextRange')) === '17:30 출근', (await txt('#nextName')) + ' ' + (await txt('#nextRange')))
  const week = await p.$$eval('#weekRows .shift', (s) => s.map((x) => x.textContent).join(''))
  check('홈: 앞으로 7일(12~18일)', week === '일야조O일야조', week)
  await shot('home')

  // 3. 근무표
  await p.click('#tabbar button[data-tab="sheet"]')
  await wait(100)
  check('근무표 제목', (await txt('#sheetTitle')) === '2026년 11월')
  check('20일 휴가·21일 대일·25일 교육', (await txt('#cal [data-k="2026-11-20"] .shift')) === '휴가' && (await txt('#cal [data-k="2026-11-21"] .shift')) === '대일' && (await txt('#cal [data-k="2026-11-25"] .shift')) === '교육')
  await p.click('#cal [data-k="2026-11-13"]')
  check('날짜 누르면 시간', (await txt('#detail')).includes('야근 · 17:30 – 08:30'), await txt('#detail'))
  await p.click('#cal [data-k="2026-11-14"]')
  check('조근은 이어지는 근무로', (await txt('#detail')).includes('조근 · ~ 08:30 퇴근'), await txt('#detail'))
  check('앞으로 7일 글은 시간만', (await p.$$eval('#weekRows small', (s) => s.map((x) => x.textContent))).slice(0, 4).join('|') === '08:30 – 17:30|17:30 – 08:30|~ 08:30 퇴근|비번')
  await shot('sheet')
  await p.click('#nextM')
  check('다음 달은 빈 안내', (await txt('#sheetTitle')) === '2026년 12월' && (await vis('#sheetEmpty')))

  // 4. 12월: 두 칸, EUC-KR
  await p.click('#tabbar button[data-tab="settings"]')
  await p.click('#monthSeg button[data-m="1"]')
  await p.setInputFiles('#csvFile', f2)
  await wait(300)
  check('EUC-KR 파일 읽기(한 사람, 고르기 없음)', (await txt('#pvTitle')) === '2026년 12월' && !(await vis('#pvPerson')), await txt('#pvTitle'))
  check('"비번"은 O로', (await txt('#pvSummary')).includes('비번 8'), await txt('#pvSummary'))
  await p.click('#pvSave')
  await wait(100)
  // 5. 달이 다른 파일
  await p.click('#monthSeg button[data-m="0"]')
  await p.setInputFiles('#csvFile', f2)
  await wait(300)
  const n2 = await p.$$eval('#pvNotes li', (ls) => ls.map((l) => l.textContent).join(' / '))
  check('달이 다르면 알려 주고 저장 막음', n2.includes('파일에는 2026년 12월') && n2.includes('다음 달(2026년 12월)을 고르면') && (await p.isDisabled('#pvSave')), n2)
  await p.click('#pvCancel')
  // 6. 붙여 넣기(탭으로 나뉜 한 줄)
  await p.click('#pasteBox summary')
  await p.fill('#pasteText', Array.from({ length: 30 }, (_, i) => cyc[i % 4]).join('\t'))
  await p.click('#pasteBtn')
  await wait(200)
  const n3 = await p.$$eval('#pvNotes li', (ls) => ls.map((l) => l.textContent).join(' / '))
  check('붙여 넣기: 30일 모두 + 덮어쓰기 안내', (await txt('#pvSummary')).includes('일근 8') && n3.includes('저장된 근무표가 있어요') && (await txt('#pvSave')) === '바꿔서 저장', (await txt('#pvSummary')) + ' | ' + n3)
  await p.click('#pvCancel')
  // 7. 엑셀 파일은 안내만
  await p.setInputFiles('#csvFile', f3)
  await wait(200)
  check('xlsx는 CSV로 저장하라는 안내', (await txt('#toast')).includes('CSV'), await txt('#toast'))
  // 8. 못 읽는 파일
  await p.fill('#pasteText', '안녕하세요\n이건 근무표가 아니에요')
  await p.click('#pasteBtn')
  await wait(100)
  check('못 읽으면 안내', (await txt('#pvNotes')).includes('근무를 찾지 못했어요') && (await p.isDisabled('#pvSave')))
  await p.click('#pvCancel')
  // 9. 저장 목록·지우기
  check('저장 목록 2달', (await p.$$eval('#savedList .saved-row', (r) => r.length)) === 2)
  await p.click('[data-del="2026-12"]')
  await wait(100)
  check('지우기', (await p.$$eval('#savedList .saved-row', (r) => r.length)) === 1)
  // 10. 화면 어둡게, 다시 열어도 유지
  await p.click('#themeSeg button[data-v="dark"]')
  await p.reload()
  await wait(300)
  check('어두운 화면 유지', (await p.evaluate(() => document.documentElement.dataset.theme)) === 'dark')
  check('다시 열어도 근무표 유지', (await txt('#heroName')) === '일근')
  await shot('home-dark')
  await p.click('#tabbar button[data-tab="sheet"]')
  await shot('sheet-dark')
  await p.click('#tabbar button[data-tab="settings"]')
  await shot('settings-dark')
  // 11. 가로 넘침 없음
  for (const t of ['home', 'sheet', 'settings']) {
    await p.click(`#tabbar button[data-tab="${t}"]`)
    check(`가로 넘침 없음(${t})`, await p.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
  }
  check('페이지 오류 없음', !errors.length, errors.join(' | '))
  await b.close()
  server.kill()
  fs.rmSync(tmp, { recursive: true, force: true })
  console.log(results.filter(Boolean).length + ' / ' + results.length)
  process.exit(results.every(Boolean) ? 0 : 1)
})().catch((e) => { console.error(e); process.exit(1) })
