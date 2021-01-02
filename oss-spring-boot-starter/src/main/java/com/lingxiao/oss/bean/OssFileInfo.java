package com.lingxiao.oss.bean;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Admin
 */
@Data
@ToString
public class OssFileInfo implements Serializable {
    private String name;
    private String path;
    private String size;
    private String time;
    private String mimeType;
    private String endUser;
    private String bucket;
}
