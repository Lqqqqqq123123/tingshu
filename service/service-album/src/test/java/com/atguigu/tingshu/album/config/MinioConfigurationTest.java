package com.atguigu.tingshu.album.config;

import io.minio.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MinioConfigurationTest {

    @Autowired
    MinioClient minioClient;

    private static final String BUCKET_NAME = "tingshu";

    @Test
    void testMinioClient() throws Exception {
        // 检查并创建桶
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(BUCKET_NAME).build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(BUCKET_NAME).build()
            );

            String policy = """
                {
                  "Statement": [{
                    "Action": "s3:GetObject",
                    "Effect": "Allow",
                    "Principal": "*",
                    "Resource": "arn:aws:s3:::%s/*"
                  }],
                  "Version": "2012-10-17"
                }
                """.formatted(BUCKET_NAME);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(BUCKET_NAME)
                            .config(policy)
                            .build()
            );
        }

        // 上传文件（从测试资源目录）
        URL resource = getClass().getClassLoader().getResource("test-file.txt");
        if (resource == null) {
            throw new IllegalStateException("Test file 'test-file.txt' not found in resources!");
        }
        Path filePath = Paths.get(resource.toURI());
        long fileSize = Files.size(filePath);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object("remote-file.txt")
                            .stream(inputStream, fileSize, -1)
                            .contentType("text/plain")
                            .build()
            );
        }
    }
}