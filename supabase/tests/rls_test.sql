-- 권한 시험: 주인 A, 가족 B, 남 C, 로그인 안 한 위젯(anon)
\set ON_ERROR_STOP 0
\set QUIET 1
insert into auth.users values ('aaaaaaaa-0000-0000-0000-000000000001'), ('bbbbbbbb-0000-0000-0000-000000000002'), ('cccccccc-0000-0000-0000-000000000003');

create temp table result (name text, ok boolean, detail text);
grant all on result to anon, authenticated;

-- A: 근무표 만들기, 바꾼 근무 넣기, 초대 코드
set role authenticated; set request.jwt.claim.sub = 'aaaaaaaa-0000-0000-0000-000000000001';
do $$ declare v uuid; begin
  insert into public.schedules (anchor_date, anchor_shift) values ('2026-11-12', '일') returning id into v;
  insert into result values ('A 근무표 만들고 바로 돌려받기(insert returning)', v is not null, coalesce(v::text, 'null'));
exception when others then insert into result values ('A 근무표 만들고 바로 돌려받기(insert returning)', false, sqlerrm); end $$;
insert into public.schedules (anchor_date, anchor_shift) select '2026-11-12', '일' where not exists (select 1 from public.schedules);
insert into public.overrides (schedule_id, day, shift) select id, '2026-11-15', '야' from public.schedules;
select create_invite() as code \gset
insert into result select 'A 코드 형식', :'code' ~ '^[A-HJ-NP-Z2-9]{6}$', :'code';
insert into result select 'A 근무표 1개 보임', count(*) = 1, count(*)::text from public.schedules;
select widget_token as token from public.schedules \gset
reset role;

-- C: 남은 못 봄, 틀린 코드 거절
set role authenticated; set request.jwt.claim.sub = 'cccccccc-0000-0000-0000-000000000003';
insert into result select 'C 근무표 안 보임', count(*) = 0, count(*)::text from public.schedules;
insert into result select 'C 바꾼 근무 안 보임', count(*) = 0, count(*)::text from public.overrides;
do $$ begin perform public.join_with_code('ZZZZZZ'); insert into result values ('C 틀린 코드 거절', false, 'joined');
exception when others then insert into result values ('C 틀린 코드 거절', true, sqlerrm); end $$;
do $$ begin insert into public.members (schedule_id, user_id) select id, auth.uid() from public.schedules;
insert into result values ('C 직접 가족 추가 막힘', false, 'inserted');
exception when others then insert into result values ('C 직접 가족 추가 막힘', true, sqlerrm); end $$;
reset role;

-- B: 코드로 참여 → 읽기만
set role authenticated; set request.jwt.claim.sub = 'bbbbbbbb-0000-0000-0000-000000000002';
insert into result select 'B 참여 전 안 보임', count(*) = 0, count(*)::text from public.schedules;
select join_with_code(lower(:'code')) is not null as joined \gset
insert into result select 'B 소문자 코드로 참여', :'joined', :'joined';
insert into result select 'B 참여 후 근무표 보임', count(*) = 1, count(*)::text from public.schedules;
insert into result select 'B 바꾼 근무 보임', count(*) = 1, string_agg(day || ' ' || shift, ',') from public.overrides;
with u as (update public.schedules set anchor_shift = '휴' returning 1) insert into result select 'B 근무표 수정 안 됨', count(*) = 0, count(*)::text || '줄 수정' from u;
do $$ begin insert into public.overrides (schedule_id, day, shift) select id, '2026-11-20', '휴' from public.schedules;
insert into result values ('B 바꾼 근무 추가 막힘', false, 'inserted');
exception when others then insert into result values ('B 바꾼 근무 추가 막힘', true, sqlerrm); end $$;
insert into result select 'B 초대 코드 안 보임', count(*) = 0, count(*)::text from public.invites;
do $$ begin perform public.create_invite(); insert into result values ('B 초대 코드 못 만듦', false, 'created');
exception when others then insert into result values ('B 초대 코드 못 만듦', true, sqlerrm); end $$;
reset role;

-- A: 가족 목록에 B가 보임, 바꾼 근무 삭제하면 updated_at 갱신(삭제 트리거)
set role authenticated; set request.jwt.claim.sub = 'aaaaaaaa-0000-0000-0000-000000000001';
insert into result select 'A 가족 목록에 B', count(*) = 1, count(*)::text from public.members;
select updated_at as before_ts from public.schedules \gset
select pg_sleep(0.01);
do $$ begin delete from public.overrides where day = '2026-11-15';
insert into result values ('A 바꾼 근무 삭제', true, 'ok');
exception when others then insert into result values ('A 바꾼 근무 삭제', false, sqlerrm); end $$;
insert into result select 'A 삭제 후 updated_at 갱신', updated_at > :'before_ts'::timestamptz, updated_at::text from public.schedules;
insert into public.overrides (schedule_id, day, shift) select id, '2026-11-16', '휴' from public.schedules;
reset role;

-- anon(위젯): 표는 못 읽고, 토큰으로 함수만
set role anon; set request.jwt.claim.sub = '';
do $$ begin perform count(*) from public.schedules; insert into result values ('anon 표 직접 읽기 막힘', false, 'read');
exception when others then insert into result values ('anon 표 직접 읽기 막힘', true, sqlerrm); end $$;
insert into result select 'anon 토큰으로 위젯 데이터', (widget_schedule(:'token') ->> 'anchor_shift') = '일', widget_schedule(:'token')::text;
insert into result select 'anon 틀린 토큰은 빈 값', widget_schedule(gen_random_uuid()) is null, coalesce(widget_schedule(gen_random_uuid())::text, 'null');
do $$ begin perform public.join_with_code('ABCDEF'); insert into result values ('anon 참여 막힘', false, 'joined');
exception when others then insert into result values ('anon 참여 막힘', true, sqlerrm); end $$;
reset role;

-- 만료된 코드: 코드를 미리 꺼내 두고 C가 직접 넣는다
update public.invites set expires_at = now() - interval '1 second';
select code as expired_code from public.invites limit 1 \gset
set role authenticated; set request.jwt.claim.sub = 'cccccccc-0000-0000-0000-000000000003';
select set_config('test.code', :'expired_code', false);
do $$ begin perform public.join_with_code(current_setting('test.code'));
  insert into result values ('C 만료 코드 거절', false, 'joined');
exception when others then insert into result values ('C 만료 코드 거절', sqlerrm = 'invalid or expired code', sqlerrm); end $$;
reset role;

-- B 나가기
set role authenticated; set request.jwt.claim.sub = 'bbbbbbbb-0000-0000-0000-000000000002';
delete from public.members where user_id = auth.uid();
insert into result select 'B 나간 뒤 안 보임', count(*) = 0, count(*)::text from public.schedules;
reset role;

\unset QUIET
select case when ok then 'PASS' else 'FAIL' end as "결과", name as "시험", left(detail, 90) as "내용" from result;
select count(*) filter (where ok) || ' / ' || count(*) as "통과" from result;
