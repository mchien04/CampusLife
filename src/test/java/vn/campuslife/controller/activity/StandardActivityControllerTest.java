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
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.service.StandardActivityService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class StandardActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StandardActivityService standardActivityService;

    @Autowired
    private ObjectMapper objectMapper;

    private StandardActivityCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new StandardActivityCreateRequest();
        createRequest.setName("Standard Activity");
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createStandardActivity_Valid_ReturnsOk() throws Exception {
        when(standardActivityService.createActivity(any(StandardActivityCreateRequest.class)))
                .thenReturn(new Response(true, "Standard activity created", null));

        mockMvc.perform(post("/api/activities/standard")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createStandardActivity_Invalid_ReturnsBadRequest() throws Exception {
        when(standardActivityService.createActivity(any(StandardActivityCreateRequest.class)))
                .thenReturn(new Response(false, "Invalid request", null));

        mockMvc.perform(post("/api/activities/standard")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void createStandardActivity_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities/standard")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStandardActivity_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities/standard")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateStandardActivity_Valid_ReturnsOk() throws Exception {
        StandardActivityUpdateRequest updateRequest = new StandardActivityUpdateRequest();
        updateRequest.setName("Updated Standard Activity");

        when(standardActivityService.updateActivity(eq(1L), any(StandardActivityUpdateRequest.class)))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(put("/api/activities/standard/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateStandardActivity_Invalid_ReturnsBadRequest() throws Exception {
        StandardActivityUpdateRequest updateRequest = new StandardActivityUpdateRequest();

        when(standardActivityService.updateActivity(eq(1L), any(StandardActivityUpdateRequest.class)))
                .thenReturn(new Response(false, "Invalid", null));

        mockMvc.perform(put("/api/activities/standard/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateStandardActivity_StudentRole_ReturnsForbidden() throws Exception {
        StandardActivityUpdateRequest updateRequest = new StandardActivityUpdateRequest();

        mockMvc.perform(put("/api/activities/standard/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getStandardActivity_Exists_ReturnsOk() throws Exception {
        when(standardActivityService.getActivity(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/activities/standard/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getStandardActivity_NotFound_ReturnsBadRequest() throws Exception {
        when(standardActivityService.getActivity(999L))
                .thenReturn(new Response(false, "Not found", null));

        mockMvc.perform(get("/api/activities/standard/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getStandardActivity_PublicAccess_ReturnsOk() throws Exception {
        when(standardActivityService.getActivity(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/activities/standard/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}
