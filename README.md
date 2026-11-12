# 교대온 — 교대근무표

오늘·내일 근무를 한눈에 보고, 가족과 근무표를 나누는 교대근무 앱입니다.

- **아이폰·PC**: 웹 앱(PWA). Safari에서 열고 "홈 화면에 추가"하면 앱처럼 쓴다. 인터넷이 없어도 열린다.
- **갤럭시**: 안드로이드 앱 + 홈 화면 위젯 4x1·2x1·1x1 (`android/`).
- **서버(선택)**: Supabase. 로그인, 근무표 저장, 가족 초대(가족은 읽기만) (`supabase/`).

서버를 설정하지 않아도 앱은 그 기기 안에서 그대로 동작합니다.

## 근무 규칙
- 반복 주기: 일 → 야 → 조 → 휴
- 일근: 한국시간 08:30–17:30
- 야근: 한국시간 17:30 시작, 다음 날(조근 날) 08:30 퇴근까지 이어지는 한 근무
- 근무가 끝나면 "퇴근 후"와 다음 날 근무를 보여 준다
- 날짜와 순환 판정은 한국시간(Asia/Seoul), 시각은 기기 시간대로 바꿔 보여 준다

## 처음 설정하는 순서 (모두 무료 요금제 기준)

### 1. Supabase (가족 공유를 쓸 때)
`supabase/README.md`를 따른다. 요약:
1. supabase.com에서 프로젝트를 만들고, SQL Editor에서 `supabase/migrations/20261112000000_init.sql`을 실행한다.
2. Authentication에서 Email을 켜고, Email Templates의 Magic Link 양식에 `{{ .Token }}`을 넣는다(앱 안에서 메일 코드로 로그인하기 위해).
3. Project Settings → API의 Project URL과 anon key를 `config.js`에 넣는다. service_role key는 넣지 않는다.

### 2. Vercel (웹 앱 공개)
1. vercel.com에서 이 GitHub 레포를 Import한다.
2. Framework Preset은 Other, Build Command는 비우고, Output Directory는 레포 루트(`.`)로 둔다. `vercel.json`과 `.vercelignore`가 들어 있다.
3. 배포할 브랜치를 고른다(지금 작업은 `claude/design-handoff` 브랜치에 있다).
4. 나온 주소를 Supabase의 Authentication → URL Configuration(Site URL, Redirect URLs)에 넣는다.

### 3. 아이폰에 설치
Safari로 웹 앱 주소를 열고 공유 버튼 → "홈 화면에 추가". 설정 탭에서 이메일로 로그인한다(메일의 코드를 넣는다).

### 4. 갤럭시에 설치
`android/README.md`를 따른다. Android Studio에서 APK를 만들어 가족 폰에 보내 설치하고, 홈 화면을 길게 눌러 교대온 위젯을 놓는다. 웹 앱 주소로 열려면 `-PwebAppUrl=<Vercel 주소>`로 빌드한다.

### 가족 초대
근무표 주인이 가족 탭에서 "초대 코드 만들기"(7일 유효) → 가족은 로그인 후 가족 탭의 "초대 코드 입력"에 넣는다. 가족은 근무표를 볼 수만 있고, 갤럭시 위젯도 주인의 근무표를 보여 준다.

## 파일
- `index.html` — 앱 화면(홈, 근무표, 가족, 설정)
- `sync.js` — 서버 연결과 안드로이드 위젯 전달
- `config.js` — Supabase 주소·anon key(비우면 기기 저장)
- `manifest.webmanifest`, `sw.js`, `icons/` — 홈 화면 설치와 오프라인
- `vercel.json`, `.vercelignore` — Vercel 배포 설정
- `supabase/` — 데이터베이스 SQL과 시험
- `android/` — 안드로이드 앱과 위젯
- `design/` — 디자인 목업(위젯, 제어 센터, Stageshow 데모)과 인계 문서

## 시험
- 권한 SQL: `PGHOST=… PGPORT=… PGUSER=postgres bash supabase/tests/run.sh` (24개)
- 웹 앱 + 서버 끝에서 끝까지: `PGHOST=… PGPORT=… bash supabase/tests/e2e/run.sh` (25개, 가짜 supabase-js + 로컬 PostgreSQL)
- 위젯 근무 계산: `android/README.md` (30개, 웹 앱 결과와 대조)

## 한계
- 아이폰 위젯·제어 센터 컨트롤은 웹 앱으로 만들 수 없어 넣지 않았다(목업만 `design/`에 있음). 애플 개발자 프로그램(연 $99)이 필요하다.
- 안드로이드 앱은 이 저장소를 만든 환경에서 빌드해 보지 못했다(안드로이드 SDK 없음). 처음 빌드 때 오류가 날 수 있다.
- 실제 Supabase 프로젝트와 실제 메일 발송, 실기기는 아직 시험하지 않았다.
