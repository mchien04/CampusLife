package vn.campuslife.util;

/**
 * Utility class for URL conversion
 */
public class UrlUtils {

    /**
     * Converts a relative path to a full URL using the provided public URL base.
     * If the path is already a full URL (starts with http:// or https://), it is
     * returned as-is.
     * If the path is a relative path (starts with /uploads/), it is prepended with
     * the publicUrl.
     * Otherwise, the original path is returned unchanged.
     *
     * @param relativePath The path to convert (can be relative or absolute)
     * @param publicUrl    The base public URL (e.g.,
     *                     "https://campuslife-v2iu.onrender.com")
     * @return The full URL if conversion is needed, otherwise the original path
     */
    public static String toFullUrl(String relativePath, String publicUrl) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return relativePath;
        }

        String trimmedPath = relativePath.trim();

        // If already a full URL, return as-is
        if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) {
            return trimmedPath;
        }

        // If relative path starting with /uploads/, prepend publicUrl
        if (trimmedPath.startsWith("/uploads/")) {
            // Remove trailing slash from publicUrl if present
            String baseUrl = publicUrl != null ? publicUrl.trim() : "";
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + trimmedPath;
        }

        // Return original if neither condition matches
        return trimmedPath;
    }

    /**
     * Normalizes a URL to a relative path for storage in database.
     * If the URL is a full URL (starts with http:// or https://), extracts the
     * relative path.
     * If the URL is already a relative path, returns it as-is.
     * 
     * @param url       The URL to normalize (can be full URL or relative path)
     * @param publicUrl The base public URL to extract from (e.g.,
     *                  "https://campuslife-v2iu.onrender.com")
     * @return Relative path (e.g., "/uploads/abc123.jpg") or original if not a
     *         valid upload URL
     */
    public static String toRelativePath(String url, String publicUrl) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        String trimmedUrl = url.trim();

        // If already a relative path starting with /uploads/, return as-is
        if (trimmedUrl.startsWith("/uploads/")) {
            return trimmedUrl;
        }

        // If full URL, extract relative path
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            // Remove trailing slash from publicUrl if present
            String baseUrl = publicUrl != null ? publicUrl.trim() : "";
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // Check if URL starts with publicUrl
            if (trimmedUrl.startsWith(baseUrl)) {
                String relativePath = trimmedUrl.substring(baseUrl.length());
                // Only return if it's a valid upload path
                if (relativePath.startsWith("/uploads/")) {
                    return relativePath;
                }
            }
        }

        // Return original if cannot normalize
        return trimmedUrl;
    }
}
