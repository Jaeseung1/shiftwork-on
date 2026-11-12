import { ShiftworkCC } from '../shiftwork-cc/index'

// 비교용 한 개짜리 안: 교대온 중간 크기 컨트롤 하나에 제목 "오늘 일근", 값 "내일 야근".
// HIG의 제목(무엇에 관한 컨트롤)·값(상태) 용도와는 조금 어긋나는 쓰임. 화면 구성은 shiftwork-cc와 같음.
export default function ShiftworkControlCenterOne() {
  return <ShiftworkCC one />
}
