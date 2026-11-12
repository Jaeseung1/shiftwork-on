import { useEffect, useState } from 'react'
import { Phone, Stage } from '../../kit'
import './home.css'

// 교대온 앱 홈(index.html v0.2)의 제품 사진. 정지 이미지.
// 출발점: gold `phone-touch`(폰 하나, 위에서 고정, 아래로 잘림). 바꾼 것: 앱의 실제 화면과
// 토스 토큰(design/pages/widget.html과 같은 값, 제3자 정리값이며 공식 토큰 아님), 한국어 문자열.
// 시각: 서울 2026-11-12(목) 13:00, 기준일 11/12 = 일근, 순환 일→야→조→휴.
// 주인공: 오늘 카드의 "일근"과 "4시간 30분 남음". 내일·앞으로 5일은 받침, 가족 공유 줄은 잘림.
// 아이콘: lucide(ISC), 앱과 같은 글리프.

const NAMES: Record<string, string> = { 일: '일근', 야: '야근', 조: '조근', 휴: '휴무' }
const CYCLE = ['일', '야', '조', '휴']
const WD = ['일', '월', '화', '수', '목', '금', '토']
// 11/12(목) = 일근
const day = (d: number) => ({ d, w: WD[(d - 12 + 4 + 70) % 7]!, s: CYCLE[(d - 12 + 40) % 4]! })
const NEXT5 = [14, 15, 16, 17, 18].map(day)

function Right() {
  return (
    <svg className="swh-ico" viewBox="0 0 24 24" aria-hidden>
      <path d="m9 18 6-6-6-6" />
    </svg>
  )
}

function App() {
  return (
    <div className="swh-app">
      <div className="swh-top">
        <span>교대온</span>
      </div>
      <div className="swh-screen">
        <div className="swh-card swh-hero">
          <div className="swh-hero__top">
            <span className="swh-cap">오늘 · 12일 목</span>
            <span className="swh-badge">일근 중</span>
          </div>
          <div className="swh-today">일근</div>
          <div className="swh-hour">08:30–17:30</div>
          <div className="swh-progress">
            <i style={{ width: '50%' }} />
          </div>
          <div className="swh-plabel">
            <span>4시간 30분 남음</span>
            <span>17:30 퇴근</span>
          </div>
        </div>
        <div className="swh-card swh-next">
          <div>
            <div className="swh-cap swh-cap--sub">내일 · 13일 금</div>
            <div className="swh-next__name">야근</div>
          </div>
          <div className="swh-next__hour">11/13 17:30–11/14 08:30</div>
        </div>
        <div className="swh-card">
          <div className="swh-head">
            <span>앞으로 5일</span>
            <span className="swh-link">근무표 보기</span>
          </div>
          <div className="swh-days">
            {NEXT5.map((n) => (
              <span className="swh-day" key={n.d}>
                <span className="swh-day__w">{n.w}</span>
                <span className="swh-day__s" data-s={n.s}>
                  {n.s}
                </span>
                <span className="swh-day__n">{n.d}</span>
              </span>
            ))}
          </div>
        </div>
        <div className="swh-card swh-row">
          <span>
            <b>가족과 근무표 공유</b>
            <small>초대 코드로 가족도 근무표를 볼 수 있어요</small>
          </span>
          <Right />
        </div>
      </div>
    </div>
  )
}

// 앱처럼 기기 테마를 따른다. 어두운 화면에서는 Phone의 상태바를 밝게(dark).
function useDarkScheme() {
  const q = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null
  const [dark, setDark] = useState(!!q?.matches)
  useEffect(() => {
    if (!q) return
    const on = () => setDark(q.matches)
    q.addEventListener('change', on)
    return () => q.removeEventListener('change', on)
  }, [q])
  return dark
}

export default function ShiftworkHome() {
  const dark = useDarkScheme()
  return (
    <Stage width={440} height={650} compact={{ below: 440, width: 358, height: 560 }} className="swh">
      {(_, __, layout) => {
        const compact = layout === 'compact'
        const pw = compact ? 326 : 360
        const w = compact ? 358 : 440
        return (
          <div className="swh-device" style={{ left: Math.round((w - pw) / 2), top: compact ? 16 : 40 }}>
            <Phone width={pw} frame="pro" dark={dark} wallpaper="var(--swh-page)">
              <App />
            </Phone>
          </div>
        )
      }}
    </Stage>
  )
}
