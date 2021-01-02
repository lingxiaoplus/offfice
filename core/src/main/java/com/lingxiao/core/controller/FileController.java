package com.lingxiao.core.controller;

import com.alibaba.fastjson.JSONObject;
import com.lingxiao.core.bean.*;
import com.lingxiao.core.service.FileService;
import com.lingxiao.oss.bean.OssFileInfo;
import com.lingxiao.oss.bean.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

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
        PageResult<OssFileInfo> files = fileService.getUploadFiles();
        result.put("code",0);
        result.put("count",files.getTotal());
        result.put("data",files.getData());
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("file/config")
    public String getConfig(){
        return officeConfigure.toString();
    }

    @PostMapping("file/upload")
    public ResponseEntity<ResponseResult<JSONObject>> uploadFile(@RequestParam(value = "file") MultipartFile file){
        return ResponseEntity.ok(fileService.upload(file));
    }

    @GetMapping("file/convert")
    public ResponseEntity<ResponseResult<ConvertResult>> convertFile(@RequestParam(value = "fileName") String fileName, @RequestParam(value = "fileUrl")String fileUrl){
        return ResponseEntity.ok(fileService.convert(fileName,fileUrl));
    }

    @RequestMapping("file/edit")
    public ModelAndView editPage(@RequestParam(value = "fileName") String fileName,
                                 @RequestParam(value = "fileUrl") String fileUrl,
                                 @RequestParam(value = "editable",defaultValue = "true") boolean editable){
        ModelAndView modelAndView = new ModelAndView("editor");
        FileModel fileModel = fileService.editFile(fileName,fileUrl);
        fileModel.getEditorConfig().editable = editable;
        modelAndView.addObject("file",fileModel);
        modelAndView.addObject("docserviceApiUrl",officeConfigure.getDocService().getUrl().getApi());
        return modelAndView;
    }

    @RequestMapping("file/createSimple")
    public ModelAndView createSimple(@RequestParam(value = "fileType") String fileType){
        ModelAndView modelAndView = new ModelAndView("editor");
        FileModel fileModel = fileService.createEmptyFile(fileType);
        modelAndView.addObject("file",fileModel);
        modelAndView.addObject("docserviceApiUrl",officeConfigure.getDocService().getUrl().getApi());
        return modelAndView;
    }
}
