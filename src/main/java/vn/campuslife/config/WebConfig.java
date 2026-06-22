package vn.campuslife.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
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
