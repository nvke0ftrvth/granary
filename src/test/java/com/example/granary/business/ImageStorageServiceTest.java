package com.example.granary.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises ImageStorageService against a real (JUnit-managed, auto-deleted)
 * temp directory rather than mocking java.nio — the value of these tests is
 * catching real I/O behavior: directory creation, filename collisions, path
 * traversal, and delete-of-a-missing-file.
 *
 * uploadDir is a @Value-injected field with no setter, so ReflectionTestUtils
 * is used to set it directly. The alternative — adding a package-private
 * setter or constructor param just for tests — was avoided here to keep the
 * production class unchanged; worth reconsidering if this pattern spreads to
 * more classes (see write-up).
 */
class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    // store
    @Test
    void store_createsUploadDirectoryIfMissing() {
        Path nestedDir = tempDir.resolve("does/not/exist/yet");
        ReflectionTestUtils.setField(service, "uploadDir", nestedDir.toString());

        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String filename = service.store(file);

        assertThat(nestedDir).exists();
        assertThat(nestedDir.resolve(filename)).exists();
    }

    @Test
    void store_prefixesFilenameWithUuidToAvoidCollisions() {
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});

        String filename = service.store(file);

        assertThat(filename).endsWith("_photo.png").isNotEqualTo("photo.png");
    }

    @Test
    void store_twoUploadsOfSameOriginalName_doNotOverwriteEachOther() {
        MultipartFile file1 = new MockMultipartFile("file", "photo.png", "image/png", "first".getBytes());
        MultipartFile file2 = new MockMultipartFile("file", "photo.png", "image/png", "second".getBytes());

        String stored1 = service.store(file1);
        String stored2 = service.store(file2);

        assertThat(stored1).isNotEqualTo(stored2);
        assertThat(tempDir.resolve(stored1)).exists();
        assertThat(tempDir.resolve(stored2)).exists();
    }

    @Test
    void store_pathTraversalInOriginalFilename_isCleaned() {
        MultipartFile file = new MockMultipartFile("file", "../../etc/passwd", "image/png", new byte[]{1});

        String filename = service.store(file);

        assertThat(filename).doesNotContain("..");
        assertThat(tempDir.resolve(filename)).exists();
    }

    @Test
    void store_filenameWithSpacesAndUnicode_isStoredSuccessfully() {
        MultipartFile file = new MockMultipartFile("file", "café menu.png", "image/png", new byte[]{1});

        String filename = service.store(file);

        assertThat(tempDir.resolve(filename)).exists();
    }

    //  delete
    @Test
    void delete_existingFile_removesIt() throws IOException {
        Path file = tempDir.resolve("a.png");
        Files.writeString(file, "content");

        service.delete("a.png");

        assertThat(file).doesNotExist();
    }

    @Test
    void delete_missingFile_doesNotThrow() {
        // Files.deleteIfExists is intentionally used in production code for
        // this reason — pin down that behavior here so a future refactor to
        // Files.delete() (which throws NoSuchFileException) gets caught.
        assertThatCode(() -> service.delete("never-existed.png")).doesNotThrowAnyException();
    }

    @Test
    void delete_thenStoreAgain_reusesDirectoryWithoutError() {
        MultipartFile upload = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        String stored = service.store(upload);

        service.delete(stored);
        String storedAgain = service.store(upload);

        assertThat(tempDir.resolve(storedAgain)).exists();
    }
}
