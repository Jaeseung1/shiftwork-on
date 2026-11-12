# Supabase

교대온의 근무표·가족 공유 데이터베이스입니다.

## 저장하는 것
- `schedules`: 내 근무표 하나(기준일, 그날 근무, 위젯 토큰). 사용자당 하나.
- `overrides`: 날짜별로 바꾼 근무. 나머지 날짜는 순환(일 → 야 → 조 → 휴)으로 계산.
- `members`: 초대 코드로 들어온 가족. 읽기만 가능.
- `invites`: 초대 코드(6자리, 7일 유효).

## 함수
- `create_invite()`: 초대 코드 만들기(근무표 주인만).
- `join_with_code(code)`: 코드로 가족 참여.
- `widget_schedule(token)`: 갤럭시 위젯용. 로그인 없이 토큰으로 근무표를 읽기만 함(오늘 기준 7일 전~60일 뒤의 바꾼 근무).

## 설치
교대온 프로젝트: `shiftwork-on`(ref `rsmuvlythhcgrfnunnrn`, 서울 ap-northeast-2). 2026-11-12에 Supabase 커넥터로 만들고 `migrations/`의 두 파일을 적용했다. 웹 앱 `config.js`에 주소와 Publishable key가 들어 있다.

새로 만들 때:
1. 프로젝트를 만들고 `migrations/`의 SQL을 이름 순서대로 실행한다(SQL Editor 또는 `supabase db push`).
2. **Authentication → Sign In / Providers**에서 **Allow anonymous sign-ins**를 켠다. 가족은 메일 없이 초대 코드만으로 들어온다(익명 로그인 후 `join_with_code`). Supabase 기본 메일은 조직 팀원 주소로만 보내기 때문이다("Other addresses fail with the error code `email_address_not_authorized`", Supabase 문서 Test auth flows).
3. **Authentication → Email Templates**의 **Magic Link**(처음 로그인하는 주소라면 **Confirm sign up**도)에 `{{ .Token }}`을 넣는다. 예: `<p>교대온 로그인 코드: {{ .Token }}</p>`. 앱은 메일의 숫자 코드를 직접 넣는다(WebView·홈 화면 웹 앱에서는 메일 링크가 다른 브라우저로 열리기 때문).
4. **Authentication → URL Configuration**의 Site URL에 웹 앱 주소(Vercel)를 넣는다.
5. **Settings → API Keys**의 Project URL과 Publishable key(`sb_publishable_…`)를 `config.js`에 넣는다. Secret key는 절대 넣지 않는다. 옛 anon JWT 키는 "work until the end of 2026"(Supabase 문서)이라 쓰지 않는다. 새 키는 `Authorization: Bearer`가 아니라 `apikey` 헤더로 보낸다(안드로이드 위젯이 그렇게 함).

## 알아 둘 점
- 근무표 주인의 메일 로그인은 Supabase 조직 팀원 주소여야 기본 메일로 코드가 온다. 다른 주소로 로그인하려면 별도 SMTP가 필요하다(Supabase 문서 "Send emails with custom SMTP").
- 기본 메일은 시간당 보낼 수 있는 수가 적다(값은 문서에 변수로만 나와 확인 못 함).
- 익명 로그인은 공개 키만 있으면 누구나 계정을 만들 수 있다. 근무표·초대 코드는 RLS로 지켜지지만, 남용이 보이면 CAPTCHA를 켠다.
- Supabase 보안 점검(2026-11-12): 남은 경고 4건은 앱이 일부러 부르는 함수(`create_invite`, `join_with_code`, `widget_schedule`)다. 권한 규칙 도우미는 `private` 스키마로 옮겨 API에 드러나지 않는다.

## 권한 시험
로컬 PostgreSQL에서 `tests/stub_supabase.sql`로 Supabase의 auth 스키마를 흉내 내고, `tests/rls_test.sql`로 주인·가족·남·위젯(anon) 권한을 시험한다.

```bash
PGHOST=<소켓 폴더> PGPORT=<포트> PGUSER=postgres bash supabase/tests/run.sh
```

2026-11-12에 PostgreSQL 16.15에서 24개 시험 모두 통과. 실제 Supabase 프로젝트에서도 같은 내용의 핵심 시험 15개를 트랜잭션 안에서 돌려 통과했고(시험 데이터는 되돌림), 공개 키로 REST를 직접 불러 표 직접 읽기·초대 함수가 막히고 위젯 함수만 열리는 것을 확인했다.

## 웹 앱까지 끝에서 끝까지 시험
`tests/e2e/`: 브라우저에는 supabase-js를 흉내 낸 `fake-supabase.js`를 넣고, 그 요청을 로컬 PostgreSQL에서 실제 RLS로 처리한다. 주인 로그인·저장·초대, 가족 참여(읽기 전용)·나가기, 위젯 브리지 전달, 서버 없는 모드를 시험한다.

```bash
PGHOST=<소켓 폴더> PGPORT=<포트> bash supabase/tests/e2e/run.sh
```

2026-11-12에 25개 모두 통과(가짜 모듈은 코드 123456을 맞는 코드로 처리). 별도로 진짜 supabase-js 2.118.0 + 실제 프로젝트로 브라우저에서 접속해 모듈 로드와 서버 응답을 확인했다. 실제 메일 발송은 아직.
