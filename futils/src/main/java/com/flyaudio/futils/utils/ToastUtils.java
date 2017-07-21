package com.flyaudio.futils.utils;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;


/**
 * TODO 提示框工具类
 * <p/>
 * @since v0.0.1
 */
public class ToastUtils {

    public static void toastShort(Context context, String text) {
        ToastShowManager.getInstance().showToastLong(context, text);
    }

    public static void toastShort(Context context, int resId) {
        ToastShowManager.getInstance().showToast(context, resId);
    }

    public static void toastLong(Context context, String text) {
        ToastShowManager.getInstance().showToastLong(context, text);
    }

    public static void toastLong(Context context, int resId) {
        ToastShowManager.getInstance().showToastLong(context, resId);
    }

    public static void toast(Context context, String text, int duration) {
        ToastShowManager.getInstance().showToast(context, text, duration);
    }

    public static void toast(Context context, int resId, int duration) {
        if (context != null) {
            String text = context.getResources().getString(resId);
            ToastShowManager.getInstance().showToast(context, text, duration);
        }
    }

    private static Toast toastPublic;
    private static TextView textViewToast;

    /**
     * 关闭所有的提示Toast信息框
     *
     * @author xnjiang
     * @since v0.0.1
     */
    public static void hideToastImage() {
        if (toastPublic != null) {
            toastPublic.cancel();
        }
    }


    private static class ToastShowManager {

        private static final String TAG = ToastShowManager.class.getSimpleName();

        private static volatile ToastShowManager mInstance = null;
        private ToastInfo mPreShowToastInfo;
        /**
         * 两个相同的Toast显示的时间间隔,以毫秒为单位.
         */
        private final long mShowSameToastInterval = 2500;

        public ToastShowManager() {
        }

        public static ToastShowManager getInstance() {
            if (mInstance == null) {
                synchronized (ToastShowManager.class) {
                    if (mInstance == null) {
                        mInstance = new ToastShowManager();
                    }
                }
            }
            return mInstance;
        }

        public void showToast(Context context, int resId) {
            if (context != null) {
                String text = context.getString(resId);
                showToast(context, text);
            }
        }

        public void showToastLong(Context context, int resId) {
            if (context != null) {
                String text = context.getString(resId);
                showToastLong(context, text);
            }
        }

        public void showToast(Context context, String text) {
            showToast(context, text, Toast.LENGTH_SHORT);
        }

        public void showToastLong(Context context, String text) {
            showToast(context, text, Toast.LENGTH_LONG);
        }

        public void showToast(Context context, String text, int duration) {
            if (mPreShowToastInfo == null) {
                updateToastInfoAndShowToast(context, text, duration);
            } else {
                if (!TextUtils.isEmpty(mPreShowToastInfo.text) && mPreShowToastInfo.text.equals(text)) {
                    /*
                     * 当接下来要显示的Toast的文本与先前的相同时,按照以下规则进行显示. #1>
					 * 如果两个Toast之间的时间间隔大于预定义的时间间隔,则显示第二个Toast. #2>
					 * 如果两个Toast的Context不相同时,则显示第二个Toast. #3> 如果不满足1,2则只更新时间.
					 */
                    final Context preToastContext = mPreShowToastInfo.contextRef.get();
                    if ((System.currentTimeMillis() - mPreShowToastInfo.showTime > mShowSameToastInterval) || (preToastContext != context)) {
                        updateToastInfoAndShowToast(context, null, duration);
                    } else {
                        // 仅仅只更新保存的Toast的时间.
                        mPreShowToastInfo.showTime = System.currentTimeMillis();
                    }
                } else {
                    updateToastInfoAndShowToast(context, text, duration);
                }
            }
        }

        /**
         * 更新保存的Toast信息,然后显示Toast.
         *
         * @param newContext
         * @param newText
         * @param duration
         */
        private void updateToastInfoAndShowToast(Context newContext, String newText, int duration) {
            if (mPreShowToastInfo == null) {
                mPreShowToastInfo = new ToastInfo();
            }

            if (newText != null) {
                mPreShowToastInfo.text = newText;
            }

            if (newContext != null) {
                mPreShowToastInfo.contextRef = new WeakReference<Context>(newContext);
            }

            Toast.makeText(newContext, mPreShowToastInfo.text, duration).show();
            mPreShowToastInfo.showTime = System.currentTimeMillis();
        }

        private static class ToastInfo {
            WeakReference<Context> contextRef;
            String text;
            long showTime;
        }
    }

/*
    public static Toast showShortToast(Context context, String msg) {
        if(null == context) return null;
        Toast toast = Toast.makeText(context, "自定义位置Toast", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        View view = View.inflate(context, R.layout.public_toast_layout, null);
        toast.setView(view);
        TextView msgtv = (TextView) view.findViewById(R.id.msg);
        msgtv.setText(msg);
            msgtv.setTextSize(17);
            msgtv.setPadding(25, 25, 25, 25);
        toast.show();
        return toast;
    }

    public static Toast showLongToast(Context context, String msg) {
        Toast toast = Toast.makeText(context, "自定义位置Toast", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        View view = View.inflate(context, R.layout.public_toast_layout, null);
        toast.setView(view);
        TextView msgtv = (TextView) view.findViewById(R.id.msg);
        msgtv.setText(msg);
            msgtv.setTextSize(17);
            msgtv.setPadding(25, 25, 25, 25);
        toast.show();
        return toast;
    }

    *//**
     * 带图片的toast
     * @param context
     * @param imageResourceId 图标资源id,如果传0，默认展示圆圈里面一个勾的图
     * @param msg  如果msg为null,只显示图标
     * @return
     *//*
    public static Toast showImageShortToast(Context context, int imageResourceId, String msg) {
        Toast toast = Toast.makeText(context, "自定义位置Toast", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        View view = View.inflate(context, R.layout.public_toast_icon_layout, null);
        toast.setView(view);
        ImageView image = (ImageView) view.findViewById(R.id.icon);
        if(imageResourceId > 0) {
            image.setImageResource(imageResourceId);
        }
        TextView msgtv = (TextView) view.findViewById(R.id.msg);
            msgtv.setTextSize(17);
        if(null != msg && msg.length() > 0) {
            msgtv.setText(msg);
            msgtv.setVisibility(View.VISIBLE);
        } else {
            msgtv.setVisibility(View.GONE);
        }
        toast.show();
        return toast;
    }
    *//**
     * 带图片的toast
     * @param context
     * @param imageResourceId 图标资源id,如果传0，默认展示圆圈里面一个勾的图
     * @param msg  如果msg为null,只显示图标
     * @return
     *//*
    public static Toast showImageLongToast(Context context, int imageResourceId, String msg) {
        Toast toast = Toast.makeText(context, "自定义位置Toast", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        View view = View.inflate(context, R.layout.public_toast_icon_layout, null);
        toast.setView(view);
        ImageView image = (ImageView) view.findViewById(R.id.icon);
        if(imageResourceId > 0) {
            image.setImageResource(imageResourceId);
        }
        TextView msgtv = (TextView) view.findViewById(R.id.msg);
            msgtv.setTextSize(16);
        if(null != msg && msg.length() > 0) {
            msgtv.setText(msg);
            msgtv.setVisibility(View.VISIBLE);
        } else {
            msgtv.setVisibility(View.GONE);
        }
        toast.show();
        return toast;
    }*/
}
