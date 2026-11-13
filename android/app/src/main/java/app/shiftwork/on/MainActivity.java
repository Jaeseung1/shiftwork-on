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

import app.shiftwork.on.widget.WidgetUpdater;

/**
 * 앱 화면은 웹 앱(index.html)을 그대로 띄운다.
 * - WEB_APP_URL이 있으면 그 주소(예: Vercel)를, 없으면 앱 안에 든 웹 파일을 연다.
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
                return assets.shouldInterceptRequest(request.getUrl());
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
