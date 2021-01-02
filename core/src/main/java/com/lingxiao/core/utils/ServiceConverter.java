package com.lingxiao.core.utils;

import com.alibaba.fastjson.JSON;
import com.lingxiao.core.bean.ConvertResult;
import com.lingxiao.core.bean.OfficeConfigure;
import com.lingxiao.core.exception.OfficeException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private String DocumentConverterUrl;
    @Autowired
    private HttpUtil httpUtil;

    @PostConstruct
    public void init(){
        DocumentConverterUrl = officeConfigure.getDocService().getUrl().getConverter();
    }

    @Data
    public static class ConvertBody {
        private String url;
        private String outputtype;
        private String filetype;
        private String title;
        private String key;
        private Boolean async;
        private String token;
    }

    public ConvertResult getConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync)  {
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
        body.async = isAsync;

        log.info("convert参数: {}",JSON.toJSONString(body));
        Map<String,String> headerMap = new HashMap<>();
        if (documentManager.tokenEnabled()) {
            Map<String, Object> map = new HashMap<>();
            map.put("payload", body);
            String token = documentManager.CreateToken(map);
            headerMap.put("Authorization","Bearer " + token);
        }
        headerMap.put("Accept", "application/json");
        try {
            Response response = httpUtil.addHeader(headerMap).doPost(DocumentConverterUrl, body);
            if (response.isSuccessful()){
                return getResponseUri(title,response.body().string());
            }else {
                log.info("请求office转换服务失败, {}",response.code());
                throw new OfficeException("请求office转换服务失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new OfficeException("请求office服务器失败");
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
        String errorMessageTemplate = "文档转换异常: ";
        switch (errorCode) {
            case -8:
                errorMessage = errorMessageTemplate + "Invalid token";
                break;
            case -7:
                errorMessage = errorMessageTemplate + "Error document request";
                break;
            case -6:
                errorMessage = errorMessageTemplate + "Error while accessing the conversion result database";
                break;
            case -5:
                errorMessage = errorMessageTemplate + "Incorrect password";
                break;
            case -4:
                errorMessage = errorMessageTemplate + "Error while downloading the document file to be converted";
                break;
            case -3:
                errorMessage = errorMessageTemplate + "Conversion error";
                break;
            case -2:
                errorMessage = errorMessageTemplate + "Conversion timeout error";
                break;
            case -1:
                errorMessage = errorMessageTemplate + "Unknown error";
                break;
            case 0:
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;
                break;
        }
        throw new OfficeException(errorMessage);
    }

    private ConvertResult getResponseUri(String title, String jsonString) {
        log.debug("获取到转换结果的response: {}",jsonString);
        ConvertResult convertResponse = JSON.parseObject(jsonString, ConvertResult.class);
        convertResponse.setFileName(title);
        if (convertResponse.getError() != null){
            processConvertServiceResponseError(convertResponse.getError());
        }
        if (convertResponse.getEndConvert()) {
            convertResponse.setProgress(100);
        }
        return convertResponse;
    }
}