# Rehearsal: 교대온 근무표 데모 (720×340)
Gold: panel-annotation + device-pair + phone-touch (유지: 패널이 폰 베젤에 걸치는 구도, 폰 크롭, 터치 커서 / 변경: 제품 다크 토큰, 한국어 문자열, Segmented로 근무 선택)
Beats: 1 15일(휴무) 선택 상태 → 2 야근 선택 → 3 저장, 달력 태그 Roll → 4 3.2초 유지 → 5 초기화(루프 복귀의 눈에 보이는 원인), 루프 약 10초
Captured: widths 720(wide), 390(compact); looks 다크만; states 시트 1장
Gate: rehearsal passed (wide, 390, --states 모두). 커버리지는 샌드박스 부하로 "inconclusive"
Film: near aligned → 제목 margin 17px, select를 Segmented로 교체 / uneven gutters → 패널 214px / phone off center → 패널을 베젤에 걸침 / loop seam → rest 타깃 추가 / compact 폰 상단 잘림 → top 14px, 높이 440
Review: 별도 fresh-eye 리뷰는 실행하지 않음
Assumptions: 다크 배경(사용자 페이지 배경 미확인); 앱의 <select> 대신 Segmented; 달력은 2026년 11월, 기준일 2026-11-12=일근, 순환 일→야→조→휴
Unresolved: 없음(게이트 기준)
Not checked: fresh-eye 리뷰, reduced motion(--reduced), 라이트 배경, 실기기/실시간 프레임 페이싱, 풀 커버리지(gap)

## v2 (라이트)
변경: 사용자 피드백("구도가 어색함", "밝고 깔끔한 라이트")에 따라 앱의 실제 라이트 토큰(#f4f6f7, #fff, #087f70, 태그 색)으로 전환, 파스텔 orbs 배경 추가, 폰 440px로 키우고 패널을 왼쪽 가장자리에 걸치게 재배치.
Gate v2: 정지 / 720 루프 / 390 루프 모두 rehearsal passed (커버리지 inconclusive, 샌드박스 부하). near-tangent(달력 카드가 위 가장자리 2px)는 폰 top -203px로 해소.
Not checked 추가: 라이트 v2의 --states 시트, fresh-eye 리뷰, 실제 앱 화면과의 나란히 비교. 다크 버전 원본은 index.dark.tsx / shiftwork.dark.css로 보관.

## v3 (토스 스타일, 2026-11-12)
변경: pages/widget.html과 같은 토스 값으로 통일(제3자 정리값, 공식 토큰 아님). 바탕 #f2f4f6, 흰 카드 그림자 0 8px 24px rgba(0,23,51,.08), 파랑 #3182f6 하나(저장 버튼, 오늘 날짜 숫자, 선택한 날 연파랑 바탕), 근무 태그는 바탕 없이 글자색만(위젯과 같은 색), 초기화는 회색 tertiary 버튼, 파스텔 orbs 배경 제거. 구도·대본·크기는 v2 그대로.
Gate v3: 정지 1280·390, 루프 1280·390 모두 rehearsal passed. 커버리지는 이번에도 inconclusive(샘플 간격 최대 약 95ms, 샌드박스 부하).
이미지: design/images/export/sw-demo-wide.png(720×340), sw-demo-compact.png(358×440). reduced motion으로 poster(15일 야근 저장 후)를 찍음. v2 이미지(sw-light-*.png)는 git 기록에만 남김.
Not checked: fresh-eye 리뷰, 실제 앱(index.html) 화면과의 나란히 비교. 실제 앱은 아직 초록(#087f70) 토큰이라 데모와 앱의 색이 지금은 다름.
v3.1: 근무 시간 정정(사용자 확인: 야근 17:30–24:00, 조근 00:00–08:30). 편집 패널 요약이 "한국시간 17:30–24:00"으로 바뀜(앱 index.html의 timezoneSummary와 같은 문구). 게이트 정지·루프 1280·390 다시 통과, 커버리지 inconclusive.
