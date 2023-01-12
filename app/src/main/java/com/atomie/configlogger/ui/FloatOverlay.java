package com.atomie.configlogger.ui;

import android.content.Context;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.atomie.configlogger.R;

public class FloatOverlay extends BaseOverlay {

    static final String TAG = "FloatOverlay";

    private int x = 0;
    private int y = 0;
    private boolean draggable = true;

    public FloatOverlay(Context context) {
        super(context);
    }

    @Override
    protected void onCreate() {
        // initialize view
        setContentView(R.layout.message_overlay);
        // set touch listener
        getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (draggable) {
                    WindowManager.LayoutParams params = getWindowLayoutParams();
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
                            params.x += movedX;
                            params.y += movedY;
                            //更新悬浮球控件位置
                            getWindowManager().updateViewLayout(view, params);
                            break;
                    }
                }
                return false;
            }
        });
        // add button click listener
        findViewById(R.id.button_drag).setOnClickListener(view -> {
            // toggle draggable
            draggable = !draggable;
            // toggle scrollable
            TextView mTextView = findViewById(R.id.msg_box);
            if (mTextView.getMovementMethod() == null) {
                mTextView.setMovementMethod(new ScrollingMovementMethod());
                mTextView.setScrollbarFadingEnabled(false);
            } else {
                mTextView.setMovementMethod(null);
            }
        });
        findViewById(R.id.button_fold).setOnClickListener(view -> {
            // toggle fold
            TextView mTextView = findViewById(R.id.msg_box);
            if (mTextView.getVisibility() == View.VISIBLE) {
                mTextView.setVisibility(View.GONE);
            } else {
                mTextView.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.button_dismiss).setOnClickListener(view -> removeOverlay());


        WindowManager.LayoutParams wmParams = getWindowLayoutParams();
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        wmParams.x = 0;
        wmParams.y = 0;

//        //刘海屏延伸到刘海里面
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            wmParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
//        }

    }

    public void toggleWindow() {
        if (getView() != null) {
            if (!getView().isAttachedToWindow()){
                showOverlay();
            } else{
                removeOverlay();
            }
        }
    }

    public void changeOverlayText(String text) {
        Log.e(TAG, "changeOverlayText: " + text);
        TextView mTextView = findViewById(R.id.msg_box);
        //mTextView.setText(text);
        // append the new string
        mTextView.append("\n" + text);

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
}