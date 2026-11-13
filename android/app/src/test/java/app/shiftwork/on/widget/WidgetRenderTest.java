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
 * 그림은 app/build/widget-shots/에 생긴다. 크기는 삼성 런처 칸을 작게 어림한 값(4x1 340×80dp, 2x1 160×80dp, 1x1 75×80dp)이다.
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
        assertTrue("…로 줄어듦: " + t.getText(), t.getLayout() == null || t.getLayout().getEllipsisCount(0) == 0);
    }

    private static int color(int res) {
        return RuntimeEnvironment.getApplication().getColor(res);
    }

    private static int vis(View root, int id) {
        return root.findViewById(id).getVisibility();
    }

    private static final int[] W4 = {R.id.today_cap, R.id.today_name, R.id.today_time, R.id.next_cap, R.id.next_name, R.id.next_time};

    /** 자기와 부모가 모두 보이는지 */
    private static boolean shown(View root, int id) {
        for (View x = root.findViewById(id); x != null && x != root; x = (View) x.getParent()) if (x.getVisibility() != View.VISIBLE) return false;
        return true;
    }

    private static void check4x1(View v) {
        for (int id : W4) if (shown(v, id)) { fits(v, id); inside(v, id); }
    }

    private static int textColor(View root, int id) {
        return ((TextView) root.findViewById(id)).getCurrentTextColor();
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w4x1_working() throws Exception {
        View v = render(WidgetRender.Kind.W4x1, "2026-11-12T13:00", "Asia/Seoul", 340, 80, "4x1-light");
        assertEquals("오늘 · 근무 중", text(v, R.id.today_cap));
        assertEquals("일근", text(v, R.id.today_name));
        assertEquals("17:30 퇴근", text(v, R.id.today_time));
        assertEquals("오늘 근무 색(일근 주황)", color(R.color.s_day), textColor(v, R.id.today_name));
        assertEquals("내일 · 13일 금", text(v, R.id.next_cap));
        assertEquals("야근", text(v, R.id.next_name));
        assertEquals("내일 이름은 기본 글자색", color(R.color.w_ink), textColor(v, R.id.next_name));
        assertEquals("17:30 출근", text(v, R.id.next_time));
        check4x1(v);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void w4x1_night_shift_dark() throws Exception {
        // 13일 야근 23:00. 내일 조근은 이어지는 근무라 끝나는 시각
        View v = render(WidgetRender.Kind.W4x1, "2026-11-13T23:00", "Asia/Seoul", 340, 80, "4x1-dark");
        assertEquals("야근", text(v, R.id.today_name));
        assertEquals("08:30 퇴근", text(v, R.id.today_time));
        assertEquals("조근", text(v, R.id.next_name));
        assertEquals("08:30 퇴근", text(v, R.id.next_time));
        check4x1(v);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w4x1_after_work() throws Exception {
        // 14일 조근이 08:30에 끝난 뒤, 15일은 야근으로 바꾼 날
        View v = render(WidgetRender.Kind.W4x1, "2026-11-14T10:00", "Asia/Seoul", 340, 80, "4x1-after");
        assertEquals("오늘 · 퇴근 후", text(v, R.id.today_cap));
        assertEquals("조근", text(v, R.id.today_name));
        assertEquals("17:30–08:30", text(v, R.id.today_time));
        assertEquals("야근", text(v, R.id.next_name));
        assertEquals("17:30 출근", text(v, R.id.next_time));
        check4x1(v);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w4x1_day_off() throws Exception {
        View v = render(WidgetRender.Kind.W4x1, "2026-11-19T12:00", "Asia/Seoul", 340, 80, "4x1-off");
        assertEquals("오늘 · 쉬는 날", text(v, R.id.today_cap));
        assertEquals("휴무", text(v, R.id.today_name));
        assertEquals(View.GONE, vis(v, R.id.today_time));
        assertEquals("일근", text(v, R.id.next_name));
        assertEquals("08:30 출근", text(v, R.id.next_time));
        check4x1(v);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w2x1_working() throws Exception {
        View v = render(WidgetRender.Kind.W2x1, "2026-11-12T13:00", "Asia/Seoul", 160, 80, "2x1-light");
        assertEquals("일근", text(v, R.id.today_name));
        assertEquals("근무 중", text(v, R.id.live));
        assertEquals("17:30 퇴근", text(v, R.id.live_time));
        assertEquals("내일 야근 · 17:30", text(v, R.id.next_time));
        for (int id : new int[]{R.id.today_name, R.id.live, R.id.live_time, R.id.next_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live, R.id.live_time, R.id.next_time);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void w2x1_night_then_morning_end() throws Exception {
        // 내일이 조근이면 시작(오늘 17:30)이 아니라 끝나는 시각
        View v = render(WidgetRender.Kind.W2x1, "2026-11-13T12:00", "Asia/Seoul", 160, 80, "2x1-dark");
        assertEquals("근무 전", text(v, R.id.live));
        assertEquals("야근 색(보라)", color(R.color.s_night), textColor(v, R.id.today_name));
        assertEquals("근무 전 칩은 회색", color(R.color.w_body), textColor(v, R.id.live));
        assertEquals("17:30 출근", text(v, R.id.live_time));
        assertEquals("내일 조근 · 08:30 퇴근", text(v, R.id.next_time));
        for (int id : new int[]{R.id.today_name, R.id.live, R.id.live_time, R.id.next_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live, R.id.live_time, R.id.next_time);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void w1x1_working() throws Exception {
        View v = render(WidgetRender.Kind.W1x1, "2026-11-12T13:00", "Asia/Seoul", 75, 80, "1x1-light");
        assertEquals("일근", text(v, R.id.today_name));
        assertEquals("17:30 퇴근", text(v, R.id.live_time));
        assertEquals(color(R.color.s_day), ((TextView) v.findViewById(R.id.live_time)).getCurrentTextColor());
        for (int id : new int[]{R.id.today_name, R.id.live_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live_time);
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void w1x1_after_work_toronto() throws Exception {
        View v = render(WidgetRender.Kind.W1x1, "2026-11-12T20:00", "America/Toronto", 75, 80, "1x1-dark");
        assertEquals("내일 야근", text(v, R.id.live_time));
        assertEquals(color(R.color.w_body), ((TextView) v.findViewById(R.id.live_time)).getCurrentTextColor());
        for (int id : new int[]{R.id.today_name, R.id.live_time}) fits(v, id);
        inside(v, R.id.today_name, R.id.live_time);
    }

    /** 글자 크기를 20% 키운 기기(설정 > 글꼴 크기)에서도 넘치지 않는지. 더 키우면 끝이 "…"로 줄어든다. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    public void bigger_font() throws Exception {
        org.robolectric.RuntimeEnvironment.setFontScale(FONT);
        try {
            View v = render(WidgetRender.Kind.W4x1, "2026-11-13T23:00", "Asia/Seoul", 340, 80, "4x1-font");
            check4x1(v);
            assertEquals("글꼴이 크면 시각 줄을 뺀다", View.GONE, vis(v, R.id.today_time));
            assertEquals(View.GONE, vis(v, R.id.next_time));
            assertEquals("내일 · 14일 토", text(v, R.id.next_cap));
            v = render(WidgetRender.Kind.W2x1, "2026-11-13T12:00", "Asia/Seoul", 160, 80, "2x1-font");
            assertEquals(View.GONE, vis(v, R.id.next_row));
            for (int id : new int[]{R.id.today_name, R.id.live, R.id.live_time}) fits(v, id);
            inside(v, R.id.today_name, R.id.live, R.id.live_time);
            v = render(WidgetRender.Kind.W2x1, "2026-11-12T20:00", "Asia/Seoul", 160, 80, "2x1-font-after");
            assertEquals(View.GONE, vis(v, R.id.live_time));
            assertEquals("내일 야근 · 17:30", text(v, R.id.next_time));
            for (int id : new int[]{R.id.today_name, R.id.live, R.id.next_time}) fits(v, id);
            inside(v, R.id.today_name, R.id.live, R.id.next_time);
            v = render(WidgetRender.Kind.W1x1, "2026-11-12T13:00", "Asia/Seoul", 75, 80, "1x1-font");
            for (int id : new int[]{R.id.today_name, R.id.live_time}) fits(v, id);
            inside(v, R.id.today_name, R.id.live_time);
        } finally {
            org.robolectric.RuntimeEnvironment.setFontScale(1f);
        }
    }

    static final float FONT = 1.2f;

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
