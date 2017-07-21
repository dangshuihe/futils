package com.flyaudio.futils.thread;


import android.os.Looper;


/**
 *
 */
public abstract class ThreadPoolTask<T> implements Runnable {
    protected String mName;
    protected boolean mRunOnMainThread;
    protected T mParameter;

    public abstract void doTask(T parameter);

    public ThreadPoolTask(String name) {
        mName = name;
    }

    public String getName() {
        return (mName == null ? "" : mName);
    }

    public ThreadPoolTask setParameter(T parameter) {
        mParameter = parameter;
        return this;
    }

    @Override
    public void run() {
        long ts;
        if (ThreadPoolManager.OPTIMIZE_DEBUG && mRunOnMainThread) {
            ts = System.currentTimeMillis();
        }
        Thread curThread = Thread.currentThread();
        if (curThread != Looper.getMainLooper().getThread()) {
            Thread.currentThread().setName(mName);
        }
        doTask(mParameter);
    }

    @Override
    public String toString() {
        return getName();
    }

    public ThreadPoolTask postShortTask() {
        ThreadPoolManager.postShortTask(this);
        return this;
    }

    public ThreadPoolTask postLongTask() {
        ThreadPoolManager.postLongTask(this);
        return this;
    }

}
