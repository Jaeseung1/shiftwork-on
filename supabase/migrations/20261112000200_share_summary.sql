-- 아이폰 단축어(제어 센터 버튼)·기타 기기용: 토큰으로 오늘·내일 근무를 글 두 줄로 돌려준다.
-- 예) 오늘 13일(금) 휴무
--     내일 14일(토) 일근 08:30–17:30
-- 날짜와 순환은 한국 날짜 기준, 시각은 p_tz(기기 시간대, 예: America/Toronto)로 바꾼다. 웹 앱 rangeInZone과 같은 표기.
-- GET /rest/v1/rpc/share_summary?p_token=…&p_tz=… 로 부를 수 있게 stable로 둔다.

create function private.shift_on(p_schedule uuid, p_anchor date, p_anchor_shift text, p_day date) returns text
language sql stable security definer set search_path = '' as $$
  select coalesce(
    (select o.shift from public.overrides o where o.schedule_id = p_schedule and o.day = p_day),
    (array['일', '야', '조', '휴'])[((array_position(array['일', '야', '조', '휴'], p_anchor_shift) - 1 + (p_day - p_anchor)) % 4 + 4) % 4 + 1]
  );
$$;

create function private.shift_line(p_label text, p_day date, p_shift text, p_tz text) returns text
language plpgsql stable set search_path = '' as $$
declare
  names constant text[] := array['일근', '야근', '조근', '휴무'];
  wd constant text[] := array['일', '월', '화', '수', '목', '금', '토'];
  a timestamp; b timestamp; range text := '';
begin
  if p_shift = '일' then a := p_day + time '08:30'; b := p_day + time '17:30';
  elsif p_shift = '야' then a := p_day + time '17:30'; b := p_day + 1 + time '08:30';
  elsif p_shift = '조' then a := p_day - 1 + time '17:30'; b := p_day + time '08:30';
  end if;
  if a is not null then
    -- 한국 시각 → 순간 → 기기 시간대 시각
    a := (a at time zone 'Asia/Seoul') at time zone p_tz;
    b := (b at time zone 'Asia/Seoul') at time zone p_tz;
    if a::date <> b::date or p_tz <> 'Asia/Seoul' then
      range := ' ' || extract(month from a)::int || '/' || extract(day from a)::int || ' ' || to_char(a, 'HH24:MI')
            || '–' || extract(month from b)::int || '/' || extract(day from b)::int || ' ' || to_char(b, 'HH24:MI');
    else
      range := ' ' || to_char(a, 'HH24:MI') || '–' || to_char(b, 'HH24:MI');
    end if;
  end if;
  return p_label || ' ' || extract(day from p_day)::int || '일(' || wd[extract(dow from p_day)::int + 1] || ') '
      || names[array_position(array['일', '야', '조', '휴'], p_shift)] || range;
end $$;

-- 시험할 수 있게 시각을 받는 버전(내부용)
create function private.share_summary_at(p_token uuid, p_tz text, p_now timestamptz) returns text
language plpgsql stable security definer set search_path = '' as $$
declare
  s public.schedules;
  today date := (p_now at time zone 'Asia/Seoul')::date;
  tz text := 'Asia/Seoul';
begin
  select * into s from public.schedules where widget_token = p_token;
  if not found then return null; end if;
  if p_tz is not null and exists (select 1 from pg_catalog.pg_timezone_names where name = p_tz) then tz := p_tz; end if;
  return private.shift_line('오늘', today, private.shift_on(s.id, s.anchor_date, s.anchor_shift, today), tz)
      || E'\n' || private.shift_line('내일', today + 1, private.shift_on(s.id, s.anchor_date, s.anchor_shift, today + 1), tz);
end $$;

create function public.share_summary(p_token uuid, p_tz text default 'Asia/Seoul') returns text
language sql stable security definer set search_path = '' as $$
  select private.share_summary_at(p_token, p_tz, now());
$$;

revoke execute on function private.shift_on(uuid, date, text, date), private.shift_line(text, date, text, text),
  private.share_summary_at(uuid, text, timestamptz), public.share_summary(uuid, text) from public, anon, authenticated;
grant execute on function public.share_summary(uuid, text) to anon, authenticated;
