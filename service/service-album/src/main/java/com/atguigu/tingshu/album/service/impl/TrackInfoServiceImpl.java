package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;
    @Autowired
    private TrackStatMapper trackStatMapper;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private VodUploadClient client;
    @Autowired
    private VodService vodService;

    @Autowired
    private AuditService auditService;



    @Override
    @Transactional
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        // 1. 先查询专辑信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());

        // 2. 构建TrackInfo对象
        TrackInfo po = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);

        // 2.1 赋值 user_id, album_id, order_num, status, source
        po.setUserId(userId);
        //  po.setAlbumId(trackInfoVo.getAlbumId());
        po.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
        po.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
        po.setSource(SystemConstant.TRACK_SOURCE_USER);

        // 2.2 如果音频的封面没有上传，则使用专辑的封面
        String url = trackInfoVo.getCoverUrl();
        if(!StringUtils.hasText(url)){
            po.setCoverUrl(albumInfo.getCoverUrl());
        }

        // 2.3 赋值当前音频的 media_size, media_type, media_duration
        TrackMediaInfoVo mediaInfoVo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());

        if(mediaInfoVo != null){
            po.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
            po.setMediaSize(mediaInfoVo.getSize());
            po.setMediaType(mediaInfoVo.getType());
        }

        // 3. 保存音频信息
        trackInfoMapper.insert(po);


        // 4. 保存音频统计信息
        this.saveTrackStat(po.getId(), "0701",  0);
        this.saveTrackStat(po.getId(), "0702" , 0);
        this.saveTrackStat(po.getId(), "0703" , 0);
        this.saveTrackStat(po.getId(), "0704" , 0);

        // 5. 更新专辑信息
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        albumInfoMapper.updateById(albumInfo);

        // todo：审核完毕后去保存信息（等音频的异步审核结束后再去调用）
        // 6.1 去审核当前文字内容
        String text = po.getTrackTitle() + po.getTrackIntro();
        String suggestion = auditService.auditText(text);

        if ("pass".equals(suggestion)) {
            // todo文本审核没问题，还得去审核音频信息：
            String taskId = auditService.startReviewTask(po.getMediaFileId());
            po.setReviewTaskId(taskId);
            po.setStatus(SystemConstant.TRACK_STATUS_REVIEWING);

        } else if ("review".equals(suggestion)) {
            po.setStatus(SystemConstant.TRACK_STATUS_REVIEW);
        } else if ("block".equals(suggestion)) {
            po.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
        }

        // 6.2 更新审核信息
        trackInfoMapper.updateById(po);

    }


    @Override
    public void saveTrackStat(Long trackId, String statType, int statNum) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(statNum);
        trackStatMapper.insert(trackStat);
    }

    @Override
    public Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.getUserTrackPage(pageInfo, trackInfoQuery);
    }

    // 表设计缺陷，就是审核状态是由文本和音频一起决定的，但是在修改时不知道之前的审核结果到底是谁有问题，所以在修改时就面临：
    // 上次的审核结果是失败，然后我们这次文本审核通过了，然后音频没改变，那我显然无法判断上次的音频合不合法。
    // 所以修改的审核就只做文本的审核，并且只在失败时赋值为未通过或者复审，成功时不管
    // 上面说的也不对，我在新增的时候如果文本审核没通过，我都没检测音频是否合法啊，所以在修改时如果文本审核通过了，我去标记一下，然后无论音频变没变，我都要去审核最终的音频，最后
    // 如果文本和音频都审核通过了，我再设置为通过即可！！！！
    @Override
    public void updateTrackInfo(TrackInfo trackInfo) {
        //1.判断音频文件是否变更
        //1.1 根据声音ID查询声音记录得到“旧”的音频文件标识
        TrackInfo oldTrackInfo = trackInfoMapper.selectById(trackInfo.getId());

        // 获取之前的音频文件的文本内容
        String oldText = oldTrackInfo.getTrackTitle() + oldTrackInfo.getTrackIntro();
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        // 标记文本审核是否成功，默认值和之前的审核结果相关，如果之前失败了，那就是false,否则就是true;
        Boolean flag1;
        if(trackInfo.getStatus().equals(SystemConstant.TRACK_STATUS_NO_PASS) || trackInfo.getStatus().equals(SystemConstant.TRACK_STATUS_REVIEW)){
            flag1 = false;
        }else{
            flag1 = true;
        }

        Boolean flag2 = false; // 音频审核是否成功，默认为成功
        Boolean is_change1 = !oldText.equals(text); // 标记文本有无修改
        Boolean is_change2 = false; // 标记音频有无修改：逻辑：如果文本校验成功，无论修改没修改，都要对音频进行审核，但如果文本已经校验失败，就不进行音频的审核

        // 文本内容发生了改变，需要去检测
        // 注意，只有审核失败才去修改，不然我们不动原来的状态，防止把之前音频审核结果给覆盖掉
        if(is_change1){
            String suggestion = auditService.auditText(text);

            if("pass".equals(suggestion)){
                flag1 = true;
            }else if("review".equals(suggestion) || "block".equals(suggestion)){
                flag1 = false;
            }
//            if ("pass".equals(suggestion)) {
//                // trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
//            } else if ("review".equals(suggestion) || ) {
//                flag = false;
//            } else if ("block".equals(suggestion)) {
//                trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
//            }
        }

        //1.2 判断文件是否被更新
        if (!trackInfo.getMediaFileId().equals(oldTrackInfo.getMediaFileId())) {
            //1.3 如果文件被更新，再次获取新音频文件信息更新：时长，大小，类型
            TrackMediaInfoVo mediaInfoVo = vodService.getTrackMediaInfo(trackInfo.getMediaFileId());
            if (mediaInfoVo != null) {
                trackInfo.setMediaType(mediaInfoVo.getType());
                trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
                trackInfo.setMediaSize(mediaInfoVo.getSize());
                trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

                // 音频文件发生更新后，必须再次进行审核
                //4. 开启音视频任务审核；更新声音表：审核任务ID-后续采用定时任务检查审核结果
                //4.1 启动审核任务得到任务ID
                //String reviewTaskId = vodService.reviewMediaTask(trackInfo.getMediaFileId());
                //4.2 更新声音表：审核任务ID，状态（审核中）

                //4.2 更新声音表：审核任务ID，状态（审核中）
                //trackInfo.setReviewTaskId(reviewTaskId);
                //trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEW_ING);
                //trackInfoMapper.updateById(trackInfo);
            }


            //1.4 从点播平台删除旧的音频文件
            vodService.deleteMedia(oldTrackInfo.getMediaFileId());
        }

        // 无论音频文件是否被更新，如果当前文本审核成功，则要对音频进行审核
        // 只有当文本审核成功时，才去审核音频文件
        if(flag1 == true){
            // todo 审核音频文件（异步审核），返回结果后再去更新声音信息
            String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
            trackInfo.setReviewTaskId(taskId);
            trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEWING);

        }
        else if(flag1 == false){
            trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

        }

        //2.更新声音信息
        trackInfoMapper.updateById(trackInfo);

    }

    @Override
    @Transactional
    public void removeTrackInfo(Long id) {
        // 1. 现根据 id 获取当前声音信息
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();

        // 2. 更新序号：删除的序号之后的声音序号减1
        LambdaUpdateWrapper<TrackInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, albumId)
                .gt(TrackInfo::getOrderNum, orderNum);

        wrapper.setSql("order_num = order_num - 1");

        trackInfoMapper.update(null,  wrapper);

        // 3. 删除当前记录
        trackInfoMapper.deleteById(id);


        // 4. 删除声音统计信息

        trackStatMapper.delete(
                new LambdaQueryWrapper<TrackStat>()
                        .eq(TrackStat::getTrackId, id)
        );

        // 5. 更新专辑信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);

        //6.删除点播平台音频文件
        vodService.deleteMedia(trackInfo.getMediaFileId());

    }
}
