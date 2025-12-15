package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
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
    @Autowired
    private ElasticsearchClient  elasticsearchClient;

    private static final Map<String, String> ORDER_MAP = Map.of(
            "1", "hotScore",
            "2", "playStatNum",
            "3", "createTime"
    );

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


    /**
     * 批量上架专辑(不严谨版本，也就是直接遍历  1-maxid)
     */

    @Override
    public void batchUpperAlbum() {
        // 1. 获取最大专辑ID
        Long maxId = 1605L;

        for (long i = 1; i <= maxId; i++) {
            try {
                this.upperAlbum(i);
            } catch (Exception e) {
                log.info("专辑" + i + " 不存在");
            }
        }

    }






    /**
     * 站内搜索
     * @param albumIndexQuery
     * @return
     */
    @Override
    @SneakyThrows
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        // 1. 解析查询参数，通过原生 es 提供的 api 去构建本次查询的 DSL
        /**
         * # 查询条件：查询关键字全文匹配，三级分类id，标签
         * # 支持分页，关键字高亮
         * # 支持根据 播放量、综合得分、发布时间排序
         * post /albuminfo/_search
         * {
         *   "query": {
         *
         *   },
         *
         *   "from": "",
         *   "size": "",
         *   "highlight":{},
         *   "sort":{},
         *   "_source":[]
         * }
         */
        SearchRequest searchRequest = this.buildDSL(albumIndexQuery);
        System.err.println("本次检索DSL：");
        System.out.println(searchRequest);


        // 2. 执行查询
        SearchResponse<AlbumInfoIndex> resp = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

        // 3. 解析响应结果
        return this.parseResponse(resp, albumIndexQuery);

    }

    private final static String INDEX_NAME = "albuminfo";
    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        // 1. 创建 dsl 的构建者以及 bool 查询的构造者
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
        // 创建bool查询条件：用于封装所有查询条件
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 1.1 先去设置分页参数
        int curPage = albumIndexQuery.getPageNo();
        int pageSize = albumIndexQuery.getPageSize();
        if(curPage <= 0) curPage = 1;
        builder.from((curPage - 1) * pageSize).size(pageSize);

        // 1.2 设置排序规则
        String order = albumIndexQuery.getOrder();
        if(StringUtils.hasText(order)){
            // 排序字段规则 排序字段对应序号：排序类型 1：综合排序（hot score) 2：播放量 3：最近更新
            String[] split = order.split(":");
            if(split != null && split.length == 2){
                String field_num = split[0];
                String field = ORDER_MAP.get(field_num);
                String type = split[1];
//                builder.sort(s -> {
//                    return s.field(t -> t.field(field).order(SortOrder.valueOf(type)));
//                });

                SortOrder sortOrder = type.equals("desc")? SortOrder.Desc : SortOrder.Asc;
                builder.sort(s -> s.field(
                        f->f.field(field).order(sortOrder)
                ));
            }
        }


        // 1.3 设置查询关键字，如果有查询关键字，记得进行高亮（标题和简介同时全文匹配）
        String keyWord = albumIndexQuery.getKeyword();
        if(StringUtils.hasText(keyWord)){


            // todo : 可以改为只匹配标题 | 简介
            // 1.4 全文匹配 标题加简介同时匹配
            boolQueryBuilder.must(
                    q -> q.multiMatch(m -> m.query(keyWord)
                            .fields("albumTitle", "albumIntro")
                    )
            );

            // 1.5 高亮
            builder.highlight(h -> h.fields
                            ("albumTitle", h1 -> h1.preTags("<font color='red'><strong>").postTags("</strong></font>"))
            );

        }

        // 1.5 分类id

        Long category3Id = albumIndexQuery.getCategory3Id();
        Long category2Id = albumIndexQuery.getCategory2Id();
        Long category1Id = albumIndexQuery.getCategory1Id();
        if(category3Id != null){
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }

        if(category2Id != null){
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }
        if(category1Id != null){
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }

        // 1.6 标签
        //多组标签List集合 每组标签形式=标签ID：标签值ID
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if(!CollectionUtils.isEmpty(attributeList))
        {
            for (String attribute : attributeList) {
                // 1.6.1 获取标签ID和标签值ID
                String [] split = attribute.split(":");
                if(split != null && split.length == 2)
                {
                    Long attributeId = Long.parseLong(split[0]);
                    Long attributeValueId = Long.parseLong(split[1]);
                    boolQueryBuilder.filter(
                            f -> f.nested(n -> n.path("attributeValueIndexList").query(
                                    q -> q.bool(b -> b
                                            .filter(
                                                    f1 -> f1.term(t -> t.field("attributeValueIndexList.attributeId").value(attributeId)))
                                            .filter(
                                                    f1 -> f1.term(t -> t.field("attributeValueIndexList.valueId").value(attributeValueId)))))
                            ));
                }
            }
        }

        builder.query(boolQueryBuilder.build()._toQuery());

        // 1.7 _source
        builder.source(s -> s.filter(
                f -> f.excludes("attributeValueIndexList",
                        "hotScore",
                        "commentStatNum",
                        "buyStatNum",
                        "subscribeStatNum",
                        "announcerName")
        ));

        return builder.build();

    }

    @Override
    public AlbumSearchResponseVo parseResponse(SearchResponse<AlbumInfoIndex> resp, AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo newResp = new AlbumSearchResponseVo();
        // 1. 分页参数的设置
        newResp.setTotal(resp.hits().total().value());
        newResp.setPageSize(albumIndexQuery.getPageSize());
        newResp.setPageNo(albumIndexQuery.getPageNo());
        newResp.setTotalPages((long) Math.ceil(newResp.getTotal() * 1.0 / newResp.getPageSize()));


        // 2. 返回结果的设置
        /**
                 hits 数组下的每一个元素都是这样子的，
                 {
                 "_index": "albuminfo",
                 "_id": "1416",
                 "_score": null,
                 "_source": {
                 "includeTrackCount": 25,
                 "category2Id": 120,
                 "category1Id": 4,
                 "albumIntro": "同名漫画由iCiyuan动漫原著，正在火热更新中！绝情总裁×温柔钢琴家！温念南三年的付出，换来的却是冷眼...",
                 "isFinished": "0",
                 "coverUrl": "https://imagev2.xmcdn.com/storages/2c9e-audiofreehighqps/42/9F/CMCoOR4ElQKiAAHbegC20LFX.jpg",
                 "payType": "0102",
                 "playStatNum": 92192,
                 "createTime": "2023-04-04T16:56:34.000Z",
                 "category3Id": 1096,
                 "albumTitle": "他说我是黑莲花|道斯基&蝎子莱莱播讲|同名漫画原作",
                 "_class": "com.atguigu.tingshu.model.search.AlbumInfoIndex",
                 "id": 1416
                 },
                 "highlight": {
                 "albumTitle": [
                 "他说我是黑莲花|<font color='red'><strong>道</strong></font>斯基&蝎子莱莱播讲|同名漫画原作"
                 ]
                 },
                 "sort": [
                 92192
                 ]
                 },
         */
        List<AlbumInfoIndexVo> list = resp.hits().hits()
                .stream()
                .map(t -> {
                    // 1. 获取数据
                    AlbumInfoIndexVo vo = BeanUtil.copyProperties(t.source(), AlbumInfoIndexVo.class);

                    // 2. 如果存在高亮数据，则设置高亮数据
                    if(!CollectionUtils.isEmpty(t.highlight())){
                        if(t.highlight().containsKey("albumTitle"))
                        {
                            vo.setAlbumTitle(t.highlight().get("albumTitle").get(0));
                        }
                    }
                    return vo;
                })
                .toList();
        newResp.setList(list);
        return newResp;
    }
}
