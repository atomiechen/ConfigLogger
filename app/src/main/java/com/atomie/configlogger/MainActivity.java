package com.atomie.configlogger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // recording related
    List<String> data = new ArrayList<>();
    String filename = "log.csv";
    FileWriter writer;

    SeekBar lightBar;
    TextView textView;
    TextView contObserver;

    Context context;
    AudioManager audioManager;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // record data
            record(action, 0, "");
            // print data
            Log.i("broadReceiver", action);
            addMessage(contObserver, action);
        }
    };

    // ref: https://stackoverflow.com/a/67355428/11854304
    ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            String key = "";
            int value = 0;
            String tag = "";

            if (uri == null) {
                key = "null";
                tag = "Unknown content change";
            } else {
                key = uri.toString();
                String database_key = uri.getLastPathSegment();
                String inter = uri.getPathSegments().get(0);
                if (inter.equals("system")) {
                    value = Settings.System.getInt(getContentResolver(), database_key, value);
                } else if (inter.equals("global")) {
                    value = Settings.Global.getInt(getContentResolver(), database_key, value);
                }
                tag = database_key;

                // update UI
                if (database_key.equals(Settings.System.SCREEN_BRIGHTNESS)) {
                    int progress = Math.round((float)value*100/256);
                    lightBar.setProgress(progress);
                    textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + value);
                }
            }

            // record data
            record(key, value, tag);
            // print data
            String msg = key+"\n"+tag + ": " + value;
            Log.i("contObserver", msg);
            addMessage(contObserver, msg);
        }
    };

    // Checks if a volume containing external storage is available
    // for read and write.
    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    // record data to memory and file
    public void record(String key, int value, String tag) {
        long cur_timestamp = System.currentTimeMillis();
        // record to memory
        String [] paras = {Long.toString(cur_timestamp), key, Integer.toString(value), tag};
        String line = String.join(",", paras);
        data.add(line);
        // record to file
        if (writer != null) {
            try {
                writer.write(line+"\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // update UI
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String cur_datetime = format.format(new Date(cur_timestamp));
        addMessage(contObserver, cur_datetime);
    }

    // Append a string to a TextView as a new line
    // 1. erase excessive lines
    // 2. scroll to the bottom if needed
    private void addMessage(TextView mTextView, String msg) {
        // append the new string
        mTextView.append("\n" + msg);

        // Erase excessive lines
        // ref: https://stackoverflow.com/a/10312621/11854304
        final int MAX_LINES = 200;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_CAMERA_BUTTON);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_NOFS);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
        filter.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        filter.addAction(Intent.ACTION_PACKAGE_FIRST_LAUNCH);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        filter.addAction(Intent.ACTION_PACKAGE_VERIFIED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        filter.addAction(Intent.ACTION_REBOOT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_UID_REMOVED);
        filter.addAction(Intent.ACTION_USER_BACKGROUND);
        filter.addAction(Intent.ACTION_USER_FOREGROUND);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        registerReceiver(broadcastReceiver, filter);

        // register content observer
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, contentObserver);
        getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, contentObserver);

        // recording related
        try {
            File file;
            if (isExternalStorageWritable()) {
                file = new File(getExternalFilesDir(null), filename);
            } else {
                file = new File(getFilesDir(), filename);
            }
            // append to file
            writer = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        context = getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        /*
          UI related code below, not important
         */

        setContentView(R.layout.activity_main);

        lightBar = findViewById(R.id.seekBar);
        textView = findViewById(R.id.textView);
        contObserver = findViewById(R.id.textView_contentObserver);

        // set scrollable
        contObserver.setMovementMethod(new ScrollingMovementMethod());
        contObserver.setScrollbarFadingEnabled(false);

        // save text when frozen
        // ref: https://stackoverflow.com/a/31541484/11854304
        contObserver.setFreezesText(true);

        int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        int progress = Math.round((float)brightness*100/256);
        lightBar.setProgress(progress);
        textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister broadcast receiver
        unregisterReceiver(broadcastReceiver);
        // unregister content observer
        getContentResolver().unregisterContentObserver(contentObserver);

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        record(key, value, tag);
        addMessage(contObserver, key + " " + value + ": " + tag);
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
        record(key, value, tag);
        addMessage(contObserver, key + " " + value + ": " + tag);
        return super.onKeyUp(keyCode, event);
    }
}