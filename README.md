# 교대온 — 교대근무표

오늘·내일 근무를 한눈에 보고, 다른 사람과 근무표를 나누는 교대근무 앱입니다.

- **아이폰·PC**: 웹 앱(PWA). Safari에서 열고 "홈 화면에 추가"하면 앱처럼 쓴다. 인터넷이 없어도 열린다. 아이폰 제어 센터에는 단축어로 오늘·내일 근무 버튼을 둔다.
- **갤럭시**: 안드로이드 앱 + 홈 화면 위젯 4x1·2x1·1x1 (`android/`).
- **서버(선택)**: Supabase. 로그인, 근무표 저장, 초대 코드 공유(공유받는 사람은 읽기만) (`supabase/`).

서버를 설정하지 않아도 앱은 그 기기 안에서 그대로 동작합니다.

## 근무 규칙
- 반복 주기: 일 → 야 → 조 → 휴
- 일근: 한국시간 08:30–17:30
- 야근: 한국시간 17:30 시작, 다음 날(조근 날) 08:30 퇴근까지 이어지는 한 근무
- 근무가 끝나면 "퇴근 후"와 다음 날 근무를 보여 준다
- 날짜와 순환 판정은 한국시간(Asia/Seoul), 시각은 기기 시간대로 바꿔 보여 준다

## 처음 설정하는 순서 (모두 무료 요금제 기준)

### 1. Supabase (공유를 쓸 때)
`supabase/README.md`를 따른다(이 레포의 프로젝트는 이미 만들어 적용해 두었다). 요약:
1. 프로젝트를 만들고 `supabase/migrations/`의 SQL을 이름 순서대로 실행한다.
2. Authentication에서 Email(이메일+비밀번호)과 익명 로그인을 켠다. 공유받는 사람은 메일 없이 초대 코드만 넣는다.
3. Project URL과 Publishable key를 `config.js`에 넣는다. secret·service_role key는 넣지 않는다.

### 2. Vercel (웹 앱 공개)
1. vercel.com에서 이 GitHub 레포를 Import한다.
2. Framework Preset은 Other, Build Command는 비우고, Output Directory는 레포 루트(`.`)로 둔다. `vercel.json`과 `.vercelignore`가 들어 있다.
3. 배포할 브랜치를 고른다(지금 작업은 `claude/design-handoff` 브랜치에 있다).
4. 나온 주소를 Supabase의 Authentication → URL Configuration(Site URL, Redirect URLs)에 넣는다.

### 3. 아이폰에 설치
1. Safari로 웹 앱 주소(Vercel)를 열고 공유 버튼 → "홈 화면에 추가".
2. 홈 화면의 교대온을 열고 설정 탭에서 이메일+비밀번호로 로그인한다(공유받는 사람은 공유 탭에 초대 코드만 넣는다).
3. 잠금 화면·홈 화면 위젯(늘 보임): 무료 앱 Scriptable을 받고, 설정 탭 "아이폰 잠금 화면 위젯"의 "위젯 코드 복사" → Scriptable에서 + → 붙여 넣기(제목 "교대온") → 잠금 화면을 길게 눌러 사용자화 → 위젯 칸에 Scriptable 직사각형 → 위젯을 눌러 Script "교대온". 코드는 `ios/widget.js`(서버에서 `widget_schedule`로 근무표 기준만 받고 계산은 폰에서, 인터넷이 없으면 마지막 근무표로). 시험: `node ios/test.mjs`(21개, Scriptable을 흉내 내 돌림). 실제 아이폰에서는 아직 안 돌려 봤다.
4. 제어 센터 버튼(iOS 18 이상, 누르면 알림): 설정 탭 "아이폰 제어 센터"의 "단축어 주소 복사" → 단축어 앱에서 "URL 콘텐츠 가져오기"(주소 붙여 넣기) → "사전 값 가져오기"(키 `today`·`tomorrow`·`text`) → "알림 보기" → 제어 센터 편집에서 "제어 항목 추가" → "단축어"로 그 단축어를 고른다. 주소는 `share_summary_json`을 부르며 토큰이 들어 있어 읽기 전용 열쇠 역할을 한다.

### 4. 갤럭시에 설치
`android/README.md`를 따른다. 정식 서명 APK(`shiftwork-on-0.3.apk` 등)를 폰에 보내 설치하고, 홈 화면을 길게 눌러 교대온 위젯을 놓는다. 갤럭시 앱은 웹 파일을 앱 안에 넣은 채로 쓰고, 업데이트는 같은 키로 서명한 새 APK를 덮어 설치한다.

### 공유(초대 코드)
근무표 주인이 공유 탭에서 "초대 코드 만들기"(7일 유효) → 받는 사람은 로그인 없이 공유 탭의 "초대 코드 입력"에 넣는다. 받는 사람은 근무표를 볼 수만 있고, 갤럭시 위젯·아이폰 단축어도 주인의 근무표를 보여 준다.

## 파일
- `index.html` — 앱 화면(홈, 근무표, 공유, 설정)
- `sync.js` — 서버 연결과 안드로이드 위젯 전달
- `config.js` — Supabase 주소·anon key(비우면 기기 저장)
- `manifest.webmanifest`, `sw.js`, `icons/` — 홈 화면 설치와 오프라인
- `vercel.json`, `.vercelignore` — Vercel 배포 설정
- `supabase/` — 데이터베이스 SQL과 시험
- `android/` — 안드로이드 앱과 위젯
- `design/` — 디자인 목업(위젯, 제어 센터, Stageshow 데모)과 인계 문서

## 시험
- 권한 SQL: `PGHOST=… PGPORT=… PGUSER=postgres bash supabase/tests/run.sh` (권한 24개 + 단축어 함수 11개)
- 웹 앱 + 서버 끝에서 끝까지: `PGHOST=… PGPORT=… bash supabase/tests/e2e/run.sh` (34개, 가짜 supabase-js + 로컬 PostgreSQL)
- 위젯: `android/README.md` (근무 계산 30개를 웹 앱 결과와 대조 + 위젯 그림 시험 10개)

## 한계
- 제어 센터 버튼에 근무 글자가 늘 보이게 하려면 네이티브 앱(WidgetKit ControlWidget)이 필요하다. Mac·Xcode와 애플 개발자 프로그램(연 $99)이 필요해, 대신 잠금 화면 위젯(Scriptable)과 단축어 버튼(누르면 알림)을 쓴다.
- 아이폰 실기기에서 단축어·제어 센터는 아직 시험하지 않았다. 갤럭시 위젯도 실기기 화면은 아직 못 봤다(그림 시험만).
