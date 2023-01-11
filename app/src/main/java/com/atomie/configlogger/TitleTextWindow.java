package com.atomie.configlogger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

//ref：https://juejin.cn/post/6844904014329413646
public class TitleTextWindow implements View.OnTouchListener {

    static String TAG = "TitleTextWindow";

    private final Context mContext;
    private WindowManager wm;
    private View rootView;
    private int downY;

    public TitleTextWindow(Context context) {
        mContext = context;
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
        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "translationY", -rootView.getMeasuredHeight(), 0);
        animator.setDuration(600);
        animator.start();
    }

    /**
     * 动画，从顶部收回
     */
    private void animDismiss(){
        if (rootView == null || rootView.getParent() == null) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "translationY", rootView.getTranslationY(), -rootView.getMeasuredHeight());
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
                if (null != rootView && null != rootView.getParent()) {
                    wm.removeView(rootView);
                }
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

    /**
     * 向外部暴露显示的方法
     */
    public void show(){
        createTitleView();
        animShow();
        Log.e(TAG, "show popup");
        //3S后自动关闭
        mHandler.sendEmptyMessageDelayed(20, 3000);
    }
    /**
     * 向外部暴露关闭的方法
     */
    public void dismiss(){
        animDismiss();
    }


    /**
     * 视图创建方法
     */
    private void createTitleView(){
        //准备Window要添加的View
        rootView = LayoutInflater.from(mContext).inflate(R.layout.header_toast, null); //这里是你弹窗的UI
        // 为titleView设置Touch事件
        rootView.setOnTouchListener(this);
        // 定义WindowManager 并且将View添加到WindowManagar中去
        wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams wm_params = new WindowManager.LayoutParams();
        wm_params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        //这里需要注意，因为不同系统版本策略不一，所以需要根据版本判断设置type，否则会引起崩溃。
        //大于android SDK 7.1.1
//        wm_params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        // 本项目使用无障碍服务的浮层
        wm_params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        wm_params.gravity =  Gravity.TOP;
        wm_params.x = 0;
        wm_params.y = 0;
        wm_params.format = -3;  // 会影响Toast中的布局消失的时候父控件和子控件消失的时机不一致，比如设置为-1之后就会不同步
        wm_params.alpha = 1f;
        wm_params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wm_params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wm.addView(rootView, wm_params);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveY = (int) event.getRawY();
                if (moveY - downY < 0) {//如果是向上滑动
                    rootView.setTranslationY(moveY - downY);
                }
                break;
            case MotionEvent.ACTION_UP:
                //达到一定比例后，松开手指将关闭弹窗
                if (Math.abs(rootView.getTranslationY()) > rootView.getMeasuredHeight() / 1.5) {
                    Log.e("TAG", "回弹");
                    animDismiss();
                } else {
                    rootView.setTranslationY(0);
                }
                break;
            default:
                break;
        }
        return true;
    }
}
