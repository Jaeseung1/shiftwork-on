package app.shiftwork.on.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.Instant;

import app.shiftwork.on.MainActivity;
import app.shiftwork.on.Store;
import app.shiftwork.on.core.AlarmPlanner;

/**
 * 다음 근무 알람 하나만 AlarmManager.setAlarmClock으로 예약한다(절전 중에도 정확히 울리고 상단에 알람 표시).
 * 근무표·알람 설정이 바뀔 때, 알람이 울린 뒤, 폰을 켠 뒤에 다시 부른다.
 */
public final class AlarmScheduler {
    static final String ACTION_RING = "app.shiftwork.on.alarm.RING";
    static final String ACTION_SNOOZE = "app.shiftwork.on.alarm.SNOOZE";
    static final String ACTION_DISMISS = "app.shiftwork.on.alarm.DISMISS";
    static final String EXTRA_SHIFT = "shift";
    static final String EXTRA_TEST = "test";
    private static final int RC_NEXT = 101, RC_SNOOZE = 102, RC_TEST = 103, RC_SHOW = 104;

    private AlarmScheduler() {}

    static PendingIntent ringIntent(Context c, int rc, String shift, boolean test) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_RING).putExtra(EXTRA_SHIFT, shift).putExtra(EXTRA_TEST, test);
        return PendingIntent.getBroadcast(c, rc, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 다음 알람을 다시 맞춘다. 맞춘 시각(ms), 없으면 0 */
    public static long reschedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.cancel(ringIntent(c, RC_NEXT, "", false));
        AlarmStore.saveNext(c, 0, "");
        if (!AlarmStore.owner(c)) return 0; // 공유받은 근무표에는 알람을 울리지 않는다
        AlarmPlanner.Next n = AlarmPlanner.next(Store.calc(c), AlarmStore.rules(c), Instant.now());
        if (n == null) return 0;
        set(c, am, n.at.toEpochMilli(), ringIntent(c, RC_NEXT, n.shift, false));
        AlarmStore.saveNext(c, n.at.toEpochMilli(), n.shift);
        return n.at.toEpochMilli();
    }

    static void snooze(Context c, String shift, int minutes) {
        set(c, c.getSystemService(AlarmManager.class), System.currentTimeMillis() + minutes * 60_000L, ringIntent(c, RC_SNOOZE, shift, false));
    }

    /** 설정 화면의 "5초 뒤 울려 보기" */
    public static void test(Context c, String shift) {
        set(c, c.getSystemService(AlarmManager.class), System.currentTimeMillis() + 5_000L, ringIntent(c, RC_TEST, shift, true));
    }

    public static boolean canExact(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private static void set(Context c, AlarmManager am, long at, PendingIntent op) {
        if (canExact(c)) {
            PendingIntent show = PendingIntent.getActivity(c, RC_SHOW, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), op);
        } else {
            // 정확한 알람 권한이 없으면 몇 분 늦을 수 있는 방식으로라도 울린다(설정 화면에서 권한을 켜라고 안내)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op);
        }
    }
}
