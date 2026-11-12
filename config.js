// 교대온 서버 설정. 비워 두면 서버 없이 이 기기에만 저장한다.
// Supabase 대시보드 → Project Settings → API의 Project URL과 anon(public) key를 넣는다.
// anon key는 공개용이며 데이터는 RLS가 지킨다. service_role key는 절대 넣지 말 것.
window.SHIFTWORK_CONFIG = {
  supabaseUrl: '',
  supabaseAnonKey: '',
}
