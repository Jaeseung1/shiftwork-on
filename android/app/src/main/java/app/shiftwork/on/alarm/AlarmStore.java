package app.shiftwork.on.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import app.shiftwork.on.core.AlarmPlanner;

/** 근무 알람 설정: 근무별 시각과, 이 기기 근무표가 내 근무표(주인)인지. 공유받은 근무표면 알람을 울리지 않는다. */
public final class AlarmStore {
    private static final String PREFS = "alarm";

    private AlarmStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** {"일":"06:30","야":"15:30"} (켠 근무만) */
    public static void saveRules(Context c, String json) {
        p(c).edit().putString("rules", json == null ? "{}" : json).apply();
    }

    public static String rulesJson(Context c) {
        return p(c).getString("rules", "{}");
    }

    public static Map<String, Integer> rules(Context c) {
        Map<String, Integer> m = new HashMap<>();
        try {
            JSONObject o = new JSONObject(rulesJson(c));
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                Integer v = AlarmPlanner.parse(o.optString(k));
                if (v != null) m.put(k, v);
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    public static void setOwner(Context c, boolean owner) {
        p(c).edit().putBoolean("owner", owner).apply();
    }

    public static boolean owner(Context c) {
        return p(c).getBoolean("owner", false);
    }

    // ---- 울리는 방식(1.4~): 소리, 진동, 점점 크게, 끄면 열 앱 ----
    /** 소리: ""=폰의 기본 알람음, "none"=소리 없음, 그 밖에는 고른 알람음·음악 파일 주소 */
    public static void setSound(Context c, String uri, String name) {
        p(c).edit().putString("sound", uri == null ? "" : uri).putString("soundName", name == null ? "" : name).apply();
    }

    public static String sound(Context c) {
        return p(c).getString("sound", "");
    }

    public static String soundName(Context c) {
        return p(c).getString("soundName", "");
    }

    public static boolean vibrate(Context c) {
        return p(c).getBoolean("vibrate", true);
    }

    public static boolean ramp(Context c) {
        return p(c).getBoolean("ramp", false);
    }

    /** 알람을 끄면 열 앱(패키지 이름, 없으면 "") */
    public static String app(Context c) {
        return p(c).getString("app", "");
    }

    public static void setOption(Context c, String key, String value) {
        SharedPreferences.Editor e = p(c).edit();
        switch (key) {
            case "vibrate": case "ramp": e.putBoolean(key, "true".equals(value)); break;
            case "app": e.putString("app", value == null ? "" : value); break;
            case "sound": // 웹에서는 기본·없음만 고른다(나머지는 앱의 고르기 화면)
                if ("".equals(value) || "none".equals(value)) e.putString("sound", value).putString("soundName", "");
                break;
            default: return;
        }
        e.apply();
    }

    public static String optionsJson(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("sound", sound(c));
            o.put("soundName", soundName(c));
            o.put("vibrate", vibrate(c));
            o.put("ramp", ramp(c));
            o.put("app", app(c));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    static void saveNext(Context c, long at, String shift) {
        p(c).edit().putLong("nextAt", at).putString("nextShift", shift).apply();
    }

    static long nextAt(Context c) {
        return p(c).getLong("nextAt", 0);
    }

    static String nextShift(Context c) {
        return p(c).getString("nextShift", "");
    }
}
