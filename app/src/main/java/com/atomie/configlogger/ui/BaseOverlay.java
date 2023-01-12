package com.atomie.configlogger.ui;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

public abstract class BaseOverlay {

    static final String TAG = "BaseOverlay";

    private final Context mContext;
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams mWindowLayoutParams;

    private View mView = null;

    public BaseOverlay(Context context) {
        mContext = context;
        windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        // generate window layout parameters
        int overlayType = mContext instanceof AccessibilityService ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        if (mContext instanceof Activity) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        }
        mWindowLayoutParams = new WindowManager.LayoutParams(
                overlayType,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT);
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mWindowLayoutParams.x = 0;
        mWindowLayoutParams.y = 0;
        mWindowLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /**
     * 向外部暴露的启动浮层的接口，可以控制是否显示
     */
    public void startOverlay(boolean show) {
        onCreate();
        if (show) {
            showOverlay();
        }
    }

    /**
     * 向外部暴露的启动浮层的接口，默认展示
     */
    public void startOverlay() {
        startOverlay(true);
    }

    /**
     * 向外部暴露的销毁浮层的接口
     */
    public void destroyOverlay() {
        removeOverlay();
        onDestroy();
    }

    protected void onCreate() {}

    protected void onDestroy() {}

    protected final void showOverlay() {
        if (mView != null && !mView.isAttachedToWindow()) {
            windowManager.addView(mView, mWindowLayoutParams);
            Log.e(TAG, "showOverlay");
        }
    }

    protected final void removeOverlay() {
        if (mView != null && mView.isAttachedToWindow()) {
            windowManager.removeView(mView);
            Log.e(TAG, "removeOverlay");
        }
    }

    protected final void setContentView(@LayoutRes int layoutResID) {
        mView = LayoutInflater.from(mContext).inflate(layoutResID, null);
    }

    protected final <T extends View> T findViewById(@IdRes int resId) {
        return mView.findViewById(resId);
    }

    protected Context getContext() {
        return mContext;
    }

    protected WindowManager getWindowManager() {
        return windowManager;
    }

    protected View getView() {
        return mView;
    }

    protected WindowManager.LayoutParams getWindowLayoutParams() {
        return mWindowLayoutParams;
    }
}
