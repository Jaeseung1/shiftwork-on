package app.shiftwork.on.core;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * 근무 알람 계획(안드로이드와 상관없는 계산만). 근무 종류(일·야·조·휴)마다 알람 시각(한국 시간, 하루 중 분)을 정해 두면,
 * 지금 다음에 울릴 알람(날짜·근무)을 근무표에서 찾는다. 바꾼 근무(overrides)도 ShiftCalc가 반영한다.
 */
public final class AlarmPlanner {
    private AlarmPlanner() {}

    public static final class Next {
        public final Instant at;
        public final LocalDate day;
        public final String shift;

        Next(Instant at, LocalDate day, String shift) {
            this.at = at;
            this.day = day;
            this.shift = shift;
        }
    }

    /** rules: 근무 → 하루 중 분(예: "일" → 390 = 06:30). 없는 근무는 알람 없음. 60일 안에 없으면 null. */
    public static Next next(ShiftCalc calc, Map<String, Integer> rules, Instant now) {
        if (calc == null || rules == null || rules.isEmpty()) return null;
        LocalDate today = ShiftCalc.kstDate(now);
        for (int i = 0; i <= 60; i++) {
            LocalDate day = today.plusDays(i);
            String s = calc.shiftOn(day);
            Integer min = rules.get(s);
            if (min == null) continue;
            Instant at = day.atTime(min / 60, min % 60).atZone(ShiftCalc.KST).toInstant();
            if (at.isAfter(now)) return new Next(at, day, s);
        }
        return null;
    }

    /** "06:30" → 390, 잘못된 값은 null */
    public static Integer parse(String hm) {
        if (hm == null || !hm.matches("\\d{1,2}:\\d{2}")) return null;
        String[] p = hm.split(":");
        int h = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]);
        return h < 24 && m < 60 ? h * 60 + m : null;
    }
}
