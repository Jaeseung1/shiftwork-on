-- 교대온 초기 스키마 (Supabase / PostgreSQL)
-- 근무는 기준일 + 순환(일 → 야 → 조 → 휴) + 날짜별로 바꾼 근무(overrides)만 저장하고, 나머지는 앱·위젯이 계산한다.
-- 권한: 근무표 주인만 고칠 수 있고, 초대 코드로 들어온 가족은 읽기만 한다.
-- 위젯: 로그인 없이 비밀 토큰(widget_token)으로 읽기 전용 함수 widget_schedule()만 부를 수 있다.

create table public.schedules (
  id           uuid primary key default gen_random_uuid(),
  owner_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  anchor_date  date not null default ((now() at time zone 'Asia/Seoul')::date),
  anchor_shift text not null default '일' check (anchor_shift in ('일', '야', '조', '휴')),
  widget_token uuid not null default gen_random_uuid() unique,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  unique (owner_id)
);

create table public.overrides (
  schedule_id uuid not null references public.schedules (id) on delete cascade,
  day         date not null,
  shift       text not null check (shift in ('일', '야', '조', '휴')),
  updated_at  timestamptz not null default now(),
  primary key (schedule_id, day)
);

create table public.members (
  schedule_id uuid not null references public.schedules (id) on delete cascade,
  user_id     uuid not null references auth.users (id) on delete cascade,
  joined_at   timestamptz not null default now(),
  primary key (schedule_id, user_id)
);

create table public.invites (
  code        text primary key check (code ~ '^[A-HJ-NP-Z2-9]{6}$'),
  schedule_id uuid not null references public.schedules (id) on delete cascade,
  created_at  timestamptz not null default now(),
  expires_at  timestamptz not null default now() + interval '7 days'
);

create index members_user_idx on public.members (user_id);
create index invites_schedule_idx on public.invites (schedule_id);

-- 권한 확인용 함수. RLS 정책끼리 서로 부르며 도는 것을 막으려고 security definer로 둔다.
create function public.is_schedule_owner(sid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.schedules s where s.id = sid and s.owner_id = auth.uid());
$$;

create function public.can_view_schedule(sid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.schedules s where s.id = sid and s.owner_id = auth.uid())
      or exists (select 1 from public.members m where m.schedule_id = sid and m.user_id = auth.uid());
$$;

alter table public.schedules enable row level security;
alter table public.overrides enable row level security;
alter table public.members   enable row level security;
alter table public.invites   enable row level security;

-- owner_id를 먼저 직접 본다: insert … returning(supabase-js의 .insert().select())에서는 함수가 방금 넣은 줄을 아직 못 보기 때문
create policy schedules_select on public.schedules for select to authenticated using (owner_id = auth.uid() or public.can_view_schedule(id));
create policy schedules_insert on public.schedules for insert to authenticated with check (owner_id = auth.uid());
create policy schedules_update on public.schedules for update to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy schedules_delete on public.schedules for delete to authenticated using (owner_id = auth.uid());

create policy overrides_select on public.overrides for select to authenticated using (public.can_view_schedule(schedule_id));
create policy overrides_insert on public.overrides for insert to authenticated with check (public.is_schedule_owner(schedule_id));
create policy overrides_update on public.overrides for update to authenticated using (public.is_schedule_owner(schedule_id)) with check (public.is_schedule_owner(schedule_id));
create policy overrides_delete on public.overrides for delete to authenticated using (public.is_schedule_owner(schedule_id));

-- 가족 목록: 본인 줄, 또는 근무표 주인. 넣기는 join_with_code()로만. 나가기·내보내기는 본인 또는 주인.
create policy members_select on public.members for select to authenticated using (user_id = auth.uid() or public.is_schedule_owner(schedule_id));
create policy members_delete on public.members for delete to authenticated using (user_id = auth.uid() or public.is_schedule_owner(schedule_id));

create policy invites_select on public.invites for select to authenticated using (public.is_schedule_owner(schedule_id));
create policy invites_delete on public.invites for delete to authenticated using (public.is_schedule_owner(schedule_id));

-- 바꾼 근무가 생기면 근무표의 updated_at을 올려, 위젯이 바뀐 것을 알 수 있게 한다.
create function public.touch_schedule() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  update public.schedules set updated_at = now() where id = coalesce(new.schedule_id, old.schedule_id);
  return null;
end $$;

create trigger overrides_touch after insert or update or delete on public.overrides
for each row execute function public.touch_schedule();

-- 초대 코드 만들기(주인만). 헷갈리는 글자(0 O 1 I)는 뺀다.
create function public.create_invite() returns text
language plpgsql volatile security definer set search_path = '' as $$
declare
  sid uuid;
  alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  c text;
begin
  select id into sid from public.schedules where owner_id = auth.uid();
  if sid is null then raise exception 'no schedule for this user' using errcode = '42501'; end if;
  loop
    c := '';
    for i in 1..6 loop c := c || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1); end loop;
    begin
      insert into public.invites (code, schedule_id) values (c, sid);
      return c;
    exception when unique_violation then
      -- 드물게 겹치면 다시 만든다
    end;
  end loop;
end $$;

-- 코드로 가족 참여. 성공하면 근무표 id를 돌려준다.
create function public.join_with_code(p_code text) returns uuid
language plpgsql volatile security definer set search_path = '' as $$
declare sid uuid;
begin
  if auth.uid() is null then raise exception 'login required' using errcode = '42501'; end if;
  select schedule_id into sid from public.invites
   where code = upper(trim(p_code)) and expires_at > now();
  if sid is null then raise exception 'invalid or expired code' using errcode = 'P0002'; end if;
  insert into public.members (schedule_id, user_id) values (sid, auth.uid()) on conflict do nothing;
  return sid;
end $$;

-- 위젯용 읽기 전용. 토큰을 아는 기기는 기준일·순환 기준·앞뒤 날짜의 바꾼 근무만 받는다.
create function public.widget_schedule(p_token uuid) returns json
language sql stable security definer set search_path = '' as $$
  select json_build_object(
    'anchor_date', s.anchor_date,
    'anchor_shift', s.anchor_shift,
    'updated_at', s.updated_at,
    'overrides', coalesce((
      select json_agg(json_build_object('day', o.day, 'shift', o.shift) order by o.day)
        from public.overrides o
       where o.schedule_id = s.id
         and o.day between ((now() at time zone 'Asia/Seoul')::date - 7) and ((now() at time zone 'Asia/Seoul')::date + 60)
    ), '[]'::json))
  from public.schedules s where s.widget_token = p_token;
$$;

-- 함수 실행 권한: 기본(public)에서 빼고 필요한 역할에만.
revoke execute on function public.is_schedule_owner(uuid), public.can_view_schedule(uuid), public.touch_schedule(),
  public.create_invite(), public.join_with_code(text), public.widget_schedule(uuid) from public;
grant execute on function public.is_schedule_owner(uuid), public.can_view_schedule(uuid) to authenticated;
grant execute on function public.create_invite(), public.join_with_code(text) to authenticated;
grant execute on function public.widget_schedule(uuid) to anon, authenticated;

-- 표 권한: 로그인한 사용자만(RLS가 줄 단위로 거름). 로그인 안 한 사용자는 위젯 함수만.
revoke all on public.schedules, public.overrides, public.members, public.invites from anon;
grant select, insert, update, delete on public.schedules, public.overrides to authenticated;
grant select, delete on public.members, public.invites to authenticated;
