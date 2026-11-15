-- 공유받은 사람 화면에 주인이 바꾼 근무가 바로 보이게 실시간(Realtime) 알림을 켠다.
-- 바꾼 근무(overrides)가 생기거나 지워지면 touch_schedule 트리거가 schedules.updated_at을 올리므로,
-- schedules 한 표만 알림 대상으로 두면 넣기·바꾸기·지우기를 모두 알 수 있다. 알림도 RLS를 따라 볼 수 있는 사람에게만 간다.
-- 로컬 시험용 PostgreSQL에는 supabase_realtime 발행이 없어서 있을 때만 더한다.
do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime')
     and not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'schedules') then
    alter publication supabase_realtime add table public.schedules;
  end if;
end $$;
