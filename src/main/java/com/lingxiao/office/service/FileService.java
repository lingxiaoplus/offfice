package com.lingxiao.office.service;

import com.alibaba.fastjson.JSONObject;
import com.lingxiao.office.bean.FileInfo;
import com.lingxiao.office.bean.FileModel;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Admin
 */
public interface FileService {
    JSONObject upload(MultipartFile file);

    List<FileInfo> getUploadFiles();

    void convert(String fileName,String fileUrl);

    void track(HttpServletRequest request, HttpServletResponse response, PrintWriter writer);

    FileModel editFile(String fileName, String fileExt,String mode);

    /**
     * 同步保存only office的文件到本地
     * @param officeUri only office获取到的下载地址
     */
    void saveFileToLocal(String officeUri,String fileName);
}
