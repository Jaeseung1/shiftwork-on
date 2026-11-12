# 교대온 홈페이지 데모·위젯 작업 인계 (2026-11-12)

## 새 Claude Code 세션에 붙여 넣을 말
레포의 claude/design-handoff 브랜치 design/HANDOFF.md를 읽고 이어서 작업해 줘. 한국어로 답해 줘. 모르는 건 모른다고 하고, 사실 주장에는 출처를 달아 줘. 내 허락 없이 다운로드는 시작하지 마.

## 목표
내 서비스 교대온(github.com/Jaeseung1/shiftwork-on, 교대 근무 달력 웹 앱)의 홈페이지 첫 화면용 데모(720×340)와 폰 위젯 목업을 Stageshow 스킬(benjitaylor/skills)로 만든다.

## 지금 상태
- examples/shiftwork-on: 근무표 화면 데모(15일 휴무 → 야근 선택 → 저장 → 초기화 루프). v3 토스 스타일(2026-11-12, 후보 4). 다크 원본은 index.dark.tsx, shiftwork.dark.css. 게이트는 정지·루프 1280, 390 모두 rehearsal passed, 커버리지는 inconclusive. REHEARSAL.md에 한계 기록.
  - 주의: 실제 앱(index.html)은 아직 초록(#087f70) 토큰이라 데모와 색이 다름. 앱도 토스 색으로 바꿀지는 미정.
- examples/shiftwork-widget: 갤럭시 4x1 위젯 Stageshow 정지 이미지(다크, 이전 디자인, 더 이상 기준 아님). 최신 디자인은 pages/widget.html.
- pages/widget.html: 위젯 토스 스타일 목업(라이트/다크 토글).
  - 4x1: 오늘(큰 파란 글씨), 내일, 앞으로 5일.
  - 2x1 (2026-11-12 추가): 왼쪽 오늘 + "근무 중" + 퇴근 시각, 오른쪽 회색 안쪽 카드에 내일.
  - 1x1 (2026-11-12 추가): 오늘 + "근무 중" + 퇴근 시각.
  - "근무 중"은 서울 시간 10:00에 본 상태 가정. 2x1·1x1은 보기 좋게 4x1보다 크게 그림(실제 비율 아님). 갤럭시 위젯 실제 치수는 미확인.
- pages/cc.html (2026-11-12 다시 정리): 토스 스타일 페이지(라이트/다크 토글)에 Stageshow로 찍은 제어 센터 이미지 한 장 + 원칙 세 줄 + 근거·한계 카드.
  - 제어 센터 안: 맨 윗줄에 교대온 중간 크기 컨트롤 2개(해 "오늘 근무/일근", 달 "내일 근무/야근"). HIG의 "symbol image, a title, and, optionally, a value" 구조를 따름. 심볼만 근무 색 틴트.
  - 이전의 한 타일 반반(오늘|내일) 구성과 여러 줄 시간 표시 타일은 뺐음. 출퇴근 시각(기기 시간대 환산)은 위젯·앱에서 보여 주기로 함.
- examples/shiftwork-cc: 위 이미지의 Stageshow 예시(index.tsx, cc.css, REHEARSAL.md). 게이트 1280·390 정지 rehearsal passed.
- 결정(2026-11-12): 제어 센터는 컨트롤 두 개 안으로 확정. 한 개짜리 안(제목 "오늘 일근", 값 "내일 야근")은 HIG의 제목·값 용도와 어긋나고 내일 근무에 심볼이 없어 채택 안 함. 코드·이미지는 커밋 6c7aae3에 있음.
- images/widget-4x1-v2.png: 이전 다크 위젯 이미지(기록용). 최신 결과는 images/export/.
- images/export/ (2026-11-12 추가, 후보 1 완료): pages/의 목업을 PNG로 내보냄. 2배율.
  - widget-{4x1,2x1,1x1}-{light,dark}.png: 위젯만, 모서리 투명(RGBA), 그림자 제외.
  - widget-wall-{light,dark}.png: 세 크기를 홈 화면 배경과 함께.
  - sw-demo-wide.png(720×340), sw-demo-compact.png(358×440): 근무표 데모 대표 장면(poster).
  - cc-wide-light.png, cc-wide-dark.png (720×480), cc-compact-light.png, cc-compact-dark.png (358×460): 제어 센터. Stageshow 스테이지(.swcc .ss-stage)만 잘라 찍음.
  - 다시 만들기: 레포 루트에서 `node design/scripts/export-mockups.js` (컨테이너에 사전 설치된 Playwright 사용).
  - 한계: 이 컨테이너에는 Noto Sans KR이 없어 한글이 WenQuanYi Zen Hei로 렌더링됨(fc-list로 확인). 실제 기기 글꼴과 모양이 다름. 글꼴 설치는 다운로드라 하지 않음.

- 앱 index.html v0.2 (2026-11-12): 위젯·제어 센터·근무표 데모와 같은 토스 스타일로 다시 씀(라이트 기본, 기기 다크 모드 따라감). 화면 이미지는 design/images/app/ (홈·근무표·가족·설정 × 라이트·다크, 서울 11/12 13:00 기준).
  - UX 변경: 숨어 있던 홈을 첫 화면·첫 탭으로, 홈에 지금 상태(실제 근무 진행률), 오늘·내일, 앞으로 5일(누르면 근무표에서 그 날짜가 열림), 가족 공유 바로가기. 근무 고르기는 드롭다운 대신 세그먼트 버튼(누르면 시간 요약 미리보기). 달력 칸은 버튼(키보드·화면 읽기). 아이콘은 lucide(ISC) SVG.
  - 상태 문구 로직은 그대로(18개 시각 비교로 확인). 진행표시줄은 원래 68%/22% 고정값이었음.
  - v0.3 (같은 날): Stageshow 규칙으로 다시 점검. 홈을 위젯 4x1 순서(오늘 크게 → 내일 → 앞으로 5일)로 합치고 겹치는 정보(근무 중·퇴근 시각·일근 반복) 제거, 근무 중에는 남은 시간 표시. "교" 글자 아이콘은 사용자 요청으로 제거, 글자 로고 "교대온"만.
  - Stageshow 검사: 실제 앱 4개 화면에 scripts/layout.mjs(390 폭) no-issues-detected. 앱 홈 목업 examples/shiftwork-home(film.mjs --still --gate 1280·390 × 라이트·다크 passed), 이미지 images/export/app-home-{wide,compact}-{light,dark}.png.

## 결정과 가정
- 2026-11-12 사용자 위임으로 정한 것: 한글 글꼴은 다운로드하지 않음, 작업은 claude/design-handoff에 계속 쌓고 PR은 만들지 않음, 제어 센터 컨트롤에 출퇴근 시각은 넣지 않음, MacRumors 접속 허용은 요청하지 않음.
- 근무 종류와 "오늘·내일"은 서울 날짜 기준, 출퇴근 시각은 한국 근무 시간(일근 08:30–17:30, 야근 17:30–08:30)을 기기 시간대로 환산해 표시.
- 야근·조근(2026-11-12 사용자 확인): 구분상 야근 17:30–24:00, 조근 00:00–08:30이지만 실제로는 이어지는 한 근무. 앱과 목업은 야근 날·조근 날 모두 17:30–다음 날 08:30 한 구간으로 보여 주고, 야근 중 퇴근 안내는 "내일 08:30 퇴근". 하루 단위로 쪼개 보여 주는 안은 시도 후 되돌림(커밋 ed6d60d와 그 되돌림).
- 근무가 끝난 뒤(일근 17:30 이후, 조근 08:30 이후)는 "퇴근 후"와 다음 날 근무를 보여 줌(2026-11-12 사용자 요청).
- 순환 일→야→조→휴, 기준일 2026-11-12 = 일근.
- 위젯은 디자인 목업이며 앱의 실제 기능이 아님.
- 토스 값은 제3자 정리 페이지(oppadu) 기준이며 공식 토큰 아님. 토스 글꼴은 미사용.

## 모르는 것·확인 안 한 것
- 아이폰 제어 센터 컨트롤의 크기별 치수, 큰 크기 구성, 값 글자 수 한도. HIG는 아이콘+제목+선택적 값이라고만 설명.
- 버튼 컨트롤 심볼에 고정 색을 줄 수 있는지. HIG는 틴트가 "a control toggle’s symbol in its on state"에 적용된다고만 씀.
- iOS 26 제어 센터의 실제 재질·치수. HIG 이미지는 저장 위치를 못 찾아 보지 못했고, MacRumors는 이 세션 네트워크에서 막혀 읽지 못함.
- 여러 줄 시간 표시 타일이 제어 센터에서 가능한지.
- 별도 리뷰어 검토, 실제 앱 화면과의 나란히 비교, reduced motion, 실기기 확인.
- 실제 설치 가능한 위젯은 네이티브 앱(안드로이드 앱 위젯, iOS WidgetKit/컨트롤)이 필요. 레포에는 manifest, 서비스 워커 없음.

## 실제 앱 개발 (2026-11-12, 사용자 결정: 무료 구성)
- 아이폰은 웹 앱(PWA), 갤럭시는 안드로이드 앱 + 위젯, 서버는 Supabase, 웹 공개는 Vercel. 애플 연회비가 없어 아이폰 위젯·제어 센터는 보류.
- 1 Supabase: supabase/ (스키마·RLS·함수, 로컬 PostgreSQL 권한 시험 24개). 시험 중 insert…returning이 RLS에 막히는 버그를 찾아 고침.
- 2 웹 앱 연결: config.js, sync.js (이메일 코드 로그인, 저장, 초대, 가족 읽기 전용, 위젯 전달). 끝에서 끝까지 시험 25개.
- 3 안드로이드: android/ (WebView + ShiftBridge + RefreshWorker + 위젯 3종, ShiftCalc 30개 시험). 안드로이드 SDK가 없어 실제 빌드는 못 함.
- 4 PWA·배포: manifest.webmanifest, sw.js, icons/, vercel.json, .vercelignore. 오프라인 새로고침 확인.
- 남은 일: 사용자가 Supabase 프로젝트·Vercel 계정 만들기, config.js 채우기, Android Studio로 APK 빌드·실기기 확인.

## 다음 세션에서 이어 할 일 (2026-11-12 기준)
- 안드로이드: 이 세션 컨테이너에서 빌드 성공(android/README.md). 새 세션은 컨테이너가 새로 만들어져 Android SDK가 없으므로, 환경 `shiftwork-on`(dl.google.com, gcp.mirrors.maven.org 허용)을 고르고 SDK를 다시 받아야 함(cmdline-tools 11076708 → platforms;android-34, build-tools;34.0.0).
- Supabase(2026-11-12 진행): 프로젝트 shiftwork-on(rsmuvlythhcgrfnunnrn, 서울) 생성, 마이그레이션 2개 적용, 실제 DB 권한 시험 통과, config.js에 Publishable key. 가족은 익명 로그인 + 초대 코드(기본 메일은 팀원 주소로만 가서). 남은 것: 사용자가 대시보드에서 익명 로그인 켜기, Magic Link 양식에 {{ .Token }}, 실제 메일 로그인 시험(사용자 허락 시 Gmail 커넥터로 코드 확인).
- Vercel: 커넥터 연결 또는 vercel.com/new에서 레포 Import(브랜치 claude/design-handoff, Framework Other). 배포 주소를 Supabase URL Configuration에 넣기.
- 실기기: 사용자가 APK(./gradlew assembleDebug) 설치 후 위젯 확인. 위젯 칸 크기는 어림값이라 실제 화면을 보고 조정.

## 다음 후보
1. ~~위젯·제어 센터 목업을 PNG로 내보내기~~ (완료, images/export/)
2. 레포에 웹 앱 manifest 추가(홈 화면에 추가용)
3. 네이티브 위젯 프로젝트 뼈대(Android, iOS)
4. ~~근무표 데모와 1x1·2x1 위젯을 토스 스타일로 통일~~ (완료)

## Stageshow 다시 돌리는 법
(이 세션에서는 /home/user/.agents/skills/stageshow 에 v1.1.1 설치. 제어 센터: examples/에 shiftwork-cc 복사 후 `?capture=shiftwork-cc`, `--still --gate`)
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
