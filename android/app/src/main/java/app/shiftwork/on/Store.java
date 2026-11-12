package app.shiftwork.on;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import app.shiftwork.on.core.ShiftCalc;

/** 위젯이 쓰는 근무표와 서버 접속 정보를 이 기기에 저장한다. */
public final class Store {
    private static final String PREFS = "shiftwork";
    private static final String SCHEDULE = "schedule";
    private static final String REMOTE = "remote";

    private Store() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** {"anchor_date":"2026-11-12","anchor_shift":"일","overrides":[{"day":"2026-11-15","shift":"야"}]} */
    public static void saveSchedule(Context c, String json) {
        prefs(c).edit().putString(SCHEDULE, json).apply();
    }

    /** {"url":"https://xxx.supabase.co","anonKey":"...","token":"..."} 또는 "" */
    public static void saveRemote(Context c, String json) {
        prefs(c).edit().putString(REMOTE, json == null ? "" : json).apply();
    }

    public static JSONObject remote(Context c) {
        String j = prefs(c).getString(REMOTE, "");
        if (j == null || j.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(j);
            return o.optString("url").isEmpty() || o.optString("token").isEmpty() ? null : o;
        } catch (Exception e) {
            return null;
        }
    }

    /** 저장된 근무표가 없거나 읽을 수 없으면 null(위젯은 "앱을 열어 주세요"를 보여 준다). */
    public static ShiftCalc calc(Context c) {
        String j = prefs(c).getString(SCHEDULE, null);
        if (j == null) return null;
        try {
            JSONObject o = new JSONObject(j);
            Map<LocalDate, String> overrides = new HashMap<>();
            JSONArray arr = o.optJSONArray("overrides");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject r = arr.getJSONObject(i);
                    overrides.put(LocalDate.parse(r.getString("day")), r.getString("shift"));
                }
            }
            return new ShiftCalc(LocalDate.parse(o.getString("anchor_date")), o.getString("anchor_shift"), overrides);
        } catch (Exception e) {
            return null;
        }
    }
}
