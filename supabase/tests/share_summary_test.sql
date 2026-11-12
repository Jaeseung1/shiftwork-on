-- share_summary 결과가 웹 앱(index.html rangeInZone, 근무 순환)과 같은지 시험. run.sh가 rls_test.sql 다음에 돌린다.
\set QUIET 1
insert into auth.users values ('dddddddd-0000-0000-0000-00000000000d') on conflict do nothing;
insert into public.schedules (id, owner_id, anchor_date, anchor_shift, widget_token)
values ('dddddddd-0000-0000-0000-0000000000aa', 'dddddddd-0000-0000-0000-00000000000d', '2026-11-12', '일', 'dddddddd-0000-0000-0000-0000000000ff');
insert into public.overrides values ('dddddddd-0000-0000-0000-0000000000aa', '2026-11-15', '야');
create temp table ss (name text, want text, got text);
grant all on ss to anon;
insert into ss values
 ('서울 11/12 13:00', E'오늘 12일(목) 일근 08:30–17:30\n내일 13일(금) 야근 11/13 17:30–11/14 08:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Asia/Seoul', '2026-11-12 13:00+09')),
 ('토론토 11/12 13:00(서울)', E'오늘 12일(목) 일근 11/11 18:30–11/12 03:30\n내일 13일(금) 야근 11/13 03:30–11/13 18:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'America/Toronto', '2026-11-12 13:00+09')),
 ('서울 11/14 조근 다음 날 바꾼 근무', E'오늘 14일(토) 조근 11/13 17:30–11/14 08:30\n내일 15일(일) 야근 11/15 17:30–11/16 08:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Asia/Seoul', '2026-11-14 10:00+09')),
 ('서울 11/16 휴무 다음(순환)', E'오늘 16일(월) 일근 08:30–17:30\n내일 17일(화) 야근 11/17 17:30–11/18 08:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Asia/Seoul', '2026-11-16 09:00+09')),
 ('기준일 이전 11/11', E'오늘 11일(수) 휴무\n내일 12일(목) 일근 08:30–17:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Asia/Seoul', '2026-11-11 12:00+09')),
 ('없는 시간대는 서울로', E'오늘 12일(목) 일근 08:30–17:30\n내일 13일(금) 야근 11/13 17:30–11/14 08:30', private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Mars/Base', '2026-11-12 13:00+09')),
 ('틀린 토큰은 빈 값', null, private.share_summary_at(gen_random_uuid(), 'Asia/Seoul', now()));
set role anon;
insert into ss select 'anon 공개 함수 호출', 'ok', case when public.share_summary('dddddddd-0000-0000-0000-0000000000ff') ->> 'today' like '오늘 %' then 'ok' else 'fail' end;
do $$ begin perform private.share_summary_at('dddddddd-0000-0000-0000-0000000000ff', 'Asia/Seoul', now()); insert into ss values ('anon 내부 함수 막힘', 'blocked', 'ran');
exception when others then insert into ss values ('anon 내부 함수 막힘', 'blocked', 'blocked'); end $$;
reset role;
\unset QUIET
select case when want is not distinct from got then 'PASS' else 'FAIL' end as "결과", name as "시험", replace(coalesce(got, 'null'), E'\n', ' / ') as "결과값" from ss;
select count(*) filter (where want is not distinct from got) || ' / ' || count(*) as "통과" from ss;
