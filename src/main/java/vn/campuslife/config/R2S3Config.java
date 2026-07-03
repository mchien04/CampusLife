package vn.campuslife.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2S3Config {

    @Bean
    @ConditionalOnProperty(name = "app.upload.provider", havingValue = "r2")
    public S3Client s3Client(UploadProperties uploadProperties) {
        UploadProperties.R2 r2 = uploadProperties.getR2();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2.getAccessKey(),
                r2.getSecretKey());

        return S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(r2.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }
}
