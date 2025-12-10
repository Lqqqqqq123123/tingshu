package com.atguigu.tingshu.album.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewTaskTest {

    @Autowired
    private ReviewTask reviewTask;

    @Test
    public void test01() {
        reviewTask.checkReviewTask();
    }
}