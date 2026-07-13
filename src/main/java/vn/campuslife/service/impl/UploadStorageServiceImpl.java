package vn.campuslife.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.service.UploadStorageService;
import vn.campuslife.util.UrlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "local", matchIfMissing = true)
public class UploadStorageServiceImpl implements UploadStorageService {

    private final UploadProperties uploadProperties;

    public UploadStorageServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
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

        Path targetDirectory = resolveTargetDirectory(relativeDirectory);
        Files.createDirectories(targetDirectory);

        String fileName = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        Path filePath = targetDirectory.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return buildRelativeUrl(relativeDirectory, fileName);
    }

    @Override
    public void delete(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path filePath = resolveFilePath(relativePath);
        Files.deleteIfExists(filePath);
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

        // Legacy bare filenames (e.g. uuid.jpg) must resolve under /uploads/
        String normalized = UrlUtils.normalizeUploadRelativePath(trimmed);
        if (normalized != null) {
            return UrlUtils.toFullUrl(normalized, uploadProperties.getPublicUrl());
        }

        return UrlUtils.toFullUrl(trimmed, uploadProperties.getPublicUrl());
    }

    @Override
    public String extractRelativePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }

        String trimmed = fileUrl.trim().replace('\\', '/');
        String publicPrefix = normalizedPublicPrefix();
        String publicUrl = uploadProperties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            if (trimmed.startsWith(base)) {
                String suffix = trimmed.substring(base.length());
                if (suffix.startsWith(publicPrefix + "/") || suffix.equals(publicPrefix)) {
                    return suffix;
                }
            }
        }

        int prefixIndex = trimmed.indexOf(publicPrefix + "/");
        if (prefixIndex >= 0) {
            return trimmed.substring(prefixIndex);
        }

        String normalized = UrlUtils.normalizeUploadRelativePath(trimmed);
        if (normalized != null) {
            return normalized;
        }

        return trimmed;
    }

    @Override
    public Path resolveFilePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path is required");
        }

        String publicPrefix = normalizedPublicPrefix();
        String normalizedPath = relativePath.replace('\\', '/');
        if (normalizedPath.startsWith(publicPrefix)) {
            normalizedPath = normalizedPath.substring(publicPrefix.length());
        }
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return Paths.get(uploadProperties.getDir()).resolve(normalizedPath).normalize();
    }

    private Path resolveTargetDirectory(String relativeDirectory) {
        Path rootPath = Paths.get(uploadProperties.getDir());
        String sanitizedDirectory = sanitizeDirectory(relativeDirectory);
        if (sanitizedDirectory.isBlank()) {
            return rootPath;
        }
        return rootPath.resolve(sanitizedDirectory).normalize();
    }

    private String buildRelativeUrl(String relativeDirectory, String fileName) {
        String publicPrefix = normalizedPublicPrefix();
        String sanitizedDirectory = sanitizeDirectory(relativeDirectory);
        if (sanitizedDirectory.isBlank()) {
            return publicPrefix + "/" + fileName;
        }
        return publicPrefix + "/" + sanitizedDirectory + "/" + fileName;
    }

    private String sanitizeDirectory(String relativeDirectory) {
        if (relativeDirectory == null || relativeDirectory.isBlank()) {
            return "";
        }
        String normalized = relativeDirectory.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizedPublicPrefix() {
        String publicPrefix = uploadProperties.getPaths().getPublicPrefix();
        if (publicPrefix == null || publicPrefix.isBlank()) {
            return "/uploads";
        }
        return publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        try {
            String safeName = Paths.get(originalFilename).getFileName().toString();
            int dotIndex = safeName.lastIndexOf('.');
            return dotIndex >= 0 ? safeName.substring(dotIndex) : "";
        } catch (InvalidPathException ex) {
            return "";
        }
    }
}
