package app.shiftwork.on.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 웹 앱과 같은 결과인지 시험. JUnit 없이 javac로도 돌린다:
 *   javac -d out android/app/src/main/java/app/shiftwork/on/core/ShiftCalc.java android/app/src/test/java/app/shiftwork/on/core/ShiftCalcTest.java
 *   java -cp out app.shiftwork.on.core.ShiftCalcTest
 * 기대값은 2026-11-12에 웹 앱(index.html)에서 같은 시각으로 뽑은 문구.
 */
public final class ShiftCalcTest {
    static int pass, fail;

    static void eq(String name, Object want, Object got) {
        if (String.valueOf(want).equals(String.valueOf(got))) pass++;
        else { fail++; System.out.println("FAIL " + name + "\n  want: " + want + "\n  got:  " + got); }
    }

    public static void main(String[] args) {
        int failed = run();
        System.out.println(pass + " / " + (pass + failed) + (failed == 0 ? " pass" : " FAIL"));
        if (failed > 0) System.exit(1);
    }

    /** 실패한 개수를 돌려준다. */
    public static int run() {
        pass = 0;
        fail = 0;
        ShiftCalc c = new ShiftCalc(LocalDate.parse("2026-11-12"), "일", new HashMap<>());
        String[][] cases = {
            // 시간대, 한국 시각, 제목 / 설명
            {"Asia/Seoul", "2026-11-12T07:00", "근무 전 / 오늘 08:30 출근"},
            {"Asia/Seoul", "2026-11-12T12:00", "일근 중 / 오늘 17:30 퇴근"},
            {"Asia/Seoul", "2026-11-12T20:00", "퇴근 후 / 내일 야근 · 17:30 출근"},
            {"Asia/Seoul", "2026-11-13T12:00", "근무 전 / 오늘 17:30 야근 시작"},
            {"Asia/Seoul", "2026-11-13T20:00", "야근 중 / 내일 08:30 퇴근"},
            {"Asia/Seoul", "2026-11-14T03:00", "야근 중 / 오늘 08:30 퇴근"},
            {"Asia/Seoul", "2026-11-14T10:00", "퇴근 후 / 내일은 휴무"},
            {"Asia/Seoul", "2026-11-15T12:00", "휴무 / 오늘은 쉬는 날"},
            {"Asia/Seoul", "2026-11-16T20:00", "퇴근 후 / 내일 야근 · 17:30 출근"},
            {"America/Toronto", "2026-11-12T07:00", "근무 전 / 오늘 18:30 출근"},
            {"America/Toronto", "2026-11-12T12:00", "일근 중 / 오늘 03:30 퇴근"},
            {"America/Toronto", "2026-11-12T20:00", "퇴근 후 / 내일 야근 · 03:30 출근"},
            {"America/Toronto", "2026-11-13T12:00", "근무 전 / 오늘 03:30 야근 시작"},
            {"America/Toronto", "2026-11-13T20:00", "야근 중 / 내일 18:30 퇴근"},
            {"America/Toronto", "2026-11-14T03:00", "야근 중 / 오늘 18:30 퇴근"},
            {"America/Toronto", "2026-11-14T10:00", "퇴근 후 / 내일은 휴무"},
            {"America/Toronto", "2026-11-15T12:00", "휴무 / 오늘은 쉬는 날"},
            {"America/Toronto", "2026-11-16T20:00", "퇴근 후 / 내일 야근 · 03:30 출근"},
        };
        for (String[] k : cases) {
            Instant now = java.time.LocalDateTime.parse(k[1]).atZone(ShiftCalc.KST).toInstant();
            ShiftCalc.Status s = c.status(now, ZoneId.of(k[0]));
            eq(k[0] + " " + k[1], k[2], s.title + " / " + s.sub);
        }
        // 진행률과 남은 시간(웹 앱 13:00 → 50%, 4시간 30분 남음 / 20:00 야근 → 12시간 30분 남음)
        ShiftCalc.Status d = c.status(java.time.LocalDateTime.parse("2026-11-12T13:00").atZone(ShiftCalc.KST).toInstant(), ShiftCalc.KST);
        eq("13:00 진행률", 50, d.progress);
        eq("13:00 남은 시간", "4시간 30분 남음", d.remaining);
        ShiftCalc.Status n = c.status(java.time.LocalDateTime.parse("2026-11-13T20:00").atZone(ShiftCalc.KST).toInstant(), ShiftCalc.KST);
        eq("야근 20:00 남은 시간", "12시간 30분 남음", n.remaining);
        ShiftCalc.Status e = c.status(java.time.LocalDateTime.parse("2026-11-12T17:00").atZone(ShiftCalc.KST).toInstant(), ShiftCalc.KST);
        eq("17:00 남은 시간", "30분 남음", e.remaining);
        // 순환과 바꾼 근무
        eq("순환 11/15", "휴", c.shiftOn(LocalDate.parse("2026-11-15")));
        eq("순환 11/11(기준일 전)", "휴", c.shiftOn(LocalDate.parse("2026-11-11")));
        eq("순환 11/1", "야", c.shiftOn(LocalDate.parse("2026-11-01")));
        Map<LocalDate, String> o = new HashMap<>();
        o.put(LocalDate.parse("2026-11-15"), "야");
        eq("바꾼 근무 11/15", "야", new ShiftCalc(LocalDate.parse("2026-11-12"), "일", o).shiftOn(LocalDate.parse("2026-11-15")));
        // 시간 표기(웹 앱 rangeInZone과 같음)
        eq("서울 일근", "08:30–17:30", ShiftCalc.range(LocalDate.parse("2026-11-12"), "일", ShiftCalc.KST));
        eq("서울 야근", "11/13 17:30–11/14 08:30", ShiftCalc.range(LocalDate.parse("2026-11-13"), "야", ShiftCalc.KST));
        eq("토론토 일근", "11/11 18:30–11/12 03:30", ShiftCalc.range(LocalDate.parse("2026-11-12"), "일", ZoneId.of("America/Toronto")));
        eq("휴무", "휴무", ShiftCalc.range(LocalDate.parse("2026-11-15"), "휴", ShiftCalc.KST));
        return fail;
    }
}
