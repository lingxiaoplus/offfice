package com.lingxiao.oss.bean;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author renml
 * @date 2021/1/7 16:55
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinIoConfigure {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
