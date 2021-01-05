package com.lingxiao.core.service;

import com.alibaba.fastjson.JSONObject;
import com.lingxiao.core.bean.ConvertResult;
import com.lingxiao.core.bean.FileModel;
import com.lingxiao.core.bean.ResponseResult;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Admin
 */
public interface FileService {
    /**
     * 上传文件
     * @param file
     * @return
     */
    ResponseResult<JSONObject> upload(MultipartFile file);

    /**
     * 获取文件列表
     * @return
     */
    PageResult<OssFileInfo> getUploadFiles();

    /**
     * 转换文件  将文件转换为only office可处理的格式
     * @param fileName
     * @param fileUrl
     * @return
     */
    ResponseResult<ConvertResult> convert(String fileName, String fileUrl);

    /**
     * 根据simple示例，创建一个新文件
     * @param fileType  文件类型
     * @return
     */
    FileModel createEmptyFile(String fileType);

    /**
     * 创建文件的配置信息： 比如这个文件是否可编辑、可保存
     * @param fileName  文件名
     * @param fileUrl  文件链接
     * @return
     */
    FileModel createFileModel(String fileName,String fileUrl);

    /**
     * 同步保存only office的文件到本地
     * @param officeUri only office获取到的下载地址
     * @param fileName 文件名
     */
    void saveFileToLocal(String officeUri,String fileName);
}
