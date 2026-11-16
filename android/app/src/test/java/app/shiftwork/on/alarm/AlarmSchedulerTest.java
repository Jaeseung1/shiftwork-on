package app.shiftwork.on.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import app.shiftwork.on.Store;

/** 근무 알람 예약·울림. 근무표는 오늘부터 4일 순환이라 2일 안에 일근이나 야근이 반드시 있다. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AlarmSchedulerTest {
    private Context c;

    @Before
    public void setUp() {
        c = RuntimeEnvironment.getApplication();
        String today = app.shiftwork.on.core.ShiftCalc.kstDate(java.time.Instant.now()).toString();
        Store.saveSchedule(c, "{\"anchor_date\":\"" + today + "\",\"anchor_shift\":\"일\",\"overrides\":[]}");
        AlarmStore.saveRules(c, "{\"일\":\"06:30\",\"야\":\"15:30\"}");
    }

    private AlarmManager am() {
        return c.getSystemService(AlarmManager.class);
    }

    @Test
    public void ownerGetsNextAlarmAsAlarmClock() {
        // 실기기에서는 USE_EXACT_ALARM 권한(알람 앱용, 자동 허용)으로 true
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true);
        AlarmStore.setOwner(c, true);
        long at = AlarmScheduler.reschedule(c);
        assertTrue("다음 알람이 있음", at > System.currentTimeMillis());
        assertTrue(at - System.currentTimeMillis() < 2L * 24 * 3600 * 1000);
        AlarmManager.AlarmClockInfo info = am().getNextAlarmClock();
        assertNotNull("상단에 알람으로 표시되는 방식(setAlarmClock)", info);
        assertEquals(at, info.getTriggerTime());
    }

    @Test
    public void withoutExactPermissionStillSchedules() {
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false);
        AlarmStore.setOwner(c, true);
        long at = AlarmScheduler.reschedule(c);
        assertTrue(at > 0);
        assertNotNull("정확한 알람 권한이 없어도 늦게라도 울리게 예약", shadowOf(am()).getNextScheduledAlarm());
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true);
    }

    @Test
    public void viewerGetsNoAlarm() {
        AlarmStore.setOwner(c, true);
        AlarmScheduler.reschedule(c);
        AlarmStore.setOwner(c, false);
        assertEquals(0, AlarmScheduler.reschedule(c));
        assertNull("공유받은 근무표에는 알람 없음", shadowOf(am()).getNextScheduledAlarm());
    }

    @Test
    public void ringPostsInsistentAlarmNotificationAndDismissStops() {
        AlarmStore.setOwner(c, true);
        new AlarmReceiver().onReceive(c, new Intent(AlarmScheduler.ACTION_RING).putExtra(AlarmScheduler.EXTRA_SHIFT, "일"));
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        Notification n = shadowOf(nm).getNotification(AlarmRinger.NOTIFY_ID);
        assertNotNull(n);
        assertEquals(Notification.CATEGORY_ALARM, n.category);
        assertTrue("끌 때까지 소리 반복", (n.flags & Notification.FLAG_INSISTENT) != 0);
        assertNotNull("잠금 화면 위 알람 화면", n.fullScreenIntent);
        NotificationChannel ch = nm.getNotificationChannel(AlarmRinger.CHANNEL);
        assertEquals("알람 소리 크기를 따름", AudioAttributes.USAGE_ALARM, ch.getAudioAttributes().getUsage());
        assertTrue("울린 뒤 다음 알람 예약", AlarmStore.nextAt(c) > System.currentTimeMillis());
        new AlarmReceiver().onReceive(c, new Intent(AlarmScheduler.ACTION_DISMISS));
        assertNull("끄기", shadowOf(nm).getNotification(AlarmRinger.NOTIFY_ID));
    }

    @Test
    public void snoozeSchedulesFiveMinutesLater() {
        AlarmStore.setOwner(c, true);
        long before = System.currentTimeMillis();
        new AlarmReceiver().onReceive(c, new Intent(AlarmScheduler.ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_SHIFT, "일"));
        boolean found = false;
        for (org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm a : shadowOf(am()).getScheduledAlarms()) {
            long d = a.getTriggerAtMs() - before;
            if (d >= 4 * 60_000 && d <= 6 * 60_000) found = true;
        }
        assertTrue("5분 뒤 다시", found);
    }
}
