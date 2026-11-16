package app.shiftwork.on.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.util.DataSource;

import java.lang.reflect.Field;

/** 알람 서비스(1.4~): 고른 소리를 알람 용도로 되풀이, 진동, 소리 없는 알림 + 잠금 화면 위 알람 화면 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AlarmServiceTest {
    private Context c;
    private static final Uri DEFAULT = Settings.System.DEFAULT_ALARM_ALERT_URI;
    private static final Uri SONG = Uri.parse("content://media/external/audio/media/42");

    @Before
    public void setUp() {
        c = RuntimeEnvironment.getApplication();
        ShadowMediaPlayer.resetStaticState();
        ShadowMediaPlayer.MediaInfo info = new ShadowMediaPlayer.MediaInfo(30_000, 0);
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(c, DEFAULT), info);
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(c, SONG), info);
        AlarmStore.setSound(c, "", "");
        AlarmStore.setOption(c, "vibrate", "true");
        AlarmStore.setOption(c, "ramp", "false");
        AlarmStore.setOption(c, "app", "");
    }

    private ServiceController<AlarmService> run(String shift) {
        Intent i = new Intent(c, AlarmService.class).putExtra(AlarmScheduler.EXTRA_SHIFT, shift);
        return Robolectric.buildService(AlarmService.class, i).create().startCommand(0, 1);
    }

    private static MediaPlayer player(AlarmService s) throws Exception {
        Field f = AlarmService.class.getDeclaredField("player");
        f.setAccessible(true);
        return (MediaPlayer) f.get(s);
    }

    @Test
    public void defaultAlarmSoundLoopsAsAlarmAndVibrates() throws Exception {
        ServiceController<AlarmService> sc = run("야");
        MediaPlayer mp = player(sc.get());
        assertNotNull("소리를 직접 낸다", mp);
        ShadowMediaPlayer sm = shadowOf(mp);
        assertEquals("기본 알람음", DataSource.toDataSource(c, DEFAULT), sm.getDataSource());
        assertTrue("끌 때까지 되풀이", mp.isLooping());
        assertEquals(ShadowMediaPlayer.State.STARTED, sm.getState());
        assertEquals("알람 소리 크기를 따름", AudioAttributes.USAGE_ALARM, AlarmService.attrs().getUsage());
        assertTrue("진동", shadowOf(c.getSystemService(Vibrator.class)).isVibrating());

        Notification n = shadowOf(sc.get()).getLastForegroundNotification();
        assertNotNull(n);
        assertEquals(Notification.CATEGORY_ALARM, n.category);
        assertNotNull("잠금 화면 위 알람 화면", n.fullScreenIntent);
        NotificationChannel ch = c.getSystemService(NotificationManager.class).getNotificationChannel(AlarmRinger.CHANNEL);
        assertNull("알림은 소리 없이(소리는 서비스가)", ch.getSound());
        assertFalse(ch.shouldVibrate());

        sc.destroy();
        assertFalse("끄면 진동도 멈춤", shadowOf(c.getSystemService(Vibrator.class)).isVibrating());
        assertEquals(ShadowMediaPlayer.State.END, sm.getState());
    }

    @Test
    public void pickedSongPlaysAndVibrationCanBeOff() throws Exception {
        AlarmStore.setSound(c, SONG.toString(), "좋아하는 노래");
        AlarmStore.setOption(c, "vibrate", "false");
        ServiceController<AlarmService> sc = run("일");
        assertEquals("고른 음악", DataSource.toDataSource(c, SONG), shadowOf(player(sc.get())).getDataSource());
        assertFalse("진동 끔", shadowOf(c.getSystemService(Vibrator.class)).isVibrating());
        sc.destroy();
    }

    @Test
    public void missingSongFallsBackToDefault() throws Exception {
        AlarmStore.setSound(c, "content://media/external/audio/media/999", "지운 노래");
        ServiceController<AlarmService> sc = run("일");
        assertEquals("고른 파일이 없으면 기본 알람음", DataSource.toDataSource(c, DEFAULT), shadowOf(player(sc.get())).getDataSource());
        sc.destroy();
    }

    @Test
    public void silentOptionStillVibrates() throws Exception {
        AlarmStore.setOption(c, "sound", "none");
        ServiceController<AlarmService> sc = run("조");
        assertNull("소리 없음", player(sc.get()));
        assertTrue(shadowOf(c.getSystemService(Vibrator.class)).isVibrating());
        sc.destroy();
    }

    @Test
    public void optionsJsonRoundTrip() throws Exception {
        AlarmStore.setOption(c, "ramp", "true");
        AlarmStore.setOption(c, "app", "com.iloen.melon");
        org.json.JSONObject o = new org.json.JSONObject(AlarmStore.optionsJson(c));
        assertTrue(o.getBoolean("ramp"));
        assertTrue(o.getBoolean("vibrate"));
        assertEquals("com.iloen.melon", o.getString("app"));
        assertEquals("", o.getString("sound"));
    }

    @Test
    public void dismissWithAppOpensIt() {
        org.robolectric.shadows.ShadowPackageManager pm = shadowOf(c.getPackageManager());
        android.content.pm.PackageInfo pi = new android.content.pm.PackageInfo();
        pi.packageName = "com.iloen.melon";
        pi.applicationInfo = new android.content.pm.ApplicationInfo();
        pi.applicationInfo.packageName = "com.iloen.melon";
        pi.applicationInfo.name = "멜론";
        pm.installPackage(pi);
        Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage("com.iloen.melon");
        android.content.pm.ResolveInfo ri = new android.content.pm.ResolveInfo();
        ri.activityInfo = new android.content.pm.ActivityInfo();
        ri.activityInfo.packageName = "com.iloen.melon";
        ri.activityInfo.name = "com.iloen.melon.Main";
        pm.addResolveInfoForIntent(launch, ri);
        AlarmStore.setOption(c, "app", "com.iloen.melon");
        assertTrue("음악 앱 목록에 보임", MusicApps.json(c).contains("com.iloen.melon"));
        AlarmActivity a = Robolectric.buildActivity(AlarmActivity.class, new Intent(c, AlarmActivity.class).putExtra(AlarmActivity.EXTRA_DISMISS, true)).create().get();
        Intent next = shadowOf(a).getNextStartedActivity();
        assertNotNull("끄면 멜론 열기", next);
        assertEquals("com.iloen.melon", next.getPackage() != null ? next.getPackage() : next.getComponent().getPackageName());
        assertTrue(a.isFinishing());
    }
}
