package app.shiftwork.on;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import app.shiftwork.on.widget.WidgetUpdater;

/**
 * 앱 화면은 웹 앱(index.html)을 그대로 띄운다.
 * - WEB_APP_URL이 있으면 그 주소(예: Vercel)를, 없으면 앱 안에 든 웹 파일을 연다.
 * - 화면 자동 업데이트(1.0~): 앱 안 주소(appassets)로 화면 파일을 열 때마다 UPDATE_BASE(GitHub Pages)의 최신 파일을 먼저 받아 쓰고,
 *   인터넷이 안 되면 앱에 든 파일을 쓴다. 주소(origin)가 그대로라 로그인·근무표 저장이 유지된다.
 *   앱이 새 APK를 받거나 설치하지는 않는다(삼성 보안 기능이 막을 수 있어서). 위젯 모양 같은 앱 자체 변경만 새 APK가 필요하다.
 * - 웹 앱은 window.ShiftBridge로 근무표와 위젯 토큰을 넘긴다(sync.js).
 */
public class MainActivity extends Activity {
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new ShiftBridge(this), "ShiftBridge");

        String appUrl = BuildConfig.WEB_APP_URL;
        final String home = appUrl.isEmpty() ? "https://" + ASSET_HOST + "/assets/web/index.html" : appUrl;
        final String homeHost = Uri.parse(home).getHost();
        final WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse fresh = fromWeb(request.getUrl());
                return fresh != null ? fresh : assets.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 앱 주소 밖의 링크는 브라우저로 연다(위젯 연결 통로가 다른 사이트에 노출되지 않게)
                Uri u = request.getUrl();
                if (homeHost != null && homeHost.equals(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(home);
        RefreshWorker.schedule(this);
    }

    private static final String WEB_PREFIX = "/assets/web/";

    /** 앱 안 화면 파일 주소 → 웹의 같은 파일 주소. 화면 파일이 아니면 null. */
    static String remoteUrl(Uri u, String base) {
        String path = u.getPath();
        if (base.isEmpty() || !ASSET_HOST.equals(u.getHost()) || path == null || !path.startsWith(WEB_PREFIX)) return null;
        String rel = path.substring(WEB_PREFIX.length());
        if (rel.isEmpty() || rel.contains("..") || mime(rel) == null) return null;
        return base + rel;
    }

    static String mime(String rel) {
        return rel.endsWith(".html") ? "text/html" : rel.endsWith(".js") ? "text/javascript"
                : rel.endsWith(".png") ? "image/png" : rel.endsWith(".webmanifest") ? "application/manifest+json" : null;
    }

    /** 앱 안 화면 파일 요청을 웹의 최신 파일로 바꿔 준다. 실패하면 null(앱에 든 파일을 쓴다). 백그라운드 스레드에서 불린다. */
    private static WebResourceResponse fromWeb(Uri u) {
        String url = remoteUrl(u, BuildConfig.UPDATE_BASE);
        if (url == null) return null;
        String mime = mime(url);
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(2500);
            c.setReadTimeout(3000);
            c.setInstanceFollowRedirects(false);
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > 4 * 1024 * 1024) return null;
                }
                return new WebResourceResponse(mime, mime.startsWith("text/") ? "UTF-8" : null, new ByteArrayInputStream(out.toByteArray()));
            }
        } catch (Exception e) {
            return null; // 인터넷 없음 등: 앱에 든 파일
        } finally {
            if (c != null) c.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WidgetUpdater.updateAll(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
