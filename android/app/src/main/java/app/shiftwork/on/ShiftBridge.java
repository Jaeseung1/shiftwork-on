package app.shiftwork.on;

import android.content.Context;
import android.webkit.JavascriptInterface;

import app.shiftwork.on.widget.WidgetUpdater;

/** 웹 앱(sync.js)이 window.ShiftBridge로 부른다. 근무표와 서버 접속 정보를 받아 위젯을 다시 그린다. */
public final class ShiftBridge {
    private final Context ctx;

    public ShiftBridge(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @JavascriptInterface
    public void setSchedule(String json) {
        Store.saveSchedule(ctx, json);
        WidgetUpdater.updateAll(ctx);
    }

    @JavascriptInterface
    public void setRemote(String json) {
        Store.saveRemote(ctx, json);
        RefreshWorker.schedule(ctx);
    }
}
