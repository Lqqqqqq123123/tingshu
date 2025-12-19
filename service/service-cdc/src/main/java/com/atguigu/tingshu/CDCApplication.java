package com.atguigu.tingshu;

import io.xzxj.canal.spring.annotation.EnableCanalListener;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author liutianba7
 * @create 2025/12/19 18:01
 */
@EnableCanalListener
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CDCApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(CDCApplication.class, args);
    }
}
