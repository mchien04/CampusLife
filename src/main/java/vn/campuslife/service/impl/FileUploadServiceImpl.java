package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.service.FileUploadService;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;

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
        return upload(false, file, uploadProperties.getPaths().getGeneral());
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return upload(true, file, uploadProperties.getPaths().getGeneral());
    }

    @Override
    public String uploadImage(MultipartFile file, String relativeDirectory) {
        return upload(true, file, relativeDirectory);
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            String relativePath = uploadStorageService.extractRelativePath(fileName);
            uploadStorageService.delete(relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    private String upload(boolean imageOnly, MultipartFile file, String relativeDirectory) {
        try {
            String relativePath = uploadStorageService.store(file, relativeDirectory, imageOnly);
            return uploadStorageService.toPublicUrl(relativePath);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
}
