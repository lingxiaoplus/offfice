package com.lingxiao.oss.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private int totalPage;
    private List<T> data;
    public PageResult(Long total, List<T> data) {
        this.total = total;
        this.data = data;
    }
}