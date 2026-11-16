package app.shiftwork.on;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.ZoneId;

import app.shiftwork.on.alarm.AlarmScheduler;
import app.shiftwork.on.alarm.AlarmStore;
import app.shiftwork.on.alarm.MusicApps;
import app.shiftwork.on.core.ShiftCalc;

import app.shiftwork.on.widget.WidgetUpdater;

/** 웹 앱(sync.js)이 window.ShiftBridge로 부른다. 근무표와 서버 접속 정보를 받아 위젯을 다시 그린다. */
public final class ShiftBridge {
    private final Context ctx;
    private final WeakReference<Activity> activity;

    public ShiftBridge(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.activity = new WeakReference<>(ctx instanceof Activity ? (Activity) ctx : null);
    }

    @JavascriptInterface
    public void setSchedule(String json) {
        Store.saveSchedule(ctx, json);
        WidgetUpdater.updateAll(ctx);
        AlarmScheduler.reschedule(ctx); // 근무가 바뀌면 알람도 다시 맞춘다
    }

    /** 이 앱의 versionCode(1.5~). 화면이 새 버전 알림을 띄울지 정할 때 쓴다(1.4 이하는 화면이 기능으로 짐작) */
    @JavascriptInterface
    public int appVersion() {
        return BuildConfig.VERSION_CODE;
    }

    @JavascriptInterface
    public void setRemote(String json) {
        Store.saveRemote(ctx, json);
        RefreshWorker.schedule(ctx);
    }

    // ---- 근무 알람(1.1~). 설정 화면은 근무표 주인에게만 보인다(sync.js) ----

    /** 이 기기 근무표가 내 근무표면 true, 공유받은 근무표면 false(알람을 끈다) */
    @JavascriptInterface
    public void setAlarmOwner(boolean owner) {
        AlarmStore.setOwner(ctx, owner);
        AlarmScheduler.reschedule(ctx);
    }

    /** {"일":"06:30","야":"15:30"} 켠 근무만. 알림 권한이 없으면 묻는다. */
    @JavascriptInterface
    public void setAlarms(String json) {
        AlarmStore.saveRules(ctx, json);
        AlarmScheduler.reschedule(ctx);
        if (!AlarmStore.rules(ctx).isEmpty()) askNotificationPermission();
    }

    @JavascriptInterface
    public String alarmRules() {
        return AlarmStore.rulesJson(ctx);
    }

    /** {"next":"11/16(월) 06:30 일근","notify":true,"exact":true} */
    @JavascriptInterface
    public String alarmStatus() {
        try {
            JSONObject o = new JSONObject();
            long at = AlarmScheduler.reschedule(ctx);
            if (at > 0) {
                java.time.ZonedDateTime z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault());
                String[] wd = {"월", "화", "수", "목", "금", "토", "일"};
                ShiftCalc calc = Store.calc(ctx);
                String shift = calc == null ? "" : " " + ShiftCalc.name(calc.shiftOn(ShiftCalc.kstDate(Instant.ofEpochMilli(at))));
                o.put("next", z.getMonthValue() + "/" + z.getDayOfMonth() + "(" + wd[z.getDayOfWeek().getValue() - 1] + ") "
                        + String.format(java.util.Locale.ROOT, "%02d:%02d", z.getHour(), z.getMinute()) + shift);
            } else o.put("next", "");
            o.put("notify", ctx.getSystemService(NotificationManager.class).areNotificationsEnabled());
            o.put("exact", AlarmScheduler.canExact(ctx));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public void testAlarm(String shift) {
        askNotificationPermission();
        AlarmScheduler.test(ctx, shift);
    }

    /** 권한이 꺼져 있을 때 설정 화면 열기: "notify" 또는 "exact" */
    @JavascriptInterface
    public void openAlarmSettings(String which) {
        Intent i;
        if ("exact".equals(which) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.getPackageName()));
        } else {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        }
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    // ---- 알람 소리(1.4~): 소리·진동·점점 크게·끄면 열 앱 ----

    /** {"sound":"","soundName":"","vibrate":true,"ramp":false,"app":""} */
    @JavascriptInterface
    public String alarmOptions() {
        return AlarmStore.optionsJson(ctx);
    }

    /** key: vibrate·ramp("true"/"false"), app(패키지), sound(""=기본 알람음, "none"=소리 없음) */
    @JavascriptInterface
    public void setAlarmOption(String key, String value) {
        AlarmStore.setOption(ctx, key, value);
    }

    /** "ringtone": 폰의 알람음 목록, "file": 내 음악 파일. 고르면 화면의 window.shiftAlarmOptionsChanged()를 부른다 */
    @JavascriptInterface
    public void pickAlarmSound(String kind) {
        Activity a = activity.get();
        if (a instanceof MainActivity) a.runOnUiThread(() -> ((MainActivity) a).pickSound(kind));
    }

    /** 깔려 있는 음악 앱 [{"pkg":"com.iloen.melon","name":"멜론"}] */
    @JavascriptInterface
    public String musicApps() {
        return MusicApps.json(ctx);
    }

    private void askNotificationPermission() {
        Activity a = activity.get();
        if (a == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        a.runOnUiThread(() -> a.requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 11));
    }
}
