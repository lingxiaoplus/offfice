package com.lingxiao.office.bean;

import com.lingxiao.office.utils.DocumentManager;
import com.lingxiao.office.utils.FileUtil;
import com.lingxiao.office.utils.ServiceConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Admin
 */
public class FileModel {
    private FileUtil fileUtil;
    private DocumentManager documentManager;
    private ServiceConverter serviceConverter;
    public String type = "desktop";
    public String documentType;
    public Document document;
    public EditorConfig editorConfig;
    public String token;

    public FileModel(FileUtil fileUtil, DocumentManager documentManager, ServiceConverter serviceConverter,String fileName) {
        if (fileName == null) {
            fileName = "";
        }
        this.fileUtil = fileUtil;
        this.documentManager = documentManager;
        this.serviceConverter = serviceConverter;

        fileName = fileName.trim();
        documentType = fileUtil.getFileType(fileName).toString().toLowerCase();

        document = new Document();
        document.title = fileName;
        document.url = documentManager.GetFileUri(fileName);
        document.fileType = fileUtil.getFileExtension(fileName).replace(".", "");
        String userId = documentManager.CurUserHostAddress(null);
        document.key = serviceConverter.GenerateRevisionId(userId + "/" + fileName);

        editorConfig = new EditorConfig();
        if (!documentManager.getEditedExts().contains(fileUtil.getFileExtension(fileName)))
            editorConfig.mode = "view";
        editorConfig.callbackUrl = documentManager.getCallbackUrl(fileName);
        editorConfig.user.id = userId;

        editorConfig.customization.goback.url = documentManager.GetServerUrl() + "/IndexServlet";
    }



    public void InitDesktop() {
        type = "embedded";
        editorConfig.InitDesktop(document.url);
    }

    public void BuildToken() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);
        token = documentManager.CreateToken(map);
    }

    public class Document {
        public String title;
        public String url;
        public String fileType;
        public String key;
    }

    public class EditorConfig {
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