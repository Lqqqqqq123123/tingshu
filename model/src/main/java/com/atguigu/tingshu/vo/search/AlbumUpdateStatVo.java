package com.atguigu.tingshu.vo.search;

import lombok.Data;

/**
 * @author liutianba7
 * @create 2025/12/17 17:17
 */
@Data
public class AlbumUpdateStatVo {
    private Long albumId;
    private String statType;
    private Integer count;
}
