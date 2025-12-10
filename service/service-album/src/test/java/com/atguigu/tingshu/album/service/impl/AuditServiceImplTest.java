package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditServiceImplTest {

    @Autowired
    private AuditService auditService;


    @Test
    public void test01() {
        String text = "傻逼";
        String result = auditService.auditText(text);
        System.out.println(result);
    }


    @Test
    public void test02() {

        String taskId = auditService.startReviewTask("5145403708750830917");
        System.out.println(taskId);
    }

    @Test
    public void test03() {
        String taskId = "1385266319-ReviewAudioVideo-d1d4694c19c74ddf781a3da9d7da631dtt0";
        String result = auditService.getReviewTaskResult(taskId);
        System.out.println(result);
    }

}