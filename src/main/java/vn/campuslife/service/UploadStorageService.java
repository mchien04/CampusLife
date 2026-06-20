package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface UploadStorageService {

    String store(MultipartFile file, String relativeDirectory, boolean imageOnly) throws IOException;

    String toPublicUrl(String relativePath);

    String extractRelativePath(String fileUrl);

    Path resolveFilePath(String relativePath);
}
