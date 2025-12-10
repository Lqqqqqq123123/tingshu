package com.atguigu.tingshu.vo.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liutianba7
 * @create 2025/12/10 11:46
 */
@Data
@Schema(description = "专辑信息：仅返回专辑id以及标题")
public class AlbumVoWithIdAndTitle {
    @Schema(description = "专辑id")
    private Long id;
    @Schema(description = "专辑标题")
    private String albumTitle;
}
