// 교대온 서버 설정. 비워 두면 서버 없이 이 기기에만 저장한다.
// Supabase 대시보드 → Project Settings → API Keys의 Project URL과 Publishable key(sb_publishable_…)를 넣는다.
// 공개용 키이며 데이터는 RLS가 지킨다. Secret key(sb_secret_…)나 service_role key는 절대 넣지 말 것.
// (예전 anon JWT 키는 Supabase 문서에 따르면 2026년 말까지만 동작)
window.SHIFTWORK_CONFIG = {
  supabaseUrl: 'https://rsmuvlythhcgrfnunnrn.supabase.co',
  supabaseAnonKey: 'sb_publishable_rYdYxGE8n8fHWgECeIAVYg_3K7VHkZF',
}
