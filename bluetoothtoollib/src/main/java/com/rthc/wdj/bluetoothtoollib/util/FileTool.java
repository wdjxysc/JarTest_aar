package com.rthc.wdj.bluetoothtoollib.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/12/7.
 */
public class FileTool {

    /**
     * 在指定目录查找 文件名包含指定字符串的文件路径 列表
     *
     * @param directory 指定目录
     */
    public static File[] getFiles(String directory) {

        File file = new File(directory);

        File[] files = file.listFiles();

        return files;
    }

    /**
     * 获取SD卡目录
     * @return
     */
    public static String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);   //判断sd卡是否存在
        if   (sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();//获取根目录
        }
        return sdDir.toString();

    }



    /**
     * 获取外置SD卡路径
     * @return  应该就一条记录或空
     */
    public static List<String> getExtSDCardPath()
    {
        List<String> lResult = new ArrayList<String>();
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("extSdCard"))
                {
                    String [] arr = line.split(" ");
                    String path = arr[1];
                    File file = new File(path);
                    if (file.isDirectory())
                    {
                        lResult.add(path);
                    }
                }
            }
            isr.close();
        } catch (Exception e) {
        }
        return lResult;
    }


    /**
     * 文件是否存在
     *
     * @param filePathName
     * @return
     */
    public static boolean isExist(String filePathName) {
        File file = new File(filePathName);
        return (!file.isDirectory() && file.exists());
    }

    public static boolean isDirExist(String filePathName) {
        if (!filePathName.endsWith("/")) filePathName += "/";
        File file = new File(filePathName);
        return (file.isDirectory() && file.exists());
    }

    /**
     * 获取路径，不带文件名，末尾带'/'
     *
     * @param filePathName
     * @return
     */
    public static String getPath(String filePathName) {
        try {
            return filePathName.substring(0, filePathName.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取目录的名称 注意：只能获取如：/aaaa/ssss/ 或 /aaaa/dsddd
     *
     * @param filePathName
     * @return
     */
    public static String getDirPathName(String filePathName) {
        try {
            if (filePathName.endsWith("/"))
                filePathName = filePathName.substring(0, filePathName.lastIndexOf('/'));
            return filePathName.substring(filePathName.lastIndexOf("/") + 1, filePathName.length());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取文件名，带后缀
     *
     * @param filePathName
     * @return
     */
    public static String getName(String filePathName) {
        try {
            return filePathName.substring(filePathName.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取文件名，不带后缀
     *
     * @return
     */
    public static String getNameNoPostfix(String filePathName) {
        try {
            return filePathName.substring(filePathName.lastIndexOf('/') + 1, filePathName.lastIndexOf('.'));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 重命名
     *
     * @param filePathName
     * @param newPathName
     */
    public static void rename(String filePathName, String newPathName) {
        if (isNullString(filePathName)) return;
        if (isNullString(newPathName)) return;

        delete(newPathName); //liuwp 20120830 新名称对应的文件可能已经存在，先删除

        File file = new File(filePathName);
        File newFile = new File(newPathName);
        file.renameTo(newFile);
    }

    /**
     * 删除文件
     */
    public static void delete(String filePathName) {
        if (isNullString(filePathName)) return;
        File file = new File(filePathName);
        if (file.isFile() && file.exists()) {
            boolean flag = file.delete();
            Log.i("LOG", "filePathName reset:" + filePathName + " flag:" + flag);
        }
    }

    /**
     * 创建目录，整个路径上的目录都会创建
     *
     * @param path
     */
    public static void createDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            boolean b = file.mkdirs();
        }
    }

    /**
     * 尝试创建空文件
     * <br> 如果文件已经存在不操作,返回true
     *
     * @param path 路径
     * @return 如果创建失败(Exception) 返回false，否则true
     */
    public static boolean createEmptyFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                return file.createNewFile();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取文件大小
     *
     * @param filePathName
     * @return
     */
    public static long getSize(String filePathName) {
        if (isNullString(filePathName)) return 0;
        File file = new File(filePathName);
        if (file.isFile()) return file.length();
        return 0;
    }

    /**
     * 读取文件数据到byte数组
     *
     * @param filePathName 文件名
     * @param readOffset   从哪里开始读
     * @param readLength   读取长度
     * @param dataBuf      保存数据的缓冲区
     * @param bufOffset    从哪里保存
     * @return
     */
    public static boolean readData(String filePathName, int readOffset, int readLength, byte[] dataBuf, int bufOffset) {
        try {
            int readedTotalSize = 0;
            int onceReadSize = 0;

            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePathName));
            in.skip(readOffset);
            while (readedTotalSize < readLength
                    && (onceReadSize = in.read(dataBuf, bufOffset + readedTotalSize, readLength - readedTotalSize)) >= 0) {
                readedTotalSize += onceReadSize;
            }
            in.read(dataBuf, bufOffset, readLength);
            in.close();
            in = null;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 将某个流的内容输出到文件
     *
     * @param in           输入流
     * @param filePathName 目标文件
     * @return
     */
    public static boolean writeFile(InputStream in, String filePathName) {

        boolean flag = false;
        OutputStream outStream = null;
        try {
            File destFile = new File(filePathName);
            if (destFile.exists()) {
                destFile.delete();
            } else {
                destFile.createNewFile();
            }
            outStream = new FileOutputStream(filePathName);
            byte[] buffer = new byte[1024];
            int count = 0;
            while (true) {
                int length = in.read(buffer, 0, 1024);
                if (length > 0) {
                    outStream.write(buffer, 0, length);
                } else {
                    break;
                }
                count += length;
            }
            if (count > 0) {
                flag = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 判断当前字符串是否为空
     *
     * @param str
     * @return
     */
    public static boolean isNullString(String str) {
        if (str == null || str.equals("")) return true;
        return false;
    }

    /**
     * 复制文件
     *
     * @param fromPathName
     * @param toPathName
     * @return
     */
    public static int copy(String fromPathName, String toPathName) {
        try {
            InputStream from = new FileInputStream(fromPathName);
            return copy(from, toPathName);
        } catch (FileNotFoundException e) {
            return -1;
        }
    }

    /**
     * 复制文件
     *
     * @param from
     * @param toPathName
     * @return
     */
    public static int copy(InputStream from, String toPathName) {
        try {
            delete(toPathName); //liuwp 20120925 先删除
            OutputStream to = new FileOutputStream(toPathName);
            byte buf[] = new byte[1024];
            int c;
            while ((c = from.read(buf)) > 0) {
                to.write(buf, 0, c);
            }
            from.close();
            to.close();
            return 0;
        } catch (Exception ex) {
            return -1;
        }
    }


    /**
     * 根据zip文件路径转换为文件路径
     *
     * @param zipFullPath 必须带.zip
     * @return
     */
    public static String zip2FileFullPath(String zipFullPath) {
        int zipIndex = zipFullPath.lastIndexOf(".zip");
        int zipIndexTmp = zipFullPath.lastIndexOf(".ZIP");
        String tmp = "";
        if (zipIndex > -1) {
            tmp = zipFullPath.substring(0, zipIndex);
        } else if (zipIndexTmp > -1) {
            tmp = zipFullPath.substring(0, zipIndexTmp);
        }
        return tmp;
    }

    /**
     * 改变文件权限
     *
     * @param permission
     * @param filePathName
     */
    public static void chmod(String permission, String filePathName) {
        try {
            String command = "chmod " + permission + " " + filePathName;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
