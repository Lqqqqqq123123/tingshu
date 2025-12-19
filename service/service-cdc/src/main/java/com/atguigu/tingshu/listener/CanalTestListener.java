package com.atguigu.tingshu.listener;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import org.springframework.boot.CommandLineRunner;

import java.net.InetSocketAddress;

public class CanalTestListener implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // 1. 创建连接
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("127.0.0.1", 11111), "tingshuTopic", "", "");

        System.out.println(">>>> Java 客户端尝试连接本地 Canal...");

        try {
            connector.connect();
            connector.subscribe(".*\\..*"); // 订阅所有
            connector.rollback();

            System.out.println(">>>> 连接成功！开始死循环监听...");

            while (true) {
                Message message = connector.getWithoutAck(100); // 获取指定数量的数据
                long batchId = message.getId();
                if (batchId != -1 && message.getEntries().size() > 0) {
                    System.out.println(">>>> 抓到数据了！当前批次大小: " + message.getEntries().size());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}