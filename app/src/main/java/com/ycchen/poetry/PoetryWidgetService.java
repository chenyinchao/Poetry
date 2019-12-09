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

    // 十分钟
    private static final int UPDATE_TIME = 10 * 60 * 1000;

    // 5秒钟
//    private static final int UPDATE_TIME = 5000;

    private static final int HTTP_REQUEST_ERROR = 11;

    private Context mContext;

    private String mNotificationId = "channelId";

    private String mNotificationName = "channelName";

    private Runnable mRunnable;

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
                            PoetryWidgetBroadcastReceiver.logtest.info("60秒还没有连上网络，停止网络请求Runnable");
                            mHandler.removeCallbacks(mRunnable);
                        }
                    }
                }, 60 * 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
        if (mPoetryWidgetBroadcastReceiver == null) {
            mPoetryWidgetBroadcastReceiver = new PoetryWidgetBroadcastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        // 熄屏
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        // 解锁
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
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
        mRunnable = new Runnable() {
            @Override
            public void run() {
                PoetryWidgetBroadcastReceiver.logtest.info("定时执行");
                mHandler.postDelayed(this, UPDATE_TIME);
                requesetHttpData(mContext);
            }
        };
        mHandler.post(mRunnable);

        mPoetryWidgetBroadcastReceiver.setDealWithHandlerMessage(new PoetryWidgetBroadcastReceiver.DealWithHandlerMessage() {
            @Override
            public void stopHandlerPost() {
                // 熄屏停止循环
                mHandler.removeCallbacks(mRunnable);
                PoetryWidgetBroadcastReceiver.logtest.info("removeCallbacks");
            }

            @Override
            public void continueHandlerPost() {
                // 解锁继续循环
                mHandler.post(mRunnable);
                PoetryWidgetBroadcastReceiver.logtest.info("continueHandlerPost");
            }
        });
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
        PoetryWidgetBroadcastReceiver.logtest.info("onDestroy");
        unregisterReceiver(mPoetryWidgetBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PoetryWidgetBroadcastReceiver.logtest.info("onStartCommand");
        // 保活
        return START_STICKY;
    }

    public void requesetHttpData(final Context context) {
        try {
            // 古诗词，一言API: https://gushi.ci/
            String url = "https://v1.jinrishici.com/all.json";
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(url).get().build();
            final Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    PoetryWidgetBroadcastReceiver.logtest.info("onFailure");
                    Message message = mHandler.obtainMessage();
                    message.what = HTTP_REQUEST_ERROR;
                    mHandler.sendMessage(message);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String reponseBody = response.body().string();
                    PoetryWidgetBroadcastReceiver.logtest.info("reponseBody: " + reponseBody);
                    if (response.isSuccessful()) {
                        PoetryBean poetryBean =
                                new Gson().fromJson(reponseBody, PoetryBean.class);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable("poetryBean", poetryBean);
                        Intent updateIntent = new Intent();
                        updateIntent.setAction(ACTION_UPDATE_POETRY_CONTENT);
                        updateIntent.setPackage("com.ycchen.poetry");
                        updateIntent.putExtras(bundle);
                        context.sendBroadcast(updateIntent);
                    }
                }
            });
        } catch (Exception e) {
            ToastUtil.showToastLong(context, "请求失败");
            PoetryWidgetBroadcastReceiver.logtest.info("Exception: 请求失败" + ", e: " + e.getMessage());
        }
    }
}

