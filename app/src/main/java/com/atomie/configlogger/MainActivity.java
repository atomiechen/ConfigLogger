package com.atomie.configlogger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    Context context;
    LocalBroadcastManager localBroadcastManager;

    SeekBar lightBar;
    TextView textView;
    TextView logTextView;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConfigLogService.ACTION_RECORD_MSG.equals(action)) {
                String msg = intent.getStringExtra(ConfigLogService.EXTRA_MSG);
                Log.i("broadReceiver", msg);
                addMessage(logTextView, msg);
            }
            if (NotificationListener.ACTION_RECORD_MSG.equals(action)) {
                String msg = intent.getStringExtra(NotificationListener.EXTRA_MSG);
                addMessage(logTextView, msg);
            }
        }
    };

    // Append a string to a TextView as a new line
    // 1. erase excessive lines
    // 2. scroll to the bottom if needed
    private void addMessage(TextView mTextView, String msg) {
        // append the new string
        mTextView.append("\n" + msg);

        // Erase excessive lines
        // ref: https://stackoverflow.com/a/10312621/11854304
        final int MAX_LINES = 1000;
        int excessLineNumber = mTextView.getLineCount() - MAX_LINES;
        if (excessLineNumber > 0) {
            int eolIndex = -1;
            CharSequence charSequence = mTextView.getText();
            for (int i=0; i<excessLineNumber; i++) {
                do {
                    eolIndex++;
                } while(eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');
            }
            if (eolIndex < charSequence.length()) {
                mTextView.getEditableText().delete(0, eolIndex+1);
            } else {
                mTextView.setText("");
            }
        }

        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        // ref: https://stackoverflow.com/a/7350267/11854304
        Layout layout = mTextView.getLayout();
        if (layout == null)
            return;
        final int scrollAmount = layout.getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            mTextView.scrollTo(0, scrollAmount);
    }

    public void clickStartService(View view) {
        ComponentName ret = startService(new Intent(this, ConfigLogService.class));
        if (ret != null) {
            addMessage(logTextView, "SERVICE started!");
        } else {
            addMessage(logTextView, "SERVICE failed to start!!!");
        }
    }

    public void clickStopService(View view) {
        boolean ret = stopService(new Intent(this, ConfigLogService.class));
        if (ret) {
            addMessage(logTextView, "SERVICE stopped!");
        } else {
            addMessage(logTextView, "SERVICE already stopped!");
        }
    }

    public void toggleOverlay(View view) {
        if (ConfigLogService.isRunning()) {
            ConfigLogService.getInstance().toggleWindow();
        }
    }

    void initialize() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConfigLogService.ACTION_RECORD_MSG);
        filter.addAction(NotificationListener.ACTION_RECORD_MSG);
        localBroadcastManager.registerReceiver(broadcastReceiver , filter);

        // start service
        clickStartService(null);
    }

    void terminate() {
        // unregister broadcast receiver
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    // ref: https://stackoverflow.com/a/14923144/11854304
    public boolean isAccessibilityServiceEnabled(Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    // ref: https://www.jianshu.com/p/981e7de2c7be
    public boolean isNotificationListenerEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    void checkPermissions() {
        try {
            boolean request = false;
            for (String per : PERMISSIONS) {
                int permission = checkSelfPermission(per);
                Log.e(per, Boolean.toString(permission == PackageManager.PERMISSION_GRANTED));
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    request = true;
                }
            }
            if (request)
                requestPermissions(PERMISSIONS, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        lightBar = findViewById(R.id.seekBar);
        textView = findViewById(R.id.textView);
        logTextView = findViewById(R.id.textView_contentObserver);

        // set scrollable
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        logTextView.setScrollbarFadingEnabled(false);

        // save text when frozen
        // ref: https://stackoverflow.com/a/31541484/11854304
        logTextView.setFreezesText(true);

//        int progress = Math.round((float)brightness*100/256);
//        lightBar.setProgress(progress);
//        textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);

        // Listen to SeekBar changes
        lightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int brightness = Math.round((float)progress*256/100);
                textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // require WRITE SETTINGS permission
                if (!Settings.System.canWrite(context)) {
                    Toast.makeText(context, "Cannot write to system settings", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    startActivity(intent);
                }
                if (Settings.System.canWrite(context) && Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0) != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        checkPermissions();

        // jump to accessibility settings
        if (!isAccessibilityServiceEnabled(ConfigLogService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }

        // jump to notification settings
        if (!isNotificationListenerEnabled(this)) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        }

        // initialization
        initialize();
    }

    @Override
    protected void onDestroy() {
        terminate();
        super.onDestroy();
    }
}