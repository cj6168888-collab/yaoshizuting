package com.yaoshizuting.service.impl;

import com.yaoshizuting.config.UploadProperties;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UploadProperties uploadProperties;

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > uploadProperties.getMaxImageSize()) {
            throw new BusinessException(400, "图片不能超过 " + uploadProperties.getMaxImageSize() / 1024 / 1024 + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(400, "仅支持 JPG、PNG、WEBP 图片");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + extension;
        Path targetDir = Path.of(uploadProperties.getRootPath(), "products", datePath).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException("图片保存失败", e);
        }

        String publicPath = uploadProperties.getPublicPath().replaceAll("/+$", "");
        return publicPath + "/products/" + datePath + "/" + fileName;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int index = filename.lastIndexOf('.');
        if (index >= 0 && index < filename.length() - 1) {
            String extension = filename.substring(index).toLowerCase(Locale.ROOT);
            if (Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
                return extension;
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
