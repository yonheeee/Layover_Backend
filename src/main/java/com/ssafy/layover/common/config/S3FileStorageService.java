package com.ssafy.layover.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Override
    public String upload(MultipartFile file, String directory) {
        if (bucket == null || bucket.isBlank()) {
            throw new FileStorageException("AWS_S3_BUCKET 설정이 필요합니다.", null);
        }

        String key = directory + "/" + StorageFileNames.randomName(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return objectUrl(key);
        } catch (IOException | RuntimeException e) {
            throw new FileStorageException("S3 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String objectUrl(String key) {
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (baseUrl.isBlank()) {
            baseUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com";
        }
        return baseUrl.replaceAll("/+$", "") + "/" + key;
    }
}
