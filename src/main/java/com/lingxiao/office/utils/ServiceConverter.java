package com.lingxiao.office.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingxiao.office.bean.OfficeConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Component
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class ServiceConverter {
    @Autowired
    private OfficeConfigure officeConfigure;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private DocumentManager documentManager;
    private int ConvertTimeout = 120000;
    private String DocumentConverterUrl;

    @PostConstruct
    public void init(){
        DocumentConverterUrl = officeConfigure.getDocService().getUrl().getConverter();
        ConvertTimeout = officeConfigure.getDocService().getTimeout();
    }
    public static class ConvertBody {
        public String url;
        public String outputtype;
        public String filetype;
        public String title;
        public String key;
        public Boolean async;
        public String token;
    }

    public String GetConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync) throws Exception {
        fromExtension = fromExtension == null || fromExtension.isEmpty() ? fileUtil.getFileExtension(documentUri) : fromExtension;

        String title = fileUtil.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        documentRevisionId = documentRevisionId == null || documentRevisionId.isEmpty() ? documentUri : documentRevisionId;

        documentRevisionId = GenerateRevisionId(documentRevisionId);

        ConvertBody body = new ConvertBody();
        body.url = documentUri;
        body.outputtype = toExtension.replace(".", "");
        body.filetype = fromExtension.replace(".", "");
        body.title = title;
        body.key = documentRevisionId;
        if (isAsync){
            body.async = true;
        }
        //Gson gson = new Gson();
        String bodyString = JSON.toJSONString(body);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(DocumentConverterUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bodyByte.length);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(ConvertTimeout);

        if (documentManager.TokenEnabled()) {
            Map<String, Object> map = new HashMap<>();
            map.put("payload", body);
            String token = documentManager.CreateToken(map);
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        connection.connect();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
        }

        InputStream stream = connection.getInputStream();

        if (stream == null){
            throw new Exception("Could not get an answer");
        }
        String jsonString = ConvertStreamToString(stream);

        connection.disconnect();

        return GetResponseUri(jsonString);
    }

    public String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20){
            expectedKey = Integer.toString(expectedKey.hashCode());
        }
        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }

    private void ProcessConvertServiceResponceError(int errorCode) throws Exception {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";

        switch (errorCode)
        {
            case -8:
                errorMessage = errorMessageTemplate + "Error document VKey";
                break;
            case -7:
                errorMessage = errorMessageTemplate + "Error document request";
                break;
            case -6:
                errorMessage = errorMessageTemplate + "Error database";
                break;
            case -5:
                errorMessage = errorMessageTemplate + "Error unexpected guid";
                break;
            case -4:
                errorMessage = errorMessageTemplate + "Error download error";
                break;
            case -3:
                errorMessage = errorMessageTemplate + "Error convertation error";
                break;
            case -2:
                errorMessage = errorMessageTemplate + "Error convertation timeout";
                break;
            case -1:
                errorMessage = errorMessageTemplate + "Error convertation unknown";
                break;
            case 0:
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;
                break;
        }

        throw new Exception(errorMessage);
    }

    private String GetResponseUri(String jsonString) throws Exception {
        JSONObject jsonObj = ConvertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null){
            ProcessConvertServiceResponceError(Math.toIntExact((long)error));
        }
        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent = 0l;
        String responseUri = null;

        if (isEndConvert)
        {
            resultPercent = 100l;
            responseUri = (String) jsonObj.get("fileUrl");
        }
        else
        {
            resultPercent = (Long) jsonObj.get("percent");
            resultPercent = resultPercent >= 100l ? 99l : resultPercent;
        }

        return resultPercent >= 100l ? responseUri : "";
    }

    private String ConvertStreamToString(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while (line != null)
        {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        String result = stringBuilder.toString();
        return result;
    }

    private JSONObject ConvertStringToJSON(String jsonString) {
        return JSON.parseObject(jsonString);
    }
}