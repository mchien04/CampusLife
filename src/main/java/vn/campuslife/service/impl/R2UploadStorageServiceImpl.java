package vn.campuslife.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "r2")
public class R2UploadStorageServiceImpl implements UploadStorageService {

    private final UploadProperties uploadProperties;
    private final S3Client s3Client;

    public R2UploadStorageServiceImpl(UploadProperties uploadProperties, S3Client s3Client) {
        this.uploadProperties = uploadProperties;
        this.s3Client = s3Client;
    }

    @Override
    public String store(MultipartFile file, String relativeDirectory, boolean imageOnly) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (imageOnly) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }
        }

        String key = buildObjectKey(relativeDirectory, file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(uploadProperties.getR2().getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return key;
    }

    @Override
    public void delete(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        String key = toObjectKey(relativePath);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(uploadProperties.getR2().getBucket())
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    @Override
    public String toPublicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return relativePath;
        }

        String trimmed = relativePath.trim().replace('\\', '/');
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String key = toObjectKey(trimmed);
        String cdnDomain = uploadProperties.getR2().getCdnDomain();
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            String base = cdnDomain.replaceAll("/+$", "");
            return base + "/" + key;
        }

        String publicUrl = uploadProperties.getPublicUrl();
        String base = publicUrl != null ? publicUrl.trim().replaceAll("/+$", "") : "";
        if (base.isEmpty()) {
            return key;
        }
        return base + "/" + key;
    }

    @Override
    public String extractRelativePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }

        String trimmed = fileUrl.trim().replace('\\', '/');

        String cdnDomain = uploadProperties.getR2().getCdnDomain();
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            String base = cdnDomain.replaceAll("/+$", "");
            if (trimmed.startsWith(base + "/")) {
                return trimmed.substring((base + "/").length());
            }
        }

        String publicUrl = uploadProperties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            String base = publicUrl.trim().replaceAll("/+$", "");
            if (trimmed.startsWith(base + "/")) {
                return toObjectKey(trimmed.substring((base + "/").length()));
            }
        }

        int uploadsIndex = trimmed.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            return toObjectKey(trimmed.substring(uploadsIndex + "/uploads/".length()));
        }

        return toObjectKey(trimmed);
    }

    @Override
    public Path resolveFilePath(String relativePath) {
        return Paths.get(toObjectKey(relativePath));
    }

    private String buildObjectKey(String relativeDirectory, String originalFilename) {
        String dir = relativeDirectory != null ? relativeDirectory.replace('\\', '/').replaceAll("^/+|/+$", "") : "";
        String fileName = UUID.randomUUID() + extractExtension(originalFilename);

        if (dir.isEmpty()) {
            return fileName;
        }
        return dir + "/" + fileName;
    }

    private String toObjectKey(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String key = path.trim().replace('\\', '/');
        if (key.startsWith("/uploads/")) {
            key = key.substring("/uploads/".length());
        } else if (key.startsWith("uploads/")) {
            key = key.substring("uploads/".length());
        }
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
