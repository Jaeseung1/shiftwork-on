package app.shiftwork.on.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** 근무 알람 계획: 2026-11-12 일근 기준, 11/15만 야근으로 바꾼 근무표(위젯 시험과 같음) */
public class AlarmPlannerTest {
    private static ShiftCalc calc() {
        Map<LocalDate, String> o = new HashMap<>();
        o.put(LocalDate.parse("2026-11-15"), "야");
        return new ShiftCalc(LocalDate.parse("2026-11-12"), "일", o);
    }

    private static Instant kst(String t) {
        return LocalDateTime.parse(t).atZone(ShiftCalc.KST).toInstant();
    }

    private static Map<String, Integer> rules(String... kv) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], AlarmPlanner.parse(kv[i + 1]));
        return m;
    }

    @Test
    public void nextIsTheNextShiftWithAnAlarm() {
        AlarmPlanner.Next n = AlarmPlanner.next(calc(), rules("일", "06:30", "야", "15:30"), kst("2026-11-12T13:00"));
        assertEquals(kst("2026-11-13T15:30"), n.at);
        assertEquals("야", n.shift);
    }

    @Test
    public void laterTodayCounts() {
        AlarmPlanner.Next n = AlarmPlanner.next(calc(), rules("일", "06:30"), kst("2026-11-12T05:00"));
        assertEquals(kst("2026-11-12T06:30"), n.at);
    }

    @Test
    public void followsChangedShifts() {
        // 11/14 조근 다음 11/15는 바꾼 야근, 11/16 일근
        AlarmPlanner.Next n = AlarmPlanner.next(calc(), rules("야", "15:30"), kst("2026-11-14T10:00"));
        assertEquals(kst("2026-11-15T15:30"), n.at);
        n = AlarmPlanner.next(calc(), rules("일", "06:30"), kst("2026-11-14T10:00"));
        assertEquals(kst("2026-11-16T06:30"), n.at);
    }

    @Test
    public void noRulesNoAlarm() {
        assertNull(AlarmPlanner.next(calc(), rules(), kst("2026-11-12T13:00")));
        assertNull(AlarmPlanner.next(null, rules("일", "06:30"), kst("2026-11-12T13:00")));
        assertNull(AlarmPlanner.parse("25:00"));
        assertNull(AlarmPlanner.parse("6시"));
        assertEquals(Integer.valueOf(390), AlarmPlanner.parse("06:30"));
    }
}
