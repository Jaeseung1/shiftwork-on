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
3. Authentication → Providers에서 Email을 켠다(이메일 로그인 링크·코드). Authentication → URL Configuration의 Site URL과 Redirect URLs에 웹 앱 주소(예: Vercel 주소)를 넣는다.
4. Project Settings → API에서 Project URL과 anon(public) key를 복사해 웹 앱의 `config.js`에 넣는다. anon key는 공개용 키이고, 데이터는 RLS 정책이 지킨다. service_role key는 절대 앱에 넣지 않는다.

## 권한 시험
로컬 PostgreSQL에서 `tests/stub_supabase.sql`로 Supabase의 auth 스키마를 흉내 내고, `tests/rls_test.sql`로 주인·가족·남·위젯(anon) 권한을 시험한다.

```bash
PGHOST=<소켓 폴더> PGPORT=<포트> PGUSER=postgres bash supabase/tests/run.sh
```

2026-11-12에 PostgreSQL 16.15에서 23개 시험 모두 통과. 실제 Supabase 프로젝트에서는 아직 실행해 보지 않았다.
