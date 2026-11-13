// ios/widget.js(Scriptable 위젯)를 Scriptable 없이 Node에서 돌려 보는 시험. 사용: node ios/test.mjs
// Scriptable의 ListWidget·Request·FileManager 등을 흉내 내고, 위젯에 들어간 글을 순서대로 모아 기대값과 비교한다.
// 기대값은 웹 앱·갤럭시 위젯 시험과 같은 근무표(2026-11-12 일근 기준, 11/15만 야근으로 바꿈).
import { readFileSync } from 'node:fs'

const SRC = readFileSync(new URL('./widget.js', import.meta.url), 'utf8')
const SCHEDULE = { anchor_date: '2026-11-12', anchor_shift: '일', overrides: [{ day: '2026-11-15', shift: '야' }] }
const CFG = { url: 'https://example.supabase.co', key: 'sb_publishable_test', token: '11111111-2222-3333-4444-555555555555', app: 'https://example.vercel.app/' }

class Color { constructor(hex) { this.hex = hex } static dynamic(a, b) { return { light: a, dark: b } } }
const Font = new Proxy({}, { get: (_, name) => (size) => ({ name, size }) })
class Size { constructor(w, h) { this.w = w; this.h = h } }
class Text { constructor(t) { this.kind = 'text'; this.text = t } centerAlignText() { this.center = true } }
class Stack {
  constructor() { this.kind = 'stack'; this.items = [] }
  addText(t) { const x = new Text(t); this.items.push(x); return x }
  addStack() { const x = new Stack(); this.items.push(x); return x }
  addSpacer(n) { this.items.push({ kind: 'spacer', n }) }
  layoutVertically() {} layoutHorizontally() {} centerAlignContent() {} setPadding() {}
}
class ListWidget extends Stack { async presentMedium() { this.presented = true } }

function run({ now, family, cfg = CFG, net = 'ok', files = new Map(), zone = 'Asia/Seoul' }) {
  process.env.TZ = zone
  const NOW = new Date(now).getTime()
  const RealDate = Date
  class FakeDate extends RealDate {
    constructor(...a) { if (a.length) super(...a); else super(NOW) }
    static now() { return NOW }
  }
  const requests = []
  class Request {
    constructor(url) { this.url = url; requests.push(url) }
    async loadJSON() { if (net !== 'ok') throw new Error('offline'); return JSON.parse(JSON.stringify(SCHEDULE)) }
  }
  const FileManager = { local: () => ({
    documentsDirectory: () => '/docs', joinPath: (a, b) => a + '/' + b,
    writeString: (p, s) => files.set(p, s), readString: (p) => files.get(p), fileExists: (p) => files.has(p),
  }) }
  let shown = null
  const Script = { setWidget: (w) => { shown = w }, complete: () => {} }
  const config = { widgetFamily: family, runsInWidget: !family.startsWith('accessory'), runsInAccessoryWidget: family.startsWith('accessory') }
  const src = SRC.replace(/^const CONFIG = .*$/m, 'const CONFIG = ' + JSON.stringify(cfg))
  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor
  const fn = new AsyncFunction('Color', 'Font', 'Size', 'ListWidget', 'FileManager', 'Request', 'config', 'Script', 'Date', src)
  return fn(Color, Font, Size, ListWidget, FileManager, Request, config, Script, FakeDate).then(() => {
    const texts = []
    const walk = (n) => n.items.forEach((x) => (x.kind === 'text' ? texts.push(x.text) : x.kind === 'stack' && walk(x)))
    walk(shown)
    return { texts, widget: shown, requests, files }
  })
}

const results = []
const eq = (name, want, got) => {
  const ok = JSON.stringify(want) === JSON.stringify(got)
  results.push(ok)
  console.log((ok ? 'PASS' : 'FAIL') + ' | ' + name + (ok ? '' : '\n   want ' + JSON.stringify(want) + '\n   got  ' + JSON.stringify(got)))
}
const kst = (s) => s + ':00+09:00'

const R = 'accessoryRectangular'
eq('잠금 화면 · 일근 중', ['일근 · 근무 중', '17:30 퇴근 · 4시간 30분', '내일 야근 · 17:30'], (await run({ now: kst('2026-11-12T13:00'), family: R })).texts)
eq('잠금 화면 · 야근 전(내일 조근은 끝나는 시각)', ['야근 · 근무 전', '17:30 출근', '내일 조근 · 08:30 퇴근'], (await run({ now: kst('2026-11-13T12:00'), family: R })).texts)
eq('잠금 화면 · 야근 중(자정 전)', ['야근 · 근무 중', '08:30 퇴근 · 9시간 30분', '내일 조근 · 08:30 퇴근'], (await run({ now: kst('2026-11-13T23:00'), family: R })).texts)
eq('잠금 화면 · 한국 자정 직후(날짜는 한국 기준)', ['조근 · 근무 중', '08:30 퇴근 · 8시간', '내일 야근 · 17:30'], (await run({ now: kst('2026-11-14T00:30'), family: R })).texts)
eq('잠금 화면 · 조근 날 새벽(야근 이어짐)', ['조근 · 근무 중', '08:30 퇴근 · 1시간 30분', '내일 야근 · 17:30'], (await run({ now: kst('2026-11-14T07:00'), family: R })).texts)
eq('잠금 화면 · 퇴근 후(내일 바꾼 근무)', ['조근 · 퇴근 후', '내일 야근 · 17:30'], (await run({ now: kst('2026-11-14T10:00'), family: R })).texts)
eq('잠금 화면 · 휴무', ['오늘 휴무', '내일 일근 · 08:30'], (await run({ now: kst('2026-11-19T12:00'), family: R })).texts)
eq('잠금 화면 · 토론토 시간대', ['일근 · 근무 중', '03:30 퇴근 · 4시간 30분', '내일 야근 · 03:30'], (await run({ now: kst('2026-11-12T13:00'), family: R, zone: 'America/Toronto' })).texts)
eq('시계 위 한 줄 · 근무 중', ['일근 · 17:30 퇴근'], (await run({ now: kst('2026-11-12T13:00'), family: 'accessoryInline' })).texts)
eq('시계 위 한 줄 · 퇴근 후', ['조근 · 내일 야근'], (await run({ now: kst('2026-11-14T10:00'), family: 'accessoryInline' })).texts)
eq('동그라미', ['일근', '17:30'], (await run({ now: kst('2026-11-12T13:00'), family: 'accessoryCircular' })).texts)
eq('홈 작은 위젯', ['오늘 · 12일 목', '일근', '근무 중', '17:30 퇴근', '내일 야근 · 17:30'], (await run({ now: kst('2026-11-12T13:00'), family: 'small' })).texts)
eq('홈 중간 위젯', ['오늘 · 12일 목', '일근', '근무 중', '08:30–17:30', '4시간 30분 남음', '내일 · 13일 금', '야근', '17:30–08:30'], (await run({ now: kst('2026-11-12T13:00'), family: 'medium' })).texts)
eq('홈 중간 위젯 · 내일 조근', ['오늘 · 13일 금', '야근', '근무 중', '17:30–08:30', '9시간 30분 남음', '내일 · 14일 토', '조근', '08:30 퇴근'], (await run({ now: kst('2026-11-13T23:00'), family: 'medium' })).texts)

const first = await run({ now: kst('2026-11-12T13:00'), family: R })
const u = new URL(first.requests[0])
eq('서버 주소(widget_schedule·토큰·키)', [CFG.url + '/rest/v1/rpc/widget_schedule', CFG.token, CFG.key], [u.origin + u.pathname, u.searchParams.get('p_token'), u.searchParams.get('apikey')])
eq('다음 새로 그릴 때(30분 뒤)', new Date(kst('2026-11-12T13:30')).toISOString(), first.widget.refreshAfterDate.toISOString())
eq('다음 새로 그릴 때(퇴근 시각)', new Date(kst('2026-11-12T17:30')).toISOString(), (await run({ now: kst('2026-11-12T17:20'), family: R })).widget.refreshAfterDate.toISOString())
eq('누르면 웹 앱 열기', CFG.app, first.widget.url)
eq('인터넷 없으면 마지막 근무표로', first.texts, (await run({ now: kst('2026-11-12T13:00'), family: R, net: 'off', files: first.files })).texts)
eq('인터넷도 저장본도 없으면 안내', ['근무표를 받지 못했어요. 인터넷 연결을 확인해 주세요'], (await run({ now: kst('2026-11-12T13:00'), family: R, net: 'off' })).texts)
eq('코드에 설정이 없으면 안내', ['교대온 앱 설정 탭에서 "위젯 코드 복사"로 코드를 다시 받아 주세요'], (await run({ now: kst('2026-11-12T13:00'), family: 'small', cfg: { url: '', key: '', token: '', app: '' } })).texts)

console.log(results.filter(Boolean).length + ' / ' + results.length)
process.exit(results.every(Boolean) ? 0 : 1)
