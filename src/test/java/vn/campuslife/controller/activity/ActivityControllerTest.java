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
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityResponse;
import vn.campuslife.model.activity.CreateActivityRequest;
import vn.campuslife.service.ActivityPhotoService;
import vn.campuslife.service.ActivityService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private ActivityPhotoService photoService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateActivityRequest createRequest;
    private ActivityResponse activityResponse;

    @BeforeEach
    void setUp() {
        createRequest = new CreateActivityRequest();
        createRequest.setName("Test Activity");
        createRequest.setType(vn.campuslife.enumeration.ActivityType.SUKIEN);

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setName("Test Activity");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createActivity_ValidRequest_ReturnsOk() throws Exception {
        when(activityService.createActivity(any(CreateActivityRequest.class)))
                .thenReturn(new Response(true, "Activity created", activityResponse));

        mockMvc.perform(post("/api/activities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createActivity_InvalidRequest_ReturnsBadRequest() throws Exception {
        when(activityService.createActivity(any(CreateActivityRequest.class)))
                .thenReturn(new Response(false, "Invalid request", null));

        mockMvc.perform(post("/api/activities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void createActivity_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getAllActivities_ReturnsList() throws Exception {
        when(activityService.getAllActivities(any()))
                .thenReturn(new Response(true, "Success", List.of(activityResponse)));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser
    void getActivityById_Exists_ReturnsOk() throws Exception {
        when(activityService.getActivityById(eq(1L), any()))
                .thenReturn(new Response(true, "Found", activityResponse));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser
    void getActivityById_NotFound_Returns404() throws Exception {
        when(activityService.getActivityById(eq(999L), any()))
                .thenReturn(new Response(false, "Not found", null));

        mockMvc.perform(get("/api/activities/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateActivity_Valid_ReturnsOk() throws Exception {
        when(activityService.updateActivity(eq(1L), any(CreateActivityRequest.class)))
                .thenReturn(new Response(true, "Updated", activityResponse));

        mockMvc.perform(put("/api/activities/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void deleteActivity_Success_ReturnsOk() throws Exception {
        when(activityService.deleteActivity(1L))
                .thenReturn(new Response(true, "Deleted", null));

        mockMvc.perform(delete("/api/activities/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void deleteActivity_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/activities/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void publishActivity_Success_ReturnsOk() throws Exception {
        when(activityService.publishActivity(1L))
                .thenReturn(new Response(true, "Published", null));

        mockMvc.perform(put("/api/activities/1/publish")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void unpublishActivity_Success_ReturnsOk() throws Exception {
        when(activityService.unpublishActivity(1L))
                .thenReturn(new Response(true, "Unpublished", null));

        mockMvc.perform(put("/api/activities/1/unpublish")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void copyActivity_Success_ReturnsOk() throws Exception {
        when(activityService.copyActivity(1L, 7))
                .thenReturn(new Response(true, "Copied", activityResponse));

        mockMvc.perform(post("/api/activities/1/copy")
                        .with(csrf())
                        .param("offsetDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getActivityPresets_ReturnsDefinitions() throws Exception {
        when(activityService.getActivityPresetDefinitions())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/activities/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void previewActivityPreset_ReturnsPreview() throws Exception {
        ActivityPresetPreviewRequest request = new ActivityPresetPreviewRequest();
        when(activityService.previewActivityPreset(any()))
                .thenReturn(new vn.campuslife.model.activity.ActivityPresetPreviewResponse());

        mockMvc.perform(post("/api/activities/presets/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser
    void getByScoreType_ReturnsList() throws Exception {
        when(activityService.getActivitiesByScoreType(ScoreType.REN_LUYEN))
                .thenReturn(List.of(activityResponse));

        mockMvc.perform(get("/api/activities/score-type/REN_LUYEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Activity"));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkRegistrationStatus_ReturnsStatus() throws Exception {
        when(activityService.checkRegistrationStatus(eq(1L), any()))
                .thenReturn(new Response(true, "Registered", Map.of("status", "APPROVED")));

        mockMvc.perform(get("/api/activities/1/registration-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser
    void checkRequiresSubmission_ReturnsResult() throws Exception {
        when(activityService.checkRequiresSubmission(1L))
                .thenReturn(new Response(true, "", Map.of("requiresSubmission", true)));

        mockMvc.perform(get("/api/activities/1/requires-submission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void backfillCheckInCodes_Success_ReturnsOk() throws Exception {
        when(activityService.backfillCheckInCodes())
                .thenReturn(new Response(true, "Backfilled 5 activities", 5));

        mockMvc.perform(post("/api/activities/backfill-checkin-codes")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void backfillCheckInCodes_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/activities/backfill-checkin-codes")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getActivitiesByMonth_ReturnsList() throws Exception {
        when(activityService.getActivitiesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(List.of(activityResponse));
        when(activityService.getActivitiesByMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(activityResponse));

        mockMvc.perform(get("/api/activities/month")
                        .param("year", "2025")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Activity"));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void searchUpcomingEvents_ReturnsList() throws Exception {
        when(activityService.searchUpcomingEvents(eq("test"), any()))
                .thenReturn(List.of(activityResponse));
        when(activityService.searchUpcomingEvents("test"))
                .thenReturn(List.of(activityResponse));

        mockMvc.perform(get("/api/activities/upcoming")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Activity"));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void myActivities_ReturnsList() throws Exception {
        when(activityService.listForCurrentUser(any()))
                .thenReturn(List.of(activityResponse));

        mockMvc.perform(get("/api/activities/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Activity"));
    }

    @Test
    @WithMockUser
    void getByDepartment_ReturnsList() throws Exception {
        when(activityService.getActivitiesForDepartment(1L))
                .thenReturn(List.of(activityResponse));

        mockMvc.perform(get("/api/activities/department/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Activity"));
    }
}
