package com.atguigu.tingshu;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author liutianba7
 * @create 2025/12/16 11:44
 */
@SpringBootTest
@Slf4j
public class LogTest {
    @Test
    public void testLog() {
        log.debug("这是debug日志");
        log.info("这是info日志");
        log.warn("这是warn日志");
        log.error("这是error日志");
    }
}
