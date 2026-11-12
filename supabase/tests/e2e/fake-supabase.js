// 시험용 가짜 supabase-js: 앱이 쓰는 부분만 같은 모양으로 흉내 내고, 실제 처리는 로컬 PostgreSQL이 한다.
const post = (url, body) => fetch(url, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json())
const KEY = 'fake-session'
const session = () => { try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null } }
class Q {
  constructor(table) { this.q = { table, op: 'select', columns: '*', filters: [] } }
  select(columns = '*', opts = {}) { if (this.q.op === 'select') { this.q.columns = columns; this.q.head = !!opts.head } else this.q.returning = true; return this }
  insert(v) { this.q.op = 'insert'; this.q.values = v; return this }
  upsert(v) { this.q.op = 'upsert'; this.q.values = v; return this }
  update(v) { this.q.op = 'update'; this.q.values = v; return this }
  delete() { this.q.op = 'delete'; return this }
  eq(c, v) { this.q.filters.push([c, 'eq', v]); return this }
  in(c, v) { this.q.filters.push([c, 'in', v]); return this }
  single() { this.mode = 'single'; return this }
  maybeSingle() { this.mode = 'maybe'; return this }
  then(ok, bad) { return this.run().then(ok, bad) }
  async run() {
    window.__calls = (window.__calls || []).concat([{ ...this.q }])
    const r = await post('/__q', { ...this.q, uid: session()?.user.id || null })
    if (r.error) return { data: null, error: r.error, count: null }
    if (this.q.head) return { data: null, error: null, count: r.data.count }
    let data = r.data
    if (this.mode === 'single') { if (data.length !== 1) return { data: null, error: { message: 'not single' } }; data = data[0] }
    if (this.mode === 'maybe') data = data[0] ?? null
    return { data, error: null }
  }
}
let pending = null
export function createClient() {
  const listeners = []
  const fire = (ev) => listeners.forEach((cb) => cb(ev, session()))
  return {
    from: (t) => new Q(t),
    rpc: async (fn, args) => { const r = await post('/__rpc', { fn, args, uid: session()?.user.id || null }); return r.error ? { data: null, error: r.error } : { data: r.data, error: null } },
    auth: {
      getSession: async () => ({ data: { session: session() } }),
      onAuthStateChange: (cb) => { listeners.push(cb); setTimeout(() => cb('INITIAL_SESSION', session()), 0); return { data: { subscription: { unsubscribe() {} } } } },
      // 시험: 가입하면 확인 메일 대기(confirmed=false). 시험 쪽에서 __confirm으로 확인 처리. 비밀번호는 'pw1234'만 맞음
      signUp: async ({ email, password }) => {
        const users = JSON.parse(localStorage.getItem('fake-users') || '{}')
        if (users[email]) return { data: { user: { id: users[email].id, identities: [] }, session: null }, error: null }
        const uid = { 'a@test.kr': 'aaaaaaaa-0000-0000-0000-00000000000a', 'b@test.kr': 'bbbbbbbb-0000-0000-0000-00000000000b' }[email]
        users[email] = { id: uid, password, confirmed: false }
        localStorage.setItem('fake-users', JSON.stringify(users))
        return { data: { user: { id: uid, identities: [{}] }, session: null }, error: null }
      },
      signInWithPassword: async ({ email, password }) => {
        const u = JSON.parse(localStorage.getItem('fake-users') || '{}')[email]
        if (!u || u.password !== password) return { data: {}, error: { message: 'Invalid login credentials' } }
        if (!u.confirmed) return { data: {}, error: { message: 'Email not confirmed' } }
        await post('/__login', { uid: u.id })
        localStorage.setItem(KEY, JSON.stringify({ user: { id: u.id, email } }))
        setTimeout(() => fire('SIGNED_IN'), 0)
        return { data: {}, error: null }
      },
      signInAnonymously: async () => {
        const uid = 'cccccccc-0000-4000-8000-' + String(Date.now()).slice(-12).padStart(12, '0')
        await post('/__login', { uid })
        const user = { id: uid, is_anonymous: true }
        localStorage.setItem(KEY, JSON.stringify({ user }))
        setTimeout(() => fire('SIGNED_IN'), 0)
        return { data: { user }, error: null }
      },
      signOut: async () => { localStorage.removeItem(KEY); fire('SIGNED_OUT'); return { error: null } },
    },
  }
}
