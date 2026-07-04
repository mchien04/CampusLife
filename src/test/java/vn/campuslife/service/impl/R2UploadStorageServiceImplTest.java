package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import vn.campuslife.config.UploadProperties;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class R2UploadStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    private UploadProperties uploadProperties;
    private R2UploadStorageServiceImpl r2UploadStorageService;

    @BeforeEach
    public void setUp() {
        uploadProperties = new UploadProperties();
        uploadProperties.setPublicUrl("http://localhost:8080");

        UploadProperties.R2 r2 = new UploadProperties.R2();
        r2.setBucket("campuslife-bucket");
        r2.setCdnDomain("https://pub-r2.campuslife.app");
        uploadProperties.setR2(r2);

        r2UploadStorageService = new R2UploadStorageServiceImpl(uploadProperties, s3Client);
    }

    @Test
    public void testStore_Success() throws IOException {
        // Arrange
        byte[] content = "Hello R2 File".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String key = r2UploadStorageService.store(file, "submissions", false);

        // Assert
        assertNotNull(key);
        assertTrue(key.startsWith("submissions/"));
        assertTrue(key.endsWith(".txt"));

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(putRequestCaptor.capture(), requestBodyCaptor.capture());

        PutObjectRequest putRequest = putRequestCaptor.getValue();
        assertEquals("campuslife-bucket", putRequest.bucket());
        assertEquals(key, putRequest.key());
        assertEquals("text/plain", putRequest.contentType());
        
        RequestBody requestBody = requestBodyCaptor.getValue();
        assertEquals(content.length, requestBody.contentStreamProvider().newStream().available());
    }

    @Test
    public void testStore_ImageOnly_Success() throws IOException {
        // Arrange
        byte[] content = "image-data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", content);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String key = r2UploadStorageService.store(file, "activities", true);

        // Assert
        assertNotNull(key);
        assertTrue(key.startsWith("activities/"));
        assertTrue(key.endsWith(".png"));
    }

    @Test
    public void testStore_ImageOnly_Failure_NotImage() {
        // Arrange
        byte[] content = "document-data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "notes.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", content);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            r2UploadStorageService.store(file, "general", true);
        });
        assertEquals("Only image files are allowed", exception.getMessage());
    }

    @Test
    public void testDelete_Success() throws IOException {
        // Arrange
        String relativePath = "general/uuid-file.jpg";

        // Act
        r2UploadStorageService.delete(relativePath);

        // Assert
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest deleteRequest = captor.getValue();
        assertEquals("campuslife-bucket", deleteRequest.bucket());
        assertEquals(relativePath, deleteRequest.key());
    }

    @Test
    public void testToPublicUrl_WithCdnDomain() {
        // Arrange
        String relativePath = "general/test.jpg";

        // Act
        String publicUrl = r2UploadStorageService.toPublicUrl(relativePath);

        // Assert
        assertEquals("https://pub-r2.campuslife.app/general/test.jpg", publicUrl);
    }

    @Test
    public void testToPublicUrl_WithoutCdnDomain() {
        // Arrange
        uploadProperties.getR2().setCdnDomain(""); // Empty CDN Domain
        String relativePath = "/uploads/general/test.jpg";

        // Act
        String publicUrl = r2UploadStorageService.toPublicUrl(relativePath);

        // Assert
        assertEquals("http://localhost:8080/uploads/general/test.jpg", publicUrl);
    }

    @Test
    public void testExtractRelativePath_WithCdnDomain() {
        // Arrange
        String fileUrl = "https://pub-r2.campuslife.app/general/uuid-photo.png";

        // Act
        String extracted = r2UploadStorageService.extractRelativePath(fileUrl);

        // Assert
        assertEquals("general/uuid-photo.png", extracted);
    }

    @Test
    public void testExtractRelativePath_WithoutCdnDomain() {
        // Arrange
        uploadProperties.getR2().setCdnDomain(""); // Empty CDN
        String fileUrl = "https://pub-r2.campuslife.app/general/uuid-photo.png";

        // Act
        String extracted = r2UploadStorageService.extractRelativePath(fileUrl);

        // Assert (returns unchanged)
        assertEquals(fileUrl, extracted);
    }

    @Test
    public void testResolveFilePath() {
        // Arrange
        String relativePath = "general/test-file.jpg";

        // Act
        Path path = r2UploadStorageService.resolveFilePath(relativePath);

        // Assert
        assertEquals(Path.of(relativePath), path);
    }
}
