package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodUploadClient client;
    @Autowired
    private Credential cred;

    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        // 1. 先把文件存储到临时路径 TODO:后续采用定时任务清理临时目录下使用完毕文件
        String filepath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);

        // 2. 构造上传请求对象
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(filepath);


        // 3. 调用上传方法
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);

            return Map.of("mediaFileId",response.getFileId(), "mediaUrl", response.getMediaUrl());
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("Upload Err", e);
            throw new RuntimeException("上传失败");
        }

    }

    @Override
    public TrackMediaInfoVo getTrackMediaInfo(String mediaFileId) {

        try{

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);

            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            // 解析响应结果
            if (resp != null) {
                //4.1 获取音频详情结果集
                MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
                //4.2 获取音频详情对象
                MediaInfo mediaInfo = mediaInfoSet[0];
                //4.2.1 从详情基础信息获取类型
                TrackMediaInfoVo vo = new TrackMediaInfoVo();
                if (mediaInfo != null) {
                    String type = mediaInfo.getBasicInfo().getType();
                    vo.setType(type);
                    //4.2.1 从详情元信息获取时长、大小
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    vo.setDuration(metaData.getDuration());
                    vo.setSize(metaData.getSize());
                    return vo;
                }
            }

        } catch (TencentCloudSDKException e) {
            log.error("获取音视频文件:{}信息失败", mediaFileId, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * 删除音视频文件
     *
     * @param mediaFileId
     */
    @Override
    public void deleteMedia(String mediaFileId) {
        try {
            //1.实例化一个认证对象
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            //4.返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            client.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
            log.error("[专辑服务]删除点播平台文件异常：{}", e);
        }
    }

}
