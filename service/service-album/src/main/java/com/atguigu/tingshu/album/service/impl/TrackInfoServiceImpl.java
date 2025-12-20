package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumStatMapper albumStatMapper;



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
            // todo文本审核没问题，还得去审核音频信息
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



    /**
     * 需求：用户未登录，可以给用户展示声音列表；用户已登录，可以给用户展示声音列表，并动态渲染付费标识
     * 分页查询专辑下声音列表（动态渲染付费标识）
     * @param pageInfo 分页参数
     * @param albumId 专辑id
     * @param userId 用户id
     * @return 分页数据
     */
    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {

        // 1. 分页查询专辑下声音列表
        pageInfo = trackInfoMapper.findAlbumTrackPage(pageInfo, albumId);

        // 2. todo:根据用户是否登录、用户是否是vip，用户是否付费动态渲染付费标识
        // 2.1 先去拿到专辑的付费类型以及试听集数
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        // 专辑付费类型： 0101-免费 0102-vip付费 0103-付费
        String payType = albumInfo.getPayType();
        Integer tracksForFree = albumInfo.getTracksForFree();

        // 如果专辑是免费的，就不用走后面的逻辑了，直接返回
        if(payType.equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) return pageInfo;

        // 2.2 未登录情况
        if(userId == null){
            if(payType.equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE) || payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)) {
                // 2.2.1 遍历当前页声音，然后过滤掉免费试听的声音，然后将其他的声音的付费标识设置为 true
                pageInfo.getRecords()
                        .stream()
                        .filter(t -> t.getOrderNum() > tracksForFree)
                        .forEach(t -> t.setIsShowPaidMark(true));
            }
        }
        // 2.3 登录情况
        else{
            // 2.3.1 获取当前用户信息
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            if(userInfoVo ==  null){
                log.info("远程调用获取用户信息失败");
                throw new RuntimeException("远程调用获取用户信息失败");
            }

            // 用户是否为VIP会员 0:普通用户  1:VIP会员
            Integer isVip = userInfoVo.getIsVip();
            Date expireTime = userInfoVo.getVipExpireTime();

            // todo:这里的代码重复，可以优化，但我懒得写了，饿了。
            // 2.3.2 如果当前用户不是 vip 或者 vip 会员已过期并且当前专辑不是免费的（免费的情况已经在上面处理过了，所以能到这里，肯定不是免费的）
            if(isVip == 0 || (isVip == 1 && expireTime.before(new Date()))){
                // todo：先去写验证当前页声音是否购买的接口
                // 1. 调用 user 服务接口 userIsPaidTrack 去检查当前声音列表哪些购买了，哪些没购买
                Map<Long, Integer> checkResult = userFeignClient.userIsPaidTrack(
                        userId,
                        albumId,
                        pageInfo.getRecords()
                                .stream()
                                .filter(t -> t.getOrderNum() > tracksForFree) // 过滤掉免费试听声音
                                .map(t -> t.getTrackId())
                                .toList()).getData();

                //2.  根据检查的结果，设置付费标识
                pageInfo.getRecords().forEach(
                        t ->{
                            // 3. 如果当前声音没有购买，则设置付费标识为 true
                            Integer tt = checkResult.get(t.getTrackId());
                            if(tt != null && tt == 0){
                                t.setIsShowPaidMark(true);
                            }
                        }
                );

                return pageInfo;
            }


            // 2.3.3 当前用户是 vip
            else{
                // 1. 如果当前作品付费类型为免费或者vip免费，则不需要处理，也就是只处理 vip 需要付费的专辑
                if(payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)){
                    // 2. 调用 user 服务接口 userIsPaidTrack 去检查当前声音列表哪些购买了，哪些没购买
                    Map<Long, Integer> checkResult = userFeignClient.userIsPaidTrack(
                            userId,
                            albumId,
                            pageInfo.getRecords()
                                    .stream()
                                    .filter(t -> t.getOrderNum() > tracksForFree) // 过滤掉免费试听声音
                                    .map(t -> t.getTrackId())
                                    .toList()).getData();

                    pageInfo.getRecords().forEach(
                            t ->{
                                // 3. 如果当前声音没有购买，则设置付费标识为 true
                                Integer tt = checkResult.get(t.getTrackId());
                                if(tt != null && tt == 0){
                                    t.setIsShowPaidMark(true);
                                }
                            }
                    );

                    return pageInfo;
                }
            }

        }

        return pageInfo;

    }

    /**
     * 查询声音统计信息
     * @param trackId
     * @return TrackStatVo
     */
    @Override
    @RedisCache(prefix = "album:trackStatVo:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    public TrackStatVo getTrackStatVo(Long trackId) {
        return trackInfoMapper.getTrackStatVo(trackId);
    }

    /**
     * 更新声音 + 专辑的统计信息
     * @param vo
     */
    @Override
    public void updateStat(TrackStatMqVo vo) {

        // 1. 更新声音统计信息
        // 1.1 先拿到本次更新的数据
        String type = vo.getStatType();
        Long trackId = vo.getTrackId();
        Long albumId = vo.getAlbumId();
        Integer count = vo.getCount();


        // 1.2 更新声音统计信息
//        LambdaQueryWrapper<TrackStat> wr1 = new LambdaQueryWrapper<>();
//        wr1.eq(TrackStat::getTrackId, trackId);
//        wr1.eq(TrackStat::getStatType, type);
//        TrackStat trackStat = trackStatMapper.selectOne(wr1);
//
//        if(trackStat != null){
//            trackStat.setStatNum(trackStat.getStatNum() + count);
//            trackStatMapper.updateById(trackStat);
//        }
        trackStatMapper.update(
                null,
                new LambdaUpdateWrapper<TrackStat>()
                        .eq(TrackStat::getTrackId, trackId)
                        .eq(TrackStat::getStatType, type)
                        .setSql("stat_num = stat_num + " +  count)
        );


        // 2. 如果是声音的播放 | 评论,那么还得更新专辑的统计信息
        if(type.equals(SystemConstant.TRACK_STAT_PLAY) || type.equals(SystemConstant.TRACK_STAT_COMMENT)){
            // 声音的统计信息:'统计类型：0701-播放量 0702-收藏量 0703-点赞量 0704-评论数',
            // 专辑的统计信息:统计类型：0401-播放量 0402-订阅量 0403-购买量 0404-评论数',
            LambdaUpdateWrapper<AlbumStat> wr2 = new LambdaUpdateWrapper<>();
            // 2.1 如果是声音的播放,就对应专辑的播放量
            wr2.eq(AlbumStat::getAlbumId, albumId);
            if(type.equals(SystemConstant.TRACK_STAT_PLAY))
                wr2.eq(AlbumStat::getStatType, SystemConstant.ALBUM_STAT_PLAY);

            // 2.2 如果是声音的评论,就对应专辑的评论数
            if(type.equals(SystemConstant.TRACK_STAT_COMMENT))
                wr2.eq(AlbumStat::getStatType, SystemConstant.ALBUM_STAT_COMMENT);

            // 2.3 更新专辑的统计信息
            albumStatMapper.update(
                    null,
                    wr2.setSql("stat_num = stat_num + " +  count)
            );
        }

    }


    /**
     * 基于用户选择的声音，动态的获取为购买的声音数量，得到声音购买列表
     * @param trackId 当前选择的声音ID
     * @param userId 当前用户ID
     * @return List<Map<String, Object>> [{name:"本集", price:"0.1", trackcount:1}, {name:"后10集", price:"1", trackcount:10}，{name:"全集", price:"4.3", trackcount:43}]
     */
    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId) {
        // 1. 先根据声音ID查询当前声音信息，拿到序号以及专辑ID
        TrackInfo curTrackInfo = trackInfoMapper.selectById(trackId);
        Assert.notNull(curTrackInfo, "当前声音不存在");
        Long albumId = curTrackInfo.getAlbumId();
        Integer orderNum = curTrackInfo.getOrderNum();

        // 1.1 获取专辑信息，从而拿到声音的单价信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        Assert.notNull(albumInfo, "专辑信息不存在");
        BigDecimal price = albumInfo.getPrice(); // 专辑单价

        // 2. 根据专辑id + 用户id，查询序号大于当前序号的声音
        LambdaQueryWrapper<TrackInfo> wr = new LambdaQueryWrapper<>();
        wr.eq(TrackInfo::getAlbumId, albumId).ge(TrackInfo::getOrderNum, orderNum);
        List<TrackInfo> list = trackInfoMapper.selectList(wr);

        // 3. 远程调用user微服务，从而获得当前用户已经购买的该专辑下的声音理解
        List<Long> paidTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
        // 4. 再从步骤2中查询结果中，过滤掉已经购买的声音
        if(paidTrackIdList != null){
            list = list.stream()
                    .filter(t -> !paidTrackIdList.contains(t.getId()))
                    .toList();
        }

        // 5. 得到所选的声音后面所有每购买的声音信息
        int size = list.size();
        List<Map<String, Object>> result = new ArrayList<>();

        // 5.1 10集10集来
        // 策略一，无论size为多少，最起码有本集
        result.add(createPlanMap("本集", 1, price));
        // 策略二，10，20，30，..，直到超过size
        for(int i = 10; i < size; i += 10){
            result.add(createPlanMap("后" + i + "集", i, new BigDecimal(i).multiply(price)));
        }

        // 策略三，只要未购买总数 > 1，就显示“全集” ---
        if(size > 1){
            // 检查最后一个加入的选项是否和全集集数一样，不一样才添加，避免重复
            Map<String, Object> lastMap = result.get(result.size() - 1);
            if ((int)lastMap.get("trackCount") != size) {
                result.add(createPlanMap("全集（后" + size + "集）", size, price.multiply(new BigDecimal(size))));
            } else {
                // 如果最后一个刚好是全集，把它的名字改成“全集”更友好
                lastMap.put("name", "全集（后" + size + "集）");
            }
        }
        return result;
    }

    /**
     * 查询用户未购买声音列表：内部接口，供订单服务调用
     * @param trackId 声音ID
     * @param userId 用户ID
     * @param trackCount 当前用户要购买的声音数量
     * @return List<TrackInfo> 从trackid的序号开始，获取trackCount个用户未购买的声音
     */
    @Override
    public List<TrackInfo> findWaitBuyTrackInfoList(Long userId, Long trackId, Integer trackCount) {
        // 1. 先拿到当前声音信息
        TrackInfo curTrackInfo = trackInfoMapper.selectById(trackId);
        Assert.notNull(curTrackInfo, "当前声音不存在");

        // 2. 拿到专辑ID，当前声音的序号
        Long albumId = curTrackInfo.getAlbumId();
        Integer orderNum = curTrackInfo.getOrderNum();


        // 3. 根据专辑ID，查询序号大于等于当前序号的声音信息
        LambdaUpdateWrapper<TrackInfo> wr = new LambdaUpdateWrapper<>();
        wr.eq(TrackInfo::getAlbumId, albumId).ge(TrackInfo::getOrderNum, orderNum);
        List<TrackInfo> list = trackInfoMapper.selectList(wr);

        // 4. 拿到当前用户已经购买的声音ID列表
        List<Long> paidTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();

        // 5. 过滤掉已经购买的声音
        if(paidTrackIdList != null){
            list = list.stream()
                    .filter(t -> !paidTrackIdList.contains(t.getId()))
                    .toList();
        }
        // 6. 返回 trackCount 个声音信息
        return list.stream()
                .limit(trackCount)
                .toList();
    }

    /**
     * 提取一个辅助方法，减少重复代码
     */
    private Map<String, Object> createPlanMap(String name, Integer count, BigDecimal price) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("trackCount", count);
        map.put("price", price);
        return map;
    }
}
