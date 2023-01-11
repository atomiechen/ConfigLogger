package com.atomie.configlogger;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.atomie.configlogger.ui.FloatOverlay;
import com.atomie.configlogger.ui.TitleTextWindow;

import org.json.JSONArray;
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
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ConfigLogService extends AccessibilityService {

    static final public String TAG = "ConfigLogService";
    static final public String ACTION_RECORD_MSG = "com.atomie.configlogger.configlogservice.record_msg";
    static final public String EXTRA_MSG = "com.atomie.configlogger.configlogservice.msg";

    private static ConfigLogService self = null;
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static ConfigLogService getInstance() {
        return self;
    }

    public static boolean isRunning() {
        return self != null;
    }

    private final AtomicInteger mLogID = new AtomicInteger(0);
    private final IntUnaryOperator operator = x -> (x < 999)? (x + 1) : 0;

    // UI overlay
    private FloatOverlay floatOverlay;

    // listening
    final Uri[] listenedURIs = {
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
            // Bluetooth related
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothDevice.ACTION_ALIAS_CHANGED,
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_CLASS_CHANGED,
            BluetoothDevice.ACTION_NAME_CHANGED,
            BluetoothDevice.ACTION_PAIRING_REQUEST,
            BluetoothAdapter.ACTION_DISCOVERY_STARTED,
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothAdapter.ACTION_STATE_CHANGED,
            BluetoothAdapter.ACTION_SCAN_MODE_CHANGED,
            BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED,
            // WiFi related
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
            WifiManager.WIFI_STATE_CHANGED_ACTION,
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,
    };

    // recording related
    String filename = "log.tsv";
    FileWriter writer;
    int brightness;
    static final HashMap<String, Integer> volume = new HashMap<>();
    static {
        // speaker
        volume.put("volume_music_speaker", 0);
        volume.put("volume_ring_speaker", 0);
        volume.put("volume_alarm_speaker", 0);
        volume.put("volume_voice_speaker", 0);
        volume.put("volume_tts_speaker", 0);
        // headset
        volume.put("volume_music_headset", 0);
        volume.put("volume_voice_headset", 0);
        volume.put("volume_tts_headset", 0);
        // headphone
        volume.put("volume_music_headphone", 0);
        volume.put("volume_voice_headphone", 0);
        volume.put("volume_tts_headphone", 0);
        // Bluetooth A2DP
        volume.put("volume_music_bt_a2dp", 0);
        volume.put("volume_voice_bt_a2dp", 0);
        volume.put("volume_tts_bt_a2dp", 0);
    }
    String packageName = "";

    LocalBroadcastManager localBroadcastManager;

    void jsonSilentPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject json = new JSONObject();
            String action = intent.getAction();

            // get extra paras into JSON string
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object obj = JSONObject.wrap(extras.get(key));
                    if (obj == null) {
                        obj = JSONObject.wrap(extras.get(key).toString());
                    }
                    jsonSilentPut(json, key, obj);
                }
            }

            // record additional information for some special actions
            switch (action) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    Configuration config = getResources().getConfiguration();
                    jsonSilentPut(json, "configuration", config.toString());
                    jsonSilentPut(json, "orientation", config.orientation);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SCREEN_ON:
                    // ref: https://stackoverflow.com/a/17348755/11854304
                    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                    if (dm != null) {
                        Display[] displays = dm.getDisplays();
                        int [] states = new int[displays.length];
                        for (int i = 0; i < displays.length; i++) {
                            states[i] = displays[i].getState();
                        }
                        jsonSilentPut(json, "displays", states);
                    }
                    break;
            }

            jsonSilentPut(json, "package", packageName);

            // record data
            record("BroadcastReceive", action, "", json.toString());
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
            JSONObject json = new JSONObject();
            String key;
            int value = 0;
            String tag = "";

            if (uri == null) {
                key = "uri_null";
            } else {
                key = uri.toString();
                String database_key = uri.getLastPathSegment();
                String inter = uri.getPathSegments().get(0);
                if ("system".equals(inter)) {
                    value = Settings.System.getInt(getContentResolver(), database_key, value);
                    tag = Settings.System.getString(getContentResolver(), database_key);
                } else if ("global".equals(inter)) {
                    value = Settings.Global.getInt(getContentResolver(), database_key, value);
                    tag = Settings.Global.getString(getContentResolver(), database_key);
                }

                // record special information
                if (Settings.System.SCREEN_BRIGHTNESS.equals(database_key)) {
                    // record brightness value difference and update
                    int diff = value - brightness;
                    jsonSilentPut(json, "diff", diff);
                    brightness = value;
                    // record brightness mode
                    int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
                    if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        jsonSilentPut(json, "mode", "man");
                    } else if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        jsonSilentPut(json, "mode", "auto");
                    } else {
                        jsonSilentPut(json, "mode", "unknown");
                    }
                }
                if (database_key.startsWith("volume_")) {
                    if (!volume.containsKey(database_key)) {
                        // record new volume value
                        volume.put(database_key, value);
                    }
                    // record volume value difference and update
                    int diff = value - volume.put(database_key, value);
                    jsonSilentPut(json, "diff", diff);
                }
            }

            jsonSilentPut(json, "package", packageName);

            // record data
            record("ContentChange", key, tag, json.toString());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        initialize();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (self != null) {
            try {
                self.disableSelf();
                self = null;
            } catch (Exception exc) {
                Log.e(TAG, "清理残留accessibility service失败", exc);
            }
        }
        self = this;
        // initialization about UI overlay
        floatOverlay = new FloatOverlay(this);
        floatOverlay.initOverlay();
    }

    @Override
    public void onDestroy() {
        floatOverlay.destroyOverlay();
        terminate();
        self = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg != null) {
            packageName = event.getPackageName().toString();
        }

        // receive notification accessibility event
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Notification notification = (Notification) event.getParcelableData();
            if (notification == null) return;
            String packagename = "package: " + event.getPackageName();
            String text = "[ACC]\n" + packagename + '\n' + notificationToString(notification);
            // update UI
            broadcast(text);
            floatOverlay.changeOverlayText(text);
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        JSONObject json = new JSONObject();
        jsonSilentPut(json, "code", event.getKeyCode());
        jsonSilentPut(json, "action", event.getAction());
        jsonSilentPut(json, "source", event.getSource());
        jsonSilentPut(json, "eventTime", event.getEventTime());
        jsonSilentPut(json, "downTime", event.getDownTime());
        jsonSilentPut(json, "package", packageName);
        jsonSilentPut(json, "keycodeString", KeyEvent.keyCodeToString(event.getKeyCode()));

        record("KeyEvent", "KeyEvent://"+event.getAction()+"/"+event.getKeyCode(), "", json.toString());
        return super.onKeyEvent(event);
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
        record_all("start");
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

    // Checks if a volume containing external storage is available
    // for read and write.
    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    void record_all(String action) {
        JSONObject json = new JSONObject();

        // store brightness
        brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        jsonSilentPut(json, "brightness", brightness);

        // store volumes
        volume.replaceAll((k, v) -> Settings.System.getInt(getContentResolver(), k, 0));
        volume.forEach((k, v) -> jsonSilentPut(json, k, v));

        // store configuration and orientation
        Configuration config = getResources().getConfiguration();
        jsonSilentPut(json, "configuration", config.toString());
        jsonSilentPut(json, "orientation", config.orientation);

        // store system settings
        jsonPutSettings(json, "system", Settings.System.class);

        // store global settings
        jsonPutSettings(json, "global", Settings.Global.class);

        // store secure settings
        jsonPutSettings(json, "secure", Settings.Secure.class);

        // record
        record("static", action, "", json.toString());
    }

    void jsonPutSettings(JSONObject json, String key, Class<?> c) {
        JSONArray jsonArray = new JSONArray();
        Field[] fields_glb = c.getFields();
        for (Field f : fields_glb) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    String name = f.getName();
                    Object obj = f.get(null);
                    if (obj != null) {
                        String database_key = obj.toString();
                        Method method = c.getMethod("getString", ContentResolver.class, String.class);
                        String value_s = (String) method.invoke(null, getContentResolver(), database_key);
                        jsonArray.put(new JSONArray().put(name).put(database_key).put(value_s));
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        jsonSilentPut(json, key, jsonArray);
    }

    private int incLogID() {
        return mLogID.getAndUpdate(operator);
    }

    // record data to memory and file
    public void record(String type, String action, String tag, String other) {
        long cur_timestamp = System.currentTimeMillis();
        // record to memory
        String [] paras = {Long.toString(cur_timestamp), Integer.toString(incLogID()), type, action, tag, other};
        String line = String.join("\t", paras);
        // record to file
        if (writer != null) {
            try {
                writer.write(line+"\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // broadcast to update UI
        String cur_datetime = format.format(new Date(cur_timestamp));
        paras[0] = " -------- " + cur_datetime + " -------- ";
        broadcast(String.join("\n", paras));
    }

    // send broadcast to notify
    private void broadcast(String msg) {
        Intent intent = new Intent(ACTION_RECORD_MSG);
        if(msg != null)
            intent.putExtra(EXTRA_MSG, msg);
        localBroadcastManager.sendBroadcast(intent);
    }

    static String notificationToString(Notification notification) {
        if (notification == null || notification.extras == null) {
            return "invalid notification";
        }
        Bundle extras = notification.extras;
        // 获取时间
        long timestamp = notification.when;
        String datetime = "time: " + format.format(new Date(timestamp));
        // 获取通知标题
        String title = "title: " + extras.getString(Notification.EXTRA_TITLE, "");
        // 获取通知内容
        String text = "text: " + extras.getString(Notification.EXTRA_TEXT, "");
        String summaryText = "summaryText: " + extras.getString(Notification.EXTRA_SUMMARY_TEXT, "");
        String bigText = "bigText: " + extras.getString(Notification.EXTRA_BIG_TEXT, "");
        String infoText = "infoText: " + extras.getString(Notification.EXTRA_INFO_TEXT, "");

        return String.join("\n",
                datetime,
                title,
                text,
                summaryText,
                bigText,
                infoText
        );
    }

    public void toggleWindow() {
        floatOverlay.toggleWindow();
    }

    public void changeOverlayText(String text) {
        floatOverlay.changeOverlayText(text);
    }

    public void showPopup() {
        TitleTextWindow titleTextWindow = new TitleTextWindow(this);
        titleTextWindow.show();
    }
}