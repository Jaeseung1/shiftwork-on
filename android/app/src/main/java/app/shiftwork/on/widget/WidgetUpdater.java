package app.shiftwork.on.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

/** 놓여 있는 모든 교대온 위젯을 다시 그린다. */
public final class WidgetUpdater {
    private WidgetUpdater() {}

    public static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        update(c, m, Widget4x1.class, WidgetRender.Kind.W4x1);
        update(c, m, Widget2x1.class, WidgetRender.Kind.W2x1);
        update(c, m, Widget1x1.class, WidgetRender.Kind.W1x1);
    }

    private static void update(Context c, AppWidgetManager m, Class<?> cls, WidgetRender.Kind kind) {
        int[] ids = m.getAppWidgetIds(new ComponentName(c, cls));
        if (ids != null && ids.length > 0) m.updateAppWidget(ids, WidgetRender.build(c, kind));
    }
}
