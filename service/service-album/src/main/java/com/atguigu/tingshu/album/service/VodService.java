package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    /**
     * 上传音频到vod，然后返回音频在voc平台的唯一标识以及在线访问地址
     * @param file 音频文件
     * @return
     */
    Map<String, String> uploadTrack(MultipartFile file);

    /**
     * 从Vod获取音频信息
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getTrackMediaInfo(@NotEmpty(message = "媒体文件Id不能为空") String mediaFileId);

    /**
     * 删除音视频文件
     * @param mediaFileId
     */
    void deleteMedia(String mediaFileId);
}
