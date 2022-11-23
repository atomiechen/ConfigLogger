package com.atomie.configlogger;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationListener extends NotificationListenerService {
    //需要开启权限：特殊访问权限->通知使用权

    static final public String TAG = "NotificationListener";
    static final public String ACTION_RECORD_MSG = "com.atomie.configlogger.notificationlistener.record_msg";
    static final public String EXTRA_MSG = "com.atomie.configlogger.notificationlistener.msg";

    Context context;
    LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    //ref:https://www.jianshu.com/p/981e7de2c7be
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG,"Notification posted");
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        //获取包名
        String packagename = "packagename: " + sbn.getPackageName();
        Bundle extras = notification.extras;
        String title = null;
        String content = null;
        if (extras != null) {
            // 获取通知标题
            title = "title: " + extras.getString(Notification.EXTRA_TITLE, "");
            // 获取通知内容
            content = "content: " + extras.getString(Notification.EXTRA_TEXT, "");
        }
        // broadcast to update UI
        broadcast(packagename);
        broadcast(title);
        broadcast(content);
//        Log.i(TAG,title);
//        Log.i(TAG,content);
//        Log.i(TAG,packagename);

        if (ConfigLogService.isRunning()) {
            String text = packagename + '\n' + title + '\n' + content;
            ConfigLogService.getInstance().changeOverlayText(text);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"Notification removed");
    }

    // send broadcast to notify
    private void broadcast(String msg) {
        Intent intent = new Intent(ACTION_RECORD_MSG);
        if(msg != null)
            intent.putExtra(EXTRA_MSG, msg);
        localBroadcastManager.sendBroadcast(intent);
    }
}
