package com.lingxiao.office.controller;

import com.alibaba.fastjson.JSONObject;
import com.lingxiao.office.bean.FileInfo;
import com.lingxiao.office.bean.FileModel;
import com.lingxiao.office.bean.OfficeConfigure;
import com.lingxiao.office.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping
@Slf4j
@EnableConfigurationProperties(value = OfficeConfigure.class)
public class FileController {
    @Autowired
    private OfficeConfigure officeConfigure;
    @Autowired
    private FileService fileService;

    /*@RequestMapping
    public ModelAndView index(){
        ModelAndView modelAndView = new ModelAndView("index");
        log.debug("配置信息： {}",officeConfigure.toString());
        List<FileInfo> uploadFiles = fileService.getUploadFiles();
        modelAndView.addObject("files",uploadFiles);
        return modelAndView;
    }*/

    @GetMapping("file/list")
    @ResponseBody
    public ResponseEntity<JSONObject> getFileList(){
        JSONObject result = new JSONObject();
        List<FileInfo> files = fileService.getUploadFiles();
        result.put("code",0);
        result.put("count",files.size());
        result.put("data",files);
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("file/config")
    public String getConfig(){
        return officeConfigure.toString();
    }

    @PostMapping("file/upload")
    public ResponseEntity<JSONObject> uploadFile(@RequestParam(value = "file") MultipartFile file){
        return ResponseEntity.ok(fileService.upload(file));
    }

    @RequestMapping("file/office")
    public String getOffice(HttpServletRequest request, HttpServletResponse response, @RequestParam("type") String editFileType){
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (editFileType.toLowerCase()) {
            case "track":
                fileService.track(request, response, writer);
                break;
            default:
                break;
        }
        return "office";
    }

    @RequestMapping("file/edit")
    public ModelAndView editPage(@RequestParam(value = "fileName",required = false) String fileName
            ,@RequestParam(value = "fileExt",required = false) String fileExt,@RequestParam(value = "mode",required = false) String mode){
        ModelAndView modelAndView = new ModelAndView("editor");
        FileModel fileModel = fileService.editFile(fileName,fileExt,mode);
        modelAndView.addObject("file",fileModel);
        modelAndView.addObject("docserviceApiUrl",officeConfigure.getDocService().getUrl().getApi());
        /*request.setAttribute("file", fileModel);
        request.setAttribute("docserviceApiUrl", ConfigManager.GetProperty("files.docservice.url.api"));
        request.getRequestDispatcher("editor.jsp").forward(request, response);*/
        return modelAndView;
    }
}
