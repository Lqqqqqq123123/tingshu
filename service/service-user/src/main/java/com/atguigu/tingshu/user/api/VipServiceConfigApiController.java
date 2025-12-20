package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;

    /**
     * 获取所有VIP服务配置
     * @return VIP服务配置列表
     */
    @Operation(summary = "获取所有VIP服务配置")
    @RedisCache(prefix = "user:vipconfig:list:", timeout = 3600)
    @GetMapping("/vipServiceConfig/findAll")
    public Result<List<VipServiceConfig>> findAll() {
        List<VipServiceConfig> list = vipServiceConfigService.list();
        return Result.ok(list);
    }


    /**
     * 根据ID查询VIP服务配置，该接口被订单微服务访问，需提供远程接口
     * @param id VIP服务配置ID
     * @return VIP服务配置
     */
    @Operation(summary = "根据ID查询VIP服务配置")
    @RedisCache(prefix = "user:vipconfig:", timeout = 3600)
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")

    public Result<VipServiceConfig> getVipServiceConfigById(@PathVariable("id") Long id){
        VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(id);
        return Result.ok(vipServiceConfig);
    }

}

