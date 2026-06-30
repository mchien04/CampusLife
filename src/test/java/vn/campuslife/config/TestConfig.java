package vn.campuslife.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public UploadProperties uploadProperties() {
        UploadProperties props = new UploadProperties();
        return props;
    }
}
