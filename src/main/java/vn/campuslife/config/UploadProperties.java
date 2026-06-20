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

    private String dir = "uploads";
    private String publicUrl = "http://localhost:8080";
    private Paths paths = new Paths();

    @Getter
    @Setter
    public static class Paths {
        private String publicPrefix = "/uploads";
        private String general = "";
        private String activityPhotos = "activities";
        private String submissions = "submissions";
    }
}
