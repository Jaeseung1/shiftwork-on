package app.shiftwork.on;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 새 버전 알림. 웹(GitHub Pages)의 downloads/version.json을 읽어 지금 앱보다 versionCode가 크면 알려 주고,
 * "받기"를 누르면 브라우저로 새 APK를 받는다(같은 서명 키라 덮어 설치되고 근무표는 남는다).
 * version.json 예: {"versionCode":4,"versionName":"0.4","url":"https://…/app-0.4.apk","notes":"화면을 다듬었어요"}
 * 앱을 열 때는 하루에 한 번만 확인하고, 설정의 "업데이트 확인"은 바로 확인한다.
 */
public final class UpdateChecker {
    private static final long DAY_MS = 24L * 3600 * 1000;

    private UpdateChecker() {}

    public static void check(Activity a, boolean manual) {
        String url = BuildConfig.UPDATE_URL;
        if (url.isEmpty()) return;
        SharedPreferences p = a.getSharedPreferences("update", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!manual && now - p.getLong("last", 0) < DAY_MS) return;
        new Thread(() -> {
            try {
                JSONObject j = new JSONObject(fetch(url));
                p.edit().putLong("last", now).apply();
                int code = j.getInt("versionCode");
                String name = j.optString("versionName", String.valueOf(code));
                String apk = j.getString("url");
                String notes = j.optString("notes", "");
                if (code > BuildConfig.VERSION_CODE && apk.startsWith("https://")) {
                    a.runOnUiThread(() -> show(a, name, apk, notes));
                } else if (manual) {
                    a.runOnUiThread(() -> Toast.makeText(a, "최신 버전(" + BuildConfig.VERSION_NAME + ")이에요", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (manual) a.runOnUiThread(() -> Toast.makeText(a, "업데이트를 확인하지 못했어요. 인터넷 연결을 확인해 주세요", Toast.LENGTH_SHORT).show());
            }
        }, "update-check").start();
    }

    private static void show(Activity a, String name, String apk, String notes) {
        if (a.isFinishing()) return;
        String msg = (notes.isEmpty() ? "" : notes + "\n\n") + "받은 파일을 열어 설치하면 돼요. 근무표와 위젯은 그대로 남아요.";
        new AlertDialog.Builder(a)
                .setTitle("새 버전 " + name + "이 나왔어요")
                .setMessage(msg)
                .setPositiveButton("받기", (d, w) -> a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk))))
                .setNegativeButton("나중에", null)
                .show();
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url + "?t=" + System.currentTimeMillis()).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setUseCaches(false);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < 64 * 1024) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }
}
