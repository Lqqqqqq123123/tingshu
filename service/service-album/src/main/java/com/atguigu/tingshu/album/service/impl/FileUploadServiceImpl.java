package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Autowired
    private AuditService auditService;

    private final String BUCKET_NAME = "tingshu";

    @Override
    public String upload(MultipartFile file) throws Exception{

        // 校验业务：
        /**
         * ImageIO.read(InputStream) 返回：
         * 成功解析 → 返回 BufferedImage 对象；
         * 无法识别格式 / 不是图片 / 流损坏 / 不支持的图片类型 → 返回 null；
         * 不是因为“流为空”，而是因为“无法解析为有效图像”。
         */
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new BusinessException(500, "图片文件格式不合法");
            }
            // ...
            int width = image.getWidth();
            int height = image.getHeight();

            if(width > 900 || height > 900){
                throw new BusinessException(500, "图片文件大小不能超过900*900");
            }
        }

        // todo:对图片内容进行审核
        String suggestion = auditService.auditImage(file);

        if("block".equals(suggestion)){
            log.info("图片内容审核结果为fail");
            throw new BusinessException(ResultCodeEnum.IMAGE_AUDIT_FAIL);
        }else if("review".equals(suggestion)){
            log.info("图片内容审核结果为review，需要人工复审");
            throw new BusinessException(ResultCodeEnum.IMAGE_AUDIT_REVIEW);
        }else if("pass".equals(suggestion)){
            log.info("图片内容审核结果为pass");
        }

        // 1. 判断桶是否存在
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());

        // 2. 不存在桶、就创建桶
        if(!exists){
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            String policy = """
                {
                  "Statement": [
                    {
                      "Action": "s3:GetObject",
                      "Effect": "Allow",
                      "Principal": "*",
                      "Resource": "arn:aws:s3:::%s/*"
                    }
                  ],
                  "Version": "2012-10-17"
                }
                """.formatted(BUCKET_NAME);

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(BUCKET_NAME).config(policy).build());
        }


        // 3. 上传文件
        InputStream inputStream = file.getInputStream();
        long fileSize = file.getSize();
        // uuid + "-" + 文件名
        String objectName = UUID.randomUUID().toString().replaceAll("-", "") +"-"+ file.getOriginalFilename();

        // 拼接文件名(年/月/日是目录，然后文件名不变）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String date = sdf.format(new Date()); // 2025/12/09
        String name = date + "/" + objectName; // name:2025/12/09/5f8d6463d1034d1d9d997d0f39a4b484-pVVUFz5T6Wtn1c84855c7058c98a1180c329340767d6.png
        String url = minioConstantProperties.getEndpointUrl() + "/" + BUCKET_NAME + "/" + name; // http://115.190.231.171:9000/tingshu/2025/12/09/333.webp
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(name)
                        .stream(inputStream, fileSize, -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("上传成功,url地址为：{}", url);
        return url;
    }
}
