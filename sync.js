// 교대온 서버 연결(Supabase)과 안드로이드 위젯 전달.
// - config.js가 비어 있으면 서버 없이 지금처럼 이 기기에만 저장한다.
// - 로그인하면 근무표를 서버에 저장하고, 초대 코드로 들어온 가족은 읽기만 한다.
// - 안드로이드 앱(WebView) 안이면 근무표와 위젯 토큰을 ShiftBridge로 넘겨 홈 화면 위젯이 쓰게 한다.
const cfg = window.SHIFTWORK_CONFIG || {}
const app = window.shiftApp
const $ = (s) => document.querySelector(s)
const SUPABASE_MODULE = 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2.45.4/+esm'
const CODE_RE = /^[A-HJ-NP-Z2-9]{6}$/

const st = { client: null, user: null, schedule: null, role: null, synced: null, applying: false, timer: null, loading: null }

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

// 서버에서 근무표를 고른다: 가족으로 참여한 근무표가 있으면 그것(읽기 전용), 없으면 내 근무표(없으면 지금 기기 데이터로 만든다)
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
  $('#acctSend').onclick = async () => {
    const email = $('#acctEmail').value.trim()
    if (!st.client) return app.toast('서버가 아직 설정되지 않았어요')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return app.toast('이메일 주소를 확인해 주세요')
    const { error } = await st.client.auth.signInWithOtp({ email, options: { emailRedirectTo: location.href.split('#')[0] } })
    if (error) return fail(error, '로그인 메일을 보내지 못했어요')
    $('#acctState').textContent = `${email}로 로그인 링크를 보냈어요. 메일의 링크를 이 기기에서 열어 주세요.`
    app.toast('메일로 로그인 링크를 보냈어요')
  }
  $('#acctOut').onclick = async () => {
    if (!st.client) return
    await st.client.auth.signOut()
    await onSession(null)
    app.toast('로그아웃했어요')
  }
  $('#joinBtn').onclick = async () => {
    const code = $('#joinCode').value.trim().toUpperCase()
    if (!CODE_RE.test(code)) return app.toast('코드 6자리를 확인해 주세요')
    if (!st.client) return app.toast('서버가 아직 설정되지 않았어요')
    if (!st.user) {
      app.toast('설정에서 먼저 로그인해 주세요')
      return app.setScreen('settings')
    }
    const { error } = await st.client.rpc('join_with_code', { p_code: code })
    if (error) return fail(error, '코드가 맞지 않거나 기간이 지났어요')
    $('#joinCode').value = ''
    await load()
    app.toast('가족 근무표를 불러왔어요')
    app.setScreen('home')
  }
  $('#leaveBtn').onclick = async () => {
    if (!st.user || st.role !== 'viewer') return
    const { error } = await st.client.from('members').delete().eq('schedule_id', st.schedule.id).eq('user_id', st.user.id)
    if (error) return fail(error, '나가지 못했어요')
    st.schedule = null
    await load()
    app.toast('가족 근무표에서 나왔어요')
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
    $('#modalText').textContent = `초대 코드 ${app.data.invite}를 가족에게 보내고, 가족은 앱의 가족 탭에서 코드를 입력하면 돼요.`
    $('#modal').classList.add('open')
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
  $('#inviteInfo').textContent = '7일 동안 쓸 수 있는 코드예요. 가족은 가족 탭에서 이 코드를 넣으면 돼요.'
  app.toast('초대 코드를 만들었어요')
}

function renderAccount() {
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
    state.textContent = '로그인하면 근무표가 저장되고 가족과 공유돼요.'
    login.hidden = false
    out.hidden = true
    return
  }
  const who = st.user.email || '이 계정'
  state.textContent = st.role === 'viewer' ? `${who} · 가족 근무표를 보는 중이에요(읽기 전용)` : `${who} · 내 근무표가 서버에 저장돼요`
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
    row.innerHTML = `<div class="avatar">가</div><div class="member-main"><strong>가족 ${count}명</strong><span>근무표를 보고 있어요</span></div><span class="role">보기</span>`
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
