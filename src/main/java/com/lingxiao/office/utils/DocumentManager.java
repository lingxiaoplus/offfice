package com.lingxiao.office.utils;

import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lingxiao.office.FileType;
import com.lingxiao.office.bean.OfficeConfigure;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

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

    public List<String> GetFileExts()
    {
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
            try
            {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception ex)
            {
                userAddress = "";
            }
        }
        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public String StoragePath(String fileName, String userAddress) {
        //String serverPath = request.getSession().getServletContext().getRealPath("");
        String serverPath = officeConfigure.getSaveRootPath();
        String storagePath = officeConfigure.getStorageFolder();
        String hostAddress = CurUserHostAddress(userAddress);
        String directory = serverPath + File.separator + storagePath + File.separator;

        File file = new File(directory);

        if (!file.exists()) {
            file.mkdirs();
        }

        directory = directory + hostAddress + File.separator;
        file = new File(directory);

        if (!file.exists()) {
            file.mkdirs();
        }
        return directory + fileName;
    }

    public String GetCorrectName(String fileName) {
        String baseName = fileUtil.getFileNameWithoutExtension(fileName);
        String ext = fileUtil.getFileExtension(fileName);
        String name = baseName + ext;
        File file = new File(StoragePath(name, null));
        for (int i = 1; file.exists(); i++) {
            name = baseName + " (" + i + ")" + ext;
            file = new File(StoragePath(name, null));
        }
        return name;
    }

    public String CreateDemo(String fileExt) throws Exception {
        String demoName = "sample." + fileExt;
        String fileName = GetCorrectName(demoName);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoName);

        File file = new File(StoragePath(fileName, null));

        try (FileOutputStream out = new FileOutputStream(file)) {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
        return fileName;
    }

    public String GetFileUri(String fileName) {
        try {
        	//String serverPath = GetServerUrl();
            String serverPath = officeConfigure.getSaveRootPath();
        	String storagePath = officeConfigure.getStorageFolder();
            String hostAddress = CurUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");
            //String filePath = serverPath + "/" + storagePath + "/"+ URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");
            return filePath;
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public String GetServerUrl() {
        //return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        return request.getScheme() + "://" + "172.19.160.1" + ":" + request.getServerPort() + request.getContextPath();
    }

    public String getCallbackUrl(String fileName) {
        String callbackServer = officeConfigure.getDocService().getUrl().getCallbackServer();
        String hostAddress = CurUserHostAddress(null);
        try
        {
            String query = "?type=track&fileName=" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return callbackServer + query;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String GetInternalExtension(FileType fileType) {
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
            Signer signer = HMACSigner.newSHA256Signer(GetTokenSecret());
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
            Verifier verifier = HMACVerifier.newVerifier(GetTokenSecret());
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public Boolean TokenEnabled() {
        String secret = GetTokenSecret();
        return secret != null && !secret.isEmpty();
    }

    private String GetTokenSecret() {
        return officeConfigure.getDocService().getSecret();
    }

    public File[] getStoredFiles() {
        String directory = filesRootPath();
        File file = new File(directory);
        return file.listFiles(File::isFile);
    }
    public String filesRootPath() {
        String hostAddress = CurUserHostAddress(null);
        //String serverPath = request.getSession().getServletContext().getRealPath("");
        String serverPath = officeConfigure.getSaveRootPath();
        String storagePath = officeConfigure.getStorageFolder();
        //String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        return directory;
    }

}