package com.lingxiao.core.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingxiao.core.DocumentSaveStatus;
import com.lingxiao.core.bean.CallbackResult;
import com.lingxiao.core.exception.OfficeException;
import com.lingxiao.core.service.FileService;
import com.lingxiao.core.utils.DocumentManager;
import lombok.extern.slf4j.Slf4j;
import org.primeframework.jwt.domain.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Scanner;

/**
 * @author Admin
 */
@RestController
@RequestMapping("/callback")
@Slf4j
public class CallbackController {
    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentManager documentManager;

    @PostMapping("office")
    public ResponseEntity<JSONObject> officeCallback(HttpServletRequest request, @RequestParam("fileName") String fileName){
        JSONObject result = new JSONObject();
        log.debug("执行回调，开始保存文件");
        try {
            Scanner scanner = new Scanner(request.getInputStream()).useDelimiter("\\A");
            String body = scanner.hasNext() ? scanner.next() : "";

            CallbackResult callbackResult = JSON.parseObject(body, CallbackResult.class);
            int status = callbackResult.getStatus();

            log.debug("saveeditedfile, status: {}", status);

            if (documentManager.tokenEnabled()) {
                String token = callbackResult.getToken();
                JWT jwt = documentManager.ReadToken(token);
                if (jwt == null) {
                    log.error("JWT.parse error");
                    result.put("error",1);
                    return ResponseEntity.ok(result);
                }
                status = jwt.getInteger("status");
                callbackResult.setUrl(jwt.getString("url"));
            }
            if (status == DocumentSaveStatus.DOCUMENT_IS_SAVING.getStatus() || status == DocumentSaveStatus.DOCUMENT_IS_SAVED.getStatus()) {
                /*
                 * 当我们关闭编辑窗口后，十秒钟左右onlyoffice会将它存储的我们的编辑后的文件，，此时status = 2，通过request发给我们，我们需要做的就是接收到文件然后回写该文件。
                 * */
                /*
                 * 定义要与文档存储服务保存的编辑文档的链接。当状态值仅等于2或3时，存在链路。
                 * */
                String downloadUri = callbackResult.getUrl();
                log.debug("文档编辑完成，现在开始保存编辑后的文档，其下载地址为: {}", downloadUri);
                fileService.saveFileToLocal(downloadUri,fileName);
            }
            /*
             * status = 1，我们给onlyoffice的服务返回{"error":"0"}的信息，这样onlyoffice会认为回调接口是没问题的，这样就可以在线编辑文档了，否则的话会弹出窗口说明
             * */
            //writer = response.getWriter();
            if(status == DocumentSaveStatus.NOT_FOUNT_DOCUMENT.getStatus() || status == DocumentSaveStatus.DOCUMENT_SAVING_FAILED.getStatus()) {
                log.error("保存失败: {}",callbackResult);
                result.put("error",1);
            }else {
                result.put("error",0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
}
