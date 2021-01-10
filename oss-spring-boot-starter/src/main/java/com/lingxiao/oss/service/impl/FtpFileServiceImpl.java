package com.lingxiao.oss.service.impl;

import com.lingxiao.oss.FileException;
import com.lingxiao.oss.bean.FtpProperties;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.PageResult;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.utils.FileUtil;
import com.lingxiao.oss.utils.FtpUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lingxiao
 */
@Slf4j
public class FtpFileServiceImpl implements OssFileService {
    private FtpProperties ftpProperties;

    public FtpFileServiceImpl(FtpProperties ftpProperties) {
        this.ftpProperties = ftpProperties;
    }

    @Override
    public OssFileInfo uploadFile(File file) {
        String fileUrl = FtpUtil.getInstance(ftpProperties).uploadFile(file.getAbsolutePath(), ftpProperties.getRootPath());
        OssFileInfo ossFileInfo = new OssFileInfo();
        ossFileInfo.setTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        ossFileInfo.setName(file.getName());
        ossFileInfo.setPath(fileUrl);
        ossFileInfo.setSize(FileUtil.getFileSize(file.getTotalSpace()));
        return ossFileInfo;
    }

    @Override
    public OssFileInfo uploadFile(File file, String folder, boolean convert) {
        if (convert){
            FtpUtil.getInstance(ftpProperties).deleteByFolder(folder.concat("/").concat(file.getName()));
        }
        String fileUrl = FtpUtil.getInstance(ftpProperties).uploadFile(file.getAbsolutePath(), ftpProperties.getRootPath().concat("/").concat(folder));
        OssFileInfo ossFileInfo = new OssFileInfo();
        ossFileInfo.setTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        ossFileInfo.setName(file.getName());
        ossFileInfo.setPath(fileUrl);
        ossFileInfo.setSize(FileUtil.getFileSize(file.getTotalSpace()));
        return ossFileInfo;
    }

    @Override
    public void deleteFile(String fileName) {
        if(!FtpUtil.getInstance(ftpProperties).deleteByFolder(fileName)){
            log.error("删除失败");
            throw new FileException("删除失败");
        }
    }

    @Override
    public void moveOrRenameFile(String oldName, String newName, String toBucket) {

    }

    @Override
    public PageResult<OssFileInfo> getFileList(String filePrefix, int pageNum, int pageSize) {
         List<OssFileInfo> fileInfoList = new ArrayList<>();
        FtpUtil.getInstance(ftpProperties).readFileByFolder(ftpProperties.getRootPath().concat("/").concat(filePrefix), fileInfoList);
        return new PageResult<>(fileInfoList.size(),1,fileInfoList);
    }

    @PreDestroy
    public void onDestroy(){
        FtpUtil.closeFTP();
    }
}
