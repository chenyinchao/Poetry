package com.ycchen.poetry;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author ycchen
 * @Description: 后台服务，发送广播更新数据
 * @since 2019/11/27 14:32
 */
public class PoetryWidgetService extends Service {

    public static final String ACTION_UPDATE_POETRY_CONTENT = "com.ycchen.UPDATE_POETRY_CONTENT";
    public static final String ACTION_RESTART_SERVICE = "com.ycchen.RESTART_SERVICE";
    //    private static final int UPDATE_TIME = 10 * 60 * 1000;
    private static final int UPDATE_TIME = 5000;
    private UpdateThread mUpdateThread;
    private Context mContext;
    private int count = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mUpdateThread = new UpdateThread();
        mUpdateThread.start();
        mContext = this.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    /**
     * Android O以上版本要求广播/服务都要显示启动，并且要加Notification
     * https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1/47533338#47533338
     */
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.preview)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
        }
        super.onDestroy();
        Intent updateIntent = new Intent();
        updateIntent.setAction(ACTION_RESTART_SERVICE);
        updateIntent.setPackage("com.ycchen.poetry");
        mContext.sendBroadcast(updateIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                count = 0;
                while (true) {
                    Log.i("CHENYINCHAO", "count: " + count);
                    count++;
                    requesetHttpData();
                    Thread.sleep(UPDATE_TIME);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void requesetHttpData() {
        try {
            // 古诗词，一言API: https://gushi.ci/
            String url = "https://v1.jinrishici.com/all.json";
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(url).get().build();
            final Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i("CHENYINCHAO", "onFailure");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String reponseBody = response.body().string();
                    Log.i("CHENYINCHAO", "onResponse: reponseBody：" + reponseBody);
                    if (response.isSuccessful()) {
                        PoetryBean poetryBean =
                                new Gson().fromJson(reponseBody, PoetryBean.class);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable("poetryBean", poetryBean);
                        Intent updateIntent = new Intent();
                        updateIntent.setAction(ACTION_UPDATE_POETRY_CONTENT);
                        updateIntent.setPackage("com.ycchen.poetry");
                        updateIntent.putExtras(bundle);
                        mContext.sendBroadcast(updateIntent);
                    }
                }
            });
        } catch (Exception e) {
            ToastUtil.showToast(mContext, "请求失败");
            Log.i("CHENYINCHAO", "Exception: 请求失败" + ", e: " + e.getMessage());
        }
    }
}

