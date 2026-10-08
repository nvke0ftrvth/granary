package com.example.granary.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3 client for Neon Object Storage. Neon only supports path-style addressing and does not need the SDK's
 * default request checksums, so both are configured here.
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.region}")
    private String region;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        List<String> missing = new ArrayList<>();
        if (endpoint.isBlank()) missing.add("AWS_ENDPOINT_URL_S3");
        if (region.isBlank()) missing.add("AWS_REGION");
        if (bucket.isBlank()) missing.add("APP_STORAGE_BUCKET");
        if (accessKey.isBlank()) missing.add("AWS_ACCESS_KEY_ID");
        if (secretKey.isBlank()) missing.add("AWS_SECRET_ACCESS_KEY");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Object storage is not configured. Set " + String.join(", ", missing)
                    + " from the Storage tab of the Neon branch's Connect dialog.");
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
