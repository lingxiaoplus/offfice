package com.lingxiao.oss.bean;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author renml
 * @date 2020/12/22 16:36
 */
@ConfigurationProperties(prefix = "ftp")
@Component
@Data
public class FtpProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String baseUrl;
    private String rootPath;
}
