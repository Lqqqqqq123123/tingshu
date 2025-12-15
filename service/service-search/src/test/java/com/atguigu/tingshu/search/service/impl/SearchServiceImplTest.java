package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchServiceImplTest {

    @Autowired
    private SearchService searchService;
    @Test
    void upperAlbum() {
        searchService.batchUpperAlbum();    
    }

}