package com.lingxiao.oss;

import com.lingxiao.oss.bean.OssProperties;
import com.lingxiao.oss.service.OssFileService;
import com.lingxiao.oss.service.impl.MinIoFileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author renml
 * @date 2021/1/7 17:43
 */
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "oss", name = "type", havingValue = "minio")
@Configuration
public class MinIoConfig {
    @Autowired
    private OssProperties minIoConfigure;

    @Bean
    public OssFileService fileService(){
        return new MinIoFileServiceImpl(minIoConfigure);
    }
}
