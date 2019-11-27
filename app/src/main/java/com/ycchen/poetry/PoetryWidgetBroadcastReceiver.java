package com.ycchen.poetry;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
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
public class PoetryWidgetBroadcastReceiver extends AppWidgetProvider {
    private static Set idsSet = new HashSet();

    // 更新widget时
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            idsSet.add(Integer.valueOf(appWidgetId));
        }
    }

    // widget初次添加或者大小改变时调用
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    // widget被删除时
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.i("CHENYINCHAO", "onDeleted");
        for (int appWidgetId : appWidgetIds) {
            idsSet.remove(Integer.valueOf(appWidgetId));
        }
        super.onDeleted(context, appWidgetIds);
    }

    // 创建widget调用
    @Override
    public void onEnabled(Context context) {
        Log.i("CHENYINCHAO", "onEnabled");
        startServie(context);
        super.onEnabled(context);
    }

    // 当最后1个widget的实例被删除时触发
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
        switch (action) {
            case PoetryWidgetService.ACTION_UPDATE_POETRY_CONTENT:
                Bundle extras = intent.getExtras();
                PoetryBean poetryBean = (PoetryBean) extras.getSerializable("poetryBean");
                String origin = poetryBean.getOrigin();
                String author = poetryBean.getAuthor();
                String content = poetryBean.getContent();
                updateAllAppWidgets(context, AppWidgetManager.getInstance(context), idsSet, origin, author, content);
                break;
            case ConnectivityManager.CONNECTIVITY_ACTION:
                // 若有连上网了，启动服务
                startServie(context);
                break;
            default:
                break;
        }
        super.onReceive(context, intent);
    }

    private void startServie(Context context) {
        /**
         * android8.0以上通过startForegroundService启动service,
         * 参考：https://blog.csdn.net/huaheshangxo/article/details/82856388
         */
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
