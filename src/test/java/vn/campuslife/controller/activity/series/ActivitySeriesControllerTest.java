package vn.campuslife.controller.activity.series;

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
import vn.campuslife.model.activity.series.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.ScorePresetService;
import vn.campuslife.service.StudentService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class ActivitySeriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivitySeriesService seriesService;

    @MockBean
    private ActivityRegistrationService activityRegistrationService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private ScorePresetService scorePresetService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createSeries_Valid_ReturnsOk() throws Exception {
        CreateSeriesRequest request = new CreateSeriesRequest();
        request.setName("Test Series");
        request.setScoreType(vn.campuslife.enumeration.ScoreType.REN_LUYEN);

        doNothing().when(scorePresetService).applySeriesPreset(any(CreateSeriesRequest.class));
        when(seriesService.createSeries(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Response(true, "Series created", null));

        mockMvc.perform(post("/api/series")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createSeries_EmptyName_ReturnsBadRequest() throws Exception {
        CreateSeriesRequest request = new CreateSeriesRequest();
        request.setName("");

        mockMvc.perform(post("/api/series")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void createSeries_StudentRole_ReturnsForbidden() throws Exception {
        CreateSeriesRequest request = new CreateSeriesRequest();
        request.setName("Test Series");

        mockMvc.perform(post("/api/series")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSeries_NoAuth_ReturnsForbidden() throws Exception {
        CreateSeriesRequest request = new CreateSeriesRequest();
        request.setName("Test Series");

        mockMvc.perform(post("/api/series")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getAllSeries_ReturnsList() throws Exception {
        when(seriesService.getAllSeries())
                .thenReturn(new Response(true, "Success", List.of()));

        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getSeriesById_Exists_ReturnsOk() throws Exception {
        when(seriesService.getSeriesById(1L))
                .thenReturn(new Response(true, "Found", Map.of("id", 1L, "name", "Series 1")));

        mockMvc.perform(get("/api/series/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getSeriesById_NotFound_Returns404() throws Exception {
        when(seriesService.getSeriesById(999L))
                .thenReturn(new Response(false, "Not found", null));

        mockMvc.perform(get("/api/series/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateSeries_Valid_ReturnsOk() throws Exception {
        UpdateSeriesRequest request = new UpdateSeriesRequest();
        request.setName("Updated Series");

        doNothing().when(scorePresetService).applySeriesPreset(any(UpdateSeriesRequest.class));
        when(seriesService.updateSeries(anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(put("/api/series/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateSeries_StudentRole_ReturnsForbidden() throws Exception {
        UpdateSeriesRequest request = new UpdateSeriesRequest();
        request.setName("Updated Series");

        mockMvc.perform(put("/api/series/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void deleteSeries_Success_ReturnsOk() throws Exception {
        when(seriesService.deleteSeries(1L))
                .thenReturn(new Response(true, "Deleted", null));

        mockMvc.perform(delete("/api/series/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void deleteSeries_Failure_ReturnsBadRequest() throws Exception {
        when(seriesService.deleteSeries(1L))
                .thenReturn(new Response(false, "Cannot delete", null));

        mockMvc.perform(delete("/api/series/1")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void deleteSeries_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/series/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createSeriesActivity_Valid_ReturnsOk() throws Exception {
        SeriesChildActivityCreateRequest request = new SeriesChildActivityCreateRequest();
        request.setName("Child Activity");

        when(seriesService.createSeriesActivity(eq(1L), any(SeriesChildActivityCreateRequest.class)))
                .thenReturn(new Response(true, "Created", null));

        mockMvc.perform(post("/api/series/1/activities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void createSeriesActivity_StudentRole_ReturnsForbidden() throws Exception {
        SeriesChildActivityCreateRequest request = new SeriesChildActivityCreateRequest();
        request.setName("Child Activity");

        mockMvc.perform(post("/api/series/1/activities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateSeriesActivity_Valid_ReturnsOk() throws Exception {
        SeriesChildActivityUpdateRequest request = new SeriesChildActivityUpdateRequest();
        request.setName("Updated Child Activity");

        when(seriesService.updateSeriesActivity(eq(1L), eq(2L), any(SeriesChildActivityUpdateRequest.class)))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(put("/api/series/1/activities/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getSeriesActivity_Exists_ReturnsOk() throws Exception {
        when(seriesService.getSeriesActivity(1L, 2L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/series/1/activities/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getActivitiesInSeries_ReturnsList() throws Exception {
        when(seriesService.getActivitiesInSeries(1L))
                .thenReturn(new Response(true, "Success", List.of()));

        mockMvc.perform(get("/api/series/1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void registerForSeries_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(seriesService.registerForSeries(1L, 10L))
                .thenReturn(new Response(true, "Registered", null));

        mockMvc.perform(post("/api/series/1/register")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void registerForSeries_StudentNotFound_ReturnsBadRequest() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);

        mockMvc.perform(post("/api/series/1/register")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void registerForSeries_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/series/1/register")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void cancelSeriesRegistration_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(activityRegistrationService.cancelSeriesRegistration(1L, 10L))
                .thenReturn(new Response(true, "Cancelled", null));

        mockMvc.perform(delete("/api/series/1/register")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMyProgress_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(seriesService.getStudentProgress(1L, 10L))
                .thenReturn(new Response(true, "Progress", Map.of("completedCount", 2)));

        mockMvc.perform(get("/api/series/1/progress/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getStudentProgress_Admin_ReturnsOk() throws Exception {
        when(seriesService.getStudentProgress(1L, 10L))
                .thenReturn(new Response(true, "Progress", Map.of("completedCount", 2)));

        mockMvc.perform(get("/api/series/1/students/10/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getSeriesProgress_Admin_ReturnsOk() throws Exception {
        when(seriesService.getSeriesProgress(1L, 0, 20, null))
                .thenReturn(new Response(true, "Progress", Map.of()));

        mockMvc.perform(get("/api/series/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getSeriesOverview_Admin_ReturnsOk() throws Exception {
        when(seriesService.getSeriesOverview(1L))
                .thenReturn(new Response(true, "Overview", Map.of()));

        mockMvc.perform(get("/api/series/1/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkMySeriesRegistration_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(seriesService.checkSeriesRegistration(1L, 10L))
                .thenReturn(new Response(true, "Registered", true));

        mockMvc.perform(get("/api/series/1/registration/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void calculateMilestone_Success_ReturnsOk() throws Exception {
        when(seriesService.calculateMilestonePoints(10L, 1L))
                .thenReturn(new Response(true, "Milestone calculated", 5));

        mockMvc.perform(post("/api/series/1/students/10/calculate-milestone")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void addActivityToSeries_Success_ReturnsOk() throws Exception {
        AddActivityToSeriesRequest request = new AddActivityToSeriesRequest();
        request.setActivityId(100L);
        request.setOrder(1);

        when(seriesService.addActivityToSeries(100L, 1L, 1))
                .thenReturn(new Response(true, "Added", null));

        mockMvc.perform(post("/api/series/1/activities/attach")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getSeriesPresets_ReturnsDefinitions() throws Exception {
        when(scorePresetService.getSeriesPresetDefinitions())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/series/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void previewSeriesPreset_ReturnsPreview() throws Exception {
        SeriesPresetPreviewRequest request = new SeriesPresetPreviewRequest();
        when(scorePresetService.previewSeriesPreset(any()))
                .thenReturn(new SeriesPresetPreviewResponse());

        mockMvc.perform(post("/api/series/presets/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}
