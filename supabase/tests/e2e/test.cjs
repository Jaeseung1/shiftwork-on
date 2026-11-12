// 웹 앱 + sync.js + 가짜 supabase-js + 로컬 PostgreSQL(실제 RLS) 끝에서 끝까지 시험. run.sh로 실행.
const { chromium } = require('playwright')
const { execFileSync } = require('child_process')
const sql = (q) => execFileSync('psql', ['-h', process.env.PGHOST, '-p', process.env.PGPORT || '5432', '-U', 'postgres', '-d', 'e2e', '-At', '-c', q], { encoding: 'utf8' }).trim()
const results = []
const check = (name, ok, detail = '') => { const r = [ok ? 'PASS' : 'FAIL', name, String(detail).slice(0, 80)]; results.push(r); console.log(r.join(' | ')) }
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
async function ctx(b, bridge) {
  const c = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'Asia/Seoul', locale: 'ko-KR' })
  await c.addInitScript((bridge) => {
    if (!sessionStorage.getItem('init')) { localStorage.setItem('shift-on-mobile-ui-v1', JSON.stringify({ anchor: '2026-11-12', anchorShift: '일', overrides: {} })); sessionStorage.setItem('init', '1') }
    if (bridge) window.ShiftBridge = { calls: [], setSchedule(j) { this.calls.push(['schedule', j]) }, setRemote(j) { this.calls.push(['remote', j]) } }
  }, bridge)
  const p = await c.newPage()
  p.errors = []
  p.on('pageerror', (e) => p.errors.push(e.message))
  await p.clock.install({ time: new Date('2026-11-12T13:00:00+09:00') })
  await p.clock.resume()
  return p
}
const txt = (p, s) => p.evaluate((s) => document.querySelector(s)?.textContent, s)
async function login(p, email) {
  await p.goto('http://127.0.0.1:' + (process.env.APP_PORT || 8765) + '/'); await wait(300)
  await p.click('.nav button[data-screen="settings"]')
  await p.fill('#acctEmail', email); await p.fill('#acctPass', 'pw1234')
  await p.click('#acctSignup'); await wait(300)
  p.signupState = await p.evaluate(() => document.querySelector('#acctState').textContent)
  await p.click('#acctSend'); await wait(300)
  p.unconfirmedToast = await p.evaluate(() => document.querySelector('#toast').textContent)
  // 메일의 확인 링크를 누른 것으로 처리
  await p.evaluate((email) => { const u = JSON.parse(localStorage.getItem('fake-users')); u[email].confirmed = true; localStorage.setItem('fake-users', JSON.stringify(u)) }, email)
  await p.fill('#acctPass', 'wrong1'); await p.click('#acctSend'); await wait(300)
  p.wrongToast = await p.evaluate(() => document.querySelector('#toast').textContent)
  await p.fill('#acctPass', 'pw1234'); await p.click('#acctSend'); await wait(900)
}
;(async () => {
  const b = await chromium.launch()
  // 1. A 로그인 → 서버에 근무표 생성
  const A = await ctx(b)
  await login(A, 'a@test.kr')
  check('가입하면 확인 메일 안내', A.signupState.includes('가입 확인 메일'), A.signupState)
  check('확인 전 로그인은 안내', A.unconfirmedToast.includes('확인 링크'), A.unconfirmedToast)
  check('틀린 비밀번호 거절', A.wrongToast.includes('맞지 않'), A.wrongToast)
  check('A 로그인 상태 문구', (await txt(A, '#acctState')).includes('내 근무표가 서버'), await txt(A, '#acctState'))
  check('A 근무표 서버 생성', sql("select count(*) from schedules") === '1', sql("select anchor_date||' '||anchor_shift from schedules"))
  // 2. A가 15일을 야근으로
  await A.click('.nav button[data-screen="calendar"]'); await A.click('.calday[data-date="2026-11-15"]'); await A.click('#seg button[data-v="야"]'); await A.click('#applyShift'); await wait(900)
  check('A 15일 야근 서버 저장', sql("select shift from overrides where day='2026-11-15'") === '야', sql("select day||shift from overrides"))
  // 3. 초대 코드
  await A.click('.nav button[data-screen="family"]'); await A.click('#newInvite'); await wait(600)
  const code = await txt(A, '#inviteCode')
  check('A 초대 코드 생성', /^[A-HJ-NP-Z2-9]{6}$/.test(code) && sql(`select count(*) from invites where code='${code}'`) === '1', code)
  await A.click('#copyCode'); await wait(300)
  check('서버 모드 초대 창에 시제품 안내 없음', await A.evaluate(() => document.querySelector('#modal .demo').hidden))
  await A.click('#closeModal')
  // 3-1. 아이폰 단축어 주소
  await A.context().grantPermissions(['clipboard-read', 'clipboard-write'], { origin: 'http://127.0.0.1:' + (process.env.APP_PORT || 8765) })
  await A.click('.nav button[data-screen="settings"]'); await A.click('#iosCopy'); await wait(300)
  const scUrl = new URL(await A.evaluate(() => navigator.clipboard.readText()))
  const aToken = sql("select widget_token from schedules where owner_id='aaaaaaaa-0000-0000-0000-00000000000a'")
  check('단축어 주소(함수·토큰·시간대·키)', scUrl.origin + scUrl.pathname === 'http://fake.local/rest/v1/rpc/share_summary_json' && scUrl.searchParams.get('p_token') === aToken && scUrl.searchParams.get('p_tz') === 'Asia/Seoul' && scUrl.searchParams.get('apikey') === 'anon', scUrl.href.replace(aToken, '<token>'))
  check('단축어 주소 복사 안내', (await txt(A, '#toast')).includes('복사'), await txt(A, '#toast'))
  check('단축어 주소로 오늘·내일 JSON', (() => { const j = JSON.parse(sql(`select public.share_summary_json('${aToken}', 'Asia/Seoul')`)); return j.today.startsWith('오늘 ') && j.tomorrow.startsWith('내일 ') })())
  // 4. 가족 B: 메일 로그인 없이 코드만으로 참여(익명)
  const B = await ctx(b, true)
  await B.goto('http://127.0.0.1:' + (process.env.APP_PORT || 8765) + '/'); await wait(400)
  await B.click('.nav button[data-screen="family"]'); await B.fill('#joinCode', code.toLowerCase()); await B.click('#joinBtn'); await wait(1200)
  check('가족은 자기 근무표를 만들지 않음', sql("select count(*) from schedules") === '1', sql("select count(*) from schedules"))
  check('B 참여 후 홈으로', (await B.evaluate(() => document.querySelector('.screen.active').id)) === 'screen-home')
  check('B 읽기 전용 표시', await B.evaluate(() => document.documentElement.hasAttribute('data-readonly')))
  check('B 계정 문구(코드로 참여)', (await txt(B, '#acctState')).includes('초대 코드로 공유받은 근무표'), await txt(B, '#acctState'))
  await B.click('.nav button[data-screen="calendar"]')
  check('B에게 A의 15일 야근 보임', (await txt(B, '.calday[data-date="2026-11-15"] .tag')) === '야근', await txt(B, '.calday[data-date="2026-11-15"] .tag'))
  await B.click('.calday[data-date="2026-11-16"]'); await wait(200)
  check('B 편집 칸 숨김', await B.evaluate(() => getComputedStyle(document.querySelector('#editor')).display === 'none'))
  await B.evaluate(() => document.querySelector('#applyShift').click()); await wait(200)
  check('B 저장 시도 막힘(토스트)', (await txt(B, '#toast')).includes('볼 수만'), await txt(B, '#toast'))
  check('B 시도 후 서버 변화 없음', sql("select count(*) from overrides") === '1')
  const remote = await B.evaluate(() => window.ShiftBridge.calls.filter((c) => c[0] === 'remote').map((c) => c[1]).filter(Boolean).at(-1))
  const token = sql("select widget_token from schedules where owner_id='aaaaaaaa-0000-0000-0000-00000000000a'")
  check('위젯 브리지에 A의 토큰 전달', !!remote && JSON.parse(remote).token === token, remote)
  const sched = await B.evaluate(() => window.ShiftBridge.calls.filter((c) => c[0] === 'schedule').at(-1)[1])
  check('위젯 브리지에 근무표 전달', JSON.parse(sched).overrides.some((o) => o.day === '2026-11-15' && o.shift === '야'), sched)
  check('안드로이드 앱 안에서는 아이폰 칸 숨김', await B.evaluate(() => document.querySelector('#iosPanel').hidden))
  // 5. A 가족 목록
  await A.reload(); await wait(800); await A.click('.nav button[data-screen="family"]'); await wait(400)
  check('A 공유받는 사람 1명 표시', (await txt(A, '.member-extra')) ?.includes('공유받는 사람 1명'), await txt(A, '.member-extra'))
  // 6. A가 15일 초기화 → B 새로고침
  await A.click('.nav button[data-screen="calendar"]'); await A.click('.calday[data-date="2026-11-15"]'); await A.click('#resetShift'); await wait(900)
  check('A 초기화 서버 삭제', sql("select count(*) from overrides") === '0')
  await B.evaluate(() => { Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true }); document.dispatchEvent(new Event('visibilitychange')) }); await wait(900)
  check('B 새로고침 후 15일 휴무', (await txt(B, '.calday[data-date="2026-11-15"] .tag')) === '휴무', await txt(B, '.calday[data-date="2026-11-15"] .tag'))
  // 7. B 나가기
  await B.click('.nav button[data-screen="family"]'); await B.click('#leaveBtn'); await wait(900)
  check('B 나간 뒤 읽기 전용 해제', !(await B.evaluate(() => document.documentElement.hasAttribute('data-readonly'))))
  check('B 나간 뒤 members 0', sql("select count(*) from members") === '0')
  // 8. 서버 없는 모드
  const L = await ctx(b)
  await L.goto('http://127.0.0.1:' + (process.env.LOCAL_PORT || 8766) + '/'); await wait(300)
  check('로컬 모드 로그인 칸 숨김', await L.evaluate(() => getComputedStyle(document.querySelector('#acctLogin')).display === 'none'))
  check('로컬 모드 계정 문구', (await txt(L, '#acctState')).includes('이 기기에만'), await txt(L, '#acctState'))
  check('로컬 모드 단축어 버튼 꺼짐', await L.evaluate(() => document.querySelector('#iosCopy').disabled && !document.querySelector('#iosPanel').hidden))
  check('로컬 모드 단축어 안내', (await txt(L, '#iosState')).includes('로그인하거나'), await txt(L, '#iosState'))
  await L.click('.nav button[data-screen="family"]'); await L.fill('#joinCode', 'ABC234'); await L.click('#joinBtn'); await wait(200)
  check('로컬 모드 참여 안내', (await txt(L, '#toast')).includes('서버'), await txt(L, '#toast'))
  await L.click('.nav button[data-screen="calendar"]'); await L.click('.calday[data-date="2026-11-20"]'); await L.click('#seg button[data-v="휴"]'); await L.click('#applyShift'); await wait(200)
  check('로컬 모드 저장은 기기에', (await L.evaluate(() => JSON.parse(localStorage.getItem('shift-on-mobile-ui-v1')).overrides['2026-11-20'])) === '휴')
  check('페이지 오류 없음', [A, B, L].every((p) => !p.errors.length), [A, B, L].flatMap((p) => p.errors).join(' | '))
  await b.close()
  console.log(results.filter((r) => r[0] === 'PASS').length + ' / ' + results.length)
  process.exit(results.every((r) => r[0] === 'PASS') ? 0 : 1)
})()
