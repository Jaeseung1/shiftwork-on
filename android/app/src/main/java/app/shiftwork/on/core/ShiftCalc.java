package app.shiftwork.on.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 교대온 근무 계산. 웹 앱(index.html)의 shiftOn·shiftInstants·rangeInZone·workState와 같은 규칙.
 * - 날짜와 순환(일 → 야 → 조 → 휴)은 한국 날짜 기준, 시각은 기기 시간대로 바꿔 보여 준다.
 * - 야근은 17:30에 시작해 다음 날 08:30(조근 날)까지 이어지는 한 근무.
 * 안드로이드에 기대지 않는 순수 Java라 javac로 따로 시험할 수 있다(ShiftCalcTest).
 */
public final class ShiftCalc {
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final String[] CYCLE = {"일", "야", "조", "휴"};
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDate anchor;
    private final int anchorIndex;
    private final Map<LocalDate, String> overrides;

    public ShiftCalc(LocalDate anchor, String anchorShift, Map<LocalDate, String> overrides) {
        this.anchor = anchor;
        int i = indexOf(anchorShift);
        this.anchorIndex = i < 0 ? 0 : i;
        this.overrides = overrides == null ? Collections.emptyMap() : new HashMap<>(overrides);
    }

    public static String name(String shift) {
        switch (shift) {
            case "일": return "일근";
            case "야": return "야근";
            case "조": return "조근";
            default: return "휴무";
        }
    }

    private static int indexOf(String shift) {
        for (int i = 0; i < CYCLE.length; i++) if (CYCLE[i].equals(shift)) return i;
        return -1;
    }

    public String shiftOn(LocalDate day) {
        String o = overrides.get(day);
        if (o != null && indexOf(o) >= 0) return o;
        long diff = ChronoUnit.DAYS.between(anchor, day);
        return CYCLE[(int) Math.floorMod(anchorIndex + diff, 4L)];
    }

    public static LocalDate kstDate(Instant now) {
        return now.atZone(KST).toLocalDate();
    }

    private static Instant kst(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(KST).toInstant();
    }

    /** 그날 근무의 시작·끝(한국 시간 기준 순간). 휴무면 null. */
    public static Instant[] instants(LocalDate day, String shift) {
        switch (shift) {
            case "일": return new Instant[]{kst(day, 8, 30), kst(day, 17, 30)};
            case "야": return new Instant[]{kst(day, 17, 30), kst(day.plusDays(1), 8, 30)};
            case "조": return new Instant[]{kst(day.minusDays(1), 17, 30), kst(day, 8, 30)};
            default: return null;
        }
    }

    public static String time(Instant t, ZoneId zone) {
        return HM.format(t.atZone(zone));
    }

    private static String dateTime(Instant t, ZoneId zone) {
        ZonedDateTime z = t.atZone(zone);
        return z.getMonthValue() + "/" + z.getDayOfMonth() + " " + HM.format(z);
    }

    /** 웹 앱 rangeInZone과 같은 표기: 날짜가 넘어가거나 기기가 한국 시간대가 아니면 날짜를 붙인다. */
    public static String range(LocalDate day, String shift, ZoneId zone) {
        Instant[] p = instants(day, shift);
        if (p == null) return "휴무";
        LocalDate a = p[0].atZone(zone).toLocalDate();
        LocalDate b = p[1].atZone(zone).toLocalDate();
        boolean cross = !a.equals(b);
        return cross || !zone.equals(KST) ? dateTime(p[0], zone) + "–" + dateTime(p[1], zone) : time(p[0], zone) + "–" + time(p[1], zone);
    }

    /** 위젯용 짧은 표기: 날짜 없이 시각만(예: 17:30–08:30). 휴무면 "". */
    public static String shortRange(LocalDate day, String shift, ZoneId zone) {
        Instant[] p = instants(day, shift);
        return p == null ? "" : time(p[0], zone) + "–" + time(p[1], zone);
    }

    /** 근무 중이면 끝나는 시각, 아니면 null. */
    public Instant endOfCurrent(Instant now) {
        LocalDate date = kstDate(now);
        Instant[] p = instants(date, shiftOn(date));
        return p != null && !now.isBefore(p[0]) && now.isBefore(p[1]) ? p[1] : null;
    }

    /** 지금 상태. 웹 앱 workState와 같은 문구. */
    public Status status(Instant now, ZoneId zone) {
        LocalDate date = kstDate(now);
        ZonedDateTime k = now.atZone(KST);
        int mins = k.getHour() * 60 + k.getMinute();
        String s = shiftOn(date);
        LocalDate tom = date.plusDays(1);
        Instant[] pair = instants(date, s);
        if (s.equals("일") && mins >= 510 && mins < 1050) return new Status("일근 중", "오늘 " + time(kst(date, 17, 30), zone) + " 퇴근", true, pair, now);
        if (s.equals("야") && mins >= 1050) return new Status("야근 중", "내일 " + time(kst(tom, 8, 30), zone) + " 퇴근", true, pair, now);
        if (s.equals("조") && mins < 510) return new Status("야근 중", "오늘 " + time(kst(date, 8, 30), zone) + " 퇴근", true, pair, now);
        if (s.equals("일") && mins < 510) return new Status("근무 전", "오늘 " + time(kst(date, 8, 30), zone) + " 출근", false, null, now);
        if (s.equals("야") && mins < 1050) return new Status("근무 전", "오늘 " + time(kst(date, 17, 30), zone) + " 야근 시작", false, null, now);
        if (s.equals("일") || s.equals("조")) {
            String ts = shiftOn(tom);
            if (ts.equals("휴")) return new Status("퇴근 후", "내일은 휴무", false, null, now);
            return new Status("퇴근 후", "내일 " + name(ts) + " · " + time(instants(tom, ts)[0], zone) + " 출근", false, null, now);
        }
        return new Status("휴무", "오늘은 쉬는 날", false, null, now);
    }

    public static final class Status {
        public final String title;
        public final String sub;
        public final boolean working;
        /** 근무 중이면 0~100, 아니면 -1 */
        public final int progress;
        /** 근무 중이면 "4시간 30분 남음", 아니면 "" */
        public final String remaining;

        Status(String title, String sub, boolean working, Instant[] pair, Instant now) {
            this.title = title;
            this.sub = sub;
            this.working = working;
            if (working && pair != null) {
                long total = Duration.between(pair[0], pair[1]).toMillis();
                long done = Duration.between(pair[0], now).toMillis();
                this.progress = (int) Math.max(0, Math.min(100, Math.round(done * 100.0 / total)));
                long left = Math.max(0, Math.round(Duration.between(now, pair[1]).toMillis() / 60000.0));
                long h = left / 60, m = left % 60;
                this.remaining = (h > 0 ? h + "시간 " : "") + (m > 0 || h == 0 ? m + "분 " : "") + "남음";
            } else {
                this.progress = -1;
                this.remaining = "";
            }
        }
    }
}
