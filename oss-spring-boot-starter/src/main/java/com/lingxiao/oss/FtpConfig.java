package com.lingxiao.oss;

import com.lingxiao.oss.bean.FtpProperties;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.service.impl.FtpFileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Admin
 */
@Configuration
@EnableConfigurationProperties(FtpProperties.class)
@ConditionalOnProperty(prefix = "oss", name = "type", havingValue = "ftp")
public class FtpConfig {
    @Autowired
    private FtpProperties ftpProperties;

    @Bean
    public OssFileService fileService(){
        return new FtpFileServiceImpl(ftpProperties);
    }
}
