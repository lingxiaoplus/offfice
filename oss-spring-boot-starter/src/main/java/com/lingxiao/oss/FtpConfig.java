package com.lingxiao.oss;

import com.lingxiao.oss.bean.FtpConfigure;
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
@EnableConfigurationProperties(FtpConfigure.class)
@ConditionalOnProperty(prefix = "ftp", name = "open",havingValue = "true")
public class FtpConfig {
    @Autowired
    private FtpConfigure ftpConfigure;

    @Bean
    public OssFileService fileService(){
        return new FtpFileServiceImpl(ftpConfigure);
    }
}
