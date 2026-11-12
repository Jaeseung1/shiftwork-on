import { Stage } from '../../kit'
import './widget.css'

// 교대온 홈 화면 위젯 (갤럭시 4x1): 디자인 목업이며 앱의 실제 기능이 아님.
// 메인은 오늘·내일 근무, 남는 오른쪽 칸은 앞으로 5일의 근무 흐름으로 채움.
// 값은 앱의 순환(일 → 야 → 조 → 휴, 기준일 2026-11-12 = 일근)에서 계산. 11/12는 목요일.
const NAMES = ['일근', '야근', '조근', '휴무']
const WEEK = ['일', '월', '화', '수', '목', '금', '토']
const mod = (n: number, m: number) => ((n % m) + m) % m
const day = (d: number) => ({ d, w: WEEK[mod(d - 12 + 4, 7)]!, s: mod(d - 12, 4) })

const MAIN = [
  { label: '오늘', ...day(12), time: '08:30–17:30' },
  { label: '내일', ...day(13), time: '17:30–24:00' },
]
const NEXT = [14, 15, 16, 17, 18].map(day)

export default function ShiftworkWidget() {
  return (
    <Stage width={440} height={150} theme="dark" ground={['#1f6f5c', '#27485f', '#0e2a33']} className="sx">
      {() => (
        <div className="sx-widget">
          {MAIN.map((c) => (
            <div className="sx-col sx-main" key={c.label}>
              <span className="sx-label">{c.label} · {c.d}일 {c.w}</span>
              <span className="sx-shift" data-s={c.s}>{NAMES[c.s]}</span>
              <span className="sx-sub">{c.time}</span>
            </div>
          ))}
          <div className="sx-week">
            <span className="sx-brand">교대온</span>
            <div className="sx-days">
              {NEXT.map((n) => (
                <span className="sx-day" key={n.d}>
                  <span className="sx-w">{n.w}</span>
                  <span className="sx-dot" data-s={n.s}>{NAMES[n.s]![0]}</span>
                  <span className="sx-n">{n.d}</span>
                </span>
              ))}
            </div>
          </div>
        </div>
      )}
    </Stage>
  )
}
