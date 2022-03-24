package com.atomie.configlogger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

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

    void initialize() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConfigLogService.ACTION_RECORD_MSG);
        localBroadcastManager.registerReceiver(broadcastReceiver , filter);

        // start service
        ComponentName ret = startService(new Intent(this, ConfigLogService.class));
        if (ret != null) {
            addMessage(logTextView, "SERVICE started!");
        } else {
            addMessage(logTextView, "SERVICE failed to start!!!");
        }
    }

    void terminate() {
        // unregister broadcast receiver
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
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

        // initialization
        initialize();
    }

    @Override
    protected void onDestroy() {
        terminate();
        super.onDestroy();
    }

    // Listen to key down events
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String key = "onKeyDown";
        int value = keyCode;
        String tag = "";
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                tag = "volume_down";
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                tag = "volume_up";
                break;
        }
        addMessage(logTextView, key + " " + tag + ": " + value);
        return super.onKeyDown(keyCode, event);
    }

    // Listen to key up events
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        String key = "onKeyUp";
        int value = keyCode;
        String tag = "";
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                tag = "volume_down";
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                tag = "volume_up";
                break;
        }
        addMessage(logTextView, key + " " + tag + ": " + value);
        return super.onKeyUp(keyCode, event);
    }
}