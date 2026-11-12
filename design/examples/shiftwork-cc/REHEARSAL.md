# shiftwork-cc 리허설 기록 (2026-11-12)

- 출발점: Stageshow gold `dynamic-island`의 폰 배치(Phone frame="pro", 가운데 정렬, 위에서 고정, 아래로 잘림)와 kit `WALLPAPER`.
- 바꾼 것: 잠금 화면 대신 제어 센터 격자(4열, 칸 72pt, 간격 16pt, 여백 33pt, 눈대중). 맨 윗줄 교대온 중간 크기 컨트롤 2개(심볼+제목+값, HIG Controls 구조), 그 아래 연결·음악·작은 컨트롤·밝기/음량은 글자 없는 채움. 바탕은 토스 회색 #f2f4f6 / 다크 #0f1115(제3자 정리값).
- 고친 것: ① 파란 Wi-Fi가 먼저 보여 교대온 줄을 맨 위로 옮기고 심볼에 근무 색 틴트 ② 390 폭 `uneven inset`(좌 9px, 우 19px) → 좌우 패딩 통일, 글자 칸 flex:1 ③ 390 폭 아래 빈 띠 → 작은 컨트롤 한 줄 추가해 잘려 나가게.
- 게이트: `film.mjs --still --gate` 1280 폭, 390 폭 모두 `rehearsal passed`. 정지 이미지라 움직임은 판정 안 함.
- 확인 안 한 것: 별도 리뷰어(references/review.md)의 fresh-eye 검토, 실제 iOS 제어 센터 스크린샷과의 나란히 비교(참고 이미지를 구하지 못함), 실기기. 글꼴은 이 컨테이너의 WenQuanYi Zen Hei로 렌더링됨.

## 한 개짜리 비교안 (examples/shiftwork-cc-one)
- shiftwork-cc의 `ShiftworkCC one` 변형. 교대온 중간 크기 컨트롤 하나(해, 제목 "오늘 일근", 값 "내일 야근"), 남는 두 칸에 작은 기본 컨트롤(잠금, 화면 미러링 자리) 두 개.
- 게이트: `film.mjs --still --gate` 1280, 390 모두 `rehearsal passed`. 두 개짜리 안은 리팩터 후 다시 찍어도 PNG가 바이트 단위로 같았음.
- 새 예시 폴더는 미리보기 서버를 다시 띄워야 `?capture=`에 잡혔음.
