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
      // 시험: 메일은 보내지 않고, 코드 123456을 넣으면 로그인
      signInWithOtp: async ({ email }) => { pending = email; return { error: null } },
      verifyOtp: async ({ email, token }) => {
        if (token !== '123456' || email !== pending) return { error: { message: 'invalid otp' } }
        const uid = { 'a@test.kr': 'aaaaaaaa-0000-0000-0000-00000000000a', 'b@test.kr': 'bbbbbbbb-0000-0000-0000-00000000000b' }[email]
        await post('/__login', { uid })
        localStorage.setItem(KEY, JSON.stringify({ user: { id: uid, email } }))
        setTimeout(() => fire('SIGNED_IN'), 0)
        return { error: null }
      },
      signOut: async () => { localStorage.removeItem(KEY); fire('SIGNED_OUT'); return { error: null } },
    },
  }
}
