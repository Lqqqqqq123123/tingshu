package com.atguigu.tingshu;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author liutianba7
 * @create 2025/12/13 10:19
 */
@SpringBootTest
public class UserFeignClientTest {
    @Autowired
    private UserFeignClient userFeignClient;

    @Test
    public void testGetUserInfo() {
        Result<UserInfoVo> result = userFeignClient.getUserInfoVo(1L);
        System.out.println(result);
    }
}
