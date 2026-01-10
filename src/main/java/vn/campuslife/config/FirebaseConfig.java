package vn.campuslife.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            String base64 = System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64");

            if (base64 == null || base64.isBlank()) {
                throw new RuntimeException("FIREBASE_SERVICE_ACCOUNT_BASE64 is not set");
            }

            byte[] decoded = Base64.getDecoder().decode(base64);

            try (InputStream is = new ByteArrayInputStream(decoded)) {

                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(is)
                        .createScoped(List.of(
                                "https://www.googleapis.com/auth/firebase.messaging",
                                "https://www.googleapis.com/auth/cloud-platform"
                        ));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
            }

            System.out.println("🔥 Firebase initialized successfully");

        } catch (Exception e) {
            throw new RuntimeException("❌ Firebase init failed", e);
        }
    }

//@PostConstruct
//public void init() {
//    try {
//        ClassPathResource resource = new ClassPathResource("firebase-admin.json");
//        System.out.println("🔥 exists = " + resource.exists());
//
//        GoogleCredentials credentials =
//                GoogleCredentials.fromStream(resource.getInputStream())
//                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
//
//        credentials.refreshAccessToken(); // 🔥 DÒNG QUYẾT ĐỊNH
//
//        System.out.println("🔥 OAuth token OK");
//
//        FirebaseOptions options = FirebaseOptions.builder()
//                .setCredentials(credentials)
//                .build();
//
//        FirebaseApp.initializeApp(options);
//        System.out.println("🔥 Firebase initialized");
//
//    } catch (Exception e) {
//        e.printStackTrace();
//        throw new RuntimeException("❌ Firebase init failed", e);
//    }
//}

}
