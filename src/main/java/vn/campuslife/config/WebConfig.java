package vn.campuslife.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "local", matchIfMissing = true)
public class WebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPrefix = uploadProperties.getPaths().getPublicPrefix();
        if (publicPrefix == null || publicPrefix.isBlank()) {
            publicPrefix = "/uploads";
        } else if (!publicPrefix.startsWith("/")) {
            publicPrefix = "/" + publicPrefix;
        }

        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations("file:" + uploadProperties.getDir() + "/");
    }
}
