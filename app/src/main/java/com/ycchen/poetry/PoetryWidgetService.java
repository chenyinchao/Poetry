package com.ycchen.poetry;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

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
    //    private static final int UPDATE_TIME = 10 * 60 * 1000;
    private static final int UPDATE_TIME = 5000;
    private static final int HTTP_REQUEST_ERROR = 11;
    private UpdateThread mUpdateThread;
    private Context mContext;
    private String mNotificationId = "channelId";
    private String mNotificationName = "channelName";
    private PoetryWidgetBroadcastReceiver mPoetryWidgetBroadcastReceiver;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == HTTP_REQUEST_ERROR) {
                ToastUtil.showToastLong(mContext, "请检查网络状态");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        boolean isMobileDataEnable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
                        boolean isWifiDataEnable = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
                        if (isMobileDataEnable || isWifiDataEnable) {
                            ToastUtil.showToastLong(mContext, "网络正常了");
                        } else {
                            // 给定20秒时间等待连接，若还是没网就结束掉服务，不然一直网络请求
                            stopSelf();
                        }
                    }
                }, 20000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mUpdateThread = new UpdateThread();
        mUpdateThread.start();
        mContext = this.getApplicationContext();
        if (mPoetryWidgetBroadcastReceiver == null) {
            mPoetryWidgetBroadcastReceiver = new PoetryWidgetBroadcastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mPoetryWidgetBroadcastReceiver, intentFilter);
        /**
         * android8.0以上通过startForegroundService启动service,
         * 参考：https://blog.csdn.net/huaheshangxo/article/details/82856388
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建 NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(mNotificationId, mNotificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(1, getNotification());
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.preview)
                .setContentTitle("App is running in background");
        //设置Notification的ChannelID,否则不能正常显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(mNotificationId);
        }
        Notification notification = builder.build();
        return notification;
    }

    @Override
    public void onDestroy() {
        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
        }
        unregisterReceiver(mPoetryWidgetBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 保活
        return START_STICKY;
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                int cout = 0;
                while (true) {
                    Log.i("CHENYINCHAO", "cout: " + cout);
                    cout++;
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
                    Message message = mHandler.obtainMessage();
                    message.what = HTTP_REQUEST_ERROR;
                    mHandler.sendMessage(message);
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

