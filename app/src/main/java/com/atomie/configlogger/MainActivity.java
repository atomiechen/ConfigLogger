package com.atomie.configlogger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // listening
    final Uri [] listenedURIs = {
            Settings.System.CONTENT_URI,
            Settings.Global.CONTENT_URI,
    };
    final String [] listenedActions = {
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_DOCK_EVENT,
            Intent.ACTION_DREAMING_STARTED,
            Intent.ACTION_DREAMING_STOPPED,
            Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE,
            Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE,
            Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_INPUT_METHOD_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MEDIA_BAD_REMOVAL,
            Intent.ACTION_MEDIA_BUTTON,
            Intent.ACTION_MEDIA_CHECKING,
            Intent.ACTION_MEDIA_EJECT,
            Intent.ACTION_MEDIA_MOUNTED,
            Intent.ACTION_MEDIA_NOFS,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_SCANNER_FINISHED,
            Intent.ACTION_MEDIA_SCANNER_STARTED,
            Intent.ACTION_MEDIA_SHARED,
            Intent.ACTION_MEDIA_UNMOUNTABLE,
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGES_SUSPENDED,
            Intent.ACTION_PACKAGES_UNSUSPENDED,
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_DATA_CLEARED,
            Intent.ACTION_PACKAGE_FIRST_LAUNCH,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_RESTARTED,
            Intent.ACTION_PACKAGE_VERIFIED,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_PROVIDER_CHANGED,
            Intent.ACTION_REBOOT,
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SHUTDOWN,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_UID_REMOVED,
            Intent.ACTION_USER_BACKGROUND,
            Intent.ACTION_USER_FOREGROUND,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_USER_UNLOCKED,
    };

    // recording related
    List<String> data = new ArrayList<>();
    String filename = "log.tsv";
    FileWriter writer;
    int brightness;
    static final HashMap<String, Integer> volume = new HashMap<>();
    static {
        volume.put("volume_music_speaker", 0);
        volume.put("volume_ring_speaker", 0);
        volume.put("volume_alarm_speaker", 0);
        volume.put("volume_voice_speaker", 0);
        volume.put("volume_tts_speaker", 0);
    }

    SeekBar lightBar;
    TextView textView;
    TextView contObserver;

    Context context;
    AudioManager audioManager;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        final ArrayList<String> tags = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int value = 0;
            tags.clear();

            // get extra paras into JSON string
            Bundle extras = intent.getExtras();
            if (extras != null) {
                JSONObject json = new JSONObject();
                for (String key : extras.keySet()) {
                    try {
                        json.put(key, JSONObject.wrap(extras.get(key)));
                    } catch(JSONException e) {
                        //Handle exception here
                        e.printStackTrace();
                    }
                }
                tags.add(json.toString());
            }

            // record additional information for some special actions
            switch (action) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    Configuration config = getResources().getConfiguration();
                    tags.add(config.toString());
                    value = config.orientation;
                    break;
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SCREEN_ON:
                    // ref: https://stackoverflow.com/a/17348755/11854304
                    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                    if (dm != null) {
                        Display [] displays = dm.getDisplays();
                        int [] states = new int[displays.length];
                        for (int i = 0; i < displays.length; i++) {
                            states[i] = displays[i].getState();
                        }
                        tags.add(Arrays.toString(states));
                    }
                    break;
            }
            
            // record data
            record(action, value, tags.toArray(new String[0]));
            // print data
            String msg = action + "\n" + value + "\n" + String.join("\n", tags);
            Log.i("broadReceiver", msg);
            addMessage(contObserver, msg);
        }
    };

    // ref: https://stackoverflow.com/a/67355428/11854304
    ContentObserver contentObserver = new ContentObserver(new Handler()) {
        final ArrayList<String> tags = new ArrayList<>();

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            tags.clear();
            String key = "";
            int value = 0;

            if (uri == null) {
                key = "uri_null";
            } else {
                key = uri.toString();
                String database_key = uri.getLastPathSegment();
                String inter = uri.getPathSegments().get(0);
                if ("system".equals(inter)) {
                    value = Settings.System.getInt(getContentResolver(), database_key, value);
                    tags.add(Settings.System.getString(getContentResolver(), database_key));
                } else if ("global".equals(inter)) {
                    value = Settings.Global.getInt(getContentResolver(), database_key, value);
                    tags.add(Settings.Global.getString(getContentResolver(), database_key));
                }

                // record special information
                if (Settings.System.SCREEN_BRIGHTNESS.equals(database_key)) {
                    // record brightness value difference and update
                    int diff = value - brightness;
                    tags.add("diff:" + Integer.toString(diff));
                    brightness = value;
                    // record brightness mode
                    int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
                    if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        tags.add("mode:man");
                    } else if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        tags.add("mode:auto");
                    } else {
                        tags.add("mode:unknown");
                    }
                }
                if (volume.containsKey(database_key)) {
                    // record volume value difference and update
                    int diff = value - volume.get(database_key);
                    tags.add("diff:" + Integer.toString(diff));
                    volume.put(database_key, value);
                }

                // update UI
                if (Settings.System.SCREEN_BRIGHTNESS.equals(database_key)) {
                    int progress = Math.round((float)value*100/256);
                    lightBar.setProgress(progress);
                    textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + value);
                }
            }

            // record data
            record(key, value, tags.toArray(new String[0]));
            // print data
            String msg = key + "\n" + value + "\n" + String.join("\n", tags);
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
    public void record(String key, int value, String... tag) {
        long cur_timestamp = System.currentTimeMillis();
        // record to memory
        String [] paras;
        if (tag != null) {
            paras = new String[tag.length+3];
            System.arraycopy(tag, 0, paras, 3, tag.length);
        } else {
            paras = new String[3];
        }
        paras[0] = Long.toString(cur_timestamp);
        paras[1] = key;
        paras[2] = Integer.toString(value);
        String line = String.join("\t", paras);
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

    void record_settings(String key, Class<?> c) {
        Field[] fields_glb = c.getFields();
        for (Field f : fields_glb) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    String name = f.getName();
                    String database_key = f.get(null).toString();
                    Method method = c.getMethod("getString", ContentResolver.class, String.class);
                    String value_s = (String) method.invoke(null, getContentResolver(), database_key);

                    record(key, 0, name, database_key, value_s);
                    String msg = key + " " + name + "\n" + database_key + ": " + value_s;
                    Log.i("contObserver", msg);
                    addMessage(contObserver, msg);
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void record_all() {
        // record brightness
        String key_bri = "static_brightness";
        brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        record(key_bri, brightness);
        addMessage(contObserver, key_bri + " brightness: "+brightness);

        // record volumes
        String key_vol = "static_volume";
        volume.replaceAll((k, v) -> Settings.System.getInt(getContentResolver(), k, 0));
        volume.forEach((k, v) -> {
            record(key_vol, v, k);
            addMessage(contObserver, key_vol + " " + k + ": " + v);
        });

        // record configuration
        String key_cfg = "static_config";
        Configuration config = getResources().getConfiguration();
        String tag = config.toString();
        record(key_cfg, 0, tag);
        addMessage(contObserver, key_cfg + "\n" + tag);


        // record system settings
        String key_sys = "static_system";
        record_settings(key_sys, Settings.System.class);
        
        // record global settings
        String key_glb = "static_global";
        record_settings(key_glb, Settings.Global.class);
    }

    void initialize() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        for (String action : listenedActions) {
            filter.addAction(action);
        }
        registerReceiver(broadcastReceiver, filter);

        // register content observer
        for (Uri uri : listenedURIs) {
            getContentResolver().registerContentObserver(uri, true, contentObserver);
        }

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

        // record all current values
        record_all();
    }

    void terminate() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        lightBar = findViewById(R.id.seekBar);
        textView = findViewById(R.id.textView);
        contObserver = findViewById(R.id.textView_contentObserver);

        // set scrollable
        contObserver.setMovementMethod(new ScrollingMovementMethod());
        contObserver.setScrollbarFadingEnabled(false);

        // save text when frozen
        // ref: https://stackoverflow.com/a/31541484/11854304
        contObserver.setFreezesText(true);

        // initialization
        initialize();

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
        terminate();
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
        addMessage(contObserver, key + " " + tag + ": " + value);
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
        addMessage(contObserver, key + " " + tag + ": " + value);
        return super.onKeyUp(keyCode, event);
    }
}