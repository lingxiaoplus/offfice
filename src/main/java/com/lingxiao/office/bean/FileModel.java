package com.lingxiao.office.bean;

import com.lingxiao.office.utils.DocumentManager;
import com.lingxiao.office.utils.FileUtil;
import com.lingxiao.office.utils.ServiceConverter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Admin
 */
@Data
public class FileModel {

    private String type = "desktop";
    private String documentType;
    private Document document;
    private EditorConfig editorConfig;
    public String token;

    public void InitDesktop() {
        type = "embedded";
        editorConfig.InitDesktop(document.url);
    }

    public void buildToken(DocumentManager documentManager) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);
        token = documentManager.CreateToken(map);
    }

    public static class Document {
        public String title;
        public String url;
        public String fileType;
        public String key;
    }

    public static class EditorConfig {
        public String mode = "edit";
        public String callbackUrl;
        public User user;
        public Customization customization;
        public Embedded embedded;

        public EditorConfig() {
            user = new User();
            customization = new Customization();
        }

        public void InitDesktop(String url) {
            embedded = new Embedded();
            embedded.saveUrl = url;
            embedded.embedUrl = url;
            embedded.shareUrl = url;
            embedded.toolbarDocked = "top";
        }

        public class User {
            public String id;
            public String name = "John Smith";
        }

        public class Customization {
            public Goback goback;

            public Customization()
            {
                goback = new Goback();
            }

            public class Goback
            {
                public String url;
            }
        }

        public class Embedded {
            public String saveUrl;
            public String embedUrl;
            public String shareUrl;
            public String toolbarDocked;
        }
    }
}