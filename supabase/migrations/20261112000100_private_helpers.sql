-- 권한 규칙(RLS) 안에서만 쓰는 도우미 함수를 API(/rest/v1/rpc)에 드러나지 않는 private 스키마로 옮긴다.
-- (Supabase 보안 점검: authenticated_security_definer_function_executable)
-- 정책은 함수를 이름이 아니라 내부 번호로 가리키므로 옮겨도 그대로 동작한다.
create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated;
alter function public.is_schedule_owner(uuid) set schema private;
alter function public.can_view_schedule(uuid) set schema private;
