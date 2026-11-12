# Rehearsal: 교대온 앱 홈 제품 사진 (정지, 440×650 / compact 358×560)
Gold: phone-touch (유지: 폰 하나를 위에서 고정해 아래로 자르는 구도, 화면 포인트 단위 / 변경: 앱 index.html v0.2 홈 화면을 그대로 옮김, 토스 토큰, 한국어). 레이아웃은 framing.md의 Pinned, bottom bleed(좌우·위 약 9%).
Beats: 정지. 시각 서울 2026-11-12 13:00(일근 중, 4시간 30분 남음, 진행 50%).
Captured: 1280·390 폭 × 라이트·다크, 모두 `film.mjs --still --gate` → rehearsal passed.
Film/눈으로 고친 것: ① 넓은 화면에서 아래 가장자리가 가족 공유 설명 글자를 가로로 자름(게이트는 못 잡음, craft "Crop only filler") → 높이 600→650, 카드 아래 빈 바탕에서 잘리게 ② 다크에서 상태바 글자가 어두운 화면 위에 어두움 → 기기 테마를 따라 Phone dark 켬.
실제 앱 검사: scripts/layout.mjs를 index.html 홈·근무표(편집 칸 열림)·가족·설정에 390 폭으로 실행, 모두 no-issues-detected(시각 승인 아님).
Review: 별도 fresh-eye 리뷰(review.md)는 하지 않음.
Assumptions: 앱을 iPhone 프레임에 넣음(웹 앱이라 기기 무관). 글꼴은 이 컨테이너의 WenQuanYi Zen Hei.
Not checked: 리뷰어 검토, 실기기, reduced motion(정지라 해당 적음).
v1.1: 사용자 요청으로 "교" 글자 아이콘 제거, 상단은 글자 로고 "교대온"만(craft.md "No invented logo… use a plain text wordmark"). 게이트가 글자 상자를 0px로 재는 tight inset을 냄 → 글자를 span으로 감싸 실제 위치(20px)로 측정되게 함. 4가지 조합 다시 rehearsal passed.
