package vn.campuslife.util;

/**
 * Utility class for URL conversion between stored relative paths and public URLs.
 */
public class UrlUtils {

    private UrlUtils() {
    }

    /**
     * Converts a relative path to a full URL using the provided public URL base.
     * If the path is already a full URL (starts with http:// or https://), it is
     * returned as-is.
     * Paths under /uploads/ (or legacy bare filenames) are prepended with publicUrl.
     */
    public static String toFullUrl(String relativePath, String publicUrl) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return relativePath;
        }

        String trimmedPath = relativePath.trim().replace('\\', '/');

        if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) {
            return trimmedPath;
        }

        String normalizedPath = normalizeUploadRelativePath(trimmedPath);
        if (normalizedPath == null) {
            return trimmedPath;
        }

        String baseUrl = publicUrl != null ? publicUrl.trim() : "";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.isEmpty()) {
            return normalizedPath;
        }
        return baseUrl + normalizedPath;
    }

    /**
     * Normalizes a URL to a relative path for storage in database.
     */
    public static String toRelativePath(String url, String publicUrl) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        String trimmedUrl = url.trim().replace('\\', '/');

        String normalizedDirect = normalizeUploadRelativePath(trimmedUrl);
        if (normalizedDirect != null && (trimmedUrl.startsWith("/uploads/")
                || trimmedUrl.startsWith("uploads/")
                || looksLikeBareUploadFilename(trimmedUrl))) {
            return normalizedDirect;
        }

        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            String baseUrl = publicUrl != null ? publicUrl.trim() : "";
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            if (!baseUrl.isEmpty() && trimmedUrl.startsWith(baseUrl)) {
                String relativePath = trimmedUrl.substring(baseUrl.length());
                String normalized = normalizeUploadRelativePath(relativePath);
                if (normalized != null) {
                    return normalized;
                }
            }

            int uploadsIndex = trimmedUrl.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                return trimmedUrl.substring(uploadsIndex);
            }
        }

        return trimmedUrl;
    }

    /**
     * Ensures paths are of the form /uploads/... when they refer to local upload files.
     * Returns null when the value should be left unchanged (e.g. external CDN object key
     * without uploads prefix that is not a bare filename).
     */
    public static String normalizeUploadRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }

        String trimmed = path.trim().replace('\\', '/');
        while (trimmed.startsWith("./")) {
            trimmed = trimmed.substring(2);
        }

        if (trimmed.startsWith("/uploads/")) {
            return trimmed;
        }
        if (trimmed.startsWith("uploads/")) {
            return "/" + trimmed;
        }
        if (looksLikeBareUploadFilename(trimmed)) {
            return "/uploads/" + trimmed;
        }
        // Known local subfolders without public prefix (legacy)
        if (trimmed.startsWith("avatars/")
                || trimmed.startsWith("activities/")
                || trimmed.startsWith("submissions/")
                || trimmed.startsWith("score-appeals/")
                || trimmed.startsWith("general/")) {
            return "/uploads/" + trimmed;
        }
        return null;
    }

    private static boolean looksLikeBareUploadFilename(String value) {
        if (value == null || value.isBlank() || value.contains("/")) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp");
    }
}
