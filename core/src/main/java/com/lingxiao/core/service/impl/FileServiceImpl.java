package com.lingxiao.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lingxiao.core.FileType;
import com.lingxiao.core.bean.*;
import com.lingxiao.core.exception.OfficeException;
import com.lingxiao.core.service.FileService;
import com.lingxiao.core.utils.*;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.PageResult;
import com.lingxiao.oss.service.OssFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;

/**
 * @author Admin
 */
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
    private OssFileService ossFileService;
    @Autowired
    private HttpUtil httpUtil;
    private final String userId = "user_123456";

    public static final String CONVERT_PATH = "/convertedFile";

    @Override
    public ResponseResult<JSONObject> upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        long curSize = file.getSize();
        if (documentManager.fileOverLoad(curSize)) {
            throw new OfficeException("File size is incorrect");
        }
        String curExt = fileUtil.getFileExtension(fileName);
        if (!documentManager.getFileExts().contains(curExt)) {
            throw new OfficeException("File type is not supported");
        }

        //fileName = documentManager.getCorrectName(fileName);
        log.info("上传的文件名: {}", fileName);
        String fileStoragePath = documentManager.storagePath(fileName);
        File saveTempFile = new File(fileStoragePath);
        log.info("上传的文件地址: {}", fileStoragePath);
        try {
            file.transferTo(saveTempFile);
            JSONObject result = new JSONObject();
            result.put("fileName", fileName);
            OssFileInfo ossFileInfo = ossFileService.uploadFile(saveTempFile);
            saveTempFile.deleteOnExit();
            result.put("fileUrl",ossFileInfo.getPath());
            return ResponseResult.ok(result);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OfficeException(e.getMessage());
        }
    }

    @Override
    public PageResult<OssFileInfo> getUploadFiles() {
        PageResult<OssFileInfo> fileList = ossFileService.getFileList("", 1, 5);
        return fileList;
    }


    @Override
    public ResponseResult<ConvertResult> convert(String fileName,String fileUrl) {
        String fileExt = fileUtil.getFileExtension(fileName);
        FileType fileType = fileUtil.getFileType(fileName);
        String internalFileExt = DocumentManager.getInternalExtension(fileType);
        if (documentManager.getViewedSuffixes().contains(fileExt)){
            ConvertResult convertResult = new ConvertResult();
            convertResult.setFileUrl(fileUrl);
            convertResult.setFileName(fileName);
            convertResult.setProgress(100);
            convertResult.setEndConvert(true);
            return ResponseResult.ok(convertResult);
        }
        if (!documentManager.getConvertSuffixes().contains(fileExt)) {
            throw new OfficeException("不支持的文件格式: "+ fileExt);
        }
        log.debug("开始文件转换: {}",fileUrl);
        String key = serviceConverter.GenerateRevisionId(fileUrl);
        ConvertResult convertResult = serviceConverter.getConvertedUri(fileUrl, fileExt, internalFileExt, key, true);
        if (!convertResult.getEndConvert()) {
            //没有转换完成
            return ResponseResult.ok(convertResult);
        }
        //转换完成，可以下载转换后的文件了
        String correctName = documentManager.getCorrectName(fileUtil.getFileNameWithoutExtension(fileName) + internalFileExt);
        try {
            File convertedFile = httpUtil.downloadFile(convertResult.getFileUrl(), documentManager.storagePath(correctName));
            log.debug("文件转换成功，上传转换文件...");
            OssFileInfo ossFileInfo = ossFileService.uploadFile(convertedFile, CONVERT_PATH,false);
            convertedFile.deleteOnExit();
            log.debug("文件转换成功: {}",ossFileInfo);
            convertResult.setFileUrl(ossFileInfo.getPath());
            return ResponseResult.ok(convertResult);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new OfficeException(ex.getMessage());
        }
    }

    @Override
    public FileModel createEmptyFile(String fileType) {
        try {
            File newFile = documentManager.createDemo(fileType);
            OssFileInfo ossFileInfo = ossFileService.uploadFile(newFile);
            String fileUrl = ossFileInfo.getPath();
            String fileName = newFile.getName();
            return createFileModel(fileName,fileUrl);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new OfficeException(ex.getMessage());
        }
    }

    @Override
    public FileModel createFileModel(String fileName,String fileUrl){
        if (StringUtils.isBlank(fileUrl)){
            throw new OfficeException("获取文件地址失败, 文件名：" + fileName);
        }
        FileModel.Document document = new FileModel.Document();
        document.title = fileName;
        document.url = fileUrl;
        document.fileType = fileUtil.getFileExtension(fileName).replace(".", "");
        //String userId = documentManager.CurUserHostAddress(null);
        document.key = serviceConverter.GenerateRevisionId(document.url);

        FileModel.EditorConfig editorConfig = new FileModel.EditorConfig();
        if (!documentManager.getEditedSuffixes().contains(fileUtil.getFileExtension(fileName))){
            editorConfig.mode = "view";
        }
        //出现无法保存相关的报错，先看一下回调路径是否设置正确
        editorConfig.callbackUrl = documentManager.getCallbackUrl(fileName);
        editorConfig.user.id = userId;
        //editorConfig.customization.goback.url = documentManager.getServerUrl() + "/IndexServlet";

        FileModel fileModel = new FileModel();
        fileModel.setDocumentType(fileUtil.getFileType(fileName.trim()).toString().toLowerCase());
        fileModel.setDocument(document);
        fileModel.setEditorConfig(editorConfig);
        //在内置窗口中打开还是新页面
        /*if ("embedded".equals(mode)) {
            fileModel.InitDesktop();
        }
        if ("view".equals(mode)) {
            fileModel.getEditorConfig().mode = "view";
        }*/
        if (documentManager.tokenEnabled()) {
            fileModel.buildToken(documentManager);
        }
        return fileModel;
    }

    @Override
    public void saveFileToLocal(String officeUri, String fileName) {
        try {
            String path = documentManager.storagePath(fileName);
            File savedFile = httpUtil.downloadFile(officeUri,path);
            ossFileService.uploadFile(savedFile,"",true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OfficeException("保存文件失败");
        }
    }
}
