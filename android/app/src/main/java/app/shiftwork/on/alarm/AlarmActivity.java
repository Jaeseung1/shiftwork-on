package app.shiftwork.on.alarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import app.shiftwork.on.R;
import app.shiftwork.on.core.ShiftCalc;

/** 잠금 화면 위에 뜨는 알람 화면: 시각, 근무 이름, [5분 뒤] [끄기]. 소리는 알람 알림이 낸다. */
public class AlarmActivity extends Activity {
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
        String shift = getIntent().getStringExtra(AlarmScheduler.EXTRA_SHIFT);
        boolean test = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_TEST, false);
        int tone = getColor(tone(shift));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(getColor(R.color.page));
        int pad = dp(28);
        root.setPadding(pad, pad, pad, pad);

        TextView clock = text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), 64, getColor(R.color.w_ink), true);
        TextView name = text((test ? "시험 · " : "") + (shift == null || shift.isEmpty() ? "근무" : ShiftCalc.name(shift)), 34, tone, true);
        TextView sub = text("근무 알람이에요", 16, getColor(R.color.w_body), false);
        root.addView(clock);
        root.addView(name);
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(48);
        Button later = button("5분 뒤", getColor(R.color.w_bg), getColor(R.color.w_ink));
        Button off = button("끄기", tone, Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(60), 1f);
        lp.setMarginEnd(dp(10));
        row.addView(later, lp);
        row.addView(off, new LinearLayout.LayoutParams(0, dp(60), 1f));
        root.addView(row, rowLp);
        setContentView(root);

        later.setOnClickListener(v -> {
            sendBroadcast(new Intent(this, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_SHIFT, shift));
            finish();
        });
        off.setOnClickListener(v -> {
            sendBroadcast(new Intent(this, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_DISMISS));
            finish();
        });
    }

    private static int tone(String s) {
        if ("일".equals(s)) return R.color.s_day;
        if ("야".equals(s)) return R.color.s_night;
        if ("조".equals(s)) return R.color.s_dawn;
        return R.color.s_off;
    }

    private TextView text(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private Button button(String t, int bg, int fg) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(16));
        b.setBackground(d);
        b.setStateListAnimator(null);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
