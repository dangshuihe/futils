package com.flyaudio.futils.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.flyaudio.futils.thread.ThreadPoolTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * Created by mrl on 2015/11/9 0009.
 * 广播接收者类的基类
 */
class BroadcastObject extends BroadcastReceiver {
    private List<WeakReference<BroadcastCallback>> mList;
    private static Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 传入参数，是否运行在主线程中
     */
    BroadcastObject() {
        mList = new ArrayList<>();
    }

    List<WeakReference<BroadcastCallback>> getBroadcastCallbackList() {
        return mList;
    }

    /**
     * 添加广播回调
     * @param cb 广播回调
     */
    void addBroadcast(WeakReference<BroadcastCallback> cb) {
        mList.add(cb);
    }


    @Override
    public void onReceive(final Context context, Intent intent) {
        Iterator<WeakReference<BroadcastCallback>> ite = mList.iterator();
        while(ite.hasNext()) {
            // 去掉那些已经被回收的广播对象
            final BroadcastCallback callback = ite.next().get();
            if (callback == null) {
                ite.remove();
                continue;
            }

            if (callback.getRunOnMainThread()) {
                // 运行在主线程上
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        // TODO 如果广播耗用太多时间，需要做上报处理
                        callback.onReceive(context);
                    }
                };
                mMainHandler.post(runnable);
            } else {
                // 运行在非主线程
                new ThreadPoolTask<Object>(intent.getAction()) {
                    @Override
                    public void doTask(Object parameter) {
                        callback.onReceive(context);
                    }
                }.postShortTask();
            }

        }
    }

}
