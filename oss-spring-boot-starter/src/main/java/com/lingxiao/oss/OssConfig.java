package com.lingxiao.oss;

import com.lingxiao.oss.bean.OssProperties;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.service.impl.OssFileServiceImpl;
import com.lingxiao.oss.utils.UploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：配置类
 *
 * @Author shf
 * @Date 2019/5/7 21:50
 * @Version V1.0
 **/
@Configuration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "oss", name = "type", havingValue = "qiniu")
public class OssConfig {
    @Autowired
    private OssProperties ossProperties;

    @Bean
    public UploadUtil uploadUtil(){
        return new UploadUtil(ossProperties);
    }

    @Bean
    public OssFileService fileService(){
        return new OssFileServiceImpl(uploadUtil());
    }
}