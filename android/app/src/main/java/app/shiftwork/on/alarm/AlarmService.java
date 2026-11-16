package app.shiftwork.on.alarm;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * 알람을 실제로 울리는 곳(1.4~). 알림 소리에 맡기지 않고 앱이 직접 끌 때까지 되풀이한다.
 * - 소리: 고른 알람음·음악 파일(없으면 폰의 기본 알람음). 알람 용도(USAGE_ALARM)라 "알람 소리 크기"를 따르고 무음·진동 모드에서도 울린다.
 * - 진동: 켜 두면 소리와 함께 되풀이. 점점 크게: 30초 동안 작은 소리에서 커진다.
 * - 다른 앱 음악은 알람 동안 잠시 멈춘다(오디오 포커스). 10분 동안 아무것도 안 누르면 멈춘다.
 */
public class AlarmService extends Service {
    static final String EXTRA_SHIFT = AlarmScheduler.EXTRA_SHIFT;
    static final String EXTRA_TEST = AlarmScheduler.EXTRA_TEST;
    private static final long TIMEOUT = 10 * 60_000L;
    private static final long RAMP_MS = 30_000L;
    private static final long[] PATTERN = {0, 800, 600};

    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Vibrator vibrator;
    private AudioFocusRequest focus;
    private long rampStart;

    static void start(Context c, String shift, boolean test) {
        Intent i = new Intent(c, AlarmService.class).putExtra(EXTRA_SHIFT, shift).putExtra(EXTRA_TEST, test);
        c.startForegroundService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, AlarmService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String shift = intent == null ? "" : intent.getStringExtra(EXTRA_SHIFT);
        boolean test = intent != null && intent.getBooleanExtra(EXTRA_TEST, false);
        Notification n = AlarmRinger.build(this, shift, test);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(AlarmRinger.NOTIFY_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(AlarmRinger.NOTIFY_ID, n);
        quiet();
        play();
        if (AlarmStore.vibrate(this)) vibrate();
        h.postDelayed(this::stopSelf, TIMEOUT);
        return START_NOT_STICKY;
    }

    static AudioAttributes attrs() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    }

    /** 고른 소리 → 기본 알람음 → 기본 벨소리 순서로, 되는 것을 쓴다 */
    static Uri[] candidates(Context c) {
        String s = AlarmStore.sound(c);
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Uri ring = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if ("none".equals(s)) return new Uri[0];
        if (s.isEmpty()) return new Uri[] {alarm, ring};
        return new Uri[] {Uri.parse(s), alarm, ring};
    }

    private void play() {
        AudioManager am = getSystemService(AudioManager.class);
        for (Uri u : candidates(this)) {
            if (u == null) continue;
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(attrs());
                mp.setDataSource(this, u);
                mp.setLooping(true);
                mp.prepare();
                if (am != null) {
                    focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs()).build();
                    am.requestAudioFocus(focus);
                }
                boolean ramp = AlarmStore.ramp(this);
                mp.setVolume(ramp ? 0.1f : 1f, ramp ? 0.1f : 1f);
                mp.start();
                player = mp;
                if (ramp) {
                    rampStart = System.currentTimeMillis();
                    h.post(this::rampStep);
                }
                return;
            } catch (Exception ignored) {
                // 지워진 음악 파일 등: 다음 후보
            }
        }
    }

    private void rampStep() {
        if (player == null) return;
        float t = Math.min(1f, (System.currentTimeMillis() - rampStart) / (float) RAMP_MS);
        float v = 0.1f + 0.9f * t;
        try { player.setVolume(v, v); } catch (Exception ignored) {}
        if (t < 1f) h.postDelayed(this::rampStep, 1000);
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        vibrator = getSystemService(Vibrator.class);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, 0), attrs());
    }

    private void quiet() {
        h.removeCallbacksAndMessages(null);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
        if (vibrator != null) vibrator.cancel();
        if (focus != null) {
            AudioManager am = getSystemService(AudioManager.class);
            if (am != null) am.abandonAudioFocusRequest(focus);
            focus = null;
        }
    }

    @Override
    public void onDestroy() {
        quiet();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
