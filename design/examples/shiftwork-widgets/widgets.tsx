import { Coffee, Moon, Sun, Sunrise } from 'lucide-react'
import { Stage } from '../../kit'
import './widgets.css'

// 교대온 갤럭시 홈 화면 위젯 시안 v4 (4x1·2x1·1x1). 정지 이미지, 실제 위젯(Android RemoteViews)의 설계도.
// 출발점: 이전 시안 shiftwork-widget(위젯 하나를 배경화면 위에) + gold static-card의 절제 규칙.
// 앱 v0.4와 같은 말: 근무마다 한 가지 색(일 주황, 야 보라, 조 파랑, 휴 초록)과 아이콘(해·달·해돋이·커피, lucide).
// 위젯 하나에는 색이 하나만: 오늘 근무의 색이 아이콘·이름·"근무 중" 칩에 쓰이고, 내일은 회색 카드에 작은 아이콘 색만.
// 시각: 서울 2026-11-13(금) 13:00, 오늘 일근(근무 중, 4시간 30분 남음), 내일 14일(토) 야근.
// 크기: 갤럭시 칸을 작게 어림한 dp의 1.5배(4x1 340×80dp → 510×120).

type S = '일' | '야' | '조' | '휴'
const NAME: Record<S, string> = { 일: '일근', 야: '야근', 조: '조근', 휴: '휴무' }
const ICON = { 일: Sun, 야: Moon, 조: Sunrise, 휴: Coffee }

function Glyph({ s, size = 15 }: { s: S; size?: number }) {
  const I = ICON[s]
  return <I className={`sw-glyph sw-c-${s}`} size={size} strokeWidth={2.4} aria-hidden />
}

function W4x1() {
  return (
    <div className="sw-w sw-4x1">
      <div className="sw-today">
        <div className="sw-cap"><Glyph s="일" />오늘 · 13일 금</div>
        <div className="sw-namerow"><span className="sw-name sw-c-일">일근</span><span className="sw-chip sw-t-일 sw-c-일">근무 중</span></div>
        <div className="sw-time">08:30–17:30 · 4시간 30분 남음</div>
      </div>
      <div className="sw-next">
        <div className="sw-cap"><Glyph s="야" size={13} />내일 · 14일 토</div>
        <div className="sw-name sw-name--sm">야근</div>
        <div className="sw-time">17:30–08:30</div>
      </div>
    </div>
  )
}

function W2x1() {
  return (
    <div className="sw-w sw-2x1">
      <div className="sw-namerow"><Glyph s="일" size={18} /><span className="sw-name sw-c-일">일근</span><span className="sw-chip sw-t-일 sw-c-일">근무 중</span></div>
      <div className="sw-time sw-time--ink">17:30 퇴근</div>
      <div className="sw-time sw-time--sub"><Glyph s="야" size={12} />내일 야근 · 17:30</div>
    </div>
  )
}

function W1x1() {
  return (
    <div className="sw-w sw-1x1">
      <Glyph s="일" size={18} />
      <span className="sw-name sw-c-일">일근</span>
      <span className="sw-time sw-time--ink">17:30 퇴근</span>
    </div>
  )
}

const SIZE = { '4x1': [510, W4x1], '2x1': [240, W2x1], '1x1': [112, W1x1] } as const

export function Widgets({ dark, kind = '4x1' }: { dark?: boolean; kind?: keyof typeof SIZE }) {
  const [w, W] = SIZE[kind]
  return (
    <Stage width={w + 90} height={210} theme={dark ? 'dark' : 'light'} className={`sw${dark ? ' sw--dark' : ''}`}>
      {() => (
        <div className="sw-home">
          <div className="sw-one" style={{ width: w }}><W /></div>
        </div>
      )}
    </Stage>
  )
}
