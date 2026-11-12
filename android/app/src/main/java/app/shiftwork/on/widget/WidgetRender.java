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
 * 위젯 그리기. 큰 근무 이름 하나, 상태 칩 하나, 파란색은 "근무 중"과 진행 막대에만.
 * - 4x1: 오늘(이름 · 상태 칩 · 시간과 남은 시간 · 진행 막대) | 내일(회색 카드)
 * - 2x1: 오늘 이름 · 상태 칩과 퇴근/출근 시각 · "내일 ○○ · 시각"
 * - 1x1: 오늘 이름 · 상태 칩 · 퇴근/출근 시각 또는 "내일 ○○"
 * 날짜·근무는 한국 날짜 기준, 시각은 기기 시간대.
 */
public final class WidgetRender {
    public enum Kind { W4x1, W2x1, W1x1 }

    private static final String[] WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    private WidgetRender() {}

    public static RemoteViews build(Context c, Kind kind) {
        return build(c, kind, Instant.now(), ZoneId.systemDefault());
    }

    /** 시험용: 시각과 시간대를 정해서 그린다. */
    static RemoteViews build(Context c, Kind kind, Instant now, ZoneId zone) {
        int layout = kind == Kind.W4x1 ? R.layout.widget_4x1 : kind == Kind.W2x1 ? R.layout.widget_2x1 : R.layout.widget_1x1;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        ShiftCalc calc = Store.calc(c);
        boolean empty = calc == null;
        v.setViewVisibility(R.id.content, empty ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.empty, empty ? View.VISIBLE : View.GONE);
        if (empty) return v;

        LocalDate today = ShiftCalc.kstDate(now);
        LocalDate tom = today.plusDays(1);
        String s = calc.shiftOn(today);
        String t = calc.shiftOn(tom);
        ShiftCalc.Status st = calc.status(now, zone);
        Instant end = calc.endOfCurrent(now);
        Instant[] todayPair = ShiftCalc.instants(today, s);

        v.setTextViewText(R.id.today_name, ShiftCalc.name(s));

        boolean working = st.working;
        // 글꼴 크기를 키운 폰에서는 칸 높이(1줄)를 넘지 않게 덜 중요한 줄 하나를 뺀다
        boolean bigFont = c.getResources().getConfiguration().fontScale > 1.1f;
        if (kind == Kind.W1x1) {
            // 칩 자리가 없으니 근무 중이면 퇴근 시각을 파란색으로, 아니면 출근 시각이나 내일 근무를 회색으로
            String when = when(st, end, todayPair, zone);
            v.setTextViewText(R.id.live_time, when.isEmpty() ? "내일 " + ShiftCalc.name(t) : when);
            color(c, v, R.id.live_time, working ? R.color.w_blue : R.color.w_body);
            if (bigFont) v.setViewPadding(R.id.root, dp(c, 8), dp(c, 6), dp(c, 6), dp(c, 6));
            return v;
        }

        // 상태 칩: 근무 중이면 파랑, 아니면 회색. 휴무 날은 이름과 겹치지 않게 "쉬는 날"
        v.setTextViewText(R.id.live, working ? "근무 중" : st.title.equals("휴무") ? "쉬는 날" : st.title);
        v.setInt(R.id.live, "setBackgroundResource", working ? R.drawable.widget_chip_on : R.drawable.widget_chip_off);
        color(c, v, R.id.live, working ? R.color.w_blue : R.color.w_body);

        if (kind == Kind.W4x1) {
            v.setTextViewText(R.id.today_cap, "오늘 · " + today.getDayOfMonth() + "일 " + week(today));
            v.setViewVisibility(R.id.today_cap, bigFont ? View.GONE : View.VISIBLE);
            // 휴무면 시간 줄을 숨긴다("휴무 / 쉬는 날"처럼 같은 말을 두 번 하지 않게)
            String range = ShiftCalc.shortRange(today, s, zone);
            v.setTextViewText(R.id.today_time, working ? range + " · " + st.remaining : range);
            v.setViewVisibility(R.id.today_time, range.isEmpty() ? View.GONE : View.VISIBLE);
            v.setTextViewText(R.id.next_cap, "내일 · " + tom.getDayOfMonth() + "일 " + week(tom));
            // 글꼴이 크면 "내일 · 13일 금" 줄을 빼고 이름에 "내일"을 붙인다
            v.setViewVisibility(R.id.next_cap, bigFont ? View.GONE : View.VISIBLE);
            v.setTextViewText(R.id.next_name, (bigFont ? "내일 " : "") + ShiftCalc.name(t));
            // 내일 조근은 오늘 밤 야근이 이어지는 것이라 끝나는 시각만
            Instant[] np = ShiftCalc.instants(tom, t);
            String next = t.equals("조") && np != null ? ShiftCalc.time(np[1], zone) + " 퇴근" : ShiftCalc.shortRange(tom, t, zone);
            v.setTextViewText(R.id.next_time, next);
            v.setViewVisibility(R.id.next_time, next.isEmpty() ? View.GONE : View.VISIBLE);
            return v;
        }

        // 2x1
        String when = when(st, end, todayPair, zone);
        v.setTextViewText(R.id.live_time, when);
        v.setViewVisibility(R.id.live_time, when.isEmpty() ? View.GONE : View.VISIBLE);
        // 내일이 조근이면 오늘 밤 야근에 이어지는 근무라 시작이 아니라 끝나는 시각을 적는다
        Instant[] p = ShiftCalc.instants(tom, t);
        String at = p == null ? "" : t.equals("조") ? " · " + ShiftCalc.time(p[1], zone) + " 퇴근" : " · " + ShiftCalc.time(p[0], zone);
        v.setTextViewText(R.id.next_time, "내일 " + ShiftCalc.name(t) + at);
        v.setViewVisibility(R.id.next_time, bigFont && !when.isEmpty() ? View.GONE : View.VISIBLE);
        return v;
    }

    /** 근무 중이면 "17:30 퇴근", 근무 전이면 "08:30 출근", 그 밖에는 "" */
    private static String when(ShiftCalc.Status st, Instant end, Instant[] todayPair, ZoneId zone) {
        if (st.working && end != null) return ShiftCalc.time(end, zone) + " 퇴근";
        if (st.title.equals("근무 전") && todayPair != null) return ShiftCalc.time(todayPair[0], zone) + " 출근";
        return "";
    }

    private static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    private static String week(LocalDate d) {
        return WEEK[d.getDayOfWeek().getValue() - 1];
    }

    /** 안드로이드 12 이상은 색 리소스를 넘겨 런처의 다크 모드를 그대로 따르고, 그 아래는 지금 색으로 칠한다. */
    private static void color(Context c, RemoteViews v, int id, int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) v.setColorStateList(id, "setTextColor", colorRes);
        else v.setTextColor(id, c.getColor(colorRes));
    }
}
