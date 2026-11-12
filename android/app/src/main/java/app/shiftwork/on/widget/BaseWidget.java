package app.shiftwork.on.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import app.shiftwork.on.RefreshWorker;

/** 세 크기 위젯의 공통 부분. 그리는 일은 WidgetRender가 한다. */
abstract class BaseWidget extends AppWidgetProvider {
    abstract WidgetRender.Kind kind();

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        manager.updateAppWidget(ids, WidgetRender.build(context, kind()));
        RefreshWorker.schedule(context);
    }

    @Override
    public void onEnabled(Context context) {
        RefreshWorker.schedule(context);
    }
}
