package app.shiftwork.on.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 알람을 끈 뒤 열 수 있는 음악 앱 목록. 폰에 깔린 것만 보인다.
 * 모든 앱 목록 권한(QUERY_ALL_PACKAGES)은 쓰지 않고, 매니페스트 <queries>에 적은 음악 앱과 '음악' 종류 앱만 본다.
 */
public final class MusicApps {
    /** 매니페스트 <queries>와 같은 목록 */
    static final String[] KNOWN = {
            "com.iloen.melon", "com.google.android.apps.youtube.music", "com.google.android.youtube", "com.spotify.music",
            "com.ktmusic.geniemusic", "skplanet.musicmate", "com.neowiz.android.bugs", "com.naver.vibe",
            "com.sec.android.app.music", "com.apple.android.music", "com.soundcloud.android"};

    private MusicApps() {}

    public static String json(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, String> apps = new LinkedHashMap<>();
        for (String pkg : KNOWN) add(pm, apps, pkg);
        try {
            Intent music = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            for (ResolveInfo r : pm.queryIntentActivities(music, 0)) add(pm, apps, r.activityInfo.packageName);
        } catch (Exception ignored) {
        }
        JSONArray a = new JSONArray();
        try {
            for (Map.Entry<String, String> e : apps.entrySet()) a.put(new JSONObject().put("pkg", e.getKey()).put("name", e.getValue()));
        } catch (Exception ignored) {
        }
        return a.toString();
    }

    private static void add(PackageManager pm, Map<String, String> apps, String pkg) {
        if (apps.containsKey(pkg) || pm.getLaunchIntentForPackage(pkg) == null) return;
        try {
            apps.put(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString());
        } catch (Exception ignored) {
        }
    }
}
