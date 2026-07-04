package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.util.UrlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class UploadStorageServiceImplTest {

    private UploadProperties uploadProperties;
    private UploadStorageServiceImpl uploadStorageService;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        uploadProperties = new UploadProperties();
        uploadProperties.setDir(tempDir.toString());
        uploadProperties.setPublicUrl("http://localhost:8080");
        
        UploadProperties.Paths paths = new UploadProperties.Paths();
        paths.setPublicPrefix("/uploads");
        paths.setGeneral("general");
        paths.setActivityPhotos("activities");
        paths.setSubmissions("submissions");
        uploadProperties.setPaths(paths);

        uploadStorageService = new UploadStorageServiceImpl(uploadProperties);
    }

    @Test
    public void testStore_Success() throws IOException {
        // Arrange
        byte[] content = "Hello Local File".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        // Act
        String relativeUrl = uploadStorageService.store(file, "general", false);

        // Assert
        assertNotNull(relativeUrl);
        assertTrue(relativeUrl.startsWith("/uploads/general/"));
        assertTrue(relativeUrl.endsWith(".txt"));

        // Verify file actually written to the directory
        Path resolvedPath = uploadStorageService.resolveFilePath(relativeUrl);
        assertTrue(Files.exists(resolvedPath));
        assertArrayEquals(content, Files.readAllBytes(resolvedPath));
    }

    @Test
    public void testStore_ImageOnly_Success() throws IOException {
        // Arrange
        byte[] content = "image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", content);

        // Act
        String relativeUrl = uploadStorageService.store(file, "avatars", true);

        // Assert
        assertNotNull(relativeUrl);
        assertTrue(relativeUrl.startsWith("/uploads/avatars/"));
        assertTrue(relativeUrl.endsWith(".png"));
    }

    @Test
    public void testStore_ImageOnly_Failure_NotImage() {
        // Arrange
        byte[] content = "not-an-image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", content);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            uploadStorageService.store(file, "documents", true);
        });
        assertEquals("Only image files are allowed", exception.getMessage());
    }

    @Test
    public void testDelete_Success() throws IOException {
        // Arrange
        byte[] content = "file-to-delete".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "delete-me.txt", "text/plain", content);
        String relativeUrl = uploadStorageService.store(file, "general", false);
        Path filePath = uploadStorageService.resolveFilePath(relativeUrl);
        assertTrue(Files.exists(filePath));

        // Act
        uploadStorageService.delete(relativeUrl);

        // Assert
        assertFalse(Files.exists(filePath));
    }

    @Test
    public void testToPublicUrl() {
        // Act
        String publicUrl = uploadStorageService.toPublicUrl("/uploads/general/test.txt");

        // Assert
        assertEquals("http://localhost:8080/uploads/general/test.txt", publicUrl);
    }

    @Test
    public void testExtractRelativePath() {
        // Act & Assert
        assertEquals("/uploads/general/test.txt", 
                uploadStorageService.extractRelativePath("http://localhost:8080/uploads/general/test.txt"));
        
        assertEquals("/uploads/general/test.txt", 
                uploadStorageService.extractRelativePath("/uploads/general/test.txt"));

        assertEquals("/uploads/general/test.txt", 
                uploadStorageService.extractRelativePath("http://otherdomain.com/uploads/general/test.txt"));
    }

    @Test
    public void testResolveFilePath() {
        // Act
        Path filePath = uploadStorageService.resolveFilePath("/uploads/general/test.txt");

        // Assert
        Path expectedPath = Path.of(uploadProperties.getDir(), "general", "test.txt").toAbsolutePath().normalize();
        assertEquals(expectedPath, filePath.toAbsolutePath().normalize());
    }
}
