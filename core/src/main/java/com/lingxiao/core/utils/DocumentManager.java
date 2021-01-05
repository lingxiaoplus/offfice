package com.lingxiao.core.utils;

import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.lingxiao.core.FileType;
import com.lingxiao.core.bean.OfficeConfigure;
import com.lingxiao.core.exception.OfficeException;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * @author lingxiao
 */
@Component
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class DocumentManager {
    @Autowired
    private OfficeConfigure officeConfigure;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private ResourceLoader resourceLoader;

    public boolean fileOverLoad(long fileSize) {
        long maxSize = officeConfigure.getFilesizeMax();
        maxSize = maxSize > 0 ? maxSize : 5 * 1024 * 1024;
        return maxSize < fileSize;
    }

    public List<String> getFileExts() {
        List<String> res = new ArrayList<>();
        res.addAll(getViewedSuffixes());
        res.addAll(getEditedSuffixes());
        res.addAll(getConvertSuffixes());
        return res;
    }

    public List<String> getViewedSuffixes() {
        String exts = officeConfigure.getDocService().getViewedDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public List<String> getEditedSuffixes() {
        String exts = officeConfigure.getDocService().getEditedDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public List<String> getConvertSuffixes() {
        String exts = officeConfigure.getDocService().getConvertDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public String storagePath(String fileName) {
        String serverPath = officeConfigure.getSaveRootPath();
        String directory = serverPath + File.separator;
        File file = new File(directory);
        if (!file.exists()) {
            try {
                Files.createDirectories(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory + fileName;
    }

    public String getCorrectName(String fileName) {
        String baseName = fileUtil.getFileNameWithoutExtension(fileName);
        String ext = fileUtil.getFileExtension(fileName);
        String name = baseName + ext;
        File file = new File(storagePath(name));
        for (int i = 1; file.exists(); i++) {
            name = baseName + " (" + i + ")" + ext;
            file = new File(storagePath(name));
        }
        return name;
    }

    public File createDemo(String fileExt) throws IOException {
        String demoName = "sample." + fileExt;
        String fileName = getCorrectName(demoName);
        File file = new File(storagePath(fileName));
        Resource resource = resourceLoader.getResource("classpath:sample." + fileExt);
        boolean copyResult = fileUtil.copyFile(resource.getFile(), file);
        if (!copyResult){
            throw new OfficeException("创建demo失败");
        }
        /*InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        }*/
        return file;
    }

    public String getCallbackUrl(String fileName) {
        String callbackServer = officeConfigure.getDocService().getUrl().getCallbackServer();
        try {
            String query = "?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
            return callbackServer + query;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getInternalExtension(FileType fileType) {
        if (fileType.equals(FileType.Text)){
            return ".docx";
        }
        if (fileType.equals(FileType.Spreadsheet)){
            return ".xlsx";
        }
        if (fileType.equals(FileType.Presentation)){
            return ".pptx";
        }
        return ".docx";
    }

    public String createToken(Map<String, Object> payloadClaims) {
        try {
            Signer signer = HMACSigner.newSHA256Signer(officeConfigure.getDocService().getSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        } catch (Exception e) {
            return "";
        }
    }

    public JWT readToken(String token) {
        try {
            Verifier verifier = HMACVerifier.newVerifier(officeConfigure.getDocService().getSecret());
            return JWT.getDecoder().decode(token, verifier);
        } catch (Exception exception) {
            return null;
        }
    }

    public boolean tokenEnabled() {
        String secret = officeConfigure.getDocService().getSecret();
        return secret != null && !secret.isEmpty();
    }

}