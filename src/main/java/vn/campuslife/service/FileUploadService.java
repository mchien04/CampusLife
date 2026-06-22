package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadFile(MultipartFile file);

    String uploadImage(MultipartFile file);

    void deleteFile(String fileName);
}
