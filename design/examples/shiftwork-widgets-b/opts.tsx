import { Coffee, Moon, Sun, CloudSun } from 'lucide-react'
import { Stage } from '../../kit'
import '../shiftwork-widgets/widgets.css'
import './opts.css'

// 교대온 4x1 위젯 시안 3가지(사용자가 고르게). 서울 2026-11-13(금) 13:00, 오늘 일근 근무 중, 내일 야근.
type S = '일' | '야' | '조' | '휴'
const ICON = { 일: Sun, 야: Moon, 조: CloudSun, 휴: Coffee }
const G = ({ s, size = 22 }: { s: S; size?: number }) => {
  const I = ICON[s]
  return <I className={`sw-glyph sw-c-${s}`} size={size} strokeWidth={2.3} aria-hidden />
}

// A: 진행 막대가 주인공
function A() {
  return (
    <div className="sw-w op-a">
      <div className="op-tile sw-t-일"><G s="일" /></div>
      <div className="op-a__main">
        <div className="op-a__top"><span className="sw-name sw-c-일">일근</span><span className="op-a__left sw-c-일">4시간 30분 남음</span></div>
        <div className="op-bar"><i className="op-bar__fill" style={{ width: '50%' }} /></div>
        <div className="op-a__times"><span>08:30</span><span className="op-a__tom"><G s="야" size={12} />내일 야근</span><span>17:30</span></div>
      </div>
    </div>
  )
}

// B: 오늘·내일 두 칸을 똑같은 모양으로, 판 없이 가는 선으로만 나눔
function B() {
  return (
    <div className="sw-w op-b">
      <div className="op-b__half">
        <div className="op-tile sw-t-일"><G s="일" /></div>
        <div><div className="sw-cap">오늘 · 근무 중</div><div className="sw-name sw-c-일">일근</div><div className="sw-time">17:30 퇴근</div></div>
      </div>
      <div className="op-b__rule" />
      <div className="op-b__half">
        <div className="op-tile sw-t-야"><G s="야" /></div>
        <div><div className="sw-cap">내일 · 14일 토</div><div className="sw-name op-ink">야근</div><div className="sw-time">17:30 출근</div></div>
      </div>
    </div>
  )
}

// C: 오늘 크게 + 앞으로 5일 흐름(앱 홈의 5일 칸과 같은 모양)
const NEXT: [string, string, S][] = [['토', '14', '야'], ['일', '15', '조'], ['월', '16', '휴'], ['화', '17', '일'], ['수', '18', '야']]
function C() {
  return (
    <div className="sw-w op-c">
      <div className="op-c__today">
        <div className="sw-cap"><G s="일" size={15} />오늘 · 근무 중</div>
        <div className="sw-name sw-c-일">일근</div>
        <div className="sw-time">17:30 퇴근</div>
      </div>
      <div className="op-c__days">
        {NEXT.map(([w, d, s]) => (
          <div className={`op-day sw-t-${s}`} key={d}><span className="op-day__w">{w}</span><span className={`op-day__s sw-c-${s}`}>{s}</span><span className="op-day__n">{d}</span></div>
        ))}
      </div>
    </div>
  )
}

const KIND = { A, B, C }
export function Opt({ k }: { k: keyof typeof KIND }) {
  const W = KIND[k]
  return (
    <Stage width={600} height={210} theme="light" className="sw">
      {() => (
        <div className="sw-home"><div className="sw-one" style={{ width: 510 }}><W /></div></div>
      )}
    </Stage>
  )
}
