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
import vn.campuslife.util.UrlUtils;

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

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(uploadProperties.getR2().getBucket())
                .key(relativePath)
                .build();

        s3Client.deleteObject(request);
    }

    @Override
    public String toPublicUrl(String relativePath) {
        String cdnDomain = uploadProperties.getR2().getCdnDomain();
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            String base = cdnDomain.replaceAll("/+$", "");
            return base + "/" + (relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
        }
        return UrlUtils.toFullUrl(relativePath, uploadProperties.getPublicUrl());
    }

    @Override
    public String extractRelativePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }

        String cdnDomain = uploadProperties.getR2().getCdnDomain();
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            String base = cdnDomain.replaceAll("/+$", "");
            if (fileUrl.startsWith(base + "/")) {
                return fileUrl.substring((base + "/").length());
            }
        }

        return fileUrl;
    }

    @Override
    public Path resolveFilePath(String relativePath) {
        return Paths.get(relativePath);
    }

    private String buildObjectKey(String relativeDirectory, String originalFilename) {
        String dir = relativeDirectory != null ? relativeDirectory.replace('\\', '/').replaceAll("^/+|/+$", "") : "";
        String fileName = UUID.randomUUID() + extractExtension(originalFilename);

        if (dir.isEmpty()) {
            return fileName;
        }
        return dir + "/" + fileName;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
