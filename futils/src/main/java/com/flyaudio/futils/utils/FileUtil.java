package com.flyaudio.futils.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.webkit.MimeTypeMap;

import com.flyaudio.futils.thread.ThreadPoolManager;
import com.flyaudio.futils.thread.ThreadPoolTask;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Signature;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


//import org.apache.http.util.EncodingUtils;

/**
 * TODO 纯文件相关的操作
 * <p>
 * 创建时间: 2015年12月21日  <br/>
 *
 * @author xinwei
 * @since v0.0.1
 */
@SuppressLint("DefaultLocale")
public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();
    public final static String FILENAME = "fileName";
    public final static String FILEPATH = "filePath";
    public final static String FILEDATE = "fileDate";
    public final static String FILESIZE = "fileSize";
    public final static String FILETYPE = "fileType";

    //关闭流的方法
    public static void closeStream(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 从Assets目录下拷贝资源
     */
    public static boolean copyFileFromAssets(Context context, String fileName, String path) {
        boolean copyIsFinish = false;
        InputStream inputStream = null;
        FileOutputStream fos = null;
        try {
            inputStream = context.getClassLoader().getResourceAsStream("assets/" + fileName);
            File file = new File(path);
            if (file.createNewFile()) {
                return false;
            }
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int i;
            while ((i = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, i);
            }
            fos.flush();
            copyIsFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
            copyIsFinish = false;
        } finally {
            closeStream(fos);
            closeStream(inputStream);
        }
        return copyIsFinish;
    }


    /**
     * (所有文件扫瞄)扫描完成后的回调，获取文件列表必须实现
     */
    public interface OnFileListAllCallback {
        /**
         * 返回查询的文件列表(所有文件信息，不含目录信息)
         *
         * @param list 文件列表 ，文件信息包含（fileName，filePath）
         */
        void SearchFileListAll(List<Map<String, Object>> list);
    }

    /**
     * (当前目录层级扫瞄)扫描完成后的回调，获取文件列表必须实现
     */
    public interface OnFileListCurrentLevelCallback {
        /**
         * 返回当前文件路径下的文件和目录
         *
         * @param list 文件列表 ，文件信息包含（fileName，filePath）
         */
        public void SearchFileListCurrentLevel(List<Map<String, Object>> list);
    }

    /**
     * 获取指定目录下的所有文件列表(扫瞄所有层级，不含目录信息)
     *
     * @param path               文件夹路径
     * @param isShow             show 只显示特定文件 hide 过滤掉特定文件
     * @param strKeyword         文件名关键字过滤
     * @param type               文件类型（".jpg"）多个类型中间用&隔开（".jpg&.png&.gif"）
     * @param onFileListCallback 回调函数必须实现，否则得不到搜索结果
     */
    public static void getFileListAll(String path, String isShow, String strKeyword, String type, final OnFileListAllCallback onFileListCallback) {

        new AsyncTask<String, String, String>() {
            ArrayList<Map<String, Object>> listAll = new ArrayList<Map<String, Object>>();

            @Override
            protected void onPostExecute(String result) {
                sortArrayList(listAll, "", true);
                onFileListCallback.SearchFileListAll(listAll);
            }

            @Override
            protected String doInBackground(String... params) {
                File file = new File(params[0]);
                if (isFileExist(file)) {
                    String showState = params[1];
                    String strKeyword = params[2];
                    String[] extensions = params[3].split("&");
                    scanSDCardAll(file, listAll, showState, strKeyword, extensions);
                }
                return null;
            }

        }.execute(path, isShow, strKeyword, type);
    }

    /**
     * 获取指定目录下的所有文件列表(扫瞄所有层级，不含目录信息)
     *
     * @param file       根目录文件
     * @param listAll    存放文件信息的数组
     * @param isShow     show 只显示特定文件 hide 过滤掉特定文件
     * @param strKeyword 文件名关键字过滤
     * @param extensions 文件类型（".jpg"）多个类型中间用&隔开（".jpg&.png&.gif"）
     */
    private static void scanSDCardAll(File file, List<Map<String, Object>> listAll, String isShow, String strKeyword, String... extensions) {
        try {   // 这里加入try，担心OOM问题
            Map<String, Object> mapFile;
            if (file.isDirectory()) {
                ExtensionFileFilter filter = new ExtensionFileFilter(isShow, strKeyword, extensions);
                File[] files = file.listFiles(filter);
                if (files != null) {
                    for (File childFile : files) {
                        if (childFile.isFile()) {
                            mapFile = new HashMap<>();
                            String filePath = childFile.getPath();
                            String fileName = childFile.getName();
                            long fileDate = childFile.lastModified();
                            long fileSize = getFileSize(childFile, false);
                            String fileType = getFileExtension(childFile);
                            mapFile.put(FILENAME, fileName);
                            mapFile.put(FILEPATH, filePath);
                            mapFile.put(FILEDATE, fileDate);
                            mapFile.put(FILESIZE, fileSize);
                            mapFile.put(FILETYPE, fileType);
                            listAll.add(mapFile);
                        } else {
                            scanSDCardAll(childFile, listAll, isShow, strKeyword, extensions);
                        }
                    }
                }
            } else {
                if (file.isFile()) {
                    mapFile = new HashMap<>();
                    String fileName = file.getName();
                    String filePath = file.getPath();
                    long fileDate = file.lastModified();
                    long fileSize = getFileSize(file, false);
                    String fileType = getFileExtension(file);
                    mapFile.put(FILENAME, fileName);
                    mapFile.put(FILEPATH, filePath);
                    mapFile.put(FILEDATE, fileDate);
                    mapFile.put(FILESIZE, fileSize);
                    mapFile.put(FILETYPE, fileType);
                    listAll.add(mapFile);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定目录下的文件列表(当前层级的文件、目录)
     *
     * @param path               文件夹路径
     * @param isShow             show 只显示特定文件 hide 过滤掉特定文件
     * @param strKeyword         文件名关键字过滤
     * @param type               文件类型（".jpg"）多个类型中间用&隔开（".jpg&.png&.gif"）
     * @param onFileListCallback 回调函数必须实现，否则得不到搜索结果
     */
    public static void getFileListCurrentLevel(String path, String isShow, String strKeyword, String type, final OnFileListCurrentLevelCallback onFileListCallback) {

        new AsyncTask<String, String, String>() {
            ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

            @Override
            protected void onPostExecute(String result) {
                onFileListCallback.SearchFileListCurrentLevel(list);
            }

            @Override
            protected String doInBackground(String... params) {
                File file = new File(params[0]);
                if (isFileExist(file)) {
                    String showState = params[1];
                    String strKeyword = params[2];
                    String[] extensions = params[3].split("&");
                    scanSDCardCurrentLevel(file, list, showState, strKeyword, extensions);
                }
                return null;
            }

        }.execute(path, isShow, strKeyword, type);
    }

    /**
     * 获取指定目录下的文件列表(当前层级的文件、目录)
     *
     * @param file       根目录文件
     * @param list       存放文件信息的数组
     * @param isShow     show 只显示特定文件 hide 过滤掉特定文件
     * @param strKeyword 文件名关键字过滤
     * @param extensions       文件类型（".jpg"）多个类型中间用&隔开（".jpg&.png&.gif"）
     */
    private static void scanSDCardCurrentLevel(File file, List<Map<String, Object>> list, String isShow, String strKeyword, String... extensions) {
        try {   // 恐防OOM
            Map<String, Object> mapFile;
            if (file.isDirectory()) {
                ExtensionFileFilter filter = new ExtensionFileFilter(isShow, strKeyword, extensions);
                File[] files = file.listFiles(filter);
                if (files != null) {
                    List<Map<String, Object>> listDir = new ArrayList<>();
                    List<Map<String, Object>> listFile = new ArrayList<>();

                    for (File childFile : files) {
                        if (childFile.isDirectory()) {
                            mapFile = new HashMap<>();
                            String filePath = childFile.getPath();
                            String fileName = childFile.getName();
                            long fileDate = childFile.lastModified();
                            long fileSize = getFileSize(childFile, false);
                            String fileType = getFileExtension(childFile);
                            mapFile.put(FILENAME, fileName);
                            mapFile.put(FILEPATH, filePath);
                            mapFile.put(FILEDATE, fileDate);
                            mapFile.put(FILESIZE, fileSize);
                            mapFile.put(FILETYPE, fileType);
                            listDir.add(mapFile);
                        } else {
                            mapFile = new HashMap<>();
                            String filePath = childFile.getPath();
                            String fileName = childFile.getName();
                            long fileDate = childFile.lastModified();
                            long fileSize = getFileSize(childFile, false);
                            String fileType = getFileExtension(childFile);
                            mapFile.put(FILENAME, fileName);
                            mapFile.put(FILEPATH, filePath);
                            mapFile.put(FILEDATE, fileDate);
                            mapFile.put(FILESIZE, fileSize);
                            mapFile.put(FILETYPE, fileType);
                            listFile.add(mapFile);
                        }
                    }
                    sortArrayList(listDir, FILENAME, true);
                    sortArrayList(listFile, FILENAME, true);
                    list = combineArrayList(listDir, listFile);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 文件过滤器
     *
     * @author Stone.J
     */
    public static class ExtensionFileFilter implements FileFilter {
        private String isShow;
        private String strKeyword;
        private String[] extensions;

        /**
         * @param isShow     show添加 hide排除
         * @param extensions 过滤的后缀名如：.png、.jpg
         */
        public ExtensionFileFilter(String isShow, String strKeyword, String... extensions) {
            this.isShow = isShow;
            this.strKeyword = strKeyword;
            this.extensions = extensions;
        }

        @Override
        public boolean accept(File pathname) {
            String strFileName = pathname.getName();
            if (pathname.isDirectory()) {
                return true;
            } else {
                if (extensions == null || extensions.length == 0) {
                    return true;
                } else {
                    if (isShow.equalsIgnoreCase("show")) {
                        for (String str : extensions) {
                            if (strFileName.toLowerCase().endsWith(str.toLowerCase())) {
                                if (!TextUtils.isEmpty(strKeyword)) {
                                    if (strFileName.toLowerCase().contains(strKeyword.toLowerCase().trim())) {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }
                            }
                        }
                        return false;
                    } else {
                        for (String str : extensions) {
                            if (strFileName.toLowerCase().endsWith(str.toLowerCase())) {
                                if (!TextUtils.isEmpty(strKeyword)) {
                                    if (strFileName.toLowerCase().contains(strKeyword.toLowerCase().trim())) {
                                        return false;
                                    }
                                } else {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }
    }

    public static class FileComparator implements Comparator<Object> {
        private String sortKey;
        private boolean sortOrder;

        /**
         * 排序比较器
         *
         * @param sortKey 排序字段
         * @param sortOrder   排序方式 true 升序 false 降序
         */
        public FileComparator(String sortKey, boolean sortOrder) {
            this.sortKey = sortKey;
            this.sortOrder = sortOrder;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Object o1, Object o2) {
            if (TextUtils.isEmpty(sortKey)) {
                return -1;
            }
            if (o1 instanceof Map) {
                Map<String, Object> map1 = (Map<String, Object>) o1;
                Map<String, Object> map2 = (Map<String, Object>) o2;
                if (!map1.containsKey(sortKey) || !map2.containsKey(sortKey)) {
                    return -1;
                }
                if (sortOrder) {
                    return map1.get(sortKey).toString().compareToIgnoreCase(map2.get(sortKey).toString());
                } else {
                    return map2.get(sortKey).toString().compareToIgnoreCase(map1.get(sortKey).toString());
                }
            } else if (o1 instanceof String) {
                String str1 = (String) o1;
                String str2 = (String) o2;
                if (sortOrder) {
                    return str1.compareToIgnoreCase(str2);
                } else {
                    return str2.compareToIgnoreCase(str1);
                }
            } else if (o1 instanceof File) {
                File file1 = (File) o1;
                File file2 = (File) o2;
                String filename1 = file1.getName();
                String filename2 = file2.getName();
                if (sortOrder) {
                    return filename1.compareToIgnoreCase(filename2);
                } else {
                    return filename2.compareToIgnoreCase(filename1);
                }
            }
            return -1;
        }

    }

    /**
     * 数组排序功能
     *
     * @param list    排序数据
     * @param sortKey 排序字段
     * @param order   排序方式 true 升序 false 降序
     */
    public static void sortArrayList(List<?> list, final String sortKey, final boolean order) {
        Collections.sort(list, new Comparator<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                if (TextUtils.isEmpty(sortKey)) {
                    return -1;
                }
                if (o1 instanceof Map) {
                    Map<String, Object> map1 = (Map<String, Object>) o1;
                    Map<String, Object> map2 = (Map<String, Object>) o2;
                    if (!map1.containsKey(sortKey) || !map2.containsKey(sortKey)) {
                        return -1;
                    }
                    if (order) {
                        return map1.get(sortKey).toString().compareToIgnoreCase(map2.get(sortKey).toString());
                    } else {
                        return map2.get(sortKey).toString().compareToIgnoreCase(map1.get(sortKey).toString());
                    }
                } else if (o1 instanceof String) {
                    String str1 = (String) o1;
                    String str2 = (String) o2;
                    if (order) {
                        return str1.compareToIgnoreCase(str2);
                    } else {
                        return str2.compareToIgnoreCase(str1);
                    }
                } else if (o1 instanceof File) {
                    File file1 = (File) o1;
                    File file2 = (File) o2;
                    String filename1 = file1.getName();
                    String filename2 = file2.getName();
                    if (order) {
                        return filename1.compareToIgnoreCase(filename2);
                    } else {
                        return filename2.compareToIgnoreCase(filename1);
                    }
                }
                return -1;
            }
        });
    }

    /**
     * 合并两个数组对象成为一个数组对象
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> combineArrayList(List<T> a1, List<T> a2) {
        for (Object obj : a2) {
            a1.add((T) obj);
        }
        return a1;
    }

    /**
     * 调用系统功能重新扫描指定的文件夹,写入系统媒体数据库
     *
     * @author xnjiang
     * @since v0.0.1
     */
    public static void scanMediaFileDataBase(Context context, String strFileName) {
        // 通常在我们的项目中，可能会遇到写本地文件，最常用的就是图片文件，在这之后需要通知系统重新扫描SD卡，
        // 在Android4.4之前也就是以发送一个Action为“Intent.ACTION_MEDIA_MOUNTED”的广播通知执行扫描。如下：

        // context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
        // Uri.parse("file://" + strRefreshDir)));

        // 但在Android4.4中，则会抛出以下异常：
        // W/ActivityManager( 498): Permission Denial: not allowed to send
        // broadcast android.intent.action.MEDIA_MOUNTED from pid=2269,
        // uid=20016
        // 那是因为Android4.4中限制了系统应用才有权限使用广播通知系统扫描SD卡。
        // 解决方式：
        // 使用MediaScannerConnection执行具体文件或文件夹进行扫描。
        // MediaScannerConnection.scanFile(context, new
        // String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()
        // + "/" + strFileName}, null, null);

        // 判断目录如果是文件，就获取其上一级路径也进行刷新
        String strFileParent = new File(strFileName).isFile() ? new File(strFileName).getParentFile().getPath() : strFileName;
        MediaScannerConnection.scanFile(context, new String[]{strFileName, strFileParent}, null, null);
    }

    /**
     * 检测SD卡是否存在
     */
    public static boolean isSDExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 多个SD卡存在时 获取外置SD卡路径<br>
     */
    public static String getExternalStorageDirectory() {
        Map<String, String> map = System.getenv();
        String[] values = new String[map.values().size()];
        map.values().toArray(values);
        String path = values[values.length - 1]; // 外置SD卡的路径
        if (path.startsWith("/mnt/") && !Environment.getExternalStorageDirectory().getAbsolutePath().equals(path)) {
            return path;
        } else {
            return null;
        }
    }

    /**
     * 获取有效的文件目录（SD卡根目录、程序根目录）<br>
     * 如果SD卡存在--返回SD根目录 <br>
     * 如果SD卡不存在 --返回程序根目录
     *
     * @return e.g. “/storage/sdcard0/” 或 “/data/data/{package name}/files/”
     */
    public static String getAvailableFilesPath(Context context) {
        if (!isSDExist()) {
            return getSystemFilesPath(context.getApplicationContext());
        }
        return getSDCardFilesPath();
    }

    /**
     * 获取SD卡根目录路径
     *
     * @return e.g. /storage/sdcard0/
     */
    public static String getSDCardFilesPath() {
        if (!isSDExist()) {
            return null;
        }
        return Environment.getExternalStorageDirectory().getPath() + "/";
    }

    /**
     * 获取SD卡根目下的下载目录路径
     *
     * @return e.g. /storage/sdcard0/Download/
     */
    public static String getSDCardDownloadPath() {
        if (!isSDExist()) {
            return null;
        }
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.pathSeparator;
    }

    /**
     * 获取SD卡根目下的缓存目录路径
     *
     * @return e.g. /storage/sdcard0/Download/cache/
     */
    public static String getSDCardCachePath() {
        if (!isSDExist()) {
            return null;
        }
        return Environment.getDownloadCacheDirectory().getPath() + File.pathSeparator;
    }

    /**
     * 私有文件目录路径
     *
     * @param ctx
     * @return /data/data/{package name}/files/
     */
    public static String getSystemFilesPath(Context ctx) {
        return ctx.getFilesDir().getPath() + File.pathSeparator;
    }

    /**
     * 系统文件中的缓存目录路径
     *
     * @param ctx
     * @return /data/data/{package name}/cache/
     */
    public static String getSystemCachePath(Context ctx) {
        return ctx.getCacheDir().getPath() + File.pathSeparator;
    }

    /**
     * 删除一个文件
     */
    public static boolean deleteFile(File file) {
        try {
            if (file.isDirectory())
                return false;
            return file.delete();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 删除一个文件
     */
    public static boolean deleteFile(String path) {
        if(TextUtils.isEmpty(path)) return false;

        File file = new File(path);
        if(null != file && file.exists())
            return deleteFile(file);

        return false;
    }

    /**
     * 删除一个目录（可以是非空目录）
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists() || dir.isFile()) {
            return false;
        }
        boolean ret = true;
        try {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    if (!file.delete()) {
                        ret = false;
                    }
                } else if (file.isDirectory()) {
                    deleteDir(file);// 递归
                }
            }
            if (!dir.delete()) {
                ret = false;
            }
        } catch (Throwable e) {
            ret = false;
        }
        return ret;
    }

    /**
     * 删除一个目录（可以是非空目录）
     */
    public static boolean deleteDir(String path) {
        return deleteDir(new File(path));
    }

    /**
     * 异步删除一个目录（可以是非空目录）
     */
    public static void deleteDirTask(final String path) {
        ThreadPoolTask task = new ThreadPoolTask("deleteDirTask") {
            @Override
            public void doTask(Object parameter) {
                deleteDir(new File(path));
            }
        };
        ThreadPoolManager.postLongTask(task);
    }

    /**
     * 拷贝一个文件,srcFile源文件，destFile目标文件
     *
     * @param srcFile  文件、目录
     * @param destFile 文件、目录
     */
    public static boolean copyFileTo(File srcFile, File destFile) {
        if (!srcFile.exists())
            return false;// 判断是否存在

        File outputFile = destFile;
        if (srcFile.isDirectory()) {
            if (!destFile.isDirectory()) {
                return false;
            }
            String newPath = destFile.getPath() + File.pathSeparator + srcFile.getName();
            createDir(newPath);
            copyFilesTo(srcFile.getPath(), newPath);
        }
        if (destFile.isDirectory() && destFile.exists()) {
            outputFile = new File(destFile.getPath() + File.pathSeparator + srcFile.getName());
        } else if (destFile.getParent() == null) {
            return false;
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(outputFile);
            byte[] buf = new byte[4096];
            int readLen;
            while ((readLen = fis.read(buf)) != -1) {
                fos.write(buf, 0, readLen);
            }
            fos.flush();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(fos);
            closeStream(fis);
        }
        return false;
    }

    /**
     * 拷贝一个文件,srcFile源文件，destFile目标文件
     *
     * @param srcPath  文件、目录的路径
     * @param destPath 文件、目录的路径
     * @return
     */
    public static boolean copyFileTo(String srcPath, String destPath) {
        return copyFileTo(new File(srcPath), new File(destPath));
    }

    /**
     * 拷贝目录下的所有文件到指定目录
     *
     * @param srcDir  完整目录
     * @param destDir 完整目录
     * @return
     */
    public static boolean copyFilesTo(File srcDir, File destDir) {
        if (!srcDir.exists() || !destDir.exists())
            return false;// 判断是否存在
        if (!srcDir.isDirectory() || !destDir.isDirectory())
            return false;// 判断是否是目录
        try {
            File[] srcFiles = srcDir.listFiles();
            if (srcFiles == null) {
                return false;
            }
            for (File srcFile : srcFiles) {
                if (srcFile.isFile()) {
                    // 获得目标文件
                    File destFile = new File(destDir.getPath() + "//" + srcFile.getName());
                    copyFileTo(srcFile, destFile);
                } else if (srcFile.isDirectory()) {
                    File theDestDir = new File(destDir.getPath() + "//" + srcFile.getName());
                    copyFilesTo(srcFile, theDestDir);
                }
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 拷贝目录下的所有文件到指定目录
     *
     * @param srcPath  完整目录的路径
     * @param destPath 完整目录的路径
     */
    public static boolean copyFilesTo(String srcPath, String destPath) {
        return copyFilesTo(new File(srcPath), new File(destPath));
    }

    /**
     * 移动一个文件、目录到一个新的文件夹
     *
     * @param srcFile  文件、目录
     * @param destFile 完整目录
     */
    public static boolean moveFileTo(File srcFile, File destFile) {
        // File (or directory) to be moved
        if (!srcFile.exists()) {
            return false;
        }
        // Destination directory
        if (!destFile.exists() || !destFile.isDirectory()) {
            return false;
        }
        try {
            // Move file to new directory
            return srcFile.renameTo(new File(destFile, srcFile.getName()));
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移动一个文件、目录到一个新的文件夹
     *
     * @param srcPath  文件、目录的路径
     * @param destPath 完整目录的路径
     */
    public static boolean moveFileTo(String srcPath, String destPath) {
        return moveFileTo(new File(srcPath), new File(destPath));
    }

    /**
     * 移动目录下的所有文件到指定目录
     *
     * @param srcDir  完整目录
     * @param destDir 完整目录
     */
    public static boolean moveFilesTo(File srcDir, File destDir) {
        if (!srcDir.isDirectory() || !destDir.isDirectory()) {
            return false;
        }
        try {
            File[] srcDirFiles = srcDir.listFiles();
            if (srcDirFiles == null) {
                return false;
            }
            for (File file : srcDirFiles) {
                moveFileTo(file, destDir);
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * 移动目录下的所有文件到指定目录
     *
     * @param srcPath  完整目录的路径
     * @param destPath 完整目录的路径
     * @return
     */
    public static boolean moveFilesTo(String srcPath, String destPath) {
        return moveFilesTo(new File(srcPath), new File(destPath));
    }

    /**
     * 给文件或目录重命名
     *
     * @param targetFile 文件、目录
     * @param newName    新的文件名字（单个文件要记得加上后缀.xxx）
     * @return
     */
    public static boolean renameFile(File targetFile, String newName) {
        if (!targetFile.exists() || targetFile.getParentFile() == null) {
            return false;
        }
        try {
            return targetFile.renameTo(new File(targetFile.getParentFile(), newName));
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 给文件或目录重命名
     *
     * @param targetPath 文件、目录的路径
     * @param newName    新的文件名字（单个文件要记得加上后缀.xxx）
     */
    public static boolean renameFile(String targetPath, String newName) {
        return renameFile(new File(targetPath), newName);
    }

    /**
     * 将二进制流写入文件
     */
    public static boolean writeFileFromStream(String path, InputStream is) {
        boolean result = false;
        FileOutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            File file = new File(path);
            os = new FileOutputStream(file, false);
            bos = new BufferedOutputStream(os);
            int readLen;
            byte[] buf = new byte[4096];
            while ((readLen = is.read(buf)) != -1) {
                bos.write(buf, 0, readLen);
            }
            bos.flush();
            result = true;
        } catch (Throwable e) {
            e.printStackTrace();
            result = false;
        } finally {
            closeStream(bos);
            closeStream(os);
        }

        return result;
    }

    /**
     * 将字符串写入文件
     */
    public static boolean writeFileFromString(String path, String data) {
        boolean result = false;
        FileWriter fw = null;
        try {
            File file = new File(path);
            if (FileUtil.checkAndMakeDir(file.getParent())) {
                fw = new FileWriter(file);
                fw.write(data);
                fw.close();
                result = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(fw);
        }

        return result;
    }

    /**
     * 将字符串追加写入文件
     */
    public static boolean writeFileFromString(String path, String data, boolean append) {
        boolean result = false;
        FileWriter fw = null;
        try {
            File file = new File(path);
            if (FileUtil.checkAndMakeDir(file.getParent())) {
                fw = new FileWriter(file, append);
                fw.write(data);
                fw.close();
                result = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(fw);
        }

        return result;
    }

    /**
     * 创建目录文件夹
     */
    public static void createDir(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File newDir = new File(path);
                // if this directory does not exists, make one.
                if (!newDir.exists()) {
                    if (!newDir.mkdirs()) {
                        Log.e("--CopyAssets--", "cannot create directory.");
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 根据给定的文件的完整路径，判断 并创建文件夹 及文件
     *
     * @author xnjiang
     * @since v0.0.1
     */
    public static boolean createDirAndFile(String filePath) {
        File file = new File(filePath);
        try {
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    return false;
                }
            }
            if (!file.exists()) {
                return file.createNewFile();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 检测文件是否存在
     */
    public static boolean isFileExist(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                return file.exists();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * 检测文件是否存在
     *
     * @return
     */
    public static boolean isFileExist(File file) {
        try {
            return file.exists();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * 比较两个文件是否相同
     *
     * @return true 相同,false 不同
     */
    public static boolean isCompareFiles(File file1, File file2) {
        return (file1.getPath().equalsIgnoreCase(file2.getPath()));
    }

    /**
     * 比较两个文件是否相同
     *
     * @param path1
     * @param path2
     * @return true 相同,false 不同
     */
    public static boolean isCompareFiles(String path1, String path2) {
        if (path1.equalsIgnoreCase(path2)) {
            return true;
        } else {
            return isCompareFiles(new File(path1), new File(path2));
        }
    }

    /**
     * 建立私有文件
     *
     * @param fileName 私有文件夹下的路径如：database\aa.db3
     */
    public static File creatSystemDataFile(Context ctx, String fileName) {
        try {
            File file = new File(getSystemFilesPath(ctx) + fileName);
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                return null;
            }

            if (!file.exists() && !file.createNewFile()) {
                return null;
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 建立私有目录
     *
     * @param dirName 私有文件夹下的路径如：database\aa.db3
     */
    public static File creatSystemDataDir(Context ctx, String dirName) {
        try {
            File dir = new File(getSystemFilesPath(ctx) + dirName);
            dir.mkdir();
            return dir;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除私有文件
     */
    public static boolean deleteSystemDataFile(Context ctx, String fileName) {
        File file = new File(getSystemFilesPath(ctx) + fileName);
        return deleteFile(file);
    }

    /**
     * 删除私有目录
     */
    public static boolean deleteSystemDataDir(Context ctx, String dirName) {
        File file = new File(getSystemFilesPath(ctx) + dirName);
        return deleteDir(file);
    }

    /**
     * 更改私有文件名
     */
    public static boolean renameSystemDataFile(Context ctx, String oldName, String newName) {
        File oldFile = new File(getSystemFilesPath(ctx) + oldName);
        File newFile = new File(getSystemFilesPath(ctx) + newName);
        try {
            return oldFile.renameTo(newFile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 在私有目录下进行文件复制
     */
    public static boolean copySystemDataFileTo(Context ctx, String srcFileName, String destFileName) {
        try {
            File srcFile = new File(getSystemFilesPath(ctx) + srcFileName);
            File destFile = new File(getSystemFilesPath(ctx) + destFileName);
            return copyFileTo(srcFile, destFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 复制私有目录里指定目录的所有文件
     */
    public static boolean copySystemDataFilesTo(Context ctx, String srcDirName, String destDirName) {
        try {
            File srcDir = new File(getSystemFilesPath(ctx) + srcDirName);
            File destDir = new File(getSystemFilesPath(ctx) + destDirName);
            return copyFilesTo(srcDir, destDir);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 移动私有目录下的单个文件
     */
    public static boolean moveSystemDataFileTo(Context ctx, String srcFileName, String destFileName) {
        File srcFile = new File(getSystemFilesPath(ctx) + srcFileName);
        File destFile = new File(getSystemFilesPath(ctx) + destFileName);
        return moveFileTo(srcFile, destFile);
    }

    /**
     * 移动私有目录下的指定目录下的所有文件
     */
    public static boolean moveSystemDataFilesTo(Context ctx, String srcDirName, String destDirName) {
        File srcDir = new File(getSystemFilesPath(ctx) + srcDirName);
        File destDir = new File(getSystemFilesPath(ctx) + destDirName);
        return moveFilesTo(srcDir, destDir);
    }

    /**
     * 将文件写入应用私有的files目录。如:writeFile("test.txt");
     */
    public static boolean writeSystemFile(Context ctx, String fileName, String content) {
        OutputStream os = null;
        try {
            os = ctx.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
            os.write(content.getBytes());
            os.flush();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(os);
        }
        return false;
    }

    /**
     * 在原有文件上继续写文件。如:appendFile("test.txt");
     */
    public static boolean appendSystemFile(Context ctx, String fileName, String content) {
        OutputStream os = null;
        try {
            os = ctx.openFileOutput(fileName, Context.MODE_APPEND);
            os.write(content.getBytes());
            os.flush();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(os);
        }
        return false;
    }

    /**
     * 从应用的私有目录files读取文件。如:readFile("test.txt");
     */
    public static String readSystemFile(Context ctx, String fileName) {
        InputStream is = null;
        ByteArrayOutputStream arrayOutputStream = null;
        try {
            is = ctx.openFileInput(fileName);
            byte[] bytes = new byte[1024];
            arrayOutputStream = new ByteArrayOutputStream();
            while (is.read(bytes) != -1) {
                arrayOutputStream.write(bytes, 0, bytes.length);
            }
            return new String(arrayOutputStream.toByteArray());
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(is);
            closeStream(arrayOutputStream);
        }
        return "";
    }

    /**
     * 写数据到SD中的文件
     */
    public static void writeFileSdcardFile(String filePath, String fileContent) {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filePath);
            byte[] bytes = fileContent.getBytes();
            fout.write(bytes);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(fout);
        }
    }

    /**
     * 读数据从SD中的文件
     */
    public static String readFileSdcardFile(String filePath) {
        String res = "";
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(filePath);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);

            //res = EncodingUtils.getString(buffer, "UTF-8");
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(fin);
        }
        return res;
    }

    /**
     * 获取文件的读写权限
     *
     * @return (-1)-不能访问,0-只读,1-读写
     */
    public static int getFilePermission(String path) {
        return getFilePermission(new File(path));
    }

    /**
     * 获取文件的读写权限
     *
     * @param f
     * @return (-1)-不能访问,0-只读,1-读写
     */
    public static int getFilePermission(File f) {
        int intPermission = 0;
        if (!f.canRead() && !f.canWrite()) {
            intPermission = -1;
        }
        if (f.canRead()) {
            if (f.canWrite()) {
                intPermission = 1;
            } else {
                intPermission = 0;
            }
        }
        return intPermission;
    }

    /***
     * 获取文件个数
     ***/
    public static int getFileCount(String path) {// 递归求取目录文件个数
        return getFileCount(new File(path));
    }

    /***
     * 获取文件个数
     ***/
    public static int getFileCount(File f) {// 递归求取目录文件个数
        int size = 0;
        if (f.isDirectory()) {
            File flist[] = f.listFiles();
            if (null != flist && flist.length > 0) {
                size = flist.length;
                for (int i = 0; i < flist.length; i++) {
                    if (flist[i].isDirectory()) {
                        size = size + getFileCount(flist[i]);
                        size--;
                    }
                }
            }
        } else {
            size = 1;
        }
        return size;
    }

    /**
     * 获取文件大小 (单位：kb)
     *
     * @param path
     * @param boolFolderCount 是否统计文件夹大小（文件夹统计比较耗时）
     * @return 文件默认返回 0
     */
    public static long getFileSize(String path, boolean boolFolderCount) {
        return getFileSize(new File(path), boolFolderCount);
    }

    /**
     * 获取文件大小 (单位：kb)
     *
     * @param f
     * @param boolFolderCount 是否统计文件夹大小（文件夹统计比较耗时）
     * @return 文件默认返回 0
     */
    public static long getFileSize(File f, boolean boolFolderCount) {
        long size = 0;

        if(null == f) return 0;

        try {
            if (f.isFile()) {// 文件处理
                if (f.exists()) {
                    size = f.length();
                }
            } else {// 文件夹处理
                if (boolFolderCount) {
                    File flist[] = f.listFiles();
                    for (int i = 0; i < flist.length; i++) {
                        if (flist[i].isDirectory()) {
                            size = size + getFileSize(flist[i], boolFolderCount);
                        } else {
                            size = size + flist[i].length();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size / 1024;
    }


    public static long getFileSizeBytes(String path, boolean boolFolderCount){
        return getFileSizeBytes(new File(path), boolFolderCount);

    }

    /**
     * 获取文件大小 (单位：kb)
     *
     * @param f
     * @param boolFolderCount 是否统计文件夹大小（文件夹统计比较耗时）
     * @return 文件默认返回 0
     */
    public static long getFileSizeBytes(File f, boolean boolFolderCount) {
        long size = 0;
        try {
            if (f.isFile()) {// 文件处理
                if (f.exists()) {
                    size = f.length();
                }
            } else {// 文件夹处理
                if (boolFolderCount) {
                    File flist[] = f.listFiles();
                    for (int i = 0; i < flist.length; i++) {
                        if (flist[i].isDirectory()) {
                            size = size + getFileSize(flist[i], boolFolderCount);
                        } else {
                            size = size + flist[i].length();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }


    /**
     * 获取文件大小 (单位：Byte)
     *
     * @param path
     * @param boolFolderCount 是否统计文件夹大小（文件夹统计比较耗时）
     * @return 文件默认返回 0
     */
    public static long getFileSizeByte(String path, boolean boolFolderCount) {
        long size = 0;
        try {
            File f = new File(path);
            if (f.isFile()) {// 文件处理
                if (f.exists()) {
                    size = f.length();
                }
            } else {// 文件夹处理
                if (boolFolderCount) {
                    File flist[] = f.listFiles();
                    for (int i = 0; i < flist.length; i++) {
                        if (flist[i].isDirectory()) {
                            size = size + getFileSize(flist[i], boolFolderCount);
                        } else {
                            size = size + flist[i].length();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }



    /***
     * 转换文件大小单位(B/KB/MB/GB)
     ***/
    public static String formatFileSize(long fileS) {// 转换文件大小
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS == 0) {
            fileSizeString = "0B";
        } else if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < (1024 * 1024)) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < (1024 * 1024 * 1024)) {
            fileSizeString = df.format((double) fileS / (1024 * 1024)) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / (1024 * 1024 * 1024)) + "GB";
        }
        return fileSizeString;
    }

    /***
     * 转换文件大小单位(B/KB/MB/GB)
     ***/
    public static String formatFileSizeNoDigits(long fileS) {// 转换文件大小
        DecimalFormat df = new DecimalFormat("#");
        String fileSizeString = "";
        if (fileS == 0) {
            fileSizeString = "0B";
        } else if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < (1024 * 1024)) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < (1024 * 1024 * 1024)) {
            fileSizeString = df.format((double) fileS / (1024 * 1024)) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / (1024 * 1024 * 1024)) + "GB";
        }
        return fileSizeString;
    }


    /**
     * 获取Phone容量信息(单位：Bytes)
     *
     * @return
     */
    public static String getPhoneCapacity() {
        // 获取本机信息
        File data = Environment.getDataDirectory();
        StatFs statFs = new StatFs(data.getPath());
        int availableBlocks = statFs.getAvailableBlocks();// 可用存储块的数量
        int blockCount = statFs.getBlockCount();// 总存储块的数量

        int size = statFs.getBlockSize();// 每块存储块的大小

        int totalSize = blockCount * size;// 总存储量

        int availableSize = availableBlocks * size;// 可用容量

        String phoneCapacity = formatFileSize(availableSize) + "/" + formatFileSize(totalSize);

        return phoneCapacity;
    }

    /**
     * 获取SDCard容量信息(单位：Bytes)
     *
     * @return
     */
    public static String getSDCardCapacity() {
        // 获取sdcard信息
        File sdData = Environment.getExternalStorageDirectory();
        StatFs sdStatFs = new StatFs(sdData.getPath());

        int sdAvailableBlocks = sdStatFs.getAvailableBlocks();// 可用存储块的数量
        int sdBlockcount = sdStatFs.getBlockCount();// 总存储块的数量
        int sdSize = sdStatFs.getBlockSize();// 每块存储块的大小
        int sdTotalSize = sdBlockcount * sdSize;
        int sdAvailableSize = sdAvailableBlocks * sdSize;

        String sdcardCapacity = formatFileSize(sdAvailableSize) + "/" + formatFileSize(sdTotalSize);
        return sdcardCapacity;
    }

    /**
     * 计算剩余空间
     *
     * @param path
     * @return
     */
    private static long getAvailableSize(String path) {
        StatFs fileStats = new StatFs(path);
        fileStats.restat(path);
        return (long) fileStats.getAvailableBlocks() * fileStats.getBlockSize(); // 注意与fileStats.getFreeBlocks()的区别
    }

    /**
     * 计算SD卡的剩余空间
     *
     * @return 剩余空间
     */
    public static long getSDAvailableSize() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return getAvailableSize(Environment.getExternalStorageDirectory().toString());
        }

        return 0;
    }

    /**
     * 计算系统的剩余空间
     *
     * @return 剩余空间
     */
    public static long getSystemAvailableSize() {
        return getAvailableSize(Environment.getDataDirectory().toString());
    }


    /**
     * 是否有足够的空间
     *
     * @param filePath 目录的路径
     * @return
     */
    public static boolean hasEnoughMemory(String filePath, long length) {
        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        if (sdcardPath != null && filePath != null && filePath.startsWith(sdcardPath)) {
            return getSDAvailableSize() > length;
        } else {
            return getSystemAvailableSize() > length;
        }
    }

    /**
     * 根据文件mimeType调用系统相关程序打开
     *
     * @param context
     * @param strPath
     */
    public static void openFileBySystemApp(Context context, String strPath) {
        openFileBySystemApp(context, new File(strPath));
    }

    /**
     * 根据文件mimeType调用系统相关程序打开
     *
     * @param context
     * @param file
     */
    public static void openFileBySystemApp(Context context, File file) {
        String mimeType = getFileMimeTypeFromFile(file);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        // 设置intent的data和Type属性。
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        context.startActivity(intent);
        String versionName = DeviceUtils.getVersionName(context);
//		if (versionName.equals("4.0.5")) {
//			AccountPreferences.getInstance().setOnLineIsShowNewIcon(true);
//		}
    }

    /**
     * 读取Assets目录下的文件内容到List<String>
     *
     * @param context
     * @return
     */
    public static List<String> readAssetsFile2List(Context context, String assetsFileName) {
        try {
            List<String> list = new ArrayList<String>();
            InputStream in = context.getResources().getAssets().open(assetsFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String str = null;
            while ((str = br.readLine()) != null) {
                list.add(str);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * InputStream.available()得到字节数，然后一次读取完。
     *
     * @param context
     * @param assetsFileName
     * @author xnjiang
     * @since v0.0.1
     */
    public static String readAssetsFile2String1(Context context, String assetsFileName) {
        String content = "";
        try {
            InputStream is = context.getAssets().open(assetsFileName);
            if (is != null) {
                DataInputStream dIs = new DataInputStream(is);
                int length = dIs.available();
                byte[] buffer = new byte[length];
                dIs.read(buffer);
                //content = EncodingUtils.getString(buffer, "UTF-8");
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * BufferedReader.readLine()行读取再加换行符，最后用StringBuilder.append()连接成字符串。
     *
     * @param context
     * @param assetsFileName
     * @return
     * @author xnjiang
     * @since v0.0.1
     */
    public static String readAssetsFile2String2(Context context, String assetsFileName) {
        StringBuilder sb = new StringBuilder("");
        String content = "";
        try {
            InputStream is = context.getAssets().open(assetsFileName);
            if (is != null) {
                BufferedReader d = new BufferedReader(new InputStreamReader(is));
                while (d.ready()) {
                    sb.append(d.readLine() + "\n");
                }
                //content = EncodingUtils.getString(sb.toString().getBytes(), "UTF-8");
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * InputStreamReader先指定以UTF8读取文件，再进行读取读取操作：
     *
     * @param context
     * @param assetName
     * @return
     * @author xnjiang
     * @since v0.0.1
     */
    public static String readAssetsFile2String3(Context context, String assetName) {
        StringBuilder sb = new StringBuilder("");
        String content = "";
        try {
            InputStream is = context.getAssets().open(assetName);
            if (is != null) {
                BufferedReader d = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while (d.ready()) {
                    sb.append(d.readLine() + "\n");
                }
                content = sb.toString();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * 复制Assets目录下的单个文件到指定文件夹
     *
     * @param ctx
     * @param assetsFile
     * @param newDir
     * @return
     */
    public static boolean copyAssetsFile2Dir(Context ctx, String assetsFile, String newDir) {
        try {
            // 在样例文件夹下创建文件
            File fileTarget = new File(newDir, assetsFile);
            // 如果文件已经存在则跳出
            if (fileTarget.exists()) {
                return true;
            }

            InputStream input = ctx.getAssets().open(assetsFile);
            OutputStream output = new FileOutputStream(fileTarget);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            input.close();
            output.close();
            buffer = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 复制Assets目录下的单个文件到指定文件夹
     *
     * @param ctx
     * @param assetsDir
     * @param newDir
     */
    public static void copyAssetsDir2Dir(Context ctx, String assetsDir, String newDir) {
        String[] files;
        try {
            files = ctx.getResources().getAssets().list(assetsDir);
        } catch (IOException e1) {
            return;
        }
        File fileTarget = new File(newDir);
        // if this directory does not exists, make one.
        if (!fileTarget.exists()) {
            if (!fileTarget.mkdirs()) {
                Log.e("--CopyAssets--", "cannot create directory.");
            }
        }
        for (int i = 0; i < files.length; i++) {
            try {
                String fileName = files[i];
                if (fileName.compareTo("images") == 0 || fileName.compareTo("sounds") == 0 || fileName.compareTo("webkit") == 0) {
                    continue;
                }
                // 判断是否为文件夹，通过是否含有“.”来区分文件夹
                if (!fileName.contains(".")) {
                    if (0 == assetsDir.length()) {
                        copyAssetsDir2Dir(ctx, fileName, newDir + fileName + "/");
                    } else {
                        copyAssetsDir2Dir(ctx, assetsDir + "/" + fileName, newDir + fileName + "/");
                    }
                    continue;
                }

                File outFile = new File(fileTarget, fileName);
                // 判断文件是否存在，存在跳过，不存在就复制
                if (outFile.exists()) {
                    continue; // outFile.delete();
                }
                InputStream input = null;
                if (0 != assetsDir.length())
                    input = ctx.getAssets().open(assetsDir + "/" + fileName);
                else
                    input = ctx.getAssets().open(fileName);
                OutputStream output = new FileOutputStream(outFile);
                // Transfer bytes from in to out
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                input.close();
                output.close();
                buffer = null;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取文件名
     *
     * @param file
     * @return
     */
    public static String getFileName(File file) {
        if (file == null) {
            return "";
        }
        return file.getName();
    }

    /**
     * 获取文件名
     *
     * @param path
     * @return
     */
    public static String getFileName(String path) {
        return getFileName(new File(path));
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param file
     * @return
     */
    public static String getFileNameNoExtension(File file) {
        if (file == null) {
            return "";
        }
        String filename = file.getName();
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param path
     * @return
     */
    public static String getFileNameNoExtension(String path) {
        return getFileNameNoExtension(new File(path));
    }

    /**
     * 获取文件扩展名(不包含前面那个.)
     *
     * @param file
     * @return
     */
    public static String getFileExtension(File file) {
        if (file == null || file.isDirectory()) {
            return "";
        }
        String filename = file.getName();
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 获取文件扩展名(不包含前面那个.)
     *
     * @param path
     * @return
     */
    public static String getFileExtension(String path) {
        return getFileExtension(new File(path));
    }

    /**
     * <b>通过文件file对象获取文件标识MimeType</b><br>
     * <br>
     * MIME type的缩写为(Multipurpose Internet Mail Extensions)代表互联网媒体类型(Internet
     * media type)，
     * MIME使用一个简单的字符串组成，最初是为了标识邮件Email附件的类型，在html文件中可以使用content-type属性表示，
     * 描述了文件类型的互联网标准。MIME类型能包含视频、图像、文本、音频、应用程序等数据。
     *
     * @param file
     * @return
     */
    public static String getFileMimeTypeFromFile(File file) {
        String extension = getFileExtension(file);
        extension = extension.replace(".", "");
        if (extension.equals("docx") || extension.equals("wps")) {
            extension = "doc";
        } else if (extension.equals("xlsx")) {
            extension = "xls";
        } else if (extension.equals("pptx")) {
            extension = "ppt";
        }
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        if (mimeTypeMap.hasExtension(extension)) {
            // 获得txt文件类型的MimeType
            return mimeTypeMap.getMimeTypeFromExtension(extension);
        } else {
            if (extension.equals("dwg")) {
                return "application/x-autocad";
            } else if (extension.equals("dxf")) {
                return "application/x-autocad";
            } else if (extension.equals("ocf")) {
                return "application/x-autocad";
            } else {
                return "*/*";
            }
        }
    }

    /**
     * <b>通过文件的扩展名Extension获取文件标识MimeType</b><br>
     * <br>
     * MIME type的缩写为(Multipurpose Internet Mail Extensions)代表互联网媒体类型(Internet
     * media type)，
     * MIME使用一个简单的字符串组成，最初是为了标识邮件Email附件的类型，在html文件中可以使用content-type属性表示，
     * 描述了文件类型的互联网标准。MIME类型能包含视频、图像、文本、音频、应用程序等数据。
     *
     * @param extension
     * @return
     */
    public static String getFileMimeTypeFromExtension(String extension) {
        extension = extension.replace(".", "");
        if (extension.equals("docx") || extension.equals("wps")) {
            extension = "doc";
        } else if (extension.equals("xlsx")) {
            extension = "xls";
        } else if (extension.equals("pptx")) {
            extension = "ppt";
        }
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        if (mimeTypeMap.hasExtension(extension)) {
            // 获得txt文件类型的MimeType
            return mimeTypeMap.getMimeTypeFromExtension(extension);
        } else {
            if (extension.equals("dwg")) {
                return "application/x-autocad";
            } else if (extension.equals("dxf")) {
                return "application/x-autocad";
            } else if (extension.equals("ocf")) {
                return "application/x-autocad";
            } else {
                return "*/*";
            }
        }
    }

    /**
     * <b>通过文件标识MimeType获取文件的扩展名Extension</b><br>
     * <br>
     * MIME type的缩写为(Multipurpose Internet Mail Extensions)代表互联网媒体类型(Internet
     * media type)，
     * MIME使用一个简单的字符串组成，最初是为了标识邮件Email附件的类型，在html文件中可以使用content-type属性表示，
     * 描述了文件类型的互联网标准。MIME类型能包含视频、图像、文本、音频、应用程序等数据。
     *
     * @param mimeType
     * @return
     */
    public static String getFileExtensionFromMimeType(String mimeType) {
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        if (mimeTypeMap.hasMimeType(mimeType)) {
            // 获得"text/html"类型所对应的文件类型如.txt、.jpeg
            return mimeTypeMap.getExtensionFromMimeType(mimeType);
        } else {
            return "";
        }
    }

    /**
     * 获取gmail附件的名称和大小
     *
     * @param context
     * @param documentUri
     * @return
     */
    public static String getAttachmetName(Context context, Uri documentUri) {
        if (null != documentUri) {
            final String uriString = documentUri.toString();
            String documentFilename = null;

            final int mailIndexPos = uriString.lastIndexOf("/attachments");
            if (mailIndexPos != -1) {
                final Uri curi = documentUri;
                final String[] projection = new String[]{OpenableColumns.DISPLAY_NAME};
                final Cursor cursor = context.getContentResolver().query(curi, projection, null, null, null);
                if (cursor != null) {
                    final int attIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (attIdx != -1) {
                        cursor.moveToFirst();
                        documentFilename = cursor.getString(attIdx);
                    }
                    cursor.close();
                }
            }
            return documentFilename;
        }
        return null;
    }

    /**
     * 获取Gmail附件的路径
     *
     * @param uri
     * @return
     */
    public static String getGamiFilePath(Context context, Uri uri) {
        String strFileName = getAttachmetName(context, uri);
        String strAbsolutePath = getAvailableFilesPath(context) + "/" + strFileName;
        try {
            InputStream attachment = context.getContentResolver().openInputStream(uri);
            if (attachment == null)
                Log.e("onCreate", "cannot access mail attachment");
            else {
                FileOutputStream tmp = new FileOutputStream(strAbsolutePath);
                byte[] buffer = new byte[1024];
                while (attachment.read(buffer) > 0)
                    tmp.write(buffer);

                tmp.close();
                attachment.close();
            }
        } catch (FileNotFoundException e) {
            strAbsolutePath = "";
            e.printStackTrace();
        } catch (IOException e) {
            strAbsolutePath = "";
            e.printStackTrace();
        }
        return strAbsolutePath;
    }

    /**
     * 获取文件的绝对路径，相应地可以改成其他多媒体类型如audio等等
     *
     * @param context 必须是Activity的实例
     * @param uri
     * @return
     */
    public static String getAbsoluteImagePath(Context context, Uri uri) {
        String strAbsolutePath = "";
        String[] proj = {MediaColumns.DATA};
        // 好像是android多媒体数据库的封装接口，具体的看Android文档
        Cursor cursor = ((Activity) context).managedQuery(uri, proj, null, null, null);
        // 按我个人理解 这个是获得用户选择的图片的索引值
        int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
        // 将光标移至开头 ，这个很重要，不小心很容易引起越界
        cursor.moveToFirst();
        // 最后根据索引值获取图片路径
        strAbsolutePath = cursor.getString(column_index);
        cursor.close();
        return strAbsolutePath;
    }

    /**
     * 创建XML字符串
     */
    public void writeXMLString() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);

            // 文档开始，标题行
            serializer.startDocument("UTF-8", null);
            // 第一层
            serializer.startTag("", "paralist");
            // 第二层数据层
            // 第一个数据
            serializer.startTag("", "para");
            serializer.startTag("", "value");
            serializer.text("Stonejxn");
            serializer.endTag("", "value");
            serializer.startTag("", "name");
            serializer.text("account");
            serializer.endTag("", "name");
            serializer.endTag("", "para");
            // 第二个数据
            serializer.startTag("", "para");
            serializer.startTag("", "value");
            serializer.text("a123456789");
            serializer.endTag("", "value");
            serializer.startTag("", "name");
            serializer.text("password");
            serializer.endTag("", "name");
            serializer.endTag("", "para");

            serializer.endTag("", "paralist");
            serializer.endDocument();
            Log.d("XML", writer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将XML字符串写入XML文件
     */
    public void writeXMLStringToXMLFile(Context ctx, String strFilePath, String strFileContent) {
        try {
            writeSystemFile(ctx, strFilePath, strFileContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 对比2个方法的返回值来判断APK升级包的签名是否一致，一致就提示可以安装。

    /**
     * 获取APK的签名信息
     *
     * @param apkPath
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String showUninstallAPKSignatures(String apkPath) {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        try {
            // apk包的文件路径
            // 这是一个Package 解释器, 是隐藏的
            // 构造函数的参数只有一个, apk文件的路径
            // PackageParser packageParser = new PackageParser(apkPath);
            Class pkgParserCls = Class.forName(PATH_PackageParser);
            Class[] typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
            Object[] valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            Object pkgParser = pkgParserCt.newInstance(valueArgs);
            // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
            // File(apkPath), apkPath,
            // metrics, 0);
            typeArgs = new Class[4];
            typeArgs[0] = File.class;
            typeArgs[1] = String.class;
            typeArgs[2] = DisplayMetrics.class;
            typeArgs[3] = Integer.TYPE;
            Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage", typeArgs);
            valueArgs = new Object[4];
            valueArgs[0] = new File(apkPath);
            valueArgs[1] = apkPath;
            valueArgs[2] = metrics;
            valueArgs[3] = PackageManager.GET_SIGNATURES;
            Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);

            typeArgs = new Class[2];
            typeArgs[0] = pkgParserPkg.getClass();
            typeArgs[1] = Integer.TYPE;
            Method pkgParser_collectCertificatesMtd = pkgParserCls.getDeclaredMethod("collectCertificates", typeArgs);
            valueArgs = new Object[2];
            valueArgs[0] = pkgParserPkg;
            valueArgs[1] = PackageManager.GET_SIGNATURES;
            pkgParser_collectCertificatesMtd.invoke(pkgParser, valueArgs);
            // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
            Field packageInfoFld = pkgParserPkg.getClass().getDeclaredField("mSignatures");
            Signature[] info = (Signature[]) packageInfoFld.get(pkgParserPkg);
            return info[0].toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取程序自身的签名：
     *
     * @param context
     * @return
     */
    public static String getSign(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
        Iterator<PackageInfo> iter = apps.iterator();
        while (iter.hasNext()) {
            PackageInfo packageinfo = iter.next();
            String packageName = packageinfo.packageName;
            if (packageName.equals(context.getPackageName())) {
                return packageinfo.signatures[0].toString();
            }
        }
        return null;
    }

    /**
     * 复制文件到目标文件夹
     */
    public static boolean copy(String srcPath, String destPath) throws IOException {
        long timestamp = System.currentTimeMillis();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File file = new File(destPath);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }

            fis = new FileInputStream(srcPath);
            fos = new FileOutputStream(destPath);

            int size = 0;
            byte[] buf = new byte[1024];
            while ((size = fis.read(buf)) != -1)
                fos.write(buf, 0, size);
            DebugUtils.d("hxd", "copy success: srcPath:" + srcPath + " destPath:" + destPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            DebugUtils.d("hxd", "copy failed: srcPath:" + srcPath + " destPath:" + destPath);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fis != null) {
                fis.close();
            }
            DebugUtils.d("hxd", "copy use time:" + (System.currentTimeMillis() - timestamp));
        }
    }

    public static boolean checkAndMakeDir(String fileDir) {
        try {
            File file = new File(fileDir);
            if (file.exists()) {
                return true;
            }
            return file.mkdirs();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isJpgFile(String fileName) {
        if (fileName != null) {
            if (fileName.endsWith(".jpg") || fileName.endsWith(".JPG")) {
                return true;
            }

            int pointIndex = fileName.lastIndexOf('.');
            if (pointIndex != -1) {
                String suffix = fileName.substring(pointIndex, fileName.length());
                if (suffix.equalsIgnoreCase(".jpg")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean fileRename(String oldPath, String newPath) {
        File file = new File(oldPath);
        if (!file.exists()) {
            return false;
        } else {
            file.renameTo(new File(newPath));
            return true;
        }
    }


    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    public static String saveCrashInfo2File(Throwable ex,Context context) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        StringBuffer sb = new StringBuffer();

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".txt";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = getAppRootDir(context) + "/cache/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 取得文件夹下的音乐路径 传入目录名称、目录路径
     *
     * @param context
     * @param rootPath
     * @return
     * @author xnjiang
     * @since v0.0.1
     */
    public static List<Map<String, Object>> getAudiosFromFolder(Context context, String rootPath) {
        List<Map<String, Object>> listPhotoInfo = new ArrayList<Map<String, Object>>();
        Map<String, Object> mItem = null;
        try {
            // 获取系通图片管理的数据库信息
            ContentResolver mContentResolver = context.getContentResolver();

            String[] projection = {BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST, MediaColumns.DATA, AudioColumns.DURATION, MediaColumns.SIZE};

            // String strSelection = Media._ID +
            // " in (select image_id from thumbnails) "
            String strSelection = BaseColumns._ID + "!='' ";
            if (!TextUtils.isEmpty(rootPath)) {
                strSelection += " and " + MediaColumns.DATA + " like '" + rootPath + "%' ";
            }
            Cursor cur = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, strSelection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if (cur.moveToFirst()) {
                do {
                    // 歌曲ID：MediaStore.Audio.Media._ID
                    int audio_id = cur.getInt(cur.getColumnIndexOrThrow(BaseColumns._ID));

                    // 歌曲的名称 ：MediaStore.Audio.Media.TITLE
                    String audio_tilte = cur.getString(cur.getColumnIndexOrThrow(MediaColumns.TITLE));

                    // 歌曲的专辑名：MediaStore.Audio.Media.ALBUM
                    String audio_album = cur.getString(cur.getColumnIndexOrThrow(AudioColumns.ALBUM));

                    // 歌曲的歌手名： MediaStore.Audio.Media.ARTIST
                    String audio_artist = cur.getString(cur.getColumnIndexOrThrow(AudioColumns.ARTIST));

                    // 歌曲文件的路径 ：MediaStore.Audio.Media.DATA
                    String audio_url = cur.getString(cur.getColumnIndexOrThrow(MediaColumns.DATA));

                    // 歌曲的总播放时长 ：MediaStore.Audio.Media.DURATION
                    int audio_duration = cur.getInt(cur.getColumnIndexOrThrow(AudioColumns.DURATION));

                    // 歌曲文件的大小 ：MediaStore.Audio.Media.SIZE
                    long audio_size = cur.getLong(cur.getColumnIndexOrThrow(MediaColumns.SIZE));

                    if (!audio_url.equals("")) {
                        mItem = new HashMap<String, Object>();
                        mItem.put("audio_id", audio_id);
                        mItem.put("audio_tilte", audio_tilte);
                        mItem.put("audio_album", audio_album);
                        mItem.put("audio_artist", audio_artist);
                        mItem.put("audio_url", audio_url);
                        mItem.put("audio_duration", audio_duration);
                        mItem.put("audio_size", audio_size);
                        listPhotoInfo.add(mItem);
                        // Log.i(TAG, "getAudiosFromFolder !rootPath = " +
                        // rootPath + ",photo_path = " + photo_path);
                    } else {
                        continue;
                    }
                } while (cur.moveToNext());
                Log.i(TAG, "getAudiosFromFolder 扫描结束!AudiosCount = " + listPhotoInfo.size());
            }
            if (cur != null) {
                cur.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listPhotoInfo;
    }

    /**
     * 获取手机通讯录联系人信息(参数 strContactsName,strContactsNumber同时为""或null查询全部通讯录)
     *
     * @param strContactsName   联系人姓名(模糊查询,默认全部)
     * @param strContactsNumber 联系人电话(模糊查询,默认全部)
     * @return 返回一个List数组包含的属性<br>
     * <b>contactsName</b> 联系人姓名<br>
     * <b>contactsNamePY</b> 联系人姓名 (拼音简码)<br>
     * <b>contactsNumber</b> 手机号<br>
     * <b>contactsEmail</b> 电子邮箱<br>
     * <b>contactsSelected</b> 选择状态(<b>true</b>选中，<b>false</b>未选中)<br>
     */
    public static List<Map<String, Object>> getContactsList(Context context, String strContactsName, String strContactsNumber) {
        List<Map<String, Object>> mListContacters = new ArrayList<Map<String, Object>>();
        HashMap<String, Object> mHashMap = new HashMap<String, Object>();
        // 获取库Phone表字段(联系人显示名称 、手机号码、联系人的ID)
        String[] projection = null;// {Phone.CONTACT_ID,
        // Phone.DISPLAY_NAME,Phone.NUMBER};
        // 设置查询条件
        String strSelection = null;// " LENGTH(TRIM(" + Phone.DISPLAY_NAME
        // +"))>1 ";
        // 设置排序方式，排序字段可以设置多个
        String strOrderBy = null;// " sort_key_alt, " + Phone.DISPLAY_NAME ;
        // // 添加查询条件联系人姓名和联系人电话
        // if (!strContactsName.equalsIgnoreCase("")) {
        // strSelection += Phone.DISPLAY_NAME + " LIKE " + "'%" +
        // strContactsName + "%'";
        // }
        // if (!strContactsNumber.equalsIgnoreCase("")) {
        // strSelection = Phone.NUMBER + " LIKE " + "'" + strContactsNumber +
        // "%'";
        // }

        ContentResolver cr = context.getContentResolver();
        Cursor c_name = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, strSelection, null, strOrderBy);
        while (c_name.moveToNext()) {
            // 得到联系人ID
            String contactsID = c_name.getString(c_name.getColumnIndex(BaseColumns._ID));
            // 得到联系人名称
            String contactsName = c_name.getString(c_name.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            // 得到手机号码
            String contactsNumber = "";
            // 得到联系人email
            String contactsEmail = "";

            // 获取与联系人ID相同的手机号码,可能不止一个
            contactsNumber = "";
            Cursor c_number = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactsID, null, null);
            while (c_number.moveToNext()) {
                String number = c_number.getString(c_number.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
                number = number.replace("-", "");
                if (number != null && !number.trim().equalsIgnoreCase(""))
                    contactsNumber = number;
            }
            c_number.close();

            // 获取与联系人ID相同的电子邮件,可能不止一个
            contactsEmail = "";
            Cursor c_email = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + contactsID, null, null);
            while (c_email.moveToNext()) {
                String email = c_email.getString(c_email.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA1));
                if (email != null && !email.trim().equalsIgnoreCase(""))
                    contactsEmail = email;
            }
            c_email.close();

            // if (StoneFunctions.isEmail(contactsEmail) ||
            // StoneFunctions.isMobileNumber(contactsNumber)) {
            mHashMap = new HashMap<String, Object>();
            mHashMap.put("contactsNamePY", "");
            mHashMap.put("contactsName", contactsName);
            mHashMap.put("contactsNumber", contactsNumber);
            mHashMap.put("contactsEmail", contactsEmail);
            mHashMap.put("contactsSelected", false);
            mListContacters.add(mHashMap);
            Log.i(TAG, "contactsName = " + contactsName + "contactsNumber = " + contactsNumber + "contactsEmail = " + contactsEmail);
            // }
        }
        c_name.close();
        return mListContacters;
    }

    /**
     * 读取给定文件中的内容信息并返回
     *
     * @param fileName
     * @return
     * @throws IOException
     * @author xnjiang
     * @since v0.0.1
     */
    public static String readFileValue(String fileName) throws IOException {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
                FileInputStream inputStream = new FileInputStream(fileName);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.close();
                inputStream.close();
                byte[] data = outputStream.toByteArray();
                return new String(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "readFileValue is Error! ErrorCode = " + e.getMessage());
        }
        return "";
    }

    /**
     * 删除SD卡中给定位置的文件
     */
    public static Boolean DeleteFileFromSD(String strURL) {
        if (strURL == null || "".equals(strURL)) {
            return true;
        }
        // /**SD卡目录获取操作*/
        // //判断SD卡是否插入
        // Result=Environment.getExternalStorageState().equalsIgnoreCase(android.os.Environment.MEDIA_MOUNTED);
        // //获得SD卡根目录：
        // File sdFileRoot = Environment.getExternalStorageDirectory();
        // //获得私有根目录(程序根目录)：
        // String fileRoot = SQLiteContext.getFilesDir()+"\\";
        File myFile = new File(strURL);
        boolean Result = false;
        if (FileUtil.isSDExist()) {
            // 删除文件夹
            Result = myFile.delete();
        }

        // /**文件夹或文件夹操作：*/
        // //建立文件或文件夹
        // if (myFile.isDirectory())//判断是文件或文件夹
        // {
        // Result=myFile.mkdir(); //建立文件夹
        // //获得文件夹的名称：
        // String FileName = myFile.getName();
        // //列出文件夹下的所有文件和文件夹名
        // File[] files = myFile.listFiles();
        // //获得文件夹的父目录
        // String parentPath = myFile.getParent();
        // //修改文件夹名字
        // File myFileNew=new File(parentPath+FileName);
        // Result=myFile.renameTo(myFileNew);
        // //删除文件夹
        // Result=myFile.delete();
        // }
        // else
        // {
        // if (!myFile.exists()) {
        // try {
        // Result=myFile.createNewFile();//建立文件
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        // //获得文件或文件夹的名称：
        // String FileName = myFile.getName();
        // //获得文件的父目录
        // String parentPath = myFile.getParent();
        // //修改文件名字
        // File myFileNew=new File(parentPath+FileName);
        // Result=myFile.renameTo(myFileNew);
        // //删除文件夹
        // Result=myFile.delete();
        // }
        return Result;
    }

    /**
     * 判断SD卡中给定位置的文件是否存在
     *
     * @param strURL
     * @return true 存在 false 不存在
     */
    public static Boolean checkFileExists(String strURL) {
        if (strURL == null || "".equals(strURL)) {
            return false;
        }
        File myFile = new File(strURL);
        boolean Result = true;
        if (FileUtil.isSDExist()) {
            Result = myFile.exists(); // 判断文件是否存在
        }
        return Result;
    }

    /**
     * 将Double型数据的小数做保留处理
     *
     * @param dblValue 输入数值
     * @param intPoint 保留位数
     * @return
     */
    public static double getDoublePoint(double dblValue, int intPoint) {
        try {
            double returnDouble;
            double parm = Math.pow(10, intPoint);
            returnDouble = ((int) (dblValue * parm)) / parm;
            return returnDouble;
        } catch (Exception e) {
            return dblValue;
        }
    }

    /**
     * 将Double型数据的小数做保留处理
     *
     * @param v     输入数值
     * @param scale 保留位数
     * @return
     */
    public static double getDoubleRound(Double v, int scale) {
        try {
            BigDecimal b = null == v ? new BigDecimal("0.0") : new BigDecimal(Double.toString(v));
            BigDecimal one = new BigDecimal("1");
            return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        } catch (Exception e) {
            return v;
        }
    }

    /**
     * 将Double型数据的小数做保留处理后转成字符串
     *
     * @param v     输入数值
     * @param scale 保留位数
     * @return
     */
    public static String getDouble2Sting(Double v, int scale) {
        try {
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(scale);
            format.setMinimumFractionDigits(scale);
            return format.format(v);
        } catch (Exception e) {
            return v.toString();
        }
    }

    /**
     * 取得指定子串在字符串中出现的次数。
     *
     * @param strMain    要扫描的字符串
     * @param strSub 子字符串
     * @return 子串在字符串中出现的次数，如果字符串为<code>null</code>或空，则返回<code>0</code>
     */
    public static int getSubStringCount(String strMain, String strSub) {
        if ((strMain == null) || (strMain.length() == 0) || (strSub == null) || (strSub.length() == 0)) {
            return 0;
        }
        int count = 0;
        int index = 0;

        while ((index = strMain.indexOf(strSub, index)) >= 0) {
            count++;
            index += strSub.length();
        }
        return count;
    }


    /**
     * TODO 检查某个应用是否安装
     *
     * @param context
     * @param packageName
     * @return
     * @author xinwei
     * @since v0.0.1
     */
    public static boolean checkAPP(final Context context, final String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }


    /**
     * 判断是否存在SD卡
     *
     * @return
     */
    public static final boolean hasSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取本地硬盘路径（sdcard路径或者app cache dir）
     *
     * @param context
     * @return
     */
    public static final String getLocalPath(Context context) {
        if (hasSDCard()) {
            return Environment.getExternalStorageDirectory().toString() + File.separator;
        } else {
            return context.getCacheDir() + File.separator;
        }
    }

    public static final String getCachePath(Context context) {
        if (hasSDCard()) {
            return Environment.getExternalStorageDirectory().toString() + File.separator + CACHE_ROOT_DIRECTORY;
        } else {
            return context.getCacheDir() + File.separator + CACHE_ROOT_DIRECTORY;
        }
    }

    public static final String CACHE_ROOT_DIRECTORY = "QtonePad"; // 缓存根目录

    /**
     * 获得当前应用的根文件存放路径
     */
    public static String getAppRootDir(Context context) {
        return getCachePath(context.getApplicationContext());
    }

    /**
     * 应用临时创建的一些 文件 在此存放
     */
    public static String getAppTempDir(Context context) {
        return (new StringBuilder(getAppRootDir(context))).append("/temp/").toString();
    }

    /**
     * crashLog、log在此存放
     */
    public static String getAppCrashLogDir(Context context) {
        return (new StringBuilder(getAppRootDir(context))).append("/crashlog/").toString();
    }

    /**
     * 用户主动下载的一些资源，不包含图片，如第三方应用安装包 在此存放
     */
    public static String getAppDownloadDir(Context context) {
        return (new StringBuilder(getAppRootDir(context))).append("/download/").toString();
    }

    /**
     * 用户主动保存下来的图片在此存放
     */
    public static String getAppImageSaveDir(Context context) {
        return (new StringBuilder(getAppRootDir(context))).append("/imageSave/").toString();
    }


    public static long getFileSizes(File f) throws Exception {//取得文件大小
        long s = 0;
        if (f.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(f);
            s = fis.available();
        } else {
            f.createNewFile();
            System.out.println("文件不存在");
        }
        return s;
    }

    // 递归
    public static long getFileSize(File f) throws Exception//取得文件夹大小
    {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSize(flist[i]);
            } else {
                size = size + flist[i].length();
            }
        }
        return size;
    }

    public static String formetFileSize(long fileS) {//转换文件大小
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS == 0) {
            fileSizeString = "0B";
        } else if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }

    public static long getlist(File f) {//递归求取目录文件个数
        long size = 0;
        File flist[] = f.listFiles();
        if (flist == null)
            return 0;
        size = flist.length;
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getlist(flist[i]);
                size--;
            }
        }
        return size;
    }

    /**
     * 拍照生成的临时照片
     */
    public static File mTmpFile;

    public static File createTmpFile(Context context) {
        mTmpFile = null;
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            // 已挂载
            File pic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
            String fileName = "multi_image_" + timeStamp + "";
            mTmpFile = new File(pic, fileName + ".jpg");
            return mTmpFile;
        } else {
            File cacheDir = context.getCacheDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
            String fileName = "multi_image_" + timeStamp + "";
            mTmpFile = new File(cacheDir, fileName + ".jpg");
            return mTmpFile;
        }

    }


    /**
     * 从contentprovider中的uri转成file文件路径
     *
     * @param context
     * @param uri
     * @return
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 清除临时图片 //data/data/cn.qtone.qfd/
     *
     * @param context
     * @param folder
     */
    public static void cleanDataTempPicture(Context context, String folder) {
        String path = FileUtil.getSystemFilesPath(context) + folder;
        File file = new File(path);
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }
    }


    /**
     * 清除临时图片 //mnt/sdcard/QtonePad/
     *
     * @param context
     * @param folder
     */
    public static void cleanTempPicture(Context context, String folder) {
        try {
            String path = folder;
            File file = new File(path);
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    f.delete();
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * 获取系统相册目录
     *
     * @param context
     * @return
     */
    public static final String getSystemCameraPath(Context context) {
        String path = getLocalPath(context) + "DCIM/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    /**
     * 获取图片url名字
     *
     * @param url
     * @return
     */
    public static String getPicFileName(String url) {
        String fileName = "";
        try {
            int dot = url.lastIndexOf("/");
            int endDot = url.lastIndexOf(".");
            if ((dot > -1) && (endDot < (url.length() - 1))) {
                fileName = url.substring(dot + 1, endDot);
            }
        } catch (Exception e) {
            fileName = "file" + System.currentTimeMillis();
        }
        return fileName;
    }

    /**
     * 返回http文件的文件名
     *
     * @param url
     * @return
     */
    public static String getHttpFileName(String url) {
        if (null != url && url.length() > 0) {
            int index = url.lastIndexOf("/");
            return url.substring(index + 1, url.length());
        } else {
            return "";
        }
    }

    /**
     * 获取网落图片资源，文件已存在则不重新下载，若文件不完整则会重新下载
     *
     * @param url      文件的地址 http://www.baidu.com/a.jpg
     * @param path     保存的路径 file://sdcard/packagename/dir
     * @param filename 保存的文件名 0.jpg
     * @return true：成功  false：失败
     */
    public static boolean downloadFile(String url, String path, String filename) {
        URL myFileURL;
        InputStream input = null;
        OutputStream output = null;
        try {
            myFileURL = new URL(url);
            //获得连接
            URLConnection conn = myFileURL.openConnection();
            //得到数据流
            File filePath = new File(path);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }
            File file;
            file = new File(path + filename);//将要保存图片的路径
            int contentLength = conn.getContentLength();
            DebugUtils.d("hxd", "url:" + url + " contentLength:" + contentLength);
            if (file.exists()) { //文件是否存在
                if (file.length() == contentLength) { //相等则文件完整,直接返回
                    DebugUtils.d("hxd", "url:" + url + " download file exists");
                    return true;
                } else { //不等的话删除文件重新下载
                    deleteFile(file);
                }
            }
            input = conn.getInputStream();
            output = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            output.flush();
            DebugUtils.d("hxd", "url:" + url + " download ok");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            DebugUtils.d("hxd", "url:" + url + " download failed");
            return false;
        } finally {
            try {
                if (null != output) {
                    output.close();
                }
                if (null != input) {
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getLogDir(Context context) {
        File fileDir = context.getExternalCacheDir();
        String dir = null;
        if (fileDir != null) {
            dir = fileDir.getAbsolutePath();
        } else {
            dir = context.getCacheDir().getAbsolutePath();
        }
        dir += "/mtc/log/";
        return dir;
    }

    public static ArrayList<String> GetFiles(String Path, String name, boolean IsIterative) {//搜索目录，扩展名，是否进入子文件夹
        File[] files = new File(Path).listFiles();
        ArrayList<String> lstFile = new ArrayList<>();
        if(null == files || files.length <= 0) return null;

        for (File f : files) {
            if (f.isFile()) {
                int index = f.getPath().lastIndexOf("/");
                String fileName = f.getPath().substring(index + 1, f.getPath().length());
                if (fileName.contains(name))
                    lstFile.add(f.getPath());

                if (!IsIterative)
                    break;
            } else if (f.isDirectory() && !f.getPath().contains("/.")) { //忽略点文件（隐藏文件/文件夹）
                GetFiles(f.getPath(), name, IsIterative);
            }
        }

        return lstFile;
    }

    /**
     * 获取网络文件源码大小，不含图片等信息大小
     * 通过代理访问，如果第一次获取不到数据，则连接第二次
     * @param destUrl 网络html地址  ps:http://www.baidu.com.cn
     * @return
     */
    public static long getWebFileCount(String destUrl) {
        if (TextUtils.isEmpty(destUrl)) return 0;
        long count = 0;

        try {
            //获取代理服务器地址、端口
            URL url = new URL(destUrl);// 通过给定的下载地址得到一个url
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();// 得到一个http连接
            conn.setConnectTimeout(10 * 1000);// 设置连接超时为5s
            conn.setReadTimeout(10 * 1000); //读取数据超时也是5s
            conn.setRequestMethod("GET");// 设置连接方式为GET

            // 如果http返回的代码是200或者206则为连接成功
            if (conn.getResponseCode() == 200 || conn.getResponseCode() == 206) {
                int length = conn.getContentLength();  //获取contentLength
                //第二种获取content-length的方法，两种方法都可以用，如果是大文件，建议第二种方法
                //String len = urlConnection.getHeaderField("content-length");
                if (length < 0) {  //长度获取不到的时候重新连接
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    conn.connect();
                }

                count = length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    public static long saveCodeImg(Bitmap bitmap, String fileName,Context context) {

        try {
            createDir(FileUtil.getSystemCameraPath(context)+"picture");
            File imageFile = new File(FileUtil.getAppImageSaveDir(context)+"picture/" + fileName);
            imageFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }
}

