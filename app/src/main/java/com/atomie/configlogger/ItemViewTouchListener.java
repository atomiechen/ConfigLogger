package com.atomie.configlogger;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

class ItemViewTouchListener implements View.OnTouchListener {
    private int x = 0;
    private int y = 0;
    private final WindowManager.LayoutParams wl;
    private final WindowManager windowManager;
    private boolean draggable;

    public ItemViewTouchListener(WindowManager.LayoutParams wl, WindowManager windowManager) {
        this.wl = wl;
        this.windowManager = windowManager;
        this.draggable = true;
    }

    public void toggleDraggable() {
        draggable = !draggable;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (draggable) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) motionEvent.getRawX();
                    y = (int) motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) motionEvent.getRawX();
                    int nowY = (int) motionEvent.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    wl.x += movedX;
                    wl.y += movedY;
                    //更新悬浮球控件位置
                    windowManager.updateViewLayout(view, wl);
                    break;
            }
        }
        return false;
    }
}