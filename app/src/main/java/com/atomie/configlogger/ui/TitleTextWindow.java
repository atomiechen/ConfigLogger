package com.atomie.configlogger.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.atomie.configlogger.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//ref：https://juejin.cn/post/6844904014329413646
public class TitleTextWindow extends BaseOverlay {

    static final String TAG = "TitleTextWindow";

    private int downY;

    public TitleTextWindow(Context context) {
        super(context);
    }

    private final android.os.Handler mHandler = new android.os.Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            animDismiss();
        }
    };

    /**
     * 动画，从顶部弹出
     */
    private void animShow(){
        //使用动画从顶部弹出
        ObjectAnimator animator = ObjectAnimator.ofFloat(getView(), "translationY", -getView().getMeasuredHeight(), 0);
        animator.setDuration(600);
        animator.start();
    }

    /**
     * 动画，从顶部收回
     */
    private void animDismiss(){
        if (getView() == null || getView().getParent() == null) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(getView(), "translationY", getView().getTranslationY(), -getView().getMeasuredHeight());
        animator.setDuration(600);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //移除HeaderToast  (一定要在动画结束的时候移除,不然下次进来的时候由于wm里边已经有控件了，所以会导致卡死)
                removeOverlay();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }

            @Override
            public void onAnimationPause(Animator animation) {
                super.onAnimationPause(animation);
            }

            @Override
            public void onAnimationResume(Animator animation) {
                super.onAnimationResume(animation);
            }
        });
    }

    @Override
    protected void onCreate(){
        // 准备Window要添加的View，是弹窗的UI
        setContentView(R.layout.header_toast);
        // 设置Touch监听事件
        getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = (int) motionEvent.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int moveY = (int) motionEvent.getRawY();
                        if (moveY - downY < 0) {//如果是向上滑动
                            getView().setTranslationY(moveY - downY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        //达到一定比例后，松开手指将关闭弹窗
                        if (Math.abs(getView().getTranslationY()) > getView().getMeasuredHeight() / 1.5) {
                            Log.e("TAG", "回弹");
                            animDismiss();
                        } else {
                            getView().setTranslationY(0);
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        // 定义布局参数
        WindowManager.LayoutParams wm_params = getWindowLayoutParams();
        wm_params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        wm_params.gravity =  Gravity.TOP;
        wm_params.x = 0;
        wm_params.y = 0;
        wm_params.format = -3;  // 会影响Toast中的布局消失的时候父控件和子控件消失的时机不一致，比如设置为-1之后就会不同步
        wm_params.alpha = 1f;
        wm_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wm_params.width = WindowManager.LayoutParams.WRAP_CONTENT;

    }

    /**
     * 向外部暴露显示的方法
     */
    @Override
    public void startOverlay(boolean show) {
        if (show) {
            super.startOverlay(true);
            animShow();
            //3S后自动关闭
            mHandler.sendEmptyMessageDelayed(20, 3000);
        }
    }

    /**
     * 向外部暴露关闭的方法
     */
    public void dismiss(){
        animDismiss();
    }

    public void changeMsgBoxText(String text) {
        Log.e(TAG, "changeMsgBoxText: " + text);
        TextView mTextView = findViewById(R.id.txt_pop_info);
//        String showText = "[New Message]\n";
//        mTextView.setText(showText);
        //利用正则表达式提取标题和内容
        String pattern1 = "(?<=title:)[\\s\\S]*?(?=text:)";
        String pattern2 = "(?<=text:)[\\s\\S]*?(?=\nsummaryText:)";
        Pattern r1 = Pattern.compile(pattern1);
        Pattern r2 = Pattern.compile(pattern2);
        Matcher m1 = r1.matcher(text);
        Matcher m2 = r2.matcher(text);
        if(m1.find() && m2.find()) {
            String showText = "[New Message]\n";
            showText += m1.group(0) + m2.group(0);
            mTextView.setText(showText);
        } else {
            Log.e(TAG,"MATCH ERROR");
            mTextView.setText("MATCH ERROR");
        }
    }
}
