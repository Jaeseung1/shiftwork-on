-- 단축어에서 꺼내기 쉽게 share_summary를 {"text": "두 줄", "today": "…", "tomorrow": "…"}로 돌려준다.
-- (글자 그대로 돌려주면 PostgREST가 JSON 문자열 "…\n…"으로 감싸 알림에 따옴표와 \n이 보인다.)
drop function public.share_summary(uuid, text);

create function public.share_summary(p_token uuid, p_tz text default 'Asia/Seoul') returns json
language sql stable security definer set search_path = '' as $$
  select case when t is null then null else json_build_object('text', t, 'today', split_part(t, E'\n', 1), 'tomorrow', split_part(t, E'\n', 2)) end
  from (select private.share_summary_at(p_token, p_tz, now()) as t) x;
$$;

revoke execute on function public.share_summary(uuid, text) from public, anon, authenticated;
grant execute on function public.share_summary(uuid, text) to anon, authenticated;
