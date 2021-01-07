package com.lingxiao.core.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * @author lingxiao
 */
@ConfigurationProperties(prefix = "office")
@Component
@Data
public class OfficeConfigure {
    private long filesizeMax;
    private String saveRootPath;
    private DocService docService = new DocService();

    @Data
    public class DocService{
        private String viewedDocs;
        private String editedDocs;
        private String convertDocs;
        private int timeout;
        private String secret;
        private Url url = new Url();
        @Data
        public class Url{
            private String converter;
            private String tempstorage;
            private String api;
            private String preloader;
            private String callbackServer;
        }
    }
}
