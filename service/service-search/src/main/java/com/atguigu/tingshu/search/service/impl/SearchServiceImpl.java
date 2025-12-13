package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.Executor;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoRepository albumInfoRepository;

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private Executor executor;


    /**
     * 将指定专辑上架到索引库
     *
     * @param albumId 专辑ID
     * @return
     */
    @Override
    public void upperAlbum(Long albumId) {
        // 1. 封装 album 索引存储的类型：AlbumInfoIndex
        // 1.1 根据专辑ID查询专辑信息
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();

//        if(albumInfo == null || albumInfo.getCode() != 200){
//            throw new RuntimeException("upperAlbum : 查询专辑信息失败");
//        }
        Assert.notNull(albumInfo, "upperAlbum : 专辑信息不存在");

        AlbumInfoIndex po = BeanUtil.copyProperties(albumInfo, AlbumInfoIndex.class);

        // 手动处理专辑属性值集合
        List<AlbumAttributeValue> list = albumInfo.getAlbumAttributeValueVoList();

        if(!CollectionUtils.isEmpty(list)){
            List<AttributeValueIndex> list1 = list.stream()
                    .map(v -> {
                        AttributeValueIndex t = new AttributeValueIndex();
                        t.setAttributeId(v.getAttributeId());
                        t.setValueId(v.getValueId());
                        return t;
                    })
                    .toList();

           po.setAttributeValueIndexList(list1);
        }

        // 1.2 获取三级分类信息
        BaseCategoryView view = albumFeignClient.getCategoryView(po.getCategory3Id()).getData();
        Assert.notNull(view, "upperAlbum : 获取三级分类信息失败");

        po.setCategory1Id(view.getCategory1Id());
        po.setCategory2Id(view.getCategory2Id());


        // 1.3 获取主播信息
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
        Assert.notNull(userInfoVo, "upperAlbum : 获取主播信息失败");
        po.setAnnouncerName(userInfoVo.getNickname());


        // 1.4 获取专辑统计信息
        // todo： 现在先随机封装，因为数据库里面的统计值都是0，没有意义，之后再去动态获取 + 封装

        po.setPlayStatNum(RandomUtil.randomInt(200, 100000));
        po.setSubscribeStatNum(RandomUtil.randomInt(100, 1000));
        po.setBuyStatNum(RandomUtil.randomInt(50, 500));
        po.setCommentStatNum(RandomUtil.randomInt(100, 1000));

        // 1.5 封装热度值
        // 计算规则：播放数 * 0.1 + 订阅数 * 0.2 + 购买数 * 0.3 + 评论数 * 0.4
        Double hotScore = po.getPlayStatNum() * 0.1 + po.getSubscribeStatNum() * 0.2 + po.getBuyStatNum() * 0.3 + po.getCommentStatNum() * 0.4;
        po.setHotScore(hotScore);
        // 2. 保存索引
        albumInfoRepository.save(po);

    }


    /**
     * 将指定专辑下架，从索引库删除文档
     *
     * @param albumId
     * @return
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoRepository.deleteById(albumId);
    }
}
