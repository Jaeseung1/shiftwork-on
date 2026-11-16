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
