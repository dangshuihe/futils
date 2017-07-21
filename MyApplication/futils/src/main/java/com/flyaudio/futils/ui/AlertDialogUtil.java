package com.flyaudio.futils.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;



/**
 * 常用dialog工具类
 */
public class AlertDialogUtil {


    /**
     *
     * @param mContext
     * @param mLayoutId
     * @param mTitleId
     * @param mTitle
     * @param mContentId
     * @param mContent
     * @param mNegativeId
     * @param mNegativeText
     * @param mPositiveId
     * @param mPositiveText
     * @param mNegativeListener
     * @param mPositiveListener
     */
     public static void showAlertDialog(Context mContext, int mLayoutId,int mTitleId,String mTitle, int mContentId, String mContent,
                                       int mNegativeId,String mNegativeText, int mPositiveId, String mPositiveText,
                                       final View.OnClickListener mNegativeListener, final View.OnClickListener mPositiveListener) {
        final AlertDialog mDialog = new AlertDialog.Builder(mContext).create();
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
        Window window = mDialog.getWindow();
        window.setContentView(mLayoutId);

        if (!TextUtils.isEmpty(mTitle)) {
            TextView mTvTitle = (TextView) window.findViewById(mTitleId);
            mTvTitle.setText(mTitle);
        }
        if (!TextUtils.isEmpty(mContent)) {
            TextView mTvContent = (TextView) window.findViewById(mContentId);
            mTvContent.setText(mContent);
        }

        if(!TextUtils.isEmpty(mNegativeText)) {
            TextView mTvNegative = (TextView) window.findViewById(mNegativeId);
            mTvNegative.setText(mNegativeText);
            mTvNegative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                    if (null != mNegativeListener) {
                        mNegativeListener.onClick(v);
                    }
                }
            });
        }

        if(!TextUtils.isEmpty(mPositiveText)) {
            TextView mTvPositive = (TextView) window.findViewById(mPositiveId);
            mTvPositive.setText(mPositiveText);
            mTvPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                    if (null != mPositiveListener) {
                        mPositiveListener.onClick(v);
                    }
                }
            });
        }

    }


}
