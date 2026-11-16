package app.shiftwork.on.alarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import app.shiftwork.on.R;
import app.shiftwork.on.core.ShiftCalc;

/**
 * 잠금 화면 위에 뜨는 알람 화면(소리는 알람 알림이 낸다).
 * 위: 날짜와 지금 시각 / 가운데: 근무 색 아이콘 칸(앱·위젯과 같은 표시), 근무 이름, 출근 시각 / 아래: 5분 뒤 다시, 끄기.
 */
public class AlarmActivity extends Activity {
    private static final String[] WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(getColor(R.color.page));
        getWindow().setNavigationBarColor(getColor(R.color.page));
        setContentView(R.layout.activity_alarm);

        String shift = getIntent().getStringExtra(AlarmScheduler.EXTRA_SHIFT);
        boolean test = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_TEST, false);
        String s = shift == null || shift.isEmpty() ? "일" : shift;
        ZonedDateTime now = ZonedDateTime.now();

        ((TextView) findViewById(R.id.alarm_date)).setText(now.getMonthValue() + "월 " + now.getDayOfMonth() + "일 " + WEEK[now.getDayOfWeek().getValue() - 1] + "요일");
        ((TextView) findViewById(R.id.alarm_clock)).setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        GradientDrawable tile = new GradientDrawable();
        tile.setColor(getColor(tint(s)));
        tile.setCornerRadius(32 * getResources().getDisplayMetrics().density);
        findViewById(R.id.alarm_tile).setBackground(tile);
        ((ImageView) findViewById(R.id.alarm_icon)).setImageResource(icon(s));

        TextView name = findViewById(R.id.alarm_name);
        name.setText(ShiftCalc.name(s));
        name.setTextColor(getColor(tone(s)));
        ((TextView) findViewById(R.id.alarm_sub)).setText((test ? "시험 알람 · " : "") + sub(s));

        findViewById(R.id.alarm_snooze).setOnClickListener(v -> {
            sendBroadcast(new Intent(this, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_SHIFT, shift));
            finish();
        });
        findViewById(R.id.alarm_off).setOnClickListener(v -> {
            sendBroadcast(new Intent(this, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_DISMISS));
            finish();
        });
    }

    /** 오늘(한국 날짜) 그 근무의 시각: 일근·야근은 출근, 조근은 퇴근 */
    private static String sub(String s) {
        LocalDate today = ShiftCalc.kstDate(Instant.now());
        Instant[] p = ShiftCalc.instants(today, s);
        if (p == null) return "쉬는 날";
        ZoneId z = ZoneId.systemDefault();
        return s.equals("조") ? ShiftCalc.time(p[1], z) + " 퇴근" : ShiftCalc.time(p[0], z) + " 출근";
    }

    private static int tone(String s) {
        switch (s) {
            case "일": return R.color.s_day;
            case "야": return R.color.s_night;
            case "조": return R.color.s_dawn;
            default: return R.color.s_off;
        }
    }

    private static int tint(String s) {
        switch (s) {
            case "일": return R.color.t_day;
            case "야": return R.color.t_night;
            case "조": return R.color.t_dawn;
            default: return R.color.t_off;
        }
    }

    private static int icon(String s) {
        switch (s) {
            case "일": return R.drawable.ic_shift_day;
            case "야": return R.drawable.ic_shift_night;
            case "조": return R.drawable.ic_shift_dawn;
            default: return R.drawable.ic_shift_off;
        }
    }
}
