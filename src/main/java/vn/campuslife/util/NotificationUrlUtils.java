package vn.campuslife.util;

/**
 * Utility class for generating notification action URLs
 */
public class NotificationUrlUtils {

    /**
     * Generates a full URL for an activity detail page
     * 
     * @param activityId The activity ID
     * @param frontendUrl The frontend base URL (e.g., "https://campus-life-react.vercel.app")
     * @return Full URL to activity detail page
     */
    public static String generateActivityUrl(Long activityId, String frontendUrl) {
        if (activityId == null) {
            return null;
        }
        
        String baseUrl = frontendUrl != null ? frontendUrl.trim() : "";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return baseUrl + "/activities/" + activityId;
    }

    /**
     * Generates a full URL for a series detail page
     * 
     * @param seriesId The series ID
     * @param frontendUrl The frontend base URL (e.g., "https://campus-life-react.vercel.app")
     * @return Full URL to series detail page
     */
    public static String generateSeriesUrl(Long seriesId, String frontendUrl) {
        if (seriesId == null) {
            return null;
        }
        
        String baseUrl = frontendUrl != null ? frontendUrl.trim() : "";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return baseUrl + "/series/" + seriesId;
    }
}

