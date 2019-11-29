package com.ycchen.poetry;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.RemoteViews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ycchen
 * @Description: BroadcastReceiver广播接收者，更新数据
 * @since 2019/11/27 11:55
 */
public class PoetryWidgetBroadcastReceiver extends AppWidgetProvider {

    public static final Logger logtest = LoggerFactory.getLogger("logtest");

    public static final String CLICK_ACTION = "com.ycchen.CLICK_ACTION";

    private static Set idsSet = new HashSet();


    private DealWithHandlerMessage mDealWithHandlerMessage;

    public void setDealWithHandlerMessage(DealWithHandlerMessage dealWithHandlerMessage) {
        this.mDealWithHandlerMessage = dealWithHandlerMessage;
    }

    interface DealWithHandlerMessage {
        // 暂停循环
        void stopHandlerPost();

        // 继续循环
        void continueHandlerPost();
    }

    // 更新widget时
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.poetry_widget_layout);
        //分别绑定id
        remoteViews.setOnClickPendingIntent(R.id.linearlayout, getPendingIntent(context, R.id.linearlayout));
        //更新widget
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    private PendingIntent getPendingIntent(Context context, int resID) {
        Intent intent = new Intent();
        intent.setClass(context, PoetryWidgetBroadcastReceiver.class);
        intent.setAction(CLICK_ACTION);
        //设置data的时候，把控件id一起设置进去，这样点击哪个控件，发送的intent中data中的id就是哪个控件的id
        intent.setData(Uri.parse("id:" + resID));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        return pendingIntent;
    }

    // widget初次添加或者大小改变时调用
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    // widget被删除时
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        logtest.info("onDeleted");
        for (int appWidgetId : appWidgetIds) {
            idsSet.remove(Integer.valueOf(appWidgetId));
        }
        super.onDeleted(context, appWidgetIds);
    }

    // 创建widget调用
    @Override
    public void onEnabled(Context context) {
        logtest.info("onEnabled");
        startServie(context);
        super.onEnabled(context);
    }

    // 当最后一个widget的实例被删除时触发
    @Override
    public void onDisabled(Context context) {
        Intent intent = new Intent(context, PoetryWidgetService.class);
        context.stopService(intent);
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.poetry_widget_layout);
        ComponentName componentName = new ComponentName(context, PoetryWidgetBroadcastReceiver.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        logtest.info("onReceive:Action: " + action);
        switch (action) {
            case PoetryWidgetService.ACTION_UPDATE_POETRY_CONTENT:
                Bundle extras = intent.getExtras();
                PoetryBean poetryBean = (PoetryBean) extras.getSerializable("poetryBean");
                String origin = poetryBean.getOrigin();
                String author = poetryBean.getAuthor();
                String content = poetryBean.getContent();
                remoteViews.setTextViewText(R.id.text_origin, origin);
                remoteViews.setTextViewText(R.id.text_author, author);
                remoteViews.setTextViewText(R.id.text_content, content);
                appWidgetManager.updateAppWidget(componentName, remoteViews);
                break;
            // 解锁，继续请求数据，并且解锁2秒后更新第一条
            case Intent.ACTION_USER_PRESENT:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDealWithHandlerMessage.continueHandlerPost();
                    }
                }, 2000);
                break;
            // 启动服务
            case CLICK_ACTION:
                Uri data = intent.getData();
                int resId = -1;
                if (data != null) {
                    resId = Integer.parseInt(data.getSchemeSpecificPart());
                }
                switch (resId) {
                    case R.id.linearlayout:
                        //  点击更新
                        //  new PoetryWidgetService().requesetHttpData(context);
                        startServie(context);
                        break;
                }
                appWidgetManager.updateAppWidget(componentName, remoteViews);
                break;
            // 熄屏，停止网络请求
            case Intent.ACTION_SCREEN_OFF:
                mDealWithHandlerMessage.stopHandlerPost();
                break;
            default:
                break;
        }
    }

    private void startServie(Context context) {
        if (isServiceRunning(PoetryWidgetService.class.getName(), context)) {
            ToastUtil.showToast(context, "桌面诗词服务正在运行");
            logtest.info("服务正在运行");
            return;
        }
        logtest.info("桌面诗词服务已经启动了，不再startService");
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

    /**
     * 判断服务是否运行
     */
    private boolean isServiceRunning(final String className, Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (info == null || info.size() == 0) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if (className.equals(aInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
