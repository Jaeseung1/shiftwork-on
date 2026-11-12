# 교대온 홈페이지 데모·위젯 작업 인계 (2026-11-12)

## 새 Claude Code 세션에 붙여 넣을 말
레포의 claude/design-handoff 브랜치 design/HANDOFF.md를 읽고 이어서 작업해 줘. 한국어로 답해 줘. 모르는 건 모른다고 하고, 사실 주장에는 출처를 달아 줘. 내 허락 없이 다운로드는 시작하지 마.

## 목표
내 서비스 교대온(github.com/Jaeseung1/shiftwork-on, 교대 근무 달력 웹 앱)의 홈페이지 첫 화면용 데모(720×340)와 폰 위젯 목업을 Stageshow 스킬(benjitaylor/skills)로 만든다.

## 지금 상태
- examples/shiftwork-on: 근무표 화면 데모(15일 휴무 → 야근 선택 → 저장 → 초기화 루프). 현재 라이트 버전. 다크 원본은 index.dark.tsx, shiftwork.dark.css. 게이트(film.mjs --gate)는 720, 390, 정지 모두 rehearsal passed, 커버리지는 inconclusive. REHEARSAL.md에 한계 기록.
- examples/shiftwork-widget: 갤럭시 4x1 위젯 Stageshow 정지 이미지(다크, 이전 디자인). 최신 디자인은 pages/widget.html(토스 스타일, 라이트/다크 토글)이 기준.
- pages/widget.html: 4x1 위젯 토스 스타일 목업. 오늘(큰 파란 글씨), 내일, 앞으로 5일. "근무 중"은 1x1·2x1에서만 쓰기로 함.
- pages/cc.html: 아이폰 제어 센터 컨트롤 목업. 오늘+내일, 근무는 서울 기준, 출퇴근은 기기 시간대로 환산(캐나다에서 보는 사람 가정).
- images/: 위 결과 이미지.
- images/export/ (2026-11-12 추가, 후보 1 완료): pages/의 목업을 PNG로 내보냄. 2배율.
  - widget-4x1-light.png, widget-4x1-dark.png: 위젯만, 모서리 투명(RGBA), 그림자 제외.
  - widget-4x1-light-wall.png, widget-4x1-dark-wall.png: 홈 화면 배경 포함.
  - cc-medium.png: 제어 센터 중간 크기(오늘+내일).
  - cc-times-toronto.png, cc-times-seoul.png: 시간 표시 타일, 기기 시간대별.
  - 다시 만들기: 레포 루트에서 `node design/scripts/export-mockups.js` (컨테이너에 사전 설치된 Playwright 사용).
  - 한계: 이 컨테이너에는 Noto Sans KR이 없어 한글이 WenQuanYi Zen Hei로 렌더링됨(fc-list로 확인). 실제 기기 글꼴과 모양이 다름. 글꼴 설치는 다운로드라 하지 않음.

## 결정과 가정
- 근무 종류와 "오늘·내일"은 서울 날짜 기준, 출퇴근 시각은 한국 근무 시간(일근 08:30–17:30, 야근 17:30–08:30)을 기기 시간대로 환산해 표시.
- 순환 일→야→조→휴, 기준일 2026-11-12 = 일근.
- 위젯은 디자인 목업이며 앱의 실제 기능이 아님.
- 토스 값은 제3자 정리 페이지(oppadu) 기준이며 공식 토큰 아님. 토스 글꼴은 미사용.

## 모르는 것·확인 안 한 것
- 아이폰 제어 센터 컨트롤의 크기별 치수, 큰 크기 구성, 값 글자 수 한도. HIG는 아이콘+제목+선택적 값이라고만 설명.
- 여러 줄 시간 표시 타일이 제어 센터에서 가능한지.
- 별도 리뷰어 검토, 실제 앱 화면과의 나란히 비교, reduced motion, 실기기 확인.
- 실제 설치 가능한 위젯은 네이티브 앱(안드로이드 앱 위젯, iOS WidgetKit/컨트롤)이 필요. 레포에는 manifest, 서비스 워커 없음.

## 다음 후보
1. ~~위젯·제어 센터 목업을 PNG로 내보내기~~ (완료, images/export/)
2. 레포에 웹 앱 manifest 추가(홈 화면에 추가용)
3. 네이티브 위젯 프로젝트 뼈대(Android, iOS)
4. 근무표 데모와 1x1·2x1 위젯을 토스 스타일로 통일

## Stageshow 다시 돌리는 법
npx skills add benjitaylor/skills --skill stageshow
cd <설치된 stageshow 폴더> && npm install && npm run dev
examples/ 폴더 안에 shiftwork-on, shiftwork-widget을 복사한 뒤
node scripts/film.mjs "http://127.0.0.1:<포트>/?capture=shiftwork-on" --select '.sw' --gate --out out.png

## 출처
- Apple HIG Controls: https://developer.apple.com/design/human-interface-guidelines/controls.md
- WWDC24 10157 정리: https://wwdcnotes.com/documentation/wwdc24-10157-extend-your-apps-controls-across-the-system/
- MacRumors iOS 18 제어 센터: https://www.macrumors.com/guide/ios-18-control-center
- 토스 값(제3자): https://www.oppadu.com/tools/design-systems-site/brand/toss.html , https://www.oppadu.com/tools/design-systems-site/brand/toss-dark-mode.html
- 이전 세션 원본 기록: /root/.claude/projects/-home-claude/33393938-2759-5a4e-a63b-7d4d3feb2612.jsonl (이 컨테이너에서만 접근 가능)
