package vn.campuslife.service.impl;

import vn.campuslife.service.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-url:http://localhost:8080}")
    private String publicUrl;

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            // Tạo thư mục uploads nếu chưa có
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Lưu file
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về relative path để frontend có thể truy cập
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            if (fileName != null && fileName.contains("/uploads/")) {
                String actualFileName = fileName.substring(fileName.lastIndexOf("/uploads/") + 9);
                Path filePath = Paths.get(uploadDir, actualFileName);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy public URL cho file (khi cần absolute URL)
     */
    public String getPublicUrl(String relativePath) {
        if (relativePath != null && relativePath.startsWith("/uploads/")) {
            return publicUrl + relativePath;
        }
        return relativePath;
    }
}
