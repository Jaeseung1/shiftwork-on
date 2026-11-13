// 교대온 위젯 (아이폰 Scriptable 앱용: 잠금 화면·홈 화면)
// 교대온 앱 설정 탭 > "아이폰 잠금 화면 위젯" > "위젯 코드 복사"로 아래 CONFIG가 채워진 코드를 받아 Scriptable에 붙여 넣는다.
// 서버에서는 근무표 기준(기준일·순환·바꾼 근무)만 받고, 오늘·내일·근무 중 여부는 이 폰에서 계산한다.
// 규칙은 웹 앱(index.html)·갤럭시 위젯(ShiftCalc.java)과 같다: 일 → 야 → 조 → 휴, 날짜는 한국 기준, 시각은 이 폰의 시간대.
const CONFIG = {"url":"","key":"","token":"","app":""}

const CYCLE = ['일', '야', '조', '휴']
const NAMES = { 일: '일근', 야: '야근', 조: '조근', 휴: '휴무' }
const WEEK = ['일', '월', '화', '수', '목', '금', '토']
const KST_MS = 9 * 3600 * 1000

const pad = (n) => String(n).padStart(2, '0')
const mod = (n, m) => ((n % m) + m) % m
const kstDate = (d) => new Date(d.getTime() + KST_MS).toISOString().slice(0, 10)
function addDays(k, n) {
  const [y, m, d] = k.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + n)).toISOString().slice(0, 10)
}
function diffDays(a, b) {
  const [ay, am, ad] = a.split('-').map(Number)
  const [by, bm, bd] = b.split('-').map(Number)
  return Math.round((Date.UTC(ay, am - 1, ad) - Date.UTC(by, bm - 1, bd)) / 86400000)
}
function kstAt(k, hm) {
  const [y, m, d] = k.split('-').map(Number)
  const [h, mi] = hm.split(':').map(Number)
  return new Date(Date.UTC(y, m - 1, d, h - 9, mi))
}
// 이 폰 시간대의 시각
const hm = (d) => pad(d.getHours()) + ':' + pad(d.getMinutes())
const weekOf = (k) => WEEK[new Date(k + 'T00:00:00Z').getUTCDay()]

function shiftOn(s, k) {
  const o = (s.overrides || []).find((x) => x.day === k)
  if (o) return o.shift
  return CYCLE[mod(CYCLE.indexOf(s.anchor_shift) + diffDays(k, s.anchor_date), 4)]
}
// 야근은 17:30부터 다음 날(조근 날) 08:30까지 이어지는 한 근무
function instants(k, sh) {
  if (sh === '일') return [kstAt(k, '08:30'), kstAt(k, '17:30')]
  if (sh === '야') return [kstAt(k, '17:30'), kstAt(addDays(k, 1), '08:30')]
  if (sh === '조') return [kstAt(addDays(k, -1), '17:30'), kstAt(k, '08:30')]
  return null
}

/** 지금 상태: 오늘·내일 근무, 상태(근무 중·근무 전·퇴근 후·쉬는 날), 짧은 시각(17:30 퇴근 등), 남은 시간 */
function state(s, now) {
  const today = kstDate(now)
  const tom = addDays(today, 1)
  const t = shiftOn(s, today)
  const n = shiftOn(s, tom)
  const p = instants(today, t)
  const np = instants(tom, n)
  let live, short = '', remaining = '', working = false
  if (!p) live = '쉬는 날'
  else if (now < p[0]) {
    live = '근무 전'
    short = hm(p[0]) + ' 출근'
  } else if (now < p[1]) {
    working = true
    live = '근무 중'
    short = hm(p[1]) + ' 퇴근'
    const left = Math.max(0, Math.round((p[1] - now) / 60000))
    const h = Math.floor(left / 60), m = left % 60
    remaining = (h ? h + '시간 ' : '') + (m || !h ? m + '분 ' : '') + '남음'
  } else live = '퇴근 후'
  // 내일이 조근이면 오늘 밤 야근이 이어지는 것이라 끝나는 시각만
  const nextTime = !np ? '' : n === '조' ? hm(np[1]) + ' 퇴근' : hm(np[0])
  // 다음에 화면이 바뀌어야 하는 때(출근·퇴근·한국 자정). iOS가 정확히 지키지는 않는다
  const marks = [p && p[0], p && p[1], kstAt(tom, '00:00')].filter((x) => x && x > now)
  return {
    today, tom, t, n, live, short, remaining, working, nextTime,
    range: p ? hm(p[0]) + '–' + hm(p[1]) : '',
    nextRange: np ? hm(np[0]) + '–' + hm(np[1]) : '',
    refresh: new Date(Math.min(now.getTime() + 30 * 60000, ...marks.map((x) => x.getTime()))),
  }
}

/** 잠금 화면 직사각형 위젯의 줄들 */
function lockLines(st) {
  const next = nextLine(st)
  if (st.t === '휴') return ['오늘 휴무', next]
  if (st.live === '퇴근 후') return [`${NAMES[st.t]} · 퇴근 후`, next]
  // 잠금 화면 직사각형은 폭이 좁아(약 160pt) "남음"은 뺀다
  return [`${NAMES[st.t]} · ${st.live}`, st.working ? `${st.short} · ${st.remaining.replace(' 남음', '')}` : st.short, next]
}

/** 잠금 화면 시계 위 한 줄 */
function inlineLine(st) {
  return st.short ? `${NAMES[st.t]} · ${st.short}` : `${NAMES[st.t]} · 내일 ${NAMES[st.n]}`
}

async function loadSchedule() {
  const fm = FileManager.local()
  const path = fm.joinPath(fm.documentsDirectory(), 'shiftwork-cache.json')
  if (CONFIG.url && CONFIG.token) {
    try {
      const r = new Request(`${CONFIG.url}/rest/v1/rpc/widget_schedule?p_token=${encodeURIComponent(CONFIG.token)}&apikey=${encodeURIComponent(CONFIG.key)}`)
      r.timeoutInterval = 10
      const j = await r.loadJSON()
      if (j && j.anchor_date) {
        fm.writeString(path, JSON.stringify(j))
        return j
      }
    } catch (e) {
      // 인터넷이 없으면 마지막으로 받은 근무표로 계산한다
    }
  }
  return fm.fileExists(path) ? JSON.parse(fm.readString(path)) : null
}

// ---- 그리기 ----
const C = {
  bg: Color.dynamic(new Color('#FFFFFF'), new Color('#1A1D24')),
  inset: Color.dynamic(new Color('#F2F4F6'), new Color('#232730')),
  ink: Color.dynamic(new Color('#191F28'), new Color('#F2F4F6')),
  body: Color.dynamic(new Color('#4E5968'), new Color('#B0B8C1')),
  sub: new Color('#8B95A1'),
  blue: Color.dynamic(new Color('#3182F6'), new Color('#4795FF')),
  blueSoft: Color.dynamic(new Color('#E8F3FF'), new Color('#1E2B40')),
}

function text(parent, str, font, color, opts = {}) {
  const t = parent.addText(str)
  t.font = font
  if (color) t.textColor = color
  t.lineLimit = 1
  t.minimumScaleFactor = opts.scale || 0.75
  if (opts.opacity) t.textOpacity = opts.opacity
  return t
}

function chip(parent, st) {
  const c = parent.addStack()
  c.setPadding(2, 7, 2, 7)
  c.cornerRadius = 9
  c.backgroundColor = st.working ? C.blueSoft : C.inset
  text(c, st.live, Font.boldSystemFont(11), st.working ? C.blue : C.body)
}

function nextLine(st) {
  return '내일 ' + NAMES[st.n] + (st.nextTime ? ' · ' + st.nextTime : '')
}

function build(family, st) {
  const w = new ListWidget()
  w.refreshAfterDate = st.refresh
  if (CONFIG.app) w.url = CONFIG.app

  // 잠금 화면: 글자색은 iOS가 정한다(한 가지 색)
  if (family === 'accessoryInline') {
    w.addText(inlineLine(st))
    return w
  }
  if (family === 'accessoryCircular') {
    w.addAccessoryWidgetBackground = true
    w.addSpacer()
    text(w, NAMES[st.t], Font.boldSystemFont(15), null, { scale: 0.6 }).centerAlignText()
    if (st.short) text(w, st.short.split(' ')[0], Font.systemFont(11), null, { scale: 0.6 }).centerAlignText()
    w.addSpacer()
    return w
  }
  if (family === 'accessoryRectangular') {
    lockLines(st).forEach((line, i) => text(w, line, i === 0 ? Font.boldSystemFont(15) : Font.systemFont(12), null, { opacity: i === 2 ? 0.7 : 0 }))
    return w
  }

  // 홈 화면(토스 스타일, 라이트·다크)
  w.backgroundColor = C.bg
  w.setPadding(14, 16, 14, 14)
  if (family === 'small') {
    text(w, `오늘 · ${Number(st.today.slice(-2))}일 ${weekOf(st.today)}`, Font.boldSystemFont(12), C.sub)
    w.addSpacer(4)
    text(w, NAMES[st.t], Font.boldSystemFont(28), C.ink)
    w.addSpacer(6)
    chip(w, st)
    w.addSpacer(6)
    if (st.t !== '휴') text(w, st.short || st.range, Font.systemFont(13), C.body)
    w.addSpacer()
    text(w, nextLine(st), Font.systemFont(12), C.sub)
    return w
  }
  // medium·large: 왼쪽 오늘, 오른쪽 회색 카드에 내일
  const row = w.addStack()
  row.centerAlignContent()
  const left = row.addStack()
  left.layoutVertically()
  text(left, `오늘 · ${Number(st.today.slice(-2))}일 ${weekOf(st.today)}`, Font.boldSystemFont(12), C.sub)
  left.addSpacer(4)
  const nameRow = left.addStack()
  nameRow.centerAlignContent()
  text(nameRow, NAMES[st.t], Font.boldSystemFont(28), C.ink)
  nameRow.addSpacer(8)
  chip(nameRow, st)
  left.addSpacer(4)
  if (st.t !== '휴') text(left, st.range, Font.systemFont(13), C.body)
  if (st.working) {
    left.addSpacer(2)
    text(left, st.remaining, Font.semiboldSystemFont(13), C.blue)
  }
  row.addSpacer()
  const card = row.addStack()
  card.layoutVertically()
  card.backgroundColor = C.inset
  card.cornerRadius = 16
  card.setPadding(12, 14, 12, 14)
  card.size = new Size(124, 0)
  text(card, `내일 · ${Number(st.tom.slice(-2))}일 ${weekOf(st.tom)}`, Font.boldSystemFont(12), C.sub)
  card.addSpacer(4)
  text(card, NAMES[st.n], Font.boldSystemFont(20), C.ink)
  if (st.nextTime) {
    card.addSpacer(4)
    text(card, st.n === '조' ? st.nextTime : st.nextRange, Font.systemFont(13), C.body)
  }
  return w
}

function message(str) {
  const w = new ListWidget()
  if (!config.runsInAccessoryWidget) w.backgroundColor = C.bg
  text(w, str, Font.systemFont(13), config.runsInAccessoryWidget ? null : C.body).lineLimit = 3
  return w
}

async function main() {
  const family = config.widgetFamily || 'medium'
  let w
  if (!CONFIG.url || !CONFIG.token) {
    w = message('교대온 앱 설정 탭에서 "위젯 코드 복사"로 코드를 다시 받아 주세요')
  } else {
    const s = await loadSchedule()
    w = s ? build(family, state(s, new Date())) : message('근무표를 받지 못했어요. 인터넷 연결을 확인해 주세요')
  }
  if (config.runsInWidget || config.runsInAccessoryWidget) Script.setWidget(w)
  else await w.presentMedium()
  Script.complete()
}

await main()
