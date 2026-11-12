-- 아이폰 단축어에서 오늘·내일을 따로 꺼내 쓰도록 JSON으로도 돌려준다(글 두 줄인 share_summary는 그대로 둔다).
-- 예) {"text": "오늘 13일(금) 휴무\n내일 14일(토) 일근 08:30–17:30", "today": "오늘 13일(금) 휴무", "tomorrow": "내일 14일(토) 일근 08:30–17:30"}
-- 단축어: URL 내용 가져오기 → 사전 값 가져오기(today 또는 tomorrow).
-- GET /rest/v1/rpc/share_summary_json?p_token=…&p_tz=…&apikey=… 로 부를 수 있게 stable로 둔다. 틀린 토큰이면 null.
-- 이미 있는 함수를 지우거나 바꾸지 않고 새 함수만 더한다.

create function public.share_summary_json(p_token uuid, p_tz text default 'Asia/Seoul') returns json
language sql stable security definer set search_path = '' as $$
  select case when t is null then null
    else json_build_object('text', t, 'today', split_part(t, E'\n', 1), 'tomorrow', split_part(t, E'\n', 2)) end
  from (select private.share_summary_at(p_token, p_tz, now()) as t) x;
$$;

revoke execute on function public.share_summary_json(uuid, text) from public, anon, authenticated;
grant execute on function public.share_summary_json(uuid, text) to anon, authenticated;
