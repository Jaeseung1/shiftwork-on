// 시험용: 가짜 Supabase 백엔드. 브라우저의 fake-supabase.js가 보낸 요청을 로컬 PostgreSQL(RLS 포함)에서 실행한다.
import http from 'node:http'
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
const [root, port, pgHost, pgPort, db] = process.argv.slice(2)
const PK = { overrides: ['schedule_id', 'day'], schedules: ['id'], members: ['schedule_id', 'user_id'], invites: ['code'] }
const ident = (s) => { if (!/^[a-z_]+$/.test(s)) throw new Error('bad ident ' + s); return s }
const lit = (v) => v === null || v === undefined ? 'null' : `'${String(v).replace(/'/g, "''")}'`
function psql(sql) {
  try {
    const out = execFileSync('psql', ['-h', pgHost, '-p', pgPort, '-U', 'postgres', '-d', db, '-At', '-v', 'ON_ERROR_STOP=1', '-c', sql], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] })
    const lines = out.trim().split('\n').filter((l) => l && !/^(BEGIN|COMMIT|SET|RESET|INSERT \d+ \d+|UPDATE \d+|DELETE \d+)$/.test(l) && l !== 'ok')
    return { out: lines.at(-1) ?? '' }
  } catch (e) { return { error: { message: String(e.stderr || e.message).replace(/^.*ERROR:\s*/s, '').trim() } } }
}
const asUser = (uid, stmt) => `begin; set local role ${uid ? 'authenticated' : 'anon'}; select 'ok' from set_config('request.jwt.claim.sub', ${lit(uid || '')}, true); ${stmt}; commit;`
const where = (filters) => filters.length ? ' where ' + filters.map(([c, op, v]) => op === 'in' ? `${ident(c)} in (${v.map(lit).join(',')})` : `${ident(c)} = ${lit(v)}`).join(' and ') : ''
function query(q) {
  const t = 'public.' + ident(q.table)
  const rowsJson = lit(JSON.stringify(Array.isArray(q.values) ? q.values : [q.values]))
  const cols = q.values ? Object.keys(Array.isArray(q.values) ? q.values[0] : q.values).map(ident) : []
  let stmt
  if (q.op === 'select') {
    stmt = q.head ? `select json_build_object('count', count(*)) from ${t}${where(q.filters)}`
      : `select coalesce(json_agg(x), '[]') from (select ${q.columns === '*' ? '*' : q.columns.split(',').map((c) => ident(c.trim())).join(',')} from ${t}${where(q.filters)}) x`
  } else if (q.op === 'insert' || q.op === 'upsert') {
    const conflict = q.op === 'upsert' ? ` on conflict (${PK[q.table].join(',')}) do update set ${cols.filter((c) => !PK[q.table].includes(c)).map((c) => `${c} = excluded.${c}`).join(',') || PK[q.table][0] + ' = excluded.' + PK[q.table][0]}` : ''
    stmt = `with r as (insert into ${t} (${cols}) select ${cols} from json_populate_recordset(null::${t}, ${rowsJson})${conflict} returning *) select coalesce(json_agg(r), '[]') from r`
  } else if (q.op === 'update') {
    stmt = `with r as (update ${t} set ${cols.map((c) => `${c} = (json_populate_record(null::${t}, ${lit(JSON.stringify(q.values))})).${c}`).join(',')}${where(q.filters)} returning *) select coalesce(json_agg(r), '[]') from r`
  } else if (q.op === 'delete') {
    stmt = `with r as (delete from ${t}${where(q.filters)} returning *) select coalesce(json_agg(r), '[]') from r`
  }
  return psql(asUser(q.uid, stmt))
}
function rpc(q) {
  const args = Object.entries(q.args || {}).map(([k, v]) => `${ident(k)} => ${lit(v)}`).join(',')
  return psql(asUser(q.uid, `select to_json(public.${ident(q.fn)}(${args}))`))
}
http.createServer((req, res) => {
  if (req.method === 'POST') {
    let body = ''
    req.on('data', (d) => (body += d))
    req.on('end', () => {
      const q = JSON.parse(body)
      let r
      if (req.url === '/__q') r = query(q)
      else if (req.url === '/__rpc') r = rpc(q)
      else if (req.url === '/__login') r = psql(`insert into auth.users values (${lit(q.uid)}) on conflict do nothing; select 'ok'`)
      res.setHeader('content-type', 'application/json')
      res.end(JSON.stringify(r.error ? { error: r.error } : { data: r.out ? JSON.parse(r.out) : null }))
    })
    return
  }
  const f = path.join(root, decodeURIComponent(req.url.split('?')[0]) === '/' ? 'index.html' : decodeURIComponent(req.url.split('?')[0]))
  if (!fs.existsSync(f)) { res.statusCode = 404; return res.end('no') }
  res.setHeader('content-type', f.endsWith('.js') ? 'text/javascript' : f.endsWith('.html') ? 'text/html; charset=utf-8' : 'application/octet-stream')
  res.end(fs.readFileSync(f))
}).listen(Number(port), () => console.log('listening', port))
