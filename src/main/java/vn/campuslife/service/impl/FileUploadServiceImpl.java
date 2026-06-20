package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.service.FileUploadService;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final UploadProperties uploadProperties;
    private final UploadStorageService uploadStorageService;

    public FileUploadServiceImpl(UploadProperties uploadProperties, UploadStorageService uploadStorageService) {
        this.uploadProperties = uploadProperties;
        this.uploadStorageService = uploadStorageService;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return upload(false, file);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return upload(true, file);
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            String relativePath = uploadStorageService.extractRelativePath(fileName);
            if (relativePath == null || relativePath.isBlank()) {
                return;
            }

            Path filePath = uploadStorageService.resolveFilePath(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    private String upload(boolean imageOnly, MultipartFile file) {
        try {
            String relativePath = uploadStorageService.store(file, uploadProperties.getPaths().getGeneral(), imageOnly);
            return uploadStorageService.toPublicUrl(relativePath);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
}
