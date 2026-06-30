package vn.campuslife.controller.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.service.MinigameActivityService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class MinigameActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinigameActivityService minigameActivityService;

    @Autowired
    private ObjectMapper objectMapper;

    private MinigameActivityCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new MinigameActivityCreateRequest();
        createRequest.setName("Minigame Activity");
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createMinigame_Valid_ReturnsOk() throws Exception {
        when(minigameActivityService.createMinigame(any(MinigameActivityCreateRequest.class)))
                .thenReturn(new Response(true, "Minigame created", null));

        mockMvc.perform(post("/api/activities/minigame")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createMinigame_Invalid_ReturnsBadRequest() throws Exception {
        when(minigameActivityService.createMinigame(any(MinigameActivityCreateRequest.class)))
                .thenReturn(new Response(false, "Invalid", null));

        mockMvc.perform(post("/api/activities/minigame")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void createMinigame_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities/minigame")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMinigame_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities/minigame")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateMinigame_Valid_ReturnsOk() throws Exception {
        MinigameActivityUpdateRequest updateRequest = new MinigameActivityUpdateRequest();
        updateRequest.setName("Updated Minigame");

        when(minigameActivityService.updateMinigame(eq(1L), any(MinigameActivityUpdateRequest.class)))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(patch("/api/activities/minigame/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateMinigame_Invalid_ReturnsBadRequest() throws Exception {
        MinigameActivityUpdateRequest updateRequest = new MinigameActivityUpdateRequest();

        when(minigameActivityService.updateMinigame(eq(1L), any(MinigameActivityUpdateRequest.class)))
                .thenReturn(new Response(false, "Invalid", null));

        mockMvc.perform(patch("/api/activities/minigame/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateMinigame_StudentRole_ReturnsForbidden() throws Exception {
        MinigameActivityUpdateRequest updateRequest = new MinigameActivityUpdateRequest();

        mockMvc.perform(patch("/api/activities/minigame/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getMinigame_Exists_ReturnsOk() throws Exception {
        when(minigameActivityService.getMinigame(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/activities/minigame/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getMinigame_NotFound_ReturnsBadRequest() throws Exception {
        when(minigameActivityService.getMinigame(999L))
                .thenReturn(new Response(false, "Not found", null));

        mockMvc.perform(get("/api/activities/minigame/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMinigame_PublicAccess_ReturnsOk() throws Exception {
        when(minigameActivityService.getMinigame(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/activities/minigame/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}
