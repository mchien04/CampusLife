package vn.campuslife.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String provider = "local";
    private String dir = "uploads";
    private String publicUrl = "http://localhost:8080";
    private Paths paths = new Paths();
    private R2 r2 = new R2();

    @Getter
    @Setter
    public static class Paths {
        private String publicPrefix = "/uploads";
        private String general = "";
        private String activityPhotos = "activities";
        private String submissions = "submissions";
        private String scoreAppeals = "score-appeals";
        private String avatars = "avatars";
    }

    @Getter
    @Setter
    public static class R2 {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String cdnDomain;
    }
}
