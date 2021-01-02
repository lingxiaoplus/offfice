package com.lingxiao.oss.service.impl;


import com.lingxiao.oss.FileException;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.PageResult;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.utils.UploadUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @author Admin
 */
@Slf4j
public class OssFileServiceImpl implements OssFileService {
    private final UploadUtil uploadUtil;
    public OssFileServiceImpl(UploadUtil uploadUtil) {
        this.uploadUtil = uploadUtil;
    }

    @Override
    public OssFileInfo uploadFile(File file) {
        try {
            return uploadUtil.upload(file);
        } catch (QiniuException e) {
            Response r = e.response;
            // 请求失败时打印的异常的信息
            log.error("上传文件失败: {}",r.toString());
            //响应的文本信息
            try {
                log.error("上传文件失败body: {}",r.bodyString());
            } catch (QiniuException ex) {
                ex.printStackTrace();
            }
        }
        throw new FileException("上传文件失败");
    }

    @Override
    public OssFileInfo uploadFile(File file, String folder,boolean convert) {
        try {
            if (convert){
                return uploadUtil.coverUpload(file,folder);
            }else {
                return uploadUtil.upload(file,folder);
            }
        } catch (QiniuException e) {
            try {
                Response r = e.response;
                // 请求失败时打印的异常的信息
                log.error("上传文件失败: {},上传文件失败body: {}",r.toString(),r.bodyString());
            } catch (QiniuException ex) {
                ex.printStackTrace();
            }
        }
        throw new FileException("上传文件失败");
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            uploadUtil.deleteFile(fileName);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            log.error("删除文件错误，错误码：{}，错误详细;{}",ex.code(),ex.response.toString());
        }
        throw new FileException("删除文件失败");
    }

    @Override
    public PageResult<OssFileInfo> getFileList(String filePrefix, int pageNum, int pageSize) {
        List<OssFileInfo> fileList = uploadUtil.getFileList(filePrefix, pageSize*pageNum);
        int totalPage = fileList.size() / pageSize + 1;
        //List<OssFileInfo> subList = fileList.subList((pageSize * (pageNum - 1)), (pageSize * pageNum));
        return new PageResult<>(fileList.size(),totalPage,fileList);
    }


    @Override
    public void moveOrRenameFile(String oldName,String newName, String toBucket) {
        try {
            uploadUtil.moveOrRenameFile(oldName, newName, toBucket);
        } catch (QiniuException ex) {
            //如果遇到异常，说明移动失败
            //ex.response.error
            log.error("移动/重命名文件错误：错误码：{}，错误详细: {}",ex.code(),ex.response.toString());
            throw new FileException("移动/重命名文件失败");
        }
    }
}
