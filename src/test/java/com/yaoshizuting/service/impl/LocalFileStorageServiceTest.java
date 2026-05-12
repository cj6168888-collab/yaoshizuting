package com.yaoshizuting.service.impl;

import com.yaoshizuting.config.UploadProperties;
import com.yaoshizuting.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceTest {

    @TempDir
    private Path tempDir;

    private UploadProperties uploadProperties;
    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        uploadProperties = new UploadProperties();
        uploadProperties.setRootPath(tempDir.toString());
        uploadProperties.setPublicPath("/uploads/");
        uploadProperties.setMaxImageSize(1024);
        fileStorageService = new LocalFileStorageService(uploadProperties);
    }

    @Test
    void storeProductImageRejectsNullFile() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.storeProductImage(null));

        assertEquals(400, exception.getCode());
        assertEquals("上传文件不能为空", exception.getMessage());
    }

    @Test
    void storeProductImageRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.storeProductImage(file));

        assertEquals(400, exception.getCode());
        assertEquals("上传文件不能为空", exception.getMessage());
    }

    @Test
    void storeProductImageRejectsOversizedFile() {
        uploadProperties.setMaxImageSize(1);
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", new byte[] {1, 2});

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.storeProductImage(file));

        assertEquals(400, exception.getCode());
        assertEquals("图片不能超过 0MB", exception.getMessage());
    }

    @Test
    void storeProductImageRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", new byte[] {1});

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.storeProductImage(file));

        assertEquals(400, exception.getCode());
        assertEquals("仅支持 JPG、PNG、WEBP 图片", exception.getMessage());
    }

    @Test
    void storeProductImageStoresFileWithOriginalExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Product.PNG", "image/png", new byte[] {1, 2, 3});

        String url = fileStorageService.storeProductImage(file);

        String datePath = LocalDate.now().toString().replace("-", "/");
        assertTrue(url.startsWith("/uploads/products/" + datePath + "/"));
        assertTrue(url.endsWith(".png"));
        Path savedFile = tempDir.resolve(url.replace("/uploads/", "").replace("/", java.io.File.separator));
        assertTrue(Files.exists(savedFile));
        assertEquals(3, Files.size(savedFile));
    }

    @Test
    void storeProductImageFallsBackToContentTypeExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "no-extension", "image/webp", new byte[] {1});

        String url = fileStorageService.storeProductImage(file);

        assertTrue(url.endsWith(".webp"));
    }

    @Test
    void storeProductImageWrapsIoFailure() throws Exception {
        Path rootFile = tempDir.resolve("not-a-directory");
        Files.write(rootFile, new byte[] {1});
        uploadProperties.setRootPath(rootFile.toString());
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {1});

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.storeProductImage(file));

        assertEquals(500, exception.getCode());
        assertEquals("图片保存失败", exception.getMessage());
    }
}
