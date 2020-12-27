package com.lingxiao.office.utils;

import com.alibaba.fastjson.JSON;
import com.lingxiao.office.bean.OfficeConfigure;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
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

    private ResponseCallback callback;
    public interface ResponseCallback {
        void onSuccess(String reponse);
        void onFailure(String message);
    }

    public void setCallback(ResponseCallback callback) {
        this.callback = callback;
    }
}
