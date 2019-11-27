package com.ycchen.poetry;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author ycchen
 * @Description: BroadcastReceiver广播接收者，更新数据
 * @since 2019/11/27 11:55
 */
public class PoetryWidgetProvider extends AppWidgetProvider {
    private static Set idsSet = new HashSet();

    // onUpdate()在更新 widget时，被执行
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            idsSet.add(Integer.valueOf(appWidgetId));
        }
    }

    // 当widget被初次添加或者当widget的大小被改变时，被调用
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId,
                newOptions);
    }

    // widget被删除时调用
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            idsSet.remove(Integer.valueOf(appWidgetId));
        }
        super.onDeleted(context, appWidgetIds);
    }

    // 第一个widget被创建时调用
    @Override
    public void onEnabled(Context context) {
        Log.i("CHENYINCHAO", "onEnabled");
        startServie(context);
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        Intent intent = new Intent(context, PoetryWidgetService.class);
        context.stopService(intent);
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.i("CHENYINCHAO", "onReceive:Action: " + action);
        if (PoetryWidgetService.ACTION_UPDATE_POETRY_CONTENT.equals(action)) {
            Bundle extras = intent.getExtras();
            PoetryBean poetryBean = (PoetryBean) extras.getSerializable("poetryBean");
            String origin = poetryBean.getOrigin();
            String author = poetryBean.getAuthor();
            String content = poetryBean.getContent();
            updateAllAppWidgets(context, AppWidgetManager.getInstance(context), idsSet, origin, author, content);
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            startServie(context);
        }
        super.onReceive(context, intent);
    }

    private void startServie(Context context) {
        Intent intent = new Intent(context, PoetryWidgetService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void updateAllAppWidgets(Context context, AppWidgetManager appWidgetManager, Set set, String origin, String author, String content) {
        Log.i("CHENYINCHAO", "updateAllAppWidgets(): size=" + set.size());
        int appID;
        Iterator it = set.iterator();
        while (it.hasNext()) {
            appID = ((Integer) it.next()).intValue();
            RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.poetry_widget_layout);
            remoteView.setTextViewText(R.id.text_origin, origin);
            remoteView.setTextViewText(R.id.text_author, author);
            remoteView.setTextViewText(R.id.text_content, content);
            appWidgetManager.updateAppWidget(appID, remoteView);
        }
    }
}
