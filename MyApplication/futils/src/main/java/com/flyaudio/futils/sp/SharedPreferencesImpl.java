/*
 * Copyright (c) 2013-2014, thinkjoy Inc. All Rights Reserved.
 * 
 * Project Name: JiaXiao4.0
 * $Id: SharedPreferencesImpl.java 2014年11月20日 下午6:58:55 $ 
 */
package com.flyaudio.futils.sp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;

/**
 * TODO 类描述：对SharedPreferences 存取进行封装
 * 使用apply方法提交，注意不能多进程操作！！！！
 * @version
 * @since v0.0.1
 */
public abstract class SharedPreferencesImpl {

	public abstract SharedPreferences getPreferences();

	/**
	 * 设置String类型程序参数
	 * 
	 * @param key
	 * @param value
	 */
	public void setStringConfig(String key, String value) {
		Editor editor = getPreferences().edit();
		editor.putString(key, value);
		editor.apply();
	}

	/**
	 * 得到String类型程序参数
	 * 
	 * @param key
	 * @param defValue
	 * @return
	 */
	public  String getStringConfig(String key, String defValue) {

		if(getPreferences() == null)
			return defValue;
		String site = getPreferences().getString(key, defValue);
		return site;
	}

	/**
	 * 设置long类型程序参数
	 * 
	 * @param key
	 * @param value
	 */
	public void setLongConfig(String key, long value) {
		Editor editor = getPreferences().edit();
		editor.putLong(key, value);
		editor.apply();
	}

	/**
	 * 得到long类型程序参数
	 * 
	 * @param key
	 * @param defValue
	 * @return
	 */
	public  long getLongConfig(String key, long defValue) {
		return getPreferences().getLong(key, defValue);
	}

	/**
	 * 设置Int类型程序参数
	 * 
	 * @param key
	 * @param value
	 */
	public  void setIntConfig(String key, int value) {
		Editor editor = getPreferences().edit();
		editor.putInt(key, value);
		editor.apply();
	}

	/**
	 * 得到Int类型程序参数
	 * 
	 * @param key
	 * @param defValue
	 * @return
	 */
	public  int getIntConfig(String key, int defValue) {
		return getPreferences().getInt(key, defValue);
	}

	/**
	 * 设置boolean类型程序参数
	 * 
	 * @param key
	 * @param value
	 */
	public  void setBooleanConfig(String key, boolean value) {
		Editor editor = getPreferences().edit();
		editor.putBoolean(key, value);
		editor.apply();
	}

	/**
	 * 得到boolean类型程序参数
	 * 
	 * @param key
	 * @param defValue
	 * @return
	 */
	public boolean getBooleanConfig(String key, boolean defValue) {
		return getPreferences().getBoolean(key, defValue);
	}

	/**
	 * 
	 * 存储浮点型数值的信息
	 * 
	 * @author xszhang
	 * @param key
	 * @param value
	 * @since v0.0.1
	 */
	public void setFloatConfig(String key, Float value) {
		Editor editor = getPreferences().edit();
		editor.putFloat(key, value);
		editor.apply();
	}

	/**
	 * 
	 * 获取浮点型数值的信息
	 * 
	 * @author xszhang
	 * @param key
	 * @return
	 * @since v0.0.1
	 */
	public Float getFloatConfig(String key, Float defValue) {
		return getPreferences().getFloat(key, defValue);
	}

	/**
	 *
	 * 存储MAP的信息
	 * @author xszhang
	 * @since v0.0.1
	 */
	public void setMapConfig(String key, String value) {
		try {
			Editor editor = getPreferences().edit();
			editor.putString(key, value);
			editor.apply();
		}catch (Exception e){

		}
	}

	/**
	 *
	 * 获取HashMap的信息
	 * @author xszhang
	 * @return
	 * @since v0.0.1
	 */
	public HashMap<String, String> getMapConfig(String key) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(key, getPreferences().getString(key, ""));
		return params;
	}

	/**
	 *
	 * 获取HashMap的信息
	 * @author xszhang
	 * @return
	 * @since v0.0.1
	 */
	public HashMap<String, Integer> getIntMapConfig(String key) {
		HashMap<String, Integer> params = new HashMap<String, Integer>();
		params.put(key, getPreferences().getInt(key, 0));
		return params;
	}

	public void setIntMapConfig(String key, int value) {
		try {
			Editor editor = getPreferences().edit();
			editor.putInt(key, value);
			editor.apply();
		}catch (Exception e){

		}
	}


	/**
	 *
	 * 删除的信息
	 * @author xszhang
	 * @return
	 * @since v0.0.1
	 */
	public void removeMapConfig(String key) {
		Editor editor = getPreferences().edit();
		editor.remove(key);
		editor.apply();
	}



}
