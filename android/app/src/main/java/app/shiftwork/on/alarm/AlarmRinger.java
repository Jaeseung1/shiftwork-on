package app.shiftwork.on.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import app.shiftwork.on.R;
import app.shiftwork.on.core.ShiftCalc;

/**
 * 알람 울리기: 알람 용도(USAGE_ALARM) 소리라 폰의 "알람 소리 크기"를 따르고 무음·진동 모드에서도 울린다.
 * 끄거나 미룰 때까지 소리를 되풀이하고(FLAG_INSISTENT), 화면이 꺼져 있으면 알람 화면(AlarmActivity)을 띄운다.
 */
public final class AlarmRinger {
    static final String CHANNEL = "shift_alarm_v1";
    static final int NOTIFY_ID = 7001;

    private AlarmRinger() {}

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "근무 알람", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("근무표에 맞춘 알람");
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ch.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[] {0, 800, 600, 800, 600});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static void ring(Context c, String shift, boolean test) {
        ensureChannel(c);
        String name = shift == null || shift.isEmpty() ? "근무" : ShiftCalc.name(shift);
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title = (test ? "시험 알람 · " : "") + name + " 알람";
        Intent full = new Intent(c, AlarmActivity.class).putExtra(AlarmScheduler.EXTRA_SHIFT, shift).putExtra(AlarmScheduler.EXTRA_TEST, test)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullPi = PendingIntent.getActivity(c, 201, full, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent off = PendingIntent.getBroadcast(c, 202, new Intent(c, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_DISMISS), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent later = PendingIntent.getBroadcast(c, 203, new Intent(c, AlarmReceiver.class).setAction(AlarmScheduler.ACTION_SNOOZE).putExtra(AlarmScheduler.EXTRA_SHIFT, shift), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_shift_day)
                .setContentTitle(title)
                .setContentText(now + " · 오늘 " + name + "이에요")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullPi, true)
                .setContentIntent(fullPi)
                .setOngoing(true)
                .setAutoCancel(false)
                .setTimeoutAfter(10 * 60_000L) // 10분 동안 아무것도 안 누르면 멈춘다
                .addAction(new Notification.Action.Builder(null, "5분 뒤", later).build())
                .addAction(new Notification.Action.Builder(null, "끄기", off).build())
                .build();
        n.flags |= Notification.FLAG_INSISTENT;
        c.getSystemService(NotificationManager.class).notify(NOTIFY_ID, n);
    }

    static void stop(Context c) {
        c.getSystemService(NotificationManager.class).cancel(NOTIFY_ID);
    }
}
