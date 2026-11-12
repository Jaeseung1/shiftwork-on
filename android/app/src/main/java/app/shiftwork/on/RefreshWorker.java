package app.shiftwork.on;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import app.shiftwork.on.widget.WidgetUpdater;

/**
 * 15분마다(안드로이드 최소 간격) 위젯을 다시 그린다.
 * 서버 정보가 있으면 먼저 Supabase의 widget_schedule(token)으로 근무표를 받아 저장한다(가족이 바꾼 근무 반영).
 * 인터넷이 없으면 저장된 근무표로 그린다.
 */
public final class RefreshWorker extends Worker {
    private static final String NAME = "shiftwork-refresh";

    public RefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context c) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(RefreshWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        JSONObject remote = Store.remote(c);
        if (remote != null) {
            try {
                String body = fetch(remote.getString("url"), remote.optString("anonKey"), remote.getString("token"));
                if (body != null && body.trim().startsWith("{")) Store.saveSchedule(c, body);
            } catch (Exception ignored) {
                // 인터넷이 없거나 서버가 쉬는 중이면 저장된 근무표로 그린다
            }
        }
        WidgetUpdater.updateAll(c);
        return Result.success();
    }

    private static String fetch(String base, String anonKey, String token) throws Exception {
        URL url = new URL(base.replaceAll("/+$", "") + "/rest/v1/rpc/widget_schedule");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("apikey", anonKey);
        con.setRequestProperty("Authorization", "Bearer " + anonKey);
        try (OutputStream out = con.getOutputStream()) {
            out.write(new JSONObject().put("p_token", token).toString().getBytes(StandardCharsets.UTF_8));
        }
        if (con.getResponseCode() != 200) return null;
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            return buf.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }
}
