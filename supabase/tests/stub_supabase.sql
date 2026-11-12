-- 로컬 시험용: Supabase가 제공하는 auth 스키마와 역할을 흉내 낸다. 실제 Supabase에는 넣지 말 것.
create schema auth;
create table auth.users (id uuid primary key);
create function auth.uid() returns uuid language sql stable as $$
  select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid
$$;
create role anon nologin;
create role authenticated nologin;
grant usage on schema public, auth to anon, authenticated;
grant execute on function auth.uid() to anon, authenticated;
