package com.lingxiao.oss.service.impl;

import com.lingxiao.oss.FileException;
import com.lingxiao.oss.bean.MinIoConfigure;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.OssProperties;
import com.lingxiao.oss.bean.PageResult;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.utils.FileUtil;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author renml
 * @date 2021/1/7 17:01
 */
@Slf4j
public class MinIoFileServiceImpl implements OssFileService {
    private MinioClient minioClient;
    private OssProperties minIoConfigure;

    public MinIoFileServiceImpl(OssProperties minIoConfigure) {
        this.minIoConfigure = minIoConfigure;
        // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
        try {
            minioClient = new MinioClient(minIoConfigure.getPrefixDomain(), minIoConfigure.getAccessKey(), minIoConfigure.getSecretKey());
            createBucket(minIoConfigure.getBucketName());
        } catch (InvalidEndpointException|InvalidPortException e) {
            e.printStackTrace();
            throw new FileException("minio初始化失败，请检查凭证是否正确");
        }
    }

    private void createBucket(String bucketName){
        // 检查存储桶是否已经存在
        try {
            boolean isExist = minioClient.bucketExists(bucketName);
            if(isExist) {
                log.debug("Bucket already exists.");
            } else {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket(bucketName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("创建Bucket失败: {}",e.getMessage());
            throw new FileException("创建Bucket失败");
        }
    }

    @Override
    public OssFileInfo uploadFile(File file) {
        // 使用putObject上传一个文件到存储桶中。
        return uploadFile(file,"",false);
    }

    @Override
    public OssFileInfo uploadFile(File file, String folder, boolean convert) {
        try {
            minioClient.putObject(minIoConfigure.getBucketName(),folder.concat("/").concat(file.getName()), file.getAbsolutePath());
            OssFileInfo ossFileInfo = new OssFileInfo();
            ossFileInfo.setBucket(minIoConfigure.getBucketName());
            ossFileInfo.setTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            ossFileInfo.setName(file.getName());
            ossFileInfo.setPath(minIoConfigure.getPrefixDomain().concat("/").concat(file.getName()));
            return ossFileInfo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileException("上传文件失败");
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(minIoConfigure.getBucketName(),fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileException("删除文件失败");
        }
    }

    @Override
    public void moveOrRenameFile(String oldName, String newName, String toBucket) {

    }

    @Override
    public PageResult<OssFileInfo> getFileList(String filePrefix, int pageNum, int pageSize) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(minIoConfigure.getBucketName(), filePrefix);
            List<OssFileInfo> fileList = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                OssFileInfo ossFileInfo = new OssFileInfo();
                ossFileInfo.setName(item.objectName());
                ossFileInfo.setTime(new SimpleDateFormat("yyyy-MM-dd").format(item.lastModified()));
                ossFileInfo.setSize(FileUtil.getFileSize(item.size()));
                ossFileInfo.setPath(minIoConfigure.getPrefixDomain().concat("/").concat(item.objectName()));
                fileList.add(ossFileInfo);
            }
            return new PageResult<>(fileList.size(),1,fileList);
        } catch (Exception  e) {
            e.printStackTrace();
            throw new FileException("获取文件失败");
        }
    }
}
