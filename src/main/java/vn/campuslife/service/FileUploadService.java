package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadFile(MultipartFile file);

    String uploadImage(MultipartFile file);

    /**
     * Upload an image into a specific subdirectory (e.g. avatars) and return the public URL.
     */
    String uploadImage(MultipartFile file, String relativeDirectory);

    void deleteFile(String fileName);
}
