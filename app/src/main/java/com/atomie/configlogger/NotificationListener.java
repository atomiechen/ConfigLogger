package com.atomie.configlogger;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
//        requestRebind(new ComponentName(this, this.getClass()));
    }

    @Override
    public void onListenerConnected() {
//        requestRebind(new ComponentName(this, this.getClass()));
        super.onListenerConnected();
    }

    @Override
    public void onListenerDisconnected() {
//        requestRebind(new ComponentName(this, this.getClass()));
        super.onListenerDisconnected();
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
        String text = packagename + '\n' + title + '\n' + content;
        broadcast(text);

        if (ConfigLogService.isRunning()) {
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
