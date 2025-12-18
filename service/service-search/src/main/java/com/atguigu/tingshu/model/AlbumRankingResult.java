package com.atguigu.tingshu.model;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import lombok.Data;

import java.util.List;

/**
 * @author liutianba7
 * @create 2025/12/18 12:40
 */
@Data
public class AlbumRankingResult {
    private List<AlbumInfoIndex> list;
}
