package com.flyaudio.futils.utils;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by root on 17-8-1.
 */

public class Logger {
    private static boolean sDebug = true;
    private static String sTag = "futils";
    private static final int JSON_INDENT = 2;
    private static boolean sNeedHeader = true;
    private static boolean sNeedClassName = true;

    public static void init(boolean debug, String tag,boolean needHeader,boolean needClassName) {
        Logger.sDebug = debug;
        Logger.sTag = tag;
        Logger.sNeedHeader = needHeader;
        Logger.sNeedClassName = needClassName;
    }

    public static void e(String msg) {
        e(null, msg);
    }

    public static void d(String msg) {
        d(null, msg);
    }


    public static void e(String tag, String msg) {
        if (!sDebug) return;
        LogText.e(getFinalTag(tag), msg);
    }

    public static void d(String tag,String msg) {
        if (!sDebug) return;
        LogText.d(getFinalTag(tag), msg);
    }

    public static void json(String json) {
        json(null, json);
    }

    public static void json(String tag, String json) {
        if (!sDebug) return;
        LogText.e(getFinalTag(tag), getPrettyJson(json));
    }

    private static String getPrettyJson(String jsonStr) {
        try {
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(jsonStr);
                return jsonObject.toString(JSON_INDENT);
            }
            if (jsonStr.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonStr);
                return jsonArray.toString(JSON_INDENT);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Invalid Json, Please Check: " + jsonStr;
    }


    private static String getFinalTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            return tag;
        }
        return sTag;
    }

    private static class LogText {
        private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════\n";
        private static final String SINGLE_DIVIDER = "────────────────────────────────────────────\n";
        private static final int E = 1;
        private static final int D = 2;

        private String mTag;

        public LogText(String tag) {
            mTag = tag;
        }


        public static void e(String tag, String content) {
            LogText logText = new LogText(tag);
            logText.setup(content,E);
        }

        public static void d(String tag, String content) {
            LogText logText = new LogText(tag);
            logText.setup(content,D);
        }

        public void setup(String content,int type) {
            setUpHeader();
            switch (type){
                case E:setUpContentE(content);
                    break;
                case D:setUpContentD(content);
                    break;
            }
            setUpFooter();

        }

        private void setUpHeader() {
            if(sNeedHeader) {
                Log.e(mTag, SINGLE_DIVIDER);
            }
        }

        private void setUpFooter() {
            if(sNeedHeader) {
                Log.e(mTag, DOUBLE_DIVIDER);
            }
        }

        public void setUpContentE(String content) {
            if(sNeedClassName) {
                StackTraceElement targetStackTraceElement = getTargetStackTraceElement();
                Log.e(mTag, "(" + targetStackTraceElement.getFileName() + ":"
                        + targetStackTraceElement.getLineNumber() + ")");
            }
            Log.e(mTag, content);
        }

        public void setUpContentD(String content) {
            if(sNeedClassName) {
                StackTraceElement targetStackTraceElement = getTargetStackTraceElement();
                Log.d(mTag, "(" + targetStackTraceElement.getFileName() + ":"
                        + targetStackTraceElement.getLineNumber() + ")");
            }
            Log.d(mTag, content);
        }

        private StackTraceElement getTargetStackTraceElement() {
            // find the target invoked method
            StackTraceElement targetStackTrace = null;
            boolean shouldTrace = false;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                boolean isLogMethod = stackTraceElement.getClassName().equals(Logger.class.getName());
                if (shouldTrace && !isLogMethod) {
                    targetStackTrace = stackTraceElement;
                    break;
                }
                shouldTrace = isLogMethod;
            }
            return targetStackTrace;
        }
    }


}
