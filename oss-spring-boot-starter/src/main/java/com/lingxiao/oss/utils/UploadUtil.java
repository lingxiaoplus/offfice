package com.lingxiao.oss.utils;

import com.lingxiao.oss.bean.OssProperties;
import com.lingxiao.oss.bean.OssFileInfo;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lingxiao
 */
@Slf4j
public class UploadUtil {
    private final OssProperties ossProperties;
    private Auth mAuth;
    public UploadUtil(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        log.info("配置文件: {}",ossProperties);
        mAuth = Auth.create(ossProperties.getAccessKey(), ossProperties.getSecretKey());
    }

    public OssProperties getOssProperties(){
        return ossProperties;
    }

    /**
     * 默认上传到rootPath
     * @return 返回图片链接
     */
    public OssFileInfo upload(File file) throws QiniuException{
        return upload(file,"/");
    }

    /**
     * @return 返回图片链接
     */
    public OssFileInfo upload(File file,String folder) throws QiniuException{
        //第二种方式: 自动识别要上传的空间(bucket)的存储区域是华东、华北、华南。
        Zone z = Zone.autoZone();
        Configuration c = new Configuration(z);
        //创建上传对象
        UploadManager uploadManager = new UploadManager(c);
        if (StringUtils.isNotBlank(ossProperties.getRootPath())){
            folder = ossProperties.getRootPath() + folder;
        }
        String concatPath = folder.concat("/").concat(file.getName());
        log.info("上传的路径: {}",concatPath);
        //调用put方法上传
        Response res = uploadManager.put(file.getPath(), concatPath, getUpToken(mAuth));
        //打印返回的信息
        log.debug("文件上传返回信息: {}",res.bodyString());
        StringMap jsonToMap = res.jsonToMap();
        String key = (String) jsonToMap.get("key");
        String url = ossProperties.getPrefixDomain() + key;
        OssFileInfo fileInfo = new OssFileInfo();
        fileInfo.setName(key);
        fileInfo.setPath(url);
        fileInfo.setSize(FileUtil.getFileSize(file.length()));
        return fileInfo;
    }

    /**
     * 覆盖上传
     * @return 返回图片链接
     */
    public OssFileInfo coverUpload(File file,String folder) throws QiniuException{
        //第二种方式: 自动识别要上传的空间(bucket)的存储区域是华东、华北、华南。
        Zone z = Zone.autoZone();
        Configuration c = new Configuration(z);
        //创建上传对象
        UploadManager uploadManager = new UploadManager(c);
        if (StringUtils.isNotBlank(ossProperties.getRootPath())){
            folder = ossProperties.getRootPath() + folder;
        }
        String remotePath = folder.concat("/").concat(file.getName());
        //调用put方法上传
        Response res = uploadManager.put(file.getPath(), remotePath, getUpToken(mAuth,remotePath));
        //打印返回的信息
        log.debug("文件上传返回信息: {}",res.bodyString());
        StringMap jsonToMap = res.jsonToMap();
        String key = (String) jsonToMap.get("key");
        String url = ossProperties.getPrefixDomain() + key;
        OssFileInfo fileInfo = new OssFileInfo();
        fileInfo.setName(key);
        fileInfo.setPath(url);
        fileInfo.setSize(FileUtil.getFileSize(file.length()));
        return fileInfo;
    }

    /**
     *
     * @param key your file key
     */
    public FileInfo getFileInfo(String key){
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        BucketManager bucketManager = new BucketManager(mAuth, cfg);
        try {
            FileInfo fileInfo = bucketManager.stat(ossProperties.getBucketName(), key);
            System.out.println(fileInfo.hash);
            System.out.println(fileInfo.fsize);
            System.out.println(fileInfo.mimeType);
            System.out.println(fileInfo.putTime);
            return fileInfo;
        } catch (QiniuException ex) {
            log.error("获取文件信息错误：{}",ex.response.toString());
        }
        return null;
    }

    /**
     * 修改文件类型
     * @param fileKey
     * @param mimeType
     * @return
     */
    public boolean changeFileType(String fileKey,String mimeType){
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        BucketManager bucketManager = new BucketManager(mAuth, cfg);
        //修改文件类型
        try {
            bucketManager.changeMime(ossProperties.getBucketName(), fileKey, mimeType);
            return true;
        } catch (QiniuException ex) {
            log.error("修改文件类型错误：{}",ex.response.toString());
        }
        return false;
    }


    /**
     *
     * @param oldName
     * @param toBucket  需要移动到的新空间  如果为空默认不移动 只是重命名
     * @param newName
     */
    public void moveOrRenameFile(String oldName, String newName, String toBucket) throws QiniuException{
        if(StringUtils.isBlank(newName)) newName = oldName;
        if(StringUtils.isBlank(toBucket)) toBucket = ossProperties.getBucketName();
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        BucketManager bucketManager = new BucketManager(mAuth, cfg);
        bucketManager.move(ossProperties.getBucketName(), oldName, toBucket, newName);
    }

    public void deleteFile(String fileKey) throws QiniuException {
        Configuration cfg = new Configuration(Region.region0());
        BucketManager bucketManager = new BucketManager(mAuth, cfg);
        bucketManager.delete(ossProperties.getBucketName(), fileKey);
    }

    /**
     * 返回文件列表
     * @param prefix 文件名前缀
     * @param limit 每次迭代的长度限制，最大1000，推荐值 1000
     * @return
     */
    public List<OssFileInfo> getFileList(String prefix, int limit){
        List<OssFileInfo> infoList = new ArrayList<>();
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        BucketManager bucketManager = new BucketManager(mAuth, cfg);
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        prefix = StringUtils.isBlank(ossProperties.getRootPath())? "": ossProperties.getRootPath();
        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(ossProperties.getBucketName(), prefix, limit, delimiter);
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] next = fileListIterator.next();
            if (next == null){
                return Collections.emptyList();
            }
            List<OssFileInfo> collect =
                    Arrays.stream(next)
                            .map(item->{
                                OssFileInfo fileInfo = new OssFileInfo();
                                String name = StringUtils.substringAfterLast(item.key, "/");
                                if (StringUtils.isBlank(name)){
                                    name = item.key;
                                }
                                fileInfo.setName(name);
                                fileInfo.setMimeType(item.mimeType);
                                fileInfo.setPath(ossProperties.getPrefixDomain() + item.key);
                                fileInfo.setEndUser(item.endUser);
                                fileInfo.setSize(FileUtil.getFileSize(item.fsize));
                                DateTime dateTime = new DateTime(item.putTime/10000);
                                String dateString = dateTime.toString("yyyy-MM-dd HH:mm:ss");
                                fileInfo.setTime(dateString);
                                fileInfo.setBucket(ossProperties.getBucketName());
                                log.debug("文件：{}",fileInfo);
                                return fileInfo;
                            })
                            .collect(Collectors.toList());
            infoList.addAll(collect);
        }
        return infoList;
    }


    /**
     * 对于配置了镜像存储的空间，如果镜像源站更新了文件内容，则默认情况下，七牛不会再主动从客户镜像源站同步新的副本，
     * 这个时候就需要利用这个prefetch接口来主动地将空间中的文件和更新后的源站副本进行同步
     * @param fileName
     * @throws QiniuException
     */
    private void updateFileContent(String fileName) throws QiniuException{
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(ossProperties.getAccessKey(), ossProperties.getSecretKey());
        BucketManager bucketManager = new BucketManager(auth, cfg);
        bucketManager.prefetch(ossProperties.getBucketName(), fileName);
    }

    //简单上传，使用默认策略，只需要设置上传的空间名就可以了
    private String getUpToken(Auth auth) {
        return auth.uploadToken(ossProperties.getBucketName());
    }

    /**
     * 覆盖上传
     * @param auth
     * @param key 文件名
     * @return
     */
    private String getUpToken(Auth auth,String key) {
        return auth.uploadToken(ossProperties.getBucketName(),key);
    }
}