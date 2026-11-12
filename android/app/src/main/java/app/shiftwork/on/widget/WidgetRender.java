package app.shiftwork.on.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import app.shiftwork.on.MainActivity;
import app.shiftwork.on.R;
import app.shiftwork.on.Store;
import app.shiftwork.on.core.ShiftCalc;

/**
 * 위젯 그리기. 디자인은 design/pages/widget.html(토스 스타일)과 같다.
 * - 4x1: 오늘(큰 파란 글씨) · 내일 · 앞으로 5일
 * - 2x1: 오늘 + 근무 중/퇴근 시각 · 내일(회색 안쪽 카드)
 * - 1x1: 오늘 + 근무 중/퇴근 시각
 * 날짜·근무는 한국 날짜 기준, 시각은 기기 시간대.
 */
public final class WidgetRender {
    public enum Kind { W4x1, W2x1, W1x1 }

    private static final String[] WEEK = {"월", "화", "수", "목", "금", "토", "일"};
    private static final int[] DAY_W = {R.id.d0_w, R.id.d1_w, R.id.d2_w, R.id.d3_w, R.id.d4_w};
    private static final int[] DAY_S = {R.id.d0_s, R.id.d1_s, R.id.d2_s, R.id.d3_s, R.id.d4_s};
    private static final int[] DAY_N = {R.id.d0_n, R.id.d1_n, R.id.d2_n, R.id.d3_n, R.id.d4_n};

    private WidgetRender() {}

    public static RemoteViews build(Context c, Kind kind) {
        int layout = kind == Kind.W4x1 ? R.layout.widget_4x1 : kind == Kind.W2x1 ? R.layout.widget_2x1 : R.layout.widget_1x1;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        ShiftCalc calc = Store.calc(c);
        boolean empty = calc == null;
        v.setViewVisibility(R.id.content, empty ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.empty, empty ? View.VISIBLE : View.GONE);
        if (empty) return v;

        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = ShiftCalc.kstDate(now);
        LocalDate tom = today.plusDays(1);
        String s = calc.shiftOn(today);
        String t = calc.shiftOn(tom);
        ShiftCalc.Status st = calc.status(now, zone);

        v.setTextViewText(R.id.today_cap, kind == Kind.W1x1 ? "오늘 · " + today.getDayOfMonth() + "일" : "오늘 · " + today.getDayOfMonth() + "일 " + week(today));
        v.setTextViewText(R.id.today_name, ShiftCalc.name(s));

        if (kind == Kind.W4x1) {
            v.setTextViewText(R.id.today_time, ShiftCalc.shortRange(today, s, zone));
            next(v, tom, t, zone, false);
            for (int i = 0; i < 5; i++) {
                LocalDate d = today.plusDays(i + 2);
                String ds = calc.shiftOn(d);
                v.setTextViewText(DAY_W[i], week(d));
                v.setTextViewText(DAY_S[i], ShiftCalc.name(ds).substring(0, 1));
                v.setTextViewText(DAY_N[i], String.valueOf(d.getDayOfMonth()));
                color(c, v, DAY_S[i], shiftColor(ds));
            }
        } else {
            // 작은 크기는 지금 상태: 근무 중이면 "근무 중 / 17:30 퇴근", 아니면 "퇴근 후 / 내일 야근 · 17:30 출근" 같은 문구
            Instant end = calc.endOfCurrent(now);
            v.setTextViewText(R.id.live, st.working ? "근무 중" : st.title);
            color(c, v, R.id.live, st.working ? R.color.w_blue : R.color.w_sub);
            v.setTextViewText(R.id.live_time, st.working && end != null ? ShiftCalc.time(end, zone) + " 퇴근" : liveLine(st, s, t, today, zone, kind));
            if (kind == Kind.W2x1) next(v, tom, t, zone, true);
        }
        return v;
    }

    private static String liveLine(ShiftCalc.Status st, String s, String t, LocalDate today, ZoneId zone, Kind kind) {
        // 2x1은 오른쪽 카드에 내일이 있으니 오늘 시각을 보여 준다
        if (kind == Kind.W2x1) return s.equals("휴") ? "" : ShiftCalc.shortRange(today, s, zone);
        // 1x1은 짧게: 근무 전이면 "08:30 출근", 퇴근 후·휴무면 "내일 야근"
        if (st.title.equals("근무 전")) return st.sub.replace("오늘 ", "");
        return "내일 " + ShiftCalc.name(t);
    }

    private static void next(RemoteViews v, LocalDate tom, String t, ZoneId zone, boolean withWeekday) {
        v.setTextViewText(R.id.next_cap, "내일 · " + tom.getDayOfMonth() + "일" + (withWeekday ? " " + week(tom) : ""));
        v.setTextViewText(R.id.next_name, ShiftCalc.name(t));
        v.setTextViewText(R.id.next_time, ShiftCalc.shortRange(tom, t, zone));
    }

    private static String week(LocalDate d) {
        return WEEK[d.getDayOfWeek().getValue() - 1];
    }

    private static int shiftColor(String s) {
        switch (s) {
            case "일": return R.color.s_day;
            case "야": return R.color.s_night;
            case "조": return R.color.s_dawn;
            default: return R.color.s_off;
        }
    }

    /** 안드로이드 12 이상은 색 리소스를 넘겨 런처의 다크 모드를 그대로 따르고, 그 아래는 지금 색으로 칠한다. */
    private static void color(Context c, RemoteViews v, int id, int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) v.setColorStateList(id, "setTextColor", colorRes);
        else v.setTextColor(id, c.getColor(colorRes));
    }
}
