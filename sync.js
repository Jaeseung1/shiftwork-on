// 교대온 서버 연결(Supabase)과 안드로이드 위젯 전달.
// - config.js가 비어 있으면 서버 없이 지금처럼 이 기기에만 저장한다.
// - 근무표 주인은 메일 코드로 로그인해 근무표를 서버에 저장한다.
// - 공유받는 사람은 메일 없이 초대 코드만 넣는다(익명 로그인 후 참여, 읽기만). Supabase 기본 메일은 팀원 주소로만 가기 때문.
// - 안드로이드 앱(WebView) 안이면 근무표와 위젯 토큰을 ShiftBridge로 넘겨 홈 화면 위젯이 쓰게 한다.
const cfg = window.SHIFTWORK_CONFIG || {}
const app = window.shiftApp
const $ = (s) => document.querySelector(s)
const SUPABASE_MODULE = 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2.118.0/+esm'
const CODE_RE = /^[A-HJ-NP-Z2-9]{6}$/

const st = { client: null, user: null, schedule: null, role: null, synced: null, applying: false, timer: null, loading: null, widgetTemplate: null }

if (app) init()

async function init() {
  app.onChange = () => {
    sendToWidget()
    schedulePush()
  }
  sendToWidget()
  bindUI()
  if (!cfg.supabaseUrl || !cfg.supabaseAnonKey) {
    renderAccount()
    return
  }
  let createClient
  try {
    ;({ createClient } = await import(cfg.supabaseModule || SUPABASE_MODULE))
  } catch (e) {
    console.warn('[교대온] Supabase를 불러오지 못함', e)
    $('#acctState').textContent = '서버에 연결하지 못했어요. 이 기기에 저장된 근무표를 보여 줘요.'
    $('#acctLogin').hidden = true
    return
  }
  st.client = createClient(cfg.supabaseUrl, cfg.supabaseAnonKey, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true },
  })
  st.client.auth.onAuthStateChange((_event, session) => {
    // 콜백 안에서 바로 다른 Supabase 호출을 기다리지 않도록 다음 틱으로 넘긴다
    setTimeout(() => onSession(session), 0)
  })
  const { data } = await st.client.auth.getSession()
  await onSession(data.session)
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') refresh()
  })
  setInterval(refresh, 5 * 60 * 1000)
}

async function onSession(session) {
  const user = session?.user || null
  if ((user?.id || null) === (st.user?.id || null) && (st.schedule || !user)) {
    renderAccount()
    return
  }
  st.user = user
  if (!user) {
    st.schedule = null
    st.role = null
    st.synced = null
    setReadOnly(false)
    renderAccount()
    renderFamily()
    sendToWidget()
    return
  }
  await load()
}

function refresh() {
  if (st.user && st.client) load()
}

// 서버에서 근무표를 고른다: 초대 코드로 참여한 근무표가 있으면 그것(읽기 전용), 없으면 내 근무표(없으면 지금 기기 데이터로 만든다)
function load() {
  if (st.loading) return st.loading
  st.loading = (async () => {
    const c = st.client
    const uid = st.user.id
    let sched = null
    let role = 'owner'
    const mem = await c.from('members').select('schedule_id').eq('user_id', uid)
    if (mem.error) return fail(mem.error, '근무표를 불러오지 못했어요')
    if (mem.data.length) {
      const r = await c.from('schedules').select('*').eq('id', mem.data[0].schedule_id).maybeSingle()
      if (r.error) return fail(r.error, '근무표를 불러오지 못했어요')
      if (r.data) {
        sched = r.data
        role = 'viewer'
      }
    }
    if (!sched && st.user.is_anonymous) {
      // 코드로 들어온 사람인데 참여한 근무표가 없으면(나갔거나 근무표가 지워짐) 기기 근무표를 그대로 둔다
      st.schedule = null
      st.role = null
      setReadOnly(false)
      renderAccount()
      renderFamily()
      sendToWidget()
      return
    }
    if (!sched) {
      const r = await c.from('schedules').select('*').eq('owner_id', uid).maybeSingle()
      if (r.error) return fail(r.error, '근무표를 불러오지 못했어요')
      sched = r.data
      if (!sched) {
        const ins = await c
          .from('schedules')
          .insert({ anchor_date: app.data.anchor, anchor_shift: app.data.anchorShift })
          .select()
          .single()
        if (ins.error) return fail(ins.error, '근무표를 만들지 못했어요')
        sched = ins.data
        const rows = Object.entries(app.data.overrides || {}).map(([day, shift]) => ({ schedule_id: sched.id, day, shift }))
        if (rows.length) {
          const up = await c.from('overrides').upsert(rows)
          if (up.error) return fail(up.error, '바꾼 근무를 올리지 못했어요')
        }
      }
    }
    const ov = await c.from('overrides').select('day,shift').eq('schedule_id', sched.id)
    if (ov.error) return fail(ov.error, '근무표를 불러오지 못했어요')
    st.schedule = sched
    st.role = role
    apply(sched, ov.data)
    renderAccount()
    renderFamily()
    sendToWidget()
  })().finally(() => {
    st.loading = null
  })
  return st.loading
}

function apply(sched, rows) {
  st.applying = true
  app.data.anchor = sched.anchor_date
  app.data.anchorShift = sched.anchor_shift
  app.data.overrides = Object.fromEntries(rows.map((r) => [r.day, r.shift]))
  app.cache()
  app.render()
  st.synced = snapshot()
  setReadOnly(st.role === 'viewer')
  st.applying = false
}

function snapshot() {
  return { anchor: app.data.anchor, anchorShift: app.data.anchorShift, overrides: { ...(app.data.overrides || {}) } }
}

function setReadOnly(on) {
  app.readOnly = on
  document.documentElement.toggleAttribute('data-readonly', on)
}

function schedulePush() {
  if (st.applying || !st.schedule || st.role !== 'owner') return
  clearTimeout(st.timer)
  st.timer = setTimeout(push, 400)
}

// 바뀐 것만 올린다
async function push() {
  const c = st.client
  const sid = st.schedule.id
  const cur = snapshot()
  const prev = st.synced
  if (cur.anchor !== prev.anchor || cur.anchorShift !== prev.anchorShift) {
    const r = await c.from('schedules').update({ anchor_date: cur.anchor, anchor_shift: cur.anchorShift }).eq('id', sid)
    if (r.error) return fail(r.error, '서버에 저장하지 못했어요')
  }
  const up = []
  const del = []
  for (const [day, shift] of Object.entries(cur.overrides)) if (prev.overrides[day] !== shift) up.push({ schedule_id: sid, day, shift })
  for (const day of Object.keys(prev.overrides)) if (!(day in cur.overrides)) del.push(day)
  if (up.length) {
    const r = await c.from('overrides').upsert(up)
    if (r.error) return fail(r.error, '서버에 저장하지 못했어요')
  }
  if (del.length) {
    const r = await c.from('overrides').delete().eq('schedule_id', sid).in('day', del)
    if (r.error) return fail(r.error, '서버에 저장하지 못했어요')
  }
  st.synced = cur
}

function fail(error, message) {
  console.warn('[교대온]', message, error)
  app.toast(message)
}

// ---- 화면 ----

function bindUI() {
  // 근무표 주인은 이메일 + 비밀번호. (Supabase 무료 요금제는 SMTP 없이 메일 양식을 못 고쳐 메일 코드를 쓸 수 없고,
  // 메일 링크는 앱·홈 화면 웹 앱 밖의 브라우저로 열리기 때문.) 가입 확인 링크는 처음 한 번만 어느 브라우저에서든 누르면 된다.
  const creds = () => {
    const email = $('#acctEmail').value.trim()
    const password = $('#acctPass').value
    if (!st.client) return app.toast('서버가 아직 설정되지 않았어요')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return app.toast('이메일 주소를 확인해 주세요')
    if (password.length < 6) return app.toast('비밀번호는 6자 이상이에요')
    return { email, password }
  }
  // 처리하는 동안 버튼을 잠가 두 번 눌리지 않게 한다(두 번 누르면 메일 발송 횟수 제한에 걸림)
  const busy = (on) => ['#acctSend', '#acctSignup'].forEach((id) => { $(id).disabled = on })
  $('#acctSend').onclick = async () => {
    const c = creds()
    if (!c) return
    busy(true)
    const { error } = await st.client.auth.signInWithPassword(c).finally(() => busy(false))
    if (error) {
      if (/not confirmed/i.test(error.message || '')) return fail(error, '메일의 가입 확인 링크를 먼저 한 번 눌러 주세요')
      return fail(error, '이메일이나 비밀번호가 맞지 않아요. 처음이면 "처음이면 가입하기"를 눌러 주세요')
    }
    $('#acctPass').value = ''
    app.toast('로그인했어요')
  }
  $('#acctSignup').onclick = async () => {
    const c = creds()
    if (!c) return
    busy(true)
    const { data, error } = await st.client.auth.signUp({ ...c, options: { emailRedirectTo: location.origin + location.pathname } }).finally(() => busy(false))
    if (error) return fail(error, /not authorized/i.test(error.message || '') ? '이 주소로는 가입 메일을 보낼 수 없어요(Supabase 팀원 주소만 가능)' : error.status === 429 ? '방금 보냈어요. 1분쯤 뒤에 다시 해 주세요' : '가입하지 못했어요')
    if (data.session) {
      $('#acctPass').value = ''
      return app.toast('가입하고 로그인했어요')
    }
    // 확인 메일이 켜져 있으면 세션 없이 돌아온다. 이미 있는 주소면 identities가 비어 있다
    if (data.user && Array.isArray(data.user.identities) && data.user.identities.length === 0) return app.toast('이미 가입된 주소예요. 로그인을 눌러 주세요')
    $('#acctState').textContent = `${c.email}로 가입 확인 메일을 보냈어요. 메일의 링크를 한 번 누른 뒤 여기서 로그인해 주세요.`
    app.toast('가입 확인 메일을 보냈어요')
  }
  $('#acctOut').onclick = async () => {
    if (!st.client) return
    await st.client.auth.signOut()
    await onSession(null)
    app.toast('로그아웃했어요')
  }
  $('#iosCopy').onclick = copyShortcut
  // 갤럭시 앱 안이면 버전과 새 버전 확인. 앱(자바)은 인터넷에서 APK를 찾지 않고, 이 화면이 받는 곳 페이지만 브라우저로 연다.
  const bridge = window.ShiftBridge
  if (bridge && typeof bridge.appVersionCode === 'function') {
    $('#appPanel').hidden = false
    try { $('#appVer').textContent = bridge.appVersion() } catch (e) {}
    $('#appUpdate').onclick = () => checkUpdate(true)
    let last = 0
    try { last = Number(localStorage.getItem('shiftwork-update-check') || 0) } catch (e) {}
    if (Date.now() - last > 24 * 3600 * 1000) setTimeout(() => checkUpdate(false), 3000)
  }
  $('#joinBtn').onclick = async () => {
    const code = $('#joinCode').value.trim().toUpperCase()
    if (!CODE_RE.test(code)) return app.toast('코드 6자리를 확인해 주세요')
    if (!st.client) return app.toast('서버가 아직 설정되지 않았어요')
    if (!st.user) {
      // 공유받는 사람은 메일 없이 익명으로 들어온다
      const { data, error } = await st.client.auth.signInAnonymously()
      if (error) return fail(error, /anonymous/i.test(error.message || '') ? '공유 참여 기능이 서버에서 아직 꺼져 있어요(Supabase 익명 로그인)' : '참여하지 못했어요. 잠시 뒤 다시 해 주세요')
      st.user = data.user
    }
    const { error } = await st.client.rpc('join_with_code', { p_code: code })
    if (error) return fail(error, '코드가 맞지 않거나 기간이 지났어요')
    $('#joinCode').value = ''
    await load()
    app.toast('공유받은 근무표를 불러왔어요')
    app.setScreen('home')
  }
  $('#leaveBtn').onclick = async () => {
    if (!st.user || st.role !== 'viewer') return
    const { error } = await st.client.from('members').delete().eq('schedule_id', st.schedule.id).eq('user_id', st.user.id)
    if (error) return fail(error, '나가지 못했어요')
    st.schedule = null
    await load()
    app.toast('공유받은 근무표에서 나왔어요')
  }
  // 서버에 연결된 주인은 진짜 초대 코드를 쓴다(아니면 앱의 시연용 코드)
  const demoNew = $('#newInvite').onclick
  const demoCopy = $('#copyCode').onclick
  $('#newInvite').onclick = (e) => (isOwnerOnline() ? createInvite() : demoNew?.(e))
  $('#copyCode').onclick = async (e) => {
    if (!isOwnerOnline()) return demoCopy?.(e)
    if (!st.inviteReady) await createInvite()
    if (!st.inviteReady) return
    navigator.clipboard?.writeText(app.data.invite).catch(() => {})
    $('#modalText').textContent = `초대 코드 ${app.data.invite}를 보내고, 받은 사람은 앱의 공유 탭에서 코드를 입력하면 돼요.`
    $('#modal .demo').hidden = true // "시제품이라 동작하지 않아요" 안내는 서버 없는 모드에서만
    $('#modal').classList.add('open')
  }
}

const UPDATE_URL = 'https://jaeseung1.github.io/shiftwork-on/downloads/version.json'
const UPDATE_PAGE = 'https://jaeseung1.github.io/shiftwork-on/downloads/'
async function checkUpdate(manual) {
  const bridge = window.ShiftBridge
  try {
    const r = await fetch(UPDATE_URL + '?t=' + Date.now(), { cache: 'no-store' })
    const j = await r.json()
    try { localStorage.setItem('shiftwork-update-check', String(Date.now())) } catch (e) {}
    const mine = Number(bridge.appVersionCode())
    if (Number(j.versionCode) > mine) {
      $('#appUpdate').textContent = `새 버전 ${j.versionName} 받으러 가기`
      $('#appUpdate').onclick = () => { location.href = UPDATE_PAGE }
      $('#appUpdate').className = 'primary full'
      app.toast(`새 버전 ${j.versionName}이 있어요. 설정에서 받을 수 있어요`)
    } else if (manual) app.toast('최신 버전이에요')
  } catch (e) {
    if (manual) app.toast('확인하지 못했어요. 인터넷 연결을 확인해 주세요')
  }
}

function isOwnerOnline() {
  return !!(st.client && st.user && st.schedule && st.role === 'owner')
}

async function createInvite() {
  const { data, error } = await st.client.rpc('create_invite')
  if (error) return fail(error, '초대 코드를 만들지 못했어요')
  app.data.invite = data
  app.cache()
  st.inviteReady = true
  $('#inviteCode').textContent = data
  $('#inviteInfo').textContent = '7일 동안 쓸 수 있는 코드예요. 받은 사람은 공유 탭에서 이 코드를 넣으면 돼요.'
  app.toast('초대 코드를 만들었어요')
}

// ---- 아이폰 단축어(제어 센터) ----
// 단축어가 로그인 없이 부를 주소. 토큰은 근무표를 읽기만 하는 열쇠(widget_schedule과 같은 토큰).
function shortcutUrl() {
  if (!st.schedule?.widget_token || !cfg.supabaseUrl || !cfg.supabaseAnonKey) return ''
  const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul'
  const q = new URLSearchParams({ p_token: st.schedule.widget_token, p_tz: tz, apikey: cfg.supabaseAnonKey })
  return `${cfg.supabaseUrl}/rest/v1/rpc/share_summary_json?${q}`
}

// 잠금 화면 위젯(Scriptable) 코드: ios/widget.js의 CONFIG 줄에 서버 주소·키·토큰을 넣어 준다.
// 아이폰 Safari는 누른 순간에만 복사를 허락하므로 코드 틀은 미리 받아 둔다.
function prefetchWidget() {
  if (st.widgetTemplate || window.ShiftBridge) return
  fetch('ios/widget.js', { cache: 'no-cache' })
    .then((r) => (r.ok ? r.text() : null))
    .then((t) => { if (t && t.includes('const CONFIG = ')) st.widgetTemplate = t })
    .catch(() => {})
}

function widgetCode() {
  if (!st.widgetTemplate || !st.schedule?.widget_token || !cfg.supabaseUrl || !cfg.supabaseAnonKey) return ''
  const conf = { url: cfg.supabaseUrl, key: cfg.supabaseAnonKey, token: st.schedule.widget_token, app: location.origin + location.pathname }
  return st.widgetTemplate.replace(/^const CONFIG = .*$/m, () => 'const CONFIG = ' + JSON.stringify(conf))
}

async function copyWidget() {
  const code = widgetCode()
  if (!code) return app.toast(st.widgetTemplate ? '먼저 로그인하거나 초대 코드로 참여해 주세요' : '위젯 코드를 불러오는 중이에요. 잠시 뒤 다시 눌러 주세요')
  const box = $('#lockCode')
  try {
    await navigator.clipboard.writeText(code)
    box.hidden = true
    app.toast('위젯 코드를 복사했어요. Scriptable에 붙여 넣어 주세요')
  } catch (e) {
    box.value = code
    box.hidden = false
    box.focus()
    box.select()
    app.toast('코드를 모두 선택해 복사해 주세요')
  }
}

function renderIos() {
  const panel = $('#iosPanel')
  if (!panel) return
  // 안드로이드 앱 안에서는 홈 화면 위젯이 있으니 숨긴다
  panel.hidden = !!window.ShiftBridge
  $('#lockPanel').hidden = !!window.ShiftBridge
  prefetchWidget()
  const ready = !!shortcutUrl()
  $('#lockCopy').disabled = !ready
  $('#lockState').textContent = ready
    ? '잠금 화면에 오늘 근무가 늘 보여요. 무료 앱 Scriptable을 써요.'
    : '로그인하거나 초대 코드로 참여하면 쓸 수 있어요(위젯이 서버에서 근무표를 읽어요).'
  if (!ready) $('#lockCode').hidden = true
  const url = shortcutUrl()
  $('#iosCopy').disabled = !url
  $('#iosState').textContent = url
    ? '단축어를 만들면 제어 센터 버튼을 눌러 오늘·내일 근무를 알림으로 볼 수 있어요.'
    : '로그인하거나 초대 코드로 참여하면 쓸 수 있어요(단축어가 서버에서 근무표를 읽어요).'
  if (!url) $('#iosUrl').hidden = true
}

async function copyShortcut() {
  const url = shortcutUrl()
  if (!url) return app.toast('먼저 로그인하거나 초대 코드로 참여해 주세요')
  const box = $('#iosUrl')
  try {
    await navigator.clipboard.writeText(url)
    box.hidden = true
    app.toast('단축어 주소를 복사했어요')
  } catch (e) {
    // 복사가 막힌 브라우저면 주소를 보여 주고 직접 복사하게 한다
    box.value = url
    box.hidden = false
    box.focus()
    box.select()
    app.toast('주소를 길게 눌러 복사해 주세요')
  }
}

function renderAccount() {
  renderIos()
  const state = $('#acctState')
  const login = $('#acctLogin')
  const out = $('#acctOut')
  if (!cfg.supabaseUrl || !cfg.supabaseAnonKey) {
    state.textContent = '서버가 아직 설정되지 않아 근무표가 이 기기에만 저장돼요.'
    login.hidden = true
    out.hidden = true
    return
  }
  if (!st.user) {
    state.textContent = '로그인하면 근무표가 저장되고 다른 사람과 공유할 수 있어요.'
    login.hidden = false
    out.hidden = true
    return
  }
  if (st.user.is_anonymous) {
    state.textContent = st.role === 'viewer' ? '초대 코드로 공유받은 근무표를 보는 중이에요(읽기 전용)' : '초대 코드를 공유 탭에서 넣어 주세요'
    login.hidden = true
    out.hidden = false
    out.textContent = '공유받은 근무표 연결 끊기'
    return
  }
  out.textContent = '로그아웃'
  const who = st.user.email || '이 계정'
  state.textContent = st.role === 'viewer' ? `${who} · 공유받은 근무표를 보는 중이에요(읽기 전용)` : `${who} · 내 근무표가 서버에 저장돼요`
  login.hidden = true
  out.hidden = false
}

async function renderFamily() {
  const leave = $('#leaveBtn')
  leave.hidden = st.role !== 'viewer'
  if (!isOwnerOnline()) return
  $('#inviteInfo').textContent = st.inviteReady ? $('#inviteInfo').textContent : '초대 코드 만들기를 누르면 7일 동안 쓸 수 있는 코드가 생겨요.'
  const { count, error } = await st.client.from('members').select('user_id', { count: 'exact', head: true }).eq('schedule_id', st.schedule.id)
  if (error) return
  const list = $('#members')
  list.querySelector('.member-extra')?.remove()
  if (count) {
    const row = document.createElement('div')
    row.className = 'member member-extra'
    row.innerHTML = `<div class="avatar">공</div><div class="member-main"><strong>공유받는 사람 ${count}명</strong><span>근무표를 보고 있어요</span></div><span class="role">보기</span>`
    list.append(row)
  }
}

// ---- 안드로이드 위젯 ----
// 앱(WebView)이 window.ShiftBridge를 넣어 주면 근무표와 서버 접속 정보를 넘긴다. 브라우저에서는 아무 일도 없다.
function sendToWidget() {
  const bridge = window.ShiftBridge
  if (!bridge) return
  try {
    const overrides = Object.entries(app.data.overrides || {}).map(([day, shift]) => ({ day, shift }))
    bridge.setSchedule(JSON.stringify({ anchor_date: app.data.anchor, anchor_shift: app.data.anchorShift, overrides }))
    if (st.schedule && cfg.supabaseUrl && cfg.supabaseAnonKey) {
      bridge.setRemote(JSON.stringify({ url: cfg.supabaseUrl, anonKey: cfg.supabaseAnonKey, token: st.schedule.widget_token }))
    } else {
      bridge.setRemote('')
    }
  } catch (e) {
    console.warn('[교대온] 위젯에 전달하지 못함', e)
  }
}
