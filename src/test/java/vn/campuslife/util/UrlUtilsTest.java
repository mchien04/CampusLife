package vn.campuslife.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlUtilsTest {

    @Test
    void toFullUrl_PrependsPublicUrlForUploadsPath() {
        assertEquals(
                "http://localhost:8080/uploads/avatars/a.jpg",
                UrlUtils.toFullUrl("/uploads/avatars/a.jpg", "http://localhost:8080"));
    }

    @Test
    void toFullUrl_NormalizesBareFilename() {
        assertEquals(
                "http://localhost:8080/uploads/bd868987-18cd-4e12-befb-b5f853822813.jpg",
                UrlUtils.toFullUrl("bd868987-18cd-4e12-befb-b5f853822813.jpg", "http://localhost:8080/"));
    }

    @Test
    void toRelativePath_ExtractsFromFullUrl() {
        assertEquals(
                "/uploads/avatars/a.jpg",
                UrlUtils.toRelativePath("http://localhost:8080/uploads/avatars/a.jpg", "http://localhost:8080"));
    }

    @Test
    void toRelativePath_NormalizesBareFilename() {
        assertEquals(
                "/uploads/x.png",
                UrlUtils.toRelativePath("x.png", "http://localhost:8080"));
    }

    @Test
    void normalizeUploadRelativePath_LeavesUnknownPaths() {
        assertNull(UrlUtils.normalizeUploadRelativePath("https://cdn.example.com/avatars/a.jpg"));
    }
}
