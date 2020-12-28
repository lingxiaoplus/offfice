package com.lingxiao.office.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingxiao.office.FileType;
import com.lingxiao.office.bean.FtpConfigure;
import com.lingxiao.office.exception.OfficeException;
import com.lingxiao.office.bean.FileInfo;
import com.lingxiao.office.bean.FileModel;
import com.lingxiao.office.service.FileService;
import com.lingxiao.office.utils.DocumentManager;
import com.lingxiao.office.utils.FileUtil;
import com.lingxiao.office.utils.FtpUtil;
import com.lingxiao.office.utils.ServiceConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primeframework.jwt.domain.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @author  renml
 * @date 2020-12-28
 */
@EnableConfigurationProperties(value = FtpConfigure.class)
@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private ServiceConverter serviceConverter;
    @Autowired
    private FtpConfigure ftpConfigure;
    public static final String UPLOAD_PATH = "/";
    public static final String CONVERT_PATH = "/convertedFile";
    public static final String TRACK_PATH = "/trackFile";

    @Override
    public JSONObject upload(MultipartFile file) {
        //response.setContentType("text/plain");
        JSONObject result = new JSONObject();
        try {
            String fileName = file.getOriginalFilename();
            long curSize = file.getSize();
            if (documentManager.getMaxFileSize() < curSize || curSize <= 0) {
                result.put("error", "File size is incorrect");
                return result;
            }

            String curExt = fileUtil.getFileExtension(fileName);
            if (!documentManager.GetFileExts().contains(curExt)) {
                result.put("error", "File type is not supported");
                return result;
            }

            fileName = documentManager.GetCorrectName(fileName);
            log.info("上传的文件名: {}", fileName);
            String fileStoragePath = documentManager.StoragePath(fileName, null);

            File saveFile = new File(fileStoragePath);
            log.info("上传的文件地址: {}", fileStoragePath);
            file.transferTo(saveFile);
            result.put("filename", fileName);

            String fileUrl = FtpUtil.getInstance(ftpConfigure).uploadFile(saveFile.getAbsolutePath(), UPLOAD_PATH);
            if (StringUtils.isNotBlank(fileUrl)) {
                saveFile.deleteOnExit();
                convert(fileName,fileUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public List<FileInfo> getUploadFiles() {
        List<FileInfo> fileInfoList = new ArrayList<>();
        FtpUtil.getInstance(ftpConfigure).readFileByFolder(UPLOAD_PATH, fileInfoList);
        return fileInfoList;
    }


    @Override
    public void convert(String fileName,String fileUrl) {
        //String fileUri = documentManager.GetFileUri(fileName);
        String fileExt = fileUtil.getFileExtension(fileName);
        FileType fileType = fileUtil.getFileType(fileName);
        String internalFileExt = DocumentManager.GetInternalExtension(fileType);
        if (!documentManager.getConvertExts().contains(fileExt)) {
            throw new OfficeException("不支持的文件格式");
        }
        log.debug("开始文件转换: {}",fileUrl);
        String key = serviceConverter.GenerateRevisionId(fileUrl);
        try {
            String newFileUri = serviceConverter.getConvertedUri(fileUrl, fileExt, internalFileExt, key, true);
            if (StringUtils.isBlank(newFileUri)) {
                throw new OfficeException("fileName: " + fileName + " uri is empty");
            }
            String correctName = documentManager.GetCorrectName(fileUtil.getFileNameWithoutExtension(fileName) + internalFileExt);
            URL url = new URL(newFileUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();
            if (stream == null) {
                throw new OfficeException("Stream is null");
            }
            File convertedFile = new File(documentManager.StoragePath(correctName, null));
            try (FileOutputStream out = new FileOutputStream(convertedFile)) {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
            }
            connection.disconnect();
            log.debug("文件转换成功，上传转换文件");
            String uploadFileUrl = FtpUtil.getInstance(ftpConfigure).uploadFile(convertedFile.getAbsolutePath(), CONVERT_PATH);
            log.debug("文件转换成功，上传转换文件 url: {}",uploadFileUrl);
            //remove source file ?
            //File sourceFile = new File(DocumentManager.StoragePath(fileName, null));
            //sourceFile.delete();
            fileName = correctName;
            //writer.write("{ \"filename\" : \"" + fileName + "\"}");
        } catch (Exception ex) {
            ex.printStackTrace();
            //writer.write("{ \"error\": \"" + ex.getMessage() + "\"}");
        }
    }


    @Override
    public void track(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
        String userAddress = request.getParameter("userAddress");
        String fileName = request.getParameter("fileName");

        String storagePath = documentManager.StoragePath(fileName, userAddress);
        String body = "";

        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            body = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        } catch (Exception ex) {
            writer.write("get request.getInputStream error:" + ex.getMessage());
            return;
        }

        if (body.isEmpty()) {
            writer.write("empty request.getInputStream");
            return;
        }

        JSONObject jsonObj = JSON.parseObject(body);
        int status;
        String downloadUri;

        if (documentManager.tokenEnabled()) {
            String token = (String) jsonObj.get("token");
            JWT jwt = documentManager.ReadToken(token);
            if (jwt == null) {
                writer.write("JWT.parse error");
                return;
            }

            status = jwt.getInteger("status");
            downloadUri = jwt.getString("url");
        } else {
            status = (int) jsonObj.get("status");
            downloadUri = (String) jsonObj.get("url");
        }

        int saved = 0;
        if (status == 2 || status == 3)//MustSave, Corrupted
        {
            try {
                URL url = new URL(downloadUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

                if (stream == null) {
                    throw new Exception("Stream is null");
                }

                File savedFile = new File(storagePath);
                try (FileOutputStream out = new FileOutputStream(savedFile)) {
                    int read;
                    final byte[] bytes = new byte[1024];
                    while ((read = stream.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }

                    out.flush();
                }
                connection.disconnect();
                String fileUrl = FtpUtil.getInstance(ftpConfigure).uploadFile(savedFile.getAbsolutePath(), TRACK_PATH);
                log.info("文件转换地址: {}",fileUrl);
                savedFile.deleteOnExit();
            } catch (Exception ex) {
                saved = 1;
            }
        }

        writer.write("{\"error\":" + saved + "}");
    }

    @Override
    public FileModel editFile(String fileName, String fileExt, String mode) {
        String fileUrl = "";
        if (StringUtils.isNotBlank(fileExt)) {
            try {
                File newFile = documentManager.createDemo(fileExt);
                fileUrl = FtpUtil.getInstance(ftpConfigure).uploadFile(newFile.getAbsolutePath(), UPLOAD_PATH);
                fileName = newFile.getName();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        fileUrl = StringUtils.isNotBlank(fileUrl) ? fileUrl : FtpUtil.getInstance(ftpConfigure).getFileUrl(UPLOAD_PATH,fileName);
        if (StringUtils.isBlank(fileUrl)){
            throw new OfficeException("获取文件地址失败, 文件名：" + fileName);
        }
        FileModel.Document document = new FileModel.Document();
        document.title = fileName;
        document.url = StringUtils.isNotBlank(fileUrl) ? fileUrl : FtpUtil.getInstance(ftpConfigure).getFileUrl(UPLOAD_PATH,fileName);
        document.fileType = fileUtil.getFileExtension(fileName).replace(".", "");
        String userId = documentManager.CurUserHostAddress(null);
        document.key = serviceConverter.GenerateRevisionId( document.url);

        FileModel.EditorConfig editorConfig = new FileModel.EditorConfig();
        if (!documentManager.getEditedExts().contains(fileUtil.getFileExtension(fileName))){
            editorConfig.mode = "view";
        }
        editorConfig.callbackUrl = documentManager.getCallbackUrl(fileName);
        editorConfig.user.id = userId;
        editorConfig.customization.goback.url = documentManager.GetServerUrl() + "/IndexServlet";

        FileModel fileModel = new FileModel();
        fileModel.setDocumentType(fileUtil.getFileType(fileName.trim()).toString().toLowerCase());
        fileModel.setDocument(document);
        fileModel.setEditorConfig(editorConfig);


        if ("embedded".equals(mode)) {
            fileModel.InitDesktop();
        }
        if ("view".equals(mode)) {
            fileModel.getEditorConfig().mode = "view";
        }
        if (documentManager.tokenEnabled()) {
            fileModel.buildToken(documentManager);
        }
        return fileModel;
    }

    @Override
    public void saveFileToLocal(String officeUri, String fileName) {
        try {
            URL url = new URL(officeUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();
            //更换为实际的路径F:\DataOfHongQuanzheng\java\eclipse-workspace\.metadata\.plugins\org.eclipse.wst.server.core\tmp0\wtpwebapps\Java Example\\app_data\192.168.56.1\
            //File savedFile = new File("F:\\DataOfHongQuanzheng\\onlyoffice_data\\app_data\\"+fileName);
            String path = documentManager.StoragePath(fileName, null);
            File savedFile = new File(path);
            try (FileOutputStream out = new FileOutputStream(savedFile)) {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
            }
            connection.disconnect();
            FtpUtil.getInstance(ftpConfigure).uploadFile(savedFile.getAbsolutePath(),UPLOAD_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OfficeException("保存本地文件失败");
        }
    }

    @PreDestroy
    public void onDestory() {
        FtpUtil.closeFTP();
    }
}
