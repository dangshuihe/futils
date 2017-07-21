package com.flyaudio.futils.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * TODO 获取手机设备相关工具类
 * <p/>
 * 创建时间: 2015年12月21日 <br/>
 * 
 * @author xinwei
 * @version
 * @since v0.0.1
 */
public abstract class DeviceUtils {
	private static final String TAG = DeviceUtils.class.getSimpleName();

    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NETWORK_TYPE_WIFI = 1;
    public static final int NETWORK_TYPE_2G = 2;
    public static final int NETWORK_TYPE_3G = 3;
	public static final int NETWORK_TYPE_4G = 4;

	/**
	 * 获取手机语言信息(例如：en、zh) <br>
	 * (设置成简体中文的时候，getLanguage()返回的是zh,getCountry()返回的是cn)
	 * 
	 * @author xnjiang
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static String getLanguage(Context context) {
		// 获取系统当前使用的语言
		return Locale.getDefault().getLanguage();
	}

	/**
	 * 获取手机国家信息(例如：EN、CN) <br>
	 * (设置成简体中文的时候，getLanguage()返回的是zh,getCountry()返回的是cn)
	 * 
	 * @author xnjiang
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static String getCountry(Context context) {
		// 获取区域
		return Locale.getDefault().getCountry();
	}

	/**
	 * @Description: 获取机串IMEI
	 * @param context
	 * @return String
	 */
	public static String getImei(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String id = tm.getDeviceId();
		if (id == null)
			return "";
		else
			return id;
	}

	/**
	 * @Description: 获取卡串IMSI
	 * @param context
	 * @return String
	 */
	public static String getIMSI(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String id = tm.getSubscriberId();
		// id = "1111111";
		if (id == null)
			return "";
		else
			return id;
	}

	/** 获取iccid */
	public static String getIccid(Context context) {
		try {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			return telephonyManager.getSimSerialNumber();
		} catch (Throwable e) {
			return "0000001";
		}
	}

	/**
	 * @Description: 获取手机型号
	 * @return String
	 */
	public static String getDeviceModel() {
		return android.os.Build.MODEL;
	}

	/** 获取android id */
	public static String getAndoidId() {
		return Settings.Secure.ANDROID_ID;
	}

	/**
	 * @Description: 获取系统版本
	 * @return String
	 */
	public static String getOS() {
		return android.os.Build.VERSION.RELEASE;
	}

	public static boolean isSimActive(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		if (tm == null) {
			return false;
		}
		return TelephonyManager.SIM_STATE_READY == tm.getSimState();
	}

	/**
	 * @Description: 获取手机屏幕宽像素
	 */
	public static int getScreenWidth(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.widthPixels;
	}

	/**
	 * @Description: 获取手机屏幕高像素
	 */
	public static int getScreenHeight(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.heightPixels;
	}

	/**
	 * @Description：获取分辨率
	 */
	public static String getResolution(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.widthPixels + "_" + dm.heightPixels;
	}

	/**
	 * @Description：获取密度
	 */
	public static int getDensity(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return (int) dm.density;
	}

	/**
	 * @Description：获取设别dpi
	 */
	public static int getDensityDpi(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.densityDpi;
	}

	/**
	 * 获取用于显示的版本号(显示如：1.0.0)
	 * 
	 * @author xnjiang
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static String getVersionName(Context context) {
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pi.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "1.0.0";
		}
	}

	/**
	 * 获取用于升级的版本号(内部识别号)
	 * 
	 * @author xnjiang
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static int getVersionCode(Context context) {
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pi.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 1.1.1.1(内部开发号)
	 * @param context
	 * @return
	 */
	public static String getBuildNo(Context context) {
		return getVersionName(context).substring(0,5)+"("+getVersionCode(context)+")";
	}
	// TODO: 以下操作时对安装包，类文件，以及APP文件的安装、卸载处理
	/**
	 * 安装APK程序代码
	 * 
	 * @param context
	 * @param apkPath
	 */
	public static void ApkInstall(Context context, String apkPath) {
		File fileAPK = new File(apkPath);
		if (fileAPK.exists() && fileAPK.getName().toLowerCase().endsWith(".apk")) {
			Intent install = new Intent();
			install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			install.setAction(Intent.ACTION_VIEW);
			install.setDataAndType(Uri.fromFile(fileAPK), "application/vnd.android.package-archive");
			context.startActivity(install);// 安装
		}
	}

	/**
	 * 卸载APK程序代码
	 * 
	 * @param context
	 * @param packageName
	 */
	public static void ApkUnInstall(Context context, String packageName) {
		if (isPackageExists(context, packageName)) {
			Uri packageURI = Uri.parse("package:" + packageName);
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
			context.startActivity(uninstallIntent);
		}
	}

	/**
	 * 获取应用程序的完整包名
	 * 
	 * @param context
	 * @return eg. com.xxx.xxx
	 */
	public static String getAppPackageName(Context context) {
		return context.getApplicationContext().getPackageName();
	}

	/**
	 * 获取当前实例所在的父包名
	 * 
	 * @param context
	 * @return eg. com.xxx.xxx
	 */
	public static String getPackageNameClass(Context context) {
		if (context == null || "".equals(context)) {
			return "";
		}
		return context.getPackageName();
	}

	/**
	 * 检测该包名所对应的应用是否存在（eg.com.org）
	 * 
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static boolean isPackageExists(Context context, String packageName) {
		if (packageName == null || "".equals(packageName)) {
			return false;
		}
		try {
			context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	/**
	 * 检测该包名所对应类是否存在（eg.com.org.MainActivity）
	 * 
	 * @param className
	 * @return
	 */
	public static boolean isClassExists(String className) {
		if (className == null || "".equals(className)) {
			return false;
		}
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/** 获取user agent */
	public static String getUserAgent(Context context) {
		final String propNames[] = {
				"ro.mediatek.platform",	// for MTK
				"ro.build.hidden_ver",	// for Samsung
				"ro.product.model"		// otherwise
		};
		String ua = null;
		for (final String propName : propNames) {
			Class<?> clazz = null;
			try {
				clazz = Class.forName("android.os.SystemProperties");
				Method method = clazz.getDeclaredMethod("get",String.class,String.class);
				method.setAccessible(true);
				ua= (String) method.invoke(null,propName,"unknow");
			} catch (Throwable t) {
			}

			if (!TextUtils.isEmpty(ua))
				return ua;
		}

		// Should not reach here
		return android.os.Build.MODEL;
	}


	/**
	 * 检测sdcard卡状态并使用提示信息提示用户(仅当sdcard不存在时才显示提示).
	 * 
	 * @return true, sdcard卡存在并且具有读写权限; false, sdcard卡不存在.
	 */
	public static boolean checkAndToastWhenSdcardNotExists(Context context, String msg) {
		/**
		 * 检测SD卡是否存在
		 */
		boolean exists = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		if (!exists) {
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
		}

		return exists;
	}

	/**
	 * 
	 * 获取设备是否是横屏(AndroidPad)设备
	 * 
	 * @author xnjiang
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static boolean getLandscapeDevice(Context context) {
		Activity activity = (Activity) context;
		int orientation = activity.getResources().getConfiguration().orientation;
		int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		return (orientation == Configuration.ORIENTATION_PORTRAIT && displayRotation % 2 != 0) || (orientation == Configuration.ORIENTATION_LANDSCAPE && displayRotation % 2 == 0);
	}

	/**
	 * 检测当前系统声音是否为正常模式
	 * 
	 * @return
	 */
	public boolean isAudioNormal(Context context) {
		AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
	}

	/**
	 * 判断当前版本是否兼容目标版本的方法
	 * 
	 * @param VersionCode
	 * @return
	 */
	public static boolean isVersionCompat(int VersionCode) {
		int currentVersion = android.os.Build.VERSION.SDK_INT;
		return currentVersion >= VersionCode;
	}

	/**
	 * 获取App安装包信息
	 * 
	 * @return
	 */
	public PackageInfo getPackageInfo(Context context) {
		PackageInfo info = null;
		try {
			info = context.getPackageManager().getPackageInfo(getAppPackageName(context), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace(System.err);
		}
		if (info == null)
			info = new PackageInfo();
		return info;
	}

	public static String getApplicationName(Context context) {
		PackageManager packageManager = null;
		ApplicationInfo applicationInfo = null;
		try {
			packageManager = context.getPackageManager();
			applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			applicationInfo = null;
		}
		return	(String) packageManager.getApplicationLabel(applicationInfo);
	}


	/**
	 * 获取当前设备是否支持电话功能
	 * @param context
	 * @return
	 */
	public static boolean getTelephony (Context context) {
		PackageManager pm = context.getPackageManager();
		// 获取是否支持电话
		return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	/**
	 * 获取当前设置的电话号码 <a
	 * href="http://www.open-open.com/lib/view/open1331537862874.html" >参考资料</a>
	 * 
	 * @author xnjiang
	 * @return
	 * @since v0.0.1
	 */
	public String getNativePhoneNumber(Context context) {
		String NativePhoneNumber = "";
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		NativePhoneNumber = telephonyManager.getLine1Number();
		return NativePhoneNumber;
	}

	/**
	 * 获取手机的IMSI码,并判断是中国移动\中国联通\中国电信 <BR>
	 * 需要加入权限 android.permission.READ_PHONE_STATE <BR>
	 * <a href="http://www.open-open.com/lib/view/open1331537862874.html"
	 * >参考资料</a>
	 * 
	 * @author xnjiang
	 * @return
	 * @since v0.0.1
	 */
	public static String getNetworkOperators(Context context) {
		String strOperators = "unknown";
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		// 返回唯一的用户ID;就是这张卡的编号神马的
		String IMSI = telephonyManager.getSubscriberId();
		// IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。
		if (IMSI != null) {
			if (IMSI.equals("46000") || IMSI.equals("46002") || IMSI.equals("46007")) {
				strOperators = "中国移动";
			} else if (IMSI.equals("46001")) {
				strOperators = "中国联通";
			} else if (IMSI.equals("46003")) {
				strOperators = "中国电信";
			}
		}
		return strOperators;
	}

	/**
	 * 获取当前网络类型 <a
	 * href="http://blog.csdn.net/shakespeare001/article/details/7505932"
	 * >参考资料</a>
	 * 
	 * @return unknown、WIFI、2G、3G
	 * @author xnjiang
	 * @return
	 * @since v0.0.1
	 */
	public static int getNetworkType(Context context) {
		int networkType = NETWORK_TYPE_UNKNOWN;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                return networkType;
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                networkType = NETWORK_TYPE_WIFI;
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                switch (networkInfo.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_CDMA:// 网络类型为CDMA
                    case TelephonyManager.NETWORK_TYPE_EDGE:// 网络类型为EDGE
                    case TelephonyManager.NETWORK_TYPE_GPRS:// 网络类型为GPRS
                        networkType = NETWORK_TYPE_2G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:// 网络类型为EVDO0
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:// 网络类型为EVDOA
                    case TelephonyManager.NETWORK_TYPE_HSDPA:// 网络类型为HSDPA
                    case TelephonyManager.NETWORK_TYPE_HSPA:// 网络类型为HSPA
                    case TelephonyManager.NETWORK_TYPE_HSUPA:// 网络类型为HSUPA
                    case TelephonyManager.NETWORK_TYPE_UMTS:// 网络类型为UMTS
                        networkType = NETWORK_TYPE_3G;
						break;
					case TelephonyManager.NETWORK_TYPE_LTE:
						networkType = NETWORK_TYPE_4G;
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable e) {

        }
		return networkType;
	}

	// 获取网络类型的名字
	public static String getNetworkTypeName(Context context) {
		int type = getNetworkType(context);
		switch (type) {
			case NETWORK_TYPE_2G:
				return "2G";
			case NETWORK_TYPE_3G:
				return "3G";
			case NETWORK_TYPE_4G:
				return "4G";
			case NETWORK_TYPE_WIFI:
				return "wifi";
			case NETWORK_TYPE_UNKNOWN:
			default:
				return "unknown";
		}
	}

	/**
	 * 检查当前是否wifi网络状态
	 * @param context
	 * @return
	 */
	public static boolean isWifiNetwork(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null) {
			return false;
		}
		return networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
	}

	public static boolean isMobileNetwork(Context context) {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if (networkInfo == null) {
				return false;
			}

			return (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
		} catch (Throwable e) {
		}
		return false;
	}

	/**
	 * 获取手机IP地址信息 <a
	 * href="http://www.cnblogs.com/lee0oo0/archive/2013/05/20/3089906.html"
	 * >参考资料</a>
	 * 
	 * @author xnjiang
	 * @return
	 * @since v0.0.1
	 */
	public static String getPhoneIP(Context context) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						// if (!inetAddress.isLoopbackAddress() && inetAddress
						// instanceof Inet6Address) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	/**
	 * 获取手机mac地址(错误返回12个0 )
	 *
	 * @author xnjiang
	 * @return
	 * @since v0.0.1
	 */

	public static String getMacAddress(Context context) {
		// 获取mac地址：
		String macAddress = "000000000000";
		try {
			WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
			if (null != info) {
				if (!TextUtils.isEmpty(info.getMacAddress())) {
					macAddress = info.getMacAddress();
				}
			}
		} catch (Throwable e) {
		}
		return macAddress;
	}

	/**
	 * 检测网络是否连接
	 * @return
	 */
	public static boolean isNetworkAvailable(Context context) {
		boolean flag = false;
        try {
            //得到网络连接信息
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //去进行判断网络是否连接
            if (connectivityManager.getActiveNetworkInfo() != null) {
                flag = connectivityManager.getActiveNetworkInfo().isAvailable();
            }
        } catch (Throwable e) {

        }

		return flag;
	}

	/**
	 * 
	 * 获取可用运存（RAM）大小 ，单位是 MB
	 * 
	 * @author byao
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static long getAvailMemory(Context context) {
		// 获取android当前可用内存大小
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		// mi.availMem; 当前系统的可用内存
		// return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
		Log.d(TAG, "可用内存:" + mi.availMem / (1024 * 1024));
		// Log.d(TAG,"总内存----"+mi.totalMem/(1024*1024));
		return mi.availMem / (1024 * 1024);
	}

	/**
	 * 
	 * 获取总运存（RAM）大小，单位是 MB
	 * 
	 * @author byao
	 * @param context
	 * @return
	 * @since v0.0.1
	 */
	public static long getTotalMemory(Context context) {
		String str1 = "/proc/meminfo";// 系统内存信息文件
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);

			str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
			Log.i(TAG, " str2: " + str2);
			arrayOfString = str2.split("\\s+");
			for (String num : arrayOfString) {
				Log.i(TAG, str2 + " " + num + "\t");
			}
			initial_memory = Integer.valueOf(arrayOfString[1]).intValue();// 获得系统总内存，单位是KB
			localBufferedReader.close();
		} catch (IOException e) {
		}
		// return Formatter.formatFileSize(context, initial_memory);//
		// Byte转换为KB或者MB，内存大小规格化
		Log.d(TAG, "总运存:" + initial_memory / 1024);
		return initial_memory / (1024);
	}

	/**
	 * 
	 * TODO 屏幕密度
	 * 
	 * @author xinwei
	 * @return
	 * @since v0.0.1
	 */
	public static float getScreenDensity(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
		return density;
	}



}