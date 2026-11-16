package app.shiftwork.on.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import app.shiftwork.on.R;
import app.shiftwork.on.core.ShiftCalc;

/**
 * 알람 울리기(1.4~): 소리·진동은 AlarmService가 직접 내고, 이 알림은 소리 없이 알람 화면(AlarmActivity)과 끄기·5분 뒤 버튼만 맡는다.
 * 1.1~1.3은 알림 소리(채널 shift_alarm_v1)로 울렸는데, 기기 설정에 따라 한 번만 울리거나 소리를 고를 수 없어서 바꿨다.
 */
public final class AlarmRinger {
    static final String CHANNEL = "shift_alarm_v2";
    static final int NOTIFY_ID = 7001;

    private AlarmRinger() {}

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel("shift_alarm_v1") != null) nm.deleteNotificationChannel("shift_alarm_v1");
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "근무 알람", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("근무표에 맞춘 알람 화면(소리와 진동은 앱의 알람 소리 설정을 따라요)");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static Notification build(Context c, String shift, boolean test) {
        ensureChannel(c);
        String name = shift == null || shift.isEmpty() ? "근무" : ShiftCalc.name(shift);
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title = (test ? "시험 알람 · " : "") + name + " 알람";
        Intent full = new Intent(c, AlarmActivity.class).putExtra(AlarmScheduler.EXTRA_SHIFT, shift).putExtra(AlarmScheduler.EXTRA_TEST, test)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullPi = PendingIntent.getActivity(c, 201, full, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // 끄기: 끈 뒤 열 앱이 있으면 알람 화면을 거쳐 연다(알림 버튼에서 다른 앱을 바로 열 수 없어서)
        PendingIntent off = AlarmStore.app(c).isEmpty()
                ? PendingIntent.getBroadcast(c, 202, new Intent(c, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_DISMISS), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)
                : PendingIntent.getActivity(c, 204, new Intent(c, AlarmActivity.class).putExtra(AlarmActivity.EXTRA_DISMISS, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent later = PendingIntent.getBroadcast(c, 203, new Intent(c, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_SHIFT, shift), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_shift_day)
                .setContentTitle(title)
                .setContentText(now + " · 오늘 " + name + "이에요")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullPi, true)
                .setContentIntent(fullPi)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(new Notification.Action.Builder(null, "5분 뒤", later).build())
                .addAction(new Notification.Action.Builder(null, "끄기", off).build())
                .build();
    }

    static void ring(Context c, String shift, boolean test) {
        AlarmService.start(c, shift, test);
    }

    static void stop(Context c) {
        AlarmService.stop(c);
        c.getSystemService(NotificationManager.class).cancel(NOTIFY_ID);
    }
}
