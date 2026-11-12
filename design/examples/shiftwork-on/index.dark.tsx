import { Stage, Phone, Roll, Segmented, Button, Icon, type Act } from '../../kit'
import './shiftwork.css'

// 교대온 (shiftwork-on): the 근무표 screen, one gesture. The 15th is a day off;
// the editor opens on it, the person picks 야근 and saves, and the day's tag on
// the calendar takes the new shift, on the row the editor sits beside.
// 초기화 puts the day back, which is the loop's seen cause. The editor is the
// app's own panel (the shift picker is a segmented control here because a
// dropdown has no room in a 340px stage), straddling the phone's bezel so the
// calendar stays whole.
//
// Real strings: 근무표, the month, the weekday row, the shift tags (the
// product's own repeating content), the editor's title, its time line, the
// four shifts and its two buttons.

type S = { pick: number; saved: number }

const NAMES = ['일근', '야근', '조근', '휴무']
const REST_DAY = 3
const initial: S = { pick: REST_DAY, saved: REST_DAY }
const poster: S = { pick: 1, saved: 1 }

// The product's own wording (timezoneSummary in index.html): 일근 08:30–17:30,
// 야근 17:30–24:00, 조근 00:00–08:30, all Korean time
const SUMMARY = ['한국시간 08:30–17:30', '한국시간 17:30–24:00', '한국시간 00:00–08:30', '한국 기준 휴무']

// 2026-11-12 is the anchor day (일근); the cycle is 일 → 야 → 조 → 휴
const TODAY = 12
const PICKED = 15
const mod = (n: number, m: number) => ((n % m) + m) % m
const cells = Array.from({ length: 35 }, (_, i) => {
  const day = i + 1
  const inMonth = day <= 30
  return { n: inMonth ? day : day - 30, shift: mod(day - TODAY, 4), dim: !inMonth }
})
const WEEK = ['일', '월', '화', '수', '목', '금', '토']

const REST = { wide: { x: 150, y: 316 }, compact: { x: 300, y: 410 } }

async function script(a: Act<S>) {
  await a.wait(1300)
  a.mark('pick')
  await a.click({ at: 'seg-1', x: 0.5, y: 0.9 })
  a.put({ pick: 1 })
  await a.wait(1000)
  a.mark('save')
  await a.click({ at: 'save', x: 0.88, y: 0.6 })
  a.put({ saved: 1 })
  await a.wait(3200)
  a.mark('reset')
  await a.click({ at: 'reset', x: 0.9, y: 0.6 })
  a.put({ saved: REST_DAY, pick: REST_DAY })
  await a.wait(600)
  await a.move('rest')
  await a.wait(900)
}

export default function ShiftworkOn() {
  return (
    <Stage<S>
      width={720}
      height={340}
      compact={{ below: 600, width: 358, height: 440 }}
      theme="dark"
      initial={initial}
      poster={poster}
      script={script}
      cursor={{ at: REST, style: 'touch' }}
      className="sw"
    >
      {(s, rect, layout) => {
        return (
          <>
            <Phone width={layout === 'compact' ? 326 : 380} dark className="sw-phone">
              <div className="sw-app">
                <div className="sw-bar" />
                <div className="sw-page">근무표</div>
                <div className="sw-card">
                  <div className="sw-nav">
                    <span className="sw-month">2026년 11월</span>
                    <span className="sw-nav__btns">
                      <span className="sw-nav__btn"><Icon name="chevron-right" size={16} style={{ transform: 'scaleX(-1)' }} /></span>
                      <span className="sw-nav__btn"><Icon name="chevron-right" size={16} /></span>
                    </span>
                  </div>
                  <div className="sw-grid sw-week">
                    {WEEK.map((w) => (
                      <span key={w}>{w}</span>
                    ))}
                  </div>
                  <div className="sw-grid">
                    {cells.map((c) => {
                      const picked = !c.dim && c.n === PICKED
                      const shift = picked ? s.saved : c.shift
                      return (
                        <span key={`${c.dim}-${c.n}`} className="sw-day" data-dim={c.dim || undefined} data-today={(!c.dim && c.n === TODAY) || undefined} data-sel={picked || undefined} data-target={picked ? 'day' : undefined}>
                          <span className="sw-num">{c.n}</span>
                          <span className="sw-tag" data-s={shift}>
                            {picked ? <Roll>{NAMES[shift]!}</Roll> : NAMES[shift]}
                          </span>
                        </span>
                      )
                    })}
                  </div>
                </div>
              </div>
            </Phone>

            <section className="sw-panel" data-target="panel">
              <div className="sw-title">{PICKED}일 근무</div>
              <div className="sw-sum">
                <Roll delay={160}>{SUMMARY[s.pick]!}</Roll>
              </div>
              <Segmented items={NAMES} value={s.pick} targets={NAMES.map((_, i) => `seg-${i}`)} cursor="pointer" />
              <div className="sw-actions">
                <Button variant="primary" data-target="save">저장</Button>
                <Button variant="secondary" data-target="reset">초기화</Button>
              </div>
            </section>

            <span data-target="rest" style={{ position: 'absolute', left: REST[layout].x, top: REST[layout].y }} />
          </>
        )
      }}
    </Stage>
  )
}
