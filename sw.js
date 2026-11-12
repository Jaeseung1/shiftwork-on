// 교대온 서비스 워커: 인터넷이 없어도 앱이 열리게 한다.
// - 앱 파일(같은 주소)은 네트워크를 먼저 쓰고, 안 되면 저장해 둔 것을 쓴다(업데이트가 바로 반영되게).
// - supabase-js 모듈(cdn.jsdelivr.net)은 저장해 둔 것을 먼저 쓴다(버전이 고정되어 있음).
// - Supabase API 요청은 건드리지 않는다.
const CACHE = 'shiftwork-v1'
const SHELL = ['./', './index.html', './sync.js', './config.js', './manifest.webmanifest', './icons/icon-192.png', './icons/icon-512.png', './icons/apple-touch-icon.png']

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (e) => {
  const req = e.request
  if (req.method !== 'GET') return
  const url = new URL(req.url)
  if (url.origin === self.location.origin) {
    e.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) caches.open(CACHE).then((c) => c.put(req, res.clone()))
          return res
        })
        .catch(() => caches.match(req, { ignoreSearch: true }).then((hit) => hit || caches.match('./index.html'))),
    )
    return
  }
  if (url.hostname === 'cdn.jsdelivr.net') {
    e.respondWith(
      caches.match(req).then(
        (hit) =>
          hit ||
          fetch(req).then((res) => {
            if (res.ok) caches.open(CACHE).then((c) => c.put(req, res.clone()))
            return res
          }),
      ),
    )
  }
})
