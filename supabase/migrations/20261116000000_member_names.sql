-- 공유받는 사람이 근무표 주인에게 보일 이름을 정한다(예: 엄마, 김교대). 주인은 공유 탭에서 그 이름을 본다.
-- 지우거나 바꾸는 것 없이 더하기만 한다: 이름 칸, 이름을 받는 참여 함수(인자 2개, 예전 1개짜리는 그대로), 본인 이름만 고치는 권한.

alter table public.members add column if not exists display_name text
  check (display_name is null or char_length(display_name) between 1 and 20);

-- 코드로 참여하면서 이름도 정한다. 이미 참여했으면 이름만 바꾼다. 성공하면 근무표 id.
create or replace function public.join_with_code(p_code text, p_name text) returns uuid
language plpgsql volatile security definer set search_path = '' as $$
declare
  sid uuid;
  nm text := nullif(left(btrim(coalesce(p_name, '')), 20), '');
begin
  if auth.uid() is null then raise exception 'login required' using errcode = '42501'; end if;
  select schedule_id into sid from public.invites
   where code = upper(trim(p_code)) and expires_at > now();
  if sid is null then raise exception 'invalid or expired code' using errcode = 'P0002'; end if;
  insert into public.members (schedule_id, user_id, display_name) values (sid, auth.uid(), nm)
    on conflict (schedule_id, user_id) do update set display_name = coalesce(excluded.display_name, public.members.display_name);
  return sid;
end $$;

revoke execute on function public.join_with_code(text, text) from public, anon, authenticated;
grant execute on function public.join_with_code(text, text) to authenticated;

-- 본인 줄의 이름만 고칠 수 있다(다른 칸·다른 사람 줄은 안 됨)
grant update (display_name) on public.members to authenticated;
create policy members_update_own_name on public.members for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
