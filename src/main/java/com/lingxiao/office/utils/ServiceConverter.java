package com.lingxiao.office.utils;

import com.alibaba.fastjson.JSON;
import com.lingxiao.office.bean.OfficeConfigure;
import com.lingxiao.office.exception.OfficeException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author  renml
 * @date 2020-12-28
 */
@Component
@Slf4j
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class ServiceConverter {
    @Autowired
    private OfficeConfigure officeConfigure;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private HttpUtil httpUtil;

    public static class ConvertBody {
        public String url;
        public String outputtype;
        public String filetype;
        public String title;
        public String key;
        public Boolean async;
        public String token;
    }

    public String getConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync) throws Exception {
        fromExtension = StringUtils.isBlank(fromExtension) ? fileUtil.getFileExtension(documentUri) : fromExtension;

        String title = fileUtil.getFileName(documentUri);
        title = StringUtils.isBlank(title) ? UUID.randomUUID().toString() : title;

        documentRevisionId = StringUtils.isBlank(documentRevisionId) ? documentUri : documentRevisionId;
        documentRevisionId = GenerateRevisionId(documentRevisionId);

        ConvertBody body = new ConvertBody();
        body.url = documentUri;
        body.outputtype = toExtension.replace(".", "");
        body.filetype = fromExtension.replace(".", "");
        body.title = title;
        body.key = documentRevisionId;
        body.async = isAsync;

        Map<String,String> headerMap = new HashMap<>();
        if (documentManager.tokenEnabled()) {
            Map<String, Object> map = new HashMap<>();
            map.put("payload", body);
            String token = documentManager.CreateToken(map);
            headerMap.put("Authorization","Bearer " + token);
        }
        headerMap.put("Accept", "application/json");
        Response response = httpUtil.addHeader(headerMap).doPost(officeConfigure.getDocService().getUrl().getConverter(), body);
        if (response.isSuccessful()){
            return getResponseUri(response.body().string());
        }else {
            log.info("请求office转换服务失败, {}",response.code());
            throw new OfficeException("请求office转换服务失败");
        }
    }

    public String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20){
            expectedKey = Integer.toString(expectedKey.hashCode());
        }
        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");
        return key.substring(0, Math.min(key.length(), 20));
    }

    private void processConvertServiceResponseError(int errorCode) {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";
        switch (errorCode) {
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
        throw new OfficeException(errorMessage);
    }

    @Data
    private static class ConvertResponse{
        private Integer error;
        private Integer percent;
        private Boolean endConvert;
        private String fileUrl;
    }

    private String getResponseUri(String jsonString) {
        log.debug("获取到转换结果的response: {}",jsonString);
        //JSONObject jsonObj = ConvertStringToJSON(jsonString);
        ConvertResponse convertResponse = JSON.parseObject(jsonString, ConvertResponse.class);
        if (convertResponse.getError() != null){
            processConvertServiceResponseError(convertResponse.getError());
        }
        int resultPercent = 0;
        String responseUri = null;
        if (convertResponse.getEndConvert()) {
            resultPercent = 100;
            responseUri = convertResponse.getFileUrl();
        } else {
            resultPercent = convertResponse.getPercent();
            resultPercent = resultPercent >= 100 ? 99 : resultPercent;
        }
        return resultPercent >= 100 ? responseUri : "";
    }

    private String convertStreamToString(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while (line != null) {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        return stringBuilder.toString();
    }
}