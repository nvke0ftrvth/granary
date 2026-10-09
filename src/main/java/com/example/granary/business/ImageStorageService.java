package com.example.granary.business;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores uploaded images in the object storage bucket. The bucket is public-read, so images are served to
 * browsers straight from the bucket's public URL.
 */
@Service
public class ImageStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public ImageStorageService(
            S3Client s3Client,
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.storage.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = endpoint.replaceAll("/+$", "") + "/" + bucket + "/";
    }

    public String store(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + safeBaseName(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType() : DEFAULT_CONTENT_TYPE;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build();

        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
            return filename;
        } catch (IOException | SdkException e) {
            throw new RuntimeException("Failed to store image file", e);
        }
    }

    public void delete(String filename) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(filename).build());
        } catch (SdkException e) {
            throw new RuntimeException("Failed to delete image file", e);
        }
    }

    public String publicUrl(String filename) {
        return publicBaseUrl + filename;
    }

    /** Drops any directory parts and replaces characters that would need escaping in a URL. */
    private static String safeBaseName(String originalFilename) {
        String cleanedName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        String baseName = Paths.get(cleanedName).getFileName().toString();
        return baseName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
