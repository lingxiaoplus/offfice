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
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    private HttpServletRequest request;

    public long getMaxFileSize() {
        long size = officeConfigure.getFilesizeMax();
        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public List<String> getFileExts() {
        List<String> res = new ArrayList<>();
        res.addAll(getViewedExts());
        res.addAll(getEditedExts());
        res.addAll(getConvertExts());
        return res;
    }

    public List<String> getViewedExts() {
        String exts = officeConfigure.getDocService().getViewedDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public List<String> getEditedExts() {
        String exts = officeConfigure.getDocService().getEditedDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public List<String> getConvertExts() {
        String exts = officeConfigure.getDocService().getConvertDocs();
        return Arrays.asList(exts.split("\\|"));
    }

    public String CurUserHostAddress(String userAddress) {
        if(userAddress == null) {
            try {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ex) {
                userAddress = "";
            }
        }
        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
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

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoName);

        File file = new File(storagePath(fileName));

        try (FileOutputStream out = new FileOutputStream(file)) {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
        return file;
    }


    public String GetServerUrl() {
        //return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        return request.getScheme() + "://" + "172.19.160.1" + ":" + request.getServerPort() + request.getContextPath();
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

    public String CreateToken(Map<String, Object> payloadClaims) {
        try {
            Signer signer = HMACSigner.newSHA256Signer(officeConfigure.getDocService().getSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet())
            {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        }
        catch (Exception e) {
            return "";
        }
    }

    public JWT ReadToken(String token) {
        try {
            Verifier verifier = HMACVerifier.newVerifier(officeConfigure.getDocService().getSecret());
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public Boolean tokenEnabled() {
        String secret = officeConfigure.getDocService().getSecret();
        return secret != null && !secret.isEmpty();
    }

}