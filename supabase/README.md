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
1. supabase.com에서 새 프로젝트를 만든다.
2. 대시보드의 SQL Editor에서 `migrations/20261112000000_init.sql` 내용을 붙여 넣고 실행한다. (Supabase CLI를 쓰면 `supabase db push`.)
3. Authentication → Providers에서 Email을 켠다. Authentication → URL Configuration의 Site URL과 Redirect URLs에 웹 앱 주소(예: Vercel 주소)를 넣는다.
   - 갤럭시 앱(WebView)과 아이폰 홈 화면 웹 앱에서는 메일 링크가 다른 브라우저로 열려 로그인이 그 안으로 들어오지 않는다. 그래서 앱은 메일의 숫자 코드를 직접 넣는 방식도 지원한다. Authentication → Email Templates의 Magic Link 양식에 `{{ .Token }}`을 넣어 코드가 메일에 보이게 한다(예: `로그인 코드: {{ .Token }}`).
4. Project Settings → API에서 Project URL과 anon(public) key를 복사해 웹 앱의 `config.js`에 넣는다. anon key는 공개용 키이고, 데이터는 RLS 정책이 지킨다. service_role key는 절대 앱에 넣지 않는다.

## 권한 시험
로컬 PostgreSQL에서 `tests/stub_supabase.sql`로 Supabase의 auth 스키마를 흉내 내고, `tests/rls_test.sql`로 주인·가족·남·위젯(anon) 권한을 시험한다.

```bash
PGHOST=<소켓 폴더> PGPORT=<포트> PGUSER=postgres bash supabase/tests/run.sh
```

2026-11-12에 PostgreSQL 16.15에서 24개 시험 모두 통과. 실제 Supabase 프로젝트에서는 아직 실행해 보지 않았다.

## 웹 앱까지 끝에서 끝까지 시험
`tests/e2e/`: 브라우저에는 supabase-js를 흉내 낸 `fake-supabase.js`를 넣고, 그 요청을 로컬 PostgreSQL에서 실제 RLS로 처리한다. 주인 로그인·저장·초대, 가족 참여(읽기 전용)·나가기, 위젯 브리지 전달, 서버 없는 모드를 시험한다.

```bash
PGHOST=<소켓 폴더> PGPORT=<포트> bash supabase/tests/e2e/run.sh
```

2026-11-12에 25개 모두 통과. 진짜 supabase-js와 실제 메일 발송은 시험하지 못했다(가짜 모듈은 코드 123456을 맞는 코드로 처리).
