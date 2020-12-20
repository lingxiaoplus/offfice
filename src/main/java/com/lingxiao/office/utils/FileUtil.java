package com.lingxiao.office.utils;

import com.lingxiao.office.FileType;
import com.lingxiao.office.bean.OfficeConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class FileUtil {
    @Autowired
    private OfficeConfigure officeConfigure;

    public OfficeConfigure getOfficeConfigure() {
        return officeConfigure;
    }

    public FileType getFileType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        if (ExtsDocument.contains(ext)){
            return FileType.Text;
        }
        if (ExtsSpreadsheet.contains(ext)){
            return FileType.Spreadsheet;
        }
        if (ExtsPresentation.contains(ext)){
            return FileType.Presentation;
        }

        return FileType.Text;
    }

    public static List<String> ExtsDocument = Arrays.asList
            (
                    ".doc", ".docx", ".docm",
                    ".dot", ".dotx", ".dotm",
                    ".odt", ".fodt", ".ott", ".rtf", ".txt",
                    ".html", ".htm", ".mht",
                    ".pdf", ".djvu", ".fb2", ".epub", ".xps"
            );

    public static List<String> ExtsSpreadsheet = Arrays.asList
            (
                    ".xls", ".xlsx", ".xlsm",
                    ".xlt", ".xltx", ".xltm",
                    ".ods", ".fods", ".ots", ".csv"
            );

    public static List<String> ExtsPresentation = Arrays.asList
            (
                    ".pps", ".ppsx", ".ppsm",
                    ".ppt", ".pptx", ".pptm",
                    ".pot", ".potx", ".potm",
                    ".odp", ".fodp", ".otp"
            );


    public String getFileName(String url) {
        if (url == null) {
            return null;
        }
        //for external file url
        String tempstorage = officeConfigure.getDocService().getUrl().getTempstorage();
        if (!tempstorage.isEmpty() && url.startsWith(tempstorage))
        {
            Map<String, String> params = GetUrlParams(url);
            return params == null ? null : params.get("filename");
        }

        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        return fileName;
    }

    public String getFileNameWithoutExtension(String url) {
        String fileName = getFileName(url);
        if (fileName == null){
            return null;
        }
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    public String getFileExtension(String url) {
        String fileName = getFileName(url);
        if (fileName == null){
            return null;
        }
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        return fileExt.toLowerCase();
    }

    public static Map<String, String> GetUrlParams(String url) {
        try {
            String query = new URL(url).getQuery();
            String[] params = query.split("&");
            Map<String, String> map = new HashMap<>();
            for (String param : params)
            {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
            return map;
        }
        catch (Exception ex) {
            return null;
        }
    }

    /**
     * 由于linux下不能获取文件的创建时间，并且java中没有对应获取文件创建时间的api，
     * 只有获取修改时间的api,所以如果想在windows下获取创建时间可以这样（
     * 适用于windows和linux，linux下获取的是访问时间即修改时间，windows下获取的是创建时间）
     * @param filePath
     * @return
     */
    public String getFileCreateTime(String filePath){
        File file = new File(filePath);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Path path= Paths.get(filePath);
            BasicFileAttributeView basicview= Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS );
            BasicFileAttributes attr = basicview.readAttributes();
            long millis = attr.creationTime().toMillis();
            return dateFormat.format(new Date(millis));
        } catch (Exception e) {
            e.printStackTrace();
            return dateFormat.format(new Date(file.lastModified()));
        }
    }
}