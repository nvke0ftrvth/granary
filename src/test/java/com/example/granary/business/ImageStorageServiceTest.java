package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Exercises ImageStorageService against a mocked S3Client: the requests it sends to the bucket, the keys it
 * generates from uploaded filenames, and the public URLs it builds.
 */
@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    private static final String ENDPOINT = "https://br-test.storage.c-2.us-east-2.aws.neon.tech";

    @Mock
    private S3Client s3Client;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(s3Client, "granary-images", ENDPOINT);
    }

    private PutObjectRequest capturePut() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, atLeastOnce()).putObject(captor.capture(), any(RequestBody.class));
        return captor.getValue();
    }

    // store
    @Test
    void store_uploadsToBucketWithContentTypeAndCacheHeaders() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String filename = service.store(file);

        PutObjectRequest request = capturePut();
        assertThat(request.bucket()).isEqualTo("granary-images");
        assertThat(request.key()).isEqualTo(filename);
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.cacheControl()).contains("immutable");
    }

    @Test
    void store_prefixesFilenameWithUuidToAvoidCollisions() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});

        String first = service.store(file);
        String second = service.store(file);

        assertThat(first).endsWith("_photo.png").isNotEqualTo("photo.png");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void store_pathTraversalInOriginalFilename_isCleaned() {
        MultipartFile file = new MockMultipartFile("file", "../../etc/passwd", "image/png", new byte[]{1});

        String filename = service.store(file);

        assertThat(filename).doesNotContain("..").doesNotContain("/").endsWith("_passwd");
    }

    @Test
    void store_filenameWithSpacesAndUnicode_usesUrlSafeKey() {
        MultipartFile file = new MockMultipartFile("file", "café menu.png", "image/png", new byte[]{1});

        String filename = service.store(file);

        assertThat(filename).matches("[A-Za-z0-9._-]+").endsWith("_menu.png");
    }

    @Test
    void store_missingContentType_fallsBackToOctetStream() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", null, new byte[]{1});

        service.store(file);

        assertThat(capturePut().contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void store_storageFailure_isWrapped() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to store image file");
    }

    // delete
    @Test
    void delete_removesObjectFromBucket() {
        service.delete("a.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("granary-images");
        assertThat(captor.getValue().key()).isEqualTo("a.png");
    }

    @Test
    void delete_storageFailure_isWrapped() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThatThrownBy(() -> service.delete("a.png"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to delete image file");
    }

    // publicUrl
    @Test
    void publicUrl_isEndpointBucketAndKey() {
        assertThat(service.publicUrl("a.png")).isEqualTo(ENDPOINT + "/granary-images/a.png");
    }

    @Test
    void publicUrl_endpointWithTrailingSlash_doesNotDoubleSlash() {
        ImageStorageService slashed = new ImageStorageService(s3Client, "granary-images", ENDPOINT + "/");

        assertThat(slashed.publicUrl("a.png")).isEqualTo(ENDPOINT + "/granary-images/a.png");
    }
}
