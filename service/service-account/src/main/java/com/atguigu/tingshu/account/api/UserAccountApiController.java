package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;


    /**
     * 获取当前用户账户余额：必须登录才能访问
     * @return
     */
    @Login
    @Operation(summary = "获取当前用户账户余额")
    @GetMapping("/userAccount/getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        // 获取当前用户ID
        Long userId = AuthContextHolder.getUserId();

        // 获取当前用户账户余额
        BigDecimal availableAmount = userAccountService.getAvailableAmount(userId);

        return Result.ok(availableAmount);
    }

    /**
     * 根据传入的参数 AccountDeductVo 扣减用户余额 + 记录操作日志 内部接口：订单服务调用
     * @param vo 扣减参数
     * @return 扣减结果
     */
    @Operation(summary = "根据传入的参数 AccountDeductVo 扣减用户余额 + 记录操作日志")
    @PostMapping("/userAccount/checkAndDeduct")
    public Result CheckAndDeduct (@RequestBody AccountDeductVo vo)
    {
        userAccountService.checkAndDeduct(vo);
        return Result.ok();
    }
}

