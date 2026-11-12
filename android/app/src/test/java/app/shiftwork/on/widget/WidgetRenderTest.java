package app.shiftwork.on.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import app.shiftwork.on.R;
import app.shiftwork.on.Store;
import app.shiftwork.on.core.ShiftCalc;

/**
 * 위젯 3종을 실제 안드로이드 레이아웃으로 그려 글자와 그림(PNG)을 확인한다(실기기 대신).
 * 그림은 app/build/widget-shots/에 생긴다. 크기는 삼성 런처 칸을 어림한 값(4x1 ≈ 350×90dp)이다.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class WidgetRenderTest {
    private static final String SCHEDULE = "{\"anchor_date\":\"2026-11-12\",\"anchor_shift\":\"일\",\"overrides\":[{\"day\":\"2026-11-15\",\"shift\":\"야\"}]}";

    private View render(WidgetRender.Kind kind, String time, String zone, int wDp, int hDp, String name) throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        Store.saveSchedule(c, SCHEDULE);
        java.time.Instant now = LocalDateTime.parse(time).atZone(ShiftCalc.KST).toInstant();
        FrameLayout parent = new FrameLayout(c);
        View v = WidgetRender.build(c, kind, now, ZoneId.of(zone)).apply(c, parent);
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int w = Math.round(wDp * dm.density), h = Math.round(hDp * dm.density);
        parent.addView(v, new ViewGroup.LayoutParams(w, h));
        parent.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w + 40, h + 40, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(name.contains("dark") ? Color.rgb(9, 11, 14) : Color.rgb(229, 232, 235));
        cv.translate(20, 20);
        parent.draw(cv);
        File dir = new File("build/widget-shots");
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return v;
    }

    /** 줄이 찌그러지거나(글자 높이보다 낮음) 위젯 안쪽 아래로 넘치면 실패 */
    private static void inside(View root, int... ids) {
        int bottom = root.getHeight() - root.getPaddingBottom();
        for (int id : ids) {
            TextView t = root.findViewById(id);
            android.graphics.Paint.FontMetricsInt fm = t.getPaint().getFontMetricsInt();
            int need = fm.descent - fm.ascent;
            int y = 0;
            for (View x = t; x != root; x = (View) x.getParent()) y += x.getTop();
            assertTrue("찌그러짐: " + t.getText() + " h=" + t.getHeight() + " need=" + need, t.getHeight() >= need - 1);
            assertTrue("아래로 넘침: " + t.getText() + " bottom=" + (y + t.getHeight()) + " max=" + bottom, y + t.getHeight() <= bottom + 1);
        }
    }

    private static String text(View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    /** 글자가 자기 칸보다 넓으면(잘림) 실패 */
    private static void fits(View root, int id) {
        TextView t = root.findViewById(id);
        float need = t.getPaint().measureText(t.getText().toString());
        assertTrue("잘림: " + t.getText() + " need=" + need + " have=" + t.getWidth(), need <= t.getWidth() + 1);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w4x1_light() throws Exception {
        View v = render(WidgetRender.Kind.W4x1, "2026-11-12T13:00", "Asia/Seoul", 340, 80, "4x1-light");
        assertEquals("오늘 · 12일 목", text(v, R.id.today_cap));
        assertEquals("일근", text(v, R.id.today_name));
        assertEquals("08:30–17:30", text(v, R.id.today_time));
        assertEquals("야근", text(v, R.id.next_name));
        assertEquals("17:30–08:30", text(v, R.id.next_time));
        assertEquals("조", text(v, R.id.d0_s));
        assertEquals("야", text(v, R.id.d1_s)); // 11/15 바꾼 근무
        for (int id : new int[]{R.id.today_cap, R.id.today_name, R.id.today_time, R.id.next_cap, R.id.next_name, R.id.next_time}) fits(v, id);
        inside(v, R.id.today_cap, R.id.today_name, R.id.today_time, R.id.next_cap, R.id.next_name, R.id.next_time, R.id.d0_w, R.id.d0_s, R.id.d0_n, R.id.d4_n);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void w4x1_dark() throws Exception {
        render(WidgetRender.Kind.W4x1, "2026-11-12T13:00", "Asia/Seoul", 340, 80, "4x1-dark");
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w2x1_working() throws Exception {
        View v = render(WidgetRender.Kind.W2x1, "2026-11-12T13:00", "Asia/Seoul", 160, 80, "2x1-light");
        assertEquals("근무 중", text(v, R.id.live));
        assertEquals("17:30 퇴근", text(v, R.id.live_time));
        assertEquals("야근", text(v, R.id.next_name));
        assertEquals("17:30 출근", text(v, R.id.next_time));
        assertEquals(View.GONE, v.findViewById(R.id.today_cap).getVisibility());
        for (int id : new int[]{R.id.today_name, R.id.live, R.id.live_time, R.id.next_cap, R.id.next_name, R.id.next_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live, R.id.live_time, R.id.next_cap, R.id.next_name, R.id.next_time);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void w1x1_after_work_toronto() throws Exception {
        View v = render(WidgetRender.Kind.W1x1, "2026-11-12T20:00", "America/Toronto", 75, 80, "1x1-dark");
        assertEquals("퇴근 후", text(v, R.id.live));
        assertEquals("내일 야근", text(v, R.id.live_time));
        for (int id : new int[]{R.id.today_name, R.id.live, R.id.live_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live, R.id.live_time);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void empty_before_first_open() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        c.getSharedPreferences("shiftwork", Context.MODE_PRIVATE).edit().clear().commit();
        View v = WidgetRender.build(c, WidgetRender.Kind.W2x1).apply(c, new FrameLayout(c));
        assertEquals(View.VISIBLE, v.findViewById(R.id.empty).getVisibility());
        assertEquals(View.GONE, v.findViewById(R.id.content).getVisibility());
    }
}
