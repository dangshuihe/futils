package com.flyaudio.futils.thread;

/**
 * Created by Administrator on 2015/11/18 0018.
 * 自定义的AsyncTask类，使用自己的线程池管理
 */
public abstract class ThreadPoolAsyncTask<Params, Progress, Result> implements Runnable {
    private String mName;
    protected Params[] mParams;

    protected ThreadPoolAsyncTask(String name, Params... params) {
        mName = name;
        mParams = params;
    }

    @Override
    public void run() {
        final Result result = doInBackground(mParams);
        ThreadPoolTask<Void> task = new ThreadPoolTask<Void>(mName + ".run()") {
            @Override
            public void doTask(Void parameter) {
                onPostExecute(result);
            }
        };
        ThreadPoolManager.postMainThread(task);
    }
    /**
     *主要完成耗时操作
     */
    protected abstract Result doInBackground(Params ... params);

    public void publishProcess(final Progress ... values) {
        ThreadPoolTask<Void> task = new ThreadPoolTask<Void>(mName + ".doInBackground()") {
            @Override
            public void doTask(Void parameter) {
                onProgressUpdate(values);
            }
        };
        ThreadPoolManager.postMainThread(task);
    }

    protected void onProgressUpdate(Progress ... values){
    }
    protected void onPostExecute(Result result) {
    }
}
