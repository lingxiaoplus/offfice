package com.lingxiao.core.utils;

import com.alibaba.fastjson.JSON;
import com.lingxiao.core.bean.OfficeConfigure;
import com.lingxiao.core.exception.OfficeException;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lingxiao
 */
@Component
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class HttpUtil {
    @Autowired
    private OfficeConfigure officeConfigure;
    private OkHttpClient client = null;
    private Headers headers;

    @PostConstruct
    private void init(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder.connectTimeout(officeConfigure.getDocService().getTimeout(), TimeUnit.MILLISECONDS).build();
    }

    public HttpUtil addHeader(Map<String,String> headerMap){
        Headers.Builder builder = new Headers.Builder();
        headerMap.forEach((key,value)-> builder.add(key,value));
        headers = builder.build();
        return this;
    }


    public <T> void doPost(String url, T data, ResponseCallback callback){
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), JSON.toJSONString(data));
        Request request = null;
        if (headers != null){
            request = new Request
                    .Builder()
                    .url(url)
                    .post(requestBody)
                    .headers(headers)
                    .build();
        }else {
            request = new Request
                    .Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
        }
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseString = response.body().string();
                callback.onSuccess(responseString);
            }
        });
    }

    public <T> Response doPost(String url, T data) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), JSON.toJSONString(data));
        Request request = null;
        if (headers != null){
            request = new Request
                    .Builder()
                    .url(url)
                    .post(requestBody)
                    .headers(headers)
                    .build();
        }else {
            request = new Request
                    .Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
        }
        return client.newCall(request).execute();
    }


    /**
     * 同步下载文件
     * @param url
     * @param downloadPath
     * @return
     * @throws IOException
     */
    public File downloadFile(String url, String downloadPath) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null){
            throw new IOException("没有response body");
        }
        long total = responseBody.contentLength();
        long sum = 0;
        InputStream inputStream = responseBody.byteStream();
        File downloadFile = new File(downloadPath);
        try (FileOutputStream out = new FileOutputStream(downloadFile)) {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
                sum += read;
                int progress = (int) (sum * 1.0f / total * 100);
            }
            out.flush();
        }
        return downloadFile;
    }

    private ResponseCallback callback;
    public interface ResponseCallback {
        void onSuccess(String reponse);
        void onFailure(String message);
    }

    public void setCallback(ResponseCallback callback) {
        this.callback = callback;
    }
}
