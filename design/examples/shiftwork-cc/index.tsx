import type { CSSProperties, ReactNode } from 'react'
import { AlarmClock, BatteryMedium, Bluetooth, Calculator, Camera, Flashlight, Mic, Moon, Pause, Plane, QrCode, Signal, SkipBack, SkipForward, Sun, SunMedium, Timer, Volume2, Wifi } from 'lucide-react'
import { Phone, Stage, WALLPAPER } from '../../kit'
import './cc.css'

// 교대온 제어 센터 컨트롤 (디자인 목업, 앱의 실제 기능 아님).
// Apple HIG Controls: "Controls contain a symbol image, a title, and, optionally, a value."
// "In Control Center, a control displays its symbol and, at larger sizes, its title and value."
// 그래서 교대온은 중간 크기 컨트롤 두 개(오늘 근무, 내일 근무)로만 들어가고,
// 각각 심볼 하나, 제목 하나, 값 하나를 가진다. 주변 기본 컨트롤은 글자 없는 채움(filler)이다.
//
// 좌표는 아이폰 화면 포인트(402 × 874). 격자: 4열, 칸 72pt, 간격 16pt, 좌우 여백 33pt.
// 이 치수와 반투명 질감은 눈대중이며 Apple이 공개한 값이 아님.
// 교대온 심볼만 근무 색으로 틴트(HIG: "The system applies this tint color to a control toggle’s symbol in its on state.").
// 아이콘: SF Symbols를 쓸 수 없어 lucide-react 한 벌을 한 굵기(2)로 통일.
//
// 실제 글자: 오늘 근무 / 일근, 내일 근무 / 야근. 나머지는 모양뿐.

const M = 33
const C = 72
const G = 16
const TOP = 70
const x = (col: number) => M + col * (C + G)
const y = (row: number) => TOP + row * (C + G)
const span = (n: number) => n * C + (n - 1) * G

const box = (col: number, row: number, w: number, h: number): CSSProperties => ({ left: x(col), top: y(row), width: span(w), height: span(h) })

function Round({ col, row, on, children }: { col: number; row: number; on?: string; children: ReactNode }) {
  return (
    <span className="cc-round" data-on={on} style={box(col, row, 1, 1)}>
      {children}
    </span>
  )
}

function Control({ col, row, icon, title, value, k }: { col: number; row: number; icon: ReactNode; title: string; value: string; k: string }) {
  return (
    <div className="cc-control" data-k={k} style={box(col, row, 2, 1)} role="img" aria-label={`${title} ${value}`}>
      <span className="cc-control__sym">{icon}</span>
      <span className="cc-control__text">
        <span className="cc-control__title">{title}</span>
        <span className="cc-control__value">{value}</span>
      </span>
    </div>
  )
}

const I = { size: 24, strokeWidth: 2, absoluteStrokeWidth: true } as const

function ControlCenter() {
  return (
    <div className="cc-grid">
      {/* 교대온: 중간 크기 컨트롤 두 개. 사용자가 맨 윗줄에 둔 배치 */}
      <Control col={0} row={0} k="0" icon={<Sun {...I} />} title="오늘 근무" value="일근" />
      <Control col={2} row={0} k="1" icon={<Moon {...I} />} title="내일 근무" value="야근" />

      {/* 연결: 2×2 판 안의 원 네 개. Wi-Fi만 켜짐 */}
      <div className="cc-pane cc-connect" style={box(0, 1, 2, 2)}>
        <span className="cc-dot"><Plane {...I} /></span>
        <span className="cc-dot"><Signal {...I} /></span>
        <span className="cc-dot" data-on="blue"><Wifi {...I} /></span>
        <span className="cc-dot"><Bluetooth {...I} /></span>
      </div>
      {/* 음악: 2×2. 제목 자리는 막대 */}
      <div className="cc-pane cc-media" style={box(2, 1, 2, 2)}>
        <span className="cc-media__art" />
        <span className="cc-media__lines"><span style={{ width: 52 }} /><span style={{ width: 34 }} /></span>
        <span className="cc-media__ctl"><SkipBack {...I} size={20} fill="currentColor" /><Pause {...I} size={26} fill="currentColor" /><SkipForward {...I} size={20} fill="currentColor" /></span>
      </div>

      {/* 아래쪽 기본 컨트롤: 작은 크기는 심볼만 */}
      <Round col={0} row={3}><Flashlight {...I} /></Round>
      <Round col={1} row={3}><Timer {...I} /></Round>
      <Round col={0} row={4}><Calculator {...I} /></Round>
      <Round col={1} row={4}><Camera {...I} /></Round>
      <Round col={0} row={5}><QrCode {...I} /></Round>
      <Round col={1} row={5}><AlarmClock {...I} /></Round>
      <Round col={2} row={5}><Mic {...I} /></Round>
      <Round col={3} row={5}><BatteryMedium {...I} /></Round>
      <span className="cc-slider" style={{ ...box(2, 3, 1, 2), ['--fill' as string]: '58%' }}><SunMedium {...I} /></span>
      <span className="cc-slider" style={{ ...box(3, 3, 1, 2), ['--fill' as string]: '40%' }}><Volume2 {...I} /></span>
    </div>
  )
}

export default function ShiftworkControlCenter() {
  return (
    <Stage width={720} height={480} compact={{ below: 560, width: 358, height: 460 }} className="swcc">
      {(_, __, layout) => {
        const compact = layout === 'compact'
        const pw = compact ? 326 : 480
        const w = compact ? 358 : 720
        return (
          <div className="swcc-device" style={{ left: Math.round((w - pw) / 2), top: compact ? 16 : 40 }}>
            <Phone width={pw} frame="pro" dark wallpaper={`linear-gradient(rgba(8, 10, 20, .5), rgba(8, 10, 20, .5)), ${WALLPAPER}`}>
              <ControlCenter />
            </Phone>
          </div>
        )
      }}
    </Stage>
  )
}
