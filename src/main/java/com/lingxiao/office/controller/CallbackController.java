package com.lingxiao.office.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingxiao.office.exception.OfficeException;
import com.lingxiao.office.service.FileService;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("office")
    public ResponseEntity<JSONObject> officeCallback(HttpServletRequest request, HttpServletResponse response, @RequestParam("fileName") String fileName){
        JSONObject result = new JSONObject();
        JSONObject jsonObj = null;
        log.debug("执行回调，开始保存文件");
        try {
            Scanner scanner = new Scanner(request.getInputStream()).useDelimiter("\\A");
            String body = scanner.hasNext() ? scanner.next() : "";
            jsonObj = JSON.parseObject(body);

            if (!jsonObj.containsKey("status")){
                log.error("返回结果中没有包含状态信息");
                throw new OfficeException("返回结果中没有包含状态信息");
            }
            int status = (int) jsonObj.get("status");

            log.debug("saveeditedfile, status: {}", status);
	            /*
	                0 - no document with the key identifier could be found,
	                1 - document is being edited,
	                2 - document is ready for saving,
	                3 - document saving error has occurred,
	                4 - document is closed with no changes,
	                6 - document is being edited, but the current document state is saved,
	                7 - error has occurred while force saving the document.
	             * */
            if (status == 2 || status == 6) {
                /*
                 * 当我们关闭编辑窗口后，十秒钟左右onlyoffice会将它存储的我们的编辑后的文件，，此时status = 2，通过request发给我们，我们需要做的就是接收到文件然后回写该文件。
                 * */
                /*
                 * 定义要与文档存储服务保存的编辑文档的链接。当状态值仅等于2或3时，存在链路。
                 * */
                String downloadUri = (String) jsonObj.get("url");
                log.debug("文档编辑完成，现在开始保存编辑后的文档，其下载地址为: {}", downloadUri);
                //解析得出文件名
                //String fileName = request.getParameter("fileName");
                log.debug("下载的文件名: {}", fileName);
                // TODO: 2020/12/20 保存文件，并且是url类型的 
                fileService.saveFileToLocal(downloadUri,fileName);
            }
            /*
             * status = 1，我们给onlyoffice的服务返回{"error":"0"}的信息，这样onlyoffice会认为回调接口是没问题的，这样就可以在线编辑文档了，否则的话会弹出窗口说明
             * */
            //writer = response.getWriter();
            if(status == 0 || status == 3) {
                log.error("保存失败: {}",jsonObj);
                result.put("error",1);
                //writer.write("{\"error\":1}");
            }else {
                result.put("error",0);
                //writer.write("{\"error\":0}");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
}
