package com.lingxiao.oss.bean;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Admin
 */
@ConfigurationProperties(prefix = "oss")
@Data
@NoArgsConstructor
public class OssProperties {
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String prefixImg;
    private String temporaryFolder;
    private String rootPath;
}