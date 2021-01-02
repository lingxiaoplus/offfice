package com.lingxiao.core.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author lingxiao
 */
@Data
public class ConvertResult {
    private String fileName;
    private String fileUrl;
    @JSONField(name = "percent")
    private int progress;

    private Integer error;
    private Boolean endConvert;
}
