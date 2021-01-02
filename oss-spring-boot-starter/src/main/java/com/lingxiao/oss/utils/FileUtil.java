package com.lingxiao.oss.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Admin
 */
public class FileUtil {
    private FileUtil(){ }
    /**
     * @param folder
     * @return
     * @throws IOException
     * 判断下载目录是否存在
     */
    public static void createFolder(String folder) {
        // 下载位置
        File downloadFile = new File(folder);
        Path path = downloadFile.toPath();
        try {
            if (downloadFile.exists()) {
                if (downloadFile.isFile()){
                    Files.delete(path);
                    Files.createDirectories(path);
                }
            }else {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 拷贝文件
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyFile(File source, File dest) {
        try (InputStream input = new FileInputStream(source);OutputStream output =new FileOutputStream(dest)){
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }



    /**
     * 获取文件夹下所有文件集合
     * @param dirPath
     * @return
     */
    public static List<File> getFiles(String dirPath) {
        try {
            File file = new File(dirPath);
            List<File> fileList = new ArrayList<>();
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isFile()) {
                    fileList.add(f);
                } else {
                    getFiles(f.getAbsolutePath());
                }
            }
            return fileList;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * 获取文件大小
     * @param filePath
     * @return
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        long size = 0;
        try (FileInputStream fis = new FileInputStream(file)){
            size = fis.available();
            //mb
            size = size / 1024 / 1024;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 获取文件大小
     * @param
     * @return
     */
    public static String getFileSize(long size) {
        NumberFormat ddf = NumberFormat.getNumberInstance() ;
        ddf.setMaximumFractionDigits(2);
        String fileSize = ddf.format(size) + "b";
        if (size > 1024L){
            double size1 = size/1024d;
            fileSize = ddf.format(size1) + "kb";
            if (size1 > 1024d){
                fileSize = ddf.format(size1/1024d) + "mb";
            }
        }
        return fileSize;
    }
}