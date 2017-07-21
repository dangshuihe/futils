package com.flyaudio.futils.thread;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



/**
 * 线程管理类，所有线程资源的申请都应该在这里
 */
public class ThreadPoolManager {
    // 是否打印线程情况
    static final boolean OPTIMIZE_DEBUG = false;
    static final String TAG = "ThreadPoolManager";

    // 线程池的分类
    private static final int LONG_TASK_POOL = 0;
    private static final int SHORT_TASK_POOL = 1;
    private static final int PIC_TASK_POOL = 2;
    private static final int POOL_COUNT = 3;

    private volatile static ThreadPoolManager sInstance = null;

    // 不同线程池的容器
    private ThreadPoolExecutorWarp[] mPoolArray = null;
    private ExecutorService mDebugThreadPool = null;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static ThreadPoolManager getInstance() {
        if (sInstance == null) {
            synchronized (ThreadPoolManager.class) {
                if (sInstance == null) {
                    sInstance = new ThreadPoolManager();
                }
            }
        }
        return sInstance;
    }

    private ThreadPoolManager() {
        mPoolArray = new ThreadPoolExecutorWarp[POOL_COUNT];
        // 耗时任务
        mPoolArray[LONG_TASK_POOL] = new ThreadPoolExecutorWarp(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());  // 等价于Executors.newCachedThreadPool();
        // 短小任务
        mPoolArray[SHORT_TASK_POOL] = new ThreadPoolExecutorWarp(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        // 图片处理任务
        mPoolArray[PIC_TASK_POOL] = new ThreadPoolExecutorWarp(0, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        // 调优时候查看线程
        if (OPTIMIZE_DEBUG) {
            mDebugThreadPool = Executors.newSingleThreadExecutor();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    for(ThreadPoolExecutorWarp pool : mPoolArray) {
                        pool.printInfo();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, e.toString());
                    }

                    mDebugThreadPool.execute(this);
                }
            };
            mDebugThreadPool.execute(runnable);
        }
    }

    /**
     * 运行到主线程
     */
    public static void postMainThread(ThreadPoolTask task) {
        getInstance().mHandler.post(task);
    }

    /**
     * 提交类似AsyncTack线程
     */
    public static void postAsyncTask(ThreadPoolAsyncTask task){
        ThreadPoolExecutor pool = getInstance().mPoolArray[LONG_TASK_POOL];
        if (! pool.isShutdown()) {
            pool.execute(task);
        }
    }

    /**
     * 提交耗时任务
     */
    public synchronized static void postLongTask(ThreadPoolTask task) {
        ThreadPoolExecutor pool = getInstance().mPoolArray[LONG_TASK_POOL];
        if (! pool.isShutdown()) {
            pool.execute(task);
        }
    }

    /**
     * 提交短小的任务到后台处理
     */
    public synchronized static void postShortTask(ThreadPoolTask task) {
        ThreadPoolExecutor pool = getInstance().mPoolArray[SHORT_TASK_POOL];
        if (! pool.isShutdown()) {
            pool.execute(task);
        }
    }

    /**
     * 提交图片的任务到后台处理
     */
    public synchronized static void postPicTask(ThreadPoolTask task) {
        ThreadPoolExecutor pool = getInstance().mPoolArray[PIC_TASK_POOL];
        if (! pool.isShutdown()) {
            pool.execute(task);
        }
    }

    /**
     * 获取处理图片的线程池，暴露出来用于给imageloader之类的开源代码使用
     * @return 图片线程池
     */
    public static ThreadPoolExecutor getPicPool() {
        return getInstance().mPoolArray[PIC_TASK_POOL];
    }

    /**
     * 关闭真个线程池
     */
    public static synchronized void exit() {
        ThreadPoolExecutorWarp[] poolArray = getInstance().mPoolArray;
        for (ThreadPoolExecutor pool : poolArray) {
            if (!pool.isShutdown()) {
                pool.shutdownNow();
            }
        }
        getInstance().mPoolArray = null;
    }

    /**
     * 用于打印线程池现在的情况
     */
    public static void printPoolInfo() {
        String msg = String.format(Locale.US, "Long/Short/Pic  %d/%d/%d.  Long largest=%d, Short/Pic wait=%d/%d",
                getInstance().mPoolArray[LONG_TASK_POOL].getActiveCount(), getInstance().mPoolArray[SHORT_TASK_POOL].getActiveCount(),  getInstance().mPoolArray[PIC_TASK_POOL].getActiveCount(),
                getInstance().mPoolArray[LONG_TASK_POOL].getLargestPoolSize(), getInstance().mPoolArray[SHORT_TASK_POOL].getQueue().size(),  getInstance().mPoolArray[PIC_TASK_POOL].getQueue().size());
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
