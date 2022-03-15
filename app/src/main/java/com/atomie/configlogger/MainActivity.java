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
import android.os.Handler;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.atomie.configlogger.MESSAGE";

    SeekBar lightBar;
    TextView textView;
    TextView contObserver;
    TextView broadReceiver;

    Context context;
    AudioManager audioManager;

    int brightness;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            broadReceiver.setText(broadReceiver.getText()+"\n"+action);
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
            String info;
            if (uri == null) {
                info = "uri == null\n";
            } else {
                info = uri + "\n";
                if (uri.equals(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS))) {
                    brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
                    int progress = Math.round((float)brightness*100/256);
                    lightBar.setProgress(progress);
                    textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);

                    info += "Brightness value: "+ brightness;
                    Log.i("MainActivity", info);
                } else if (uri.equals(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE))) {
                    int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                    if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        info += "Brightness mode: manual";
                    } else {
                        info += "Brightness mode: auto";
                    }
                } else if (uri.equals(Settings.System.getUriFor(Settings.Global.AIRPLANE_MODE_ON))) {
                    int v = Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                    info += "Airplane mode on: "+ v;
                    Log.i("MainActivity", info);
                } else if (uri.toString().equals("content://settings/system/volume_music_speaker")) {
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    info += "Volume: "+ currentVolume;
                    Log.i("MainActivity", info);
                } else {
                    info += "Unknown content change";
                }
            }
            contObserver.setText(contObserver.getText()+"\n"+info);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lightBar = findViewById(R.id.seekBar);
        textView = findViewById(R.id.textView);
        contObserver = findViewById(R.id.textView_contentObserver);
        broadReceiver = findViewById(R.id.textView_broadcastReceiver);

        // set scrollable
        contObserver.setMovementMethod(new ScrollingMovementMethod());
        broadReceiver.setMovementMethod(new ScrollingMovementMethod());

        // save text when frozen
        // ref: https://stackoverflow.com/a/31541484/11854304
        contObserver.setFreezesText(true);
        broadReceiver.setFreezesText(true);

        context = getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        int progress = Math.round((float)brightness*100/256);
        lightBar.setProgress(progress);
        textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);

        // Listen to SeekBar changes
        lightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightness = Math.round((float)progress*256/100);
                textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
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

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SET_WALLPAPER);
        registerReceiver(broadcastReceiver, filter);

        // register content observer
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, contentObserver);
        getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, contentObserver);
//        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, contentObserver);
//        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, contentObserver);
//        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, contentObserver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister broadcast receiver
        unregisterReceiver(broadcastReceiver);
        // unregister content observer
        getContentResolver().unregisterContentObserver(contentObserver);
    }

    // Listen to the volume keys
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Toast.makeText(context, "Volume down", Toast.LENGTH_SHORT).show();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                Toast.makeText(context, "Volume up", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void sendMessage(View view) {
        // Do something
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editTextTextPersonName);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}