package vn.campuslife.controller.score;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.campuslife.model.Response;
import vn.campuslife.service.RecalculationJobService;
import vn.campuslife.service.ScoreService;
import vn.campuslife.service.StudentService;

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
public class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScoreService scoreService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private RecalculationJobService recalculationJobService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void viewScores_ReturnsOk() throws Exception {
        when(scoreService.viewScores(10L, 1L))
                .thenReturn(new Response(true, "Scores", Map.of("total", 15)));

        mockMvc.perform(get("/api/scores/student/10/semester/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    void viewScores_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/scores/student/10/semester/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getTotalScore_ReturnsOk() throws Exception {
        when(scoreService.getTotalScore(10L, 1L))
                .thenReturn(new Response(true, "Total", 15));

        mockMvc.perform(get("/api/scores/student/10/semester/1/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getStudentRanking_WithParams_ReturnsOk() throws Exception {
        when(scoreService.getStudentRanking(1L, null, null, null, "DESC"))
                .thenReturn(new Response(true, "Ranking", null));

        mockMvc.perform(get("/api/scores/ranking")
                        .param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getStudentRanking_WithScoreType_ReturnsOk() throws Exception {
        when(scoreService.getStudentRanking(1L, vn.campuslife.enumeration.ScoreType.REN_LUYEN, 1L, 1L, "ASC"))
                .thenReturn(new Response(true, "Ranking", null));

        mockMvc.perform(get("/api/scores/ranking")
                        .param("semesterId", "1")
                        .param("scoreType", "REN_LUYEN")
                        .param("departmentId", "1")
                        .param("classId", "1")
                        .param("sortOrder", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getStudentRanking_InvalidScoreType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/scores/ranking")
                        .param("semesterId", "1")
                        .param("scoreType", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void recalculateStudentScore_Success_ReturnsOk() throws Exception {
        when(scoreService.recalculateStudentScore(10L, 1L))
                .thenReturn(new Response(true, "Recalculated", null));

        mockMvc.perform(post("/api/scores/recalculate/student/10")
                        .with(csrf())
                        .param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void recalculateStudentScore_NoSemester_ReturnsOk() throws Exception {
        when(scoreService.recalculateStudentScore(10L, null))
                .thenReturn(new Response(true, "Recalculated", null));

        mockMvc.perform(post("/api/scores/recalculate/student/10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void recalculateStudentScore_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/scores/recalculate/student/10")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void recalculateAllScores_Success_ReturnsOk() throws Exception {
        when(scoreService.recalculateAllStudentScores(1L))
                .thenReturn(new Response(true, "Recalculated all", null));

        mockMvc.perform(post("/api/scores/recalculate/all")
                        .with(csrf())
                        .param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void recalculateAllScores_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/scores/recalculate/all")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getScoreHistory_WithParams_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);
        when(scoreService.getScoreHistory(eq(10L), eq(1L), any(), eq(0), eq(20), any(), any(), any(), any()))
                .thenReturn(new Response(true, "History", null));

        mockMvc.perform(get("/api/scores/history/student/10")
                        .param("semesterId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getScoreHistory_WithScoreType_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);
        when(scoreService.getScoreHistory(eq(10L), eq(1L), eq(vn.campuslife.enumeration.ScoreType.REN_LUYEN), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(new Response(true, "History", null));

        mockMvc.perform(get("/api/scores/history/student/10")
                        .param("semesterId", "1")
                        .param("scoreType", "REN_LUYEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getScoreHistory_InvalidScoreType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/scores/history/student/10")
                        .param("semesterId", "1")
                        .param("scoreType", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void startAsyncRecalculation_Success_ReturnsOk() throws Exception {
        when(recalculationJobService.startAsyncRecalculation(1L, null))
                .thenReturn(new Response(true, "Job started", Map.of("jobId", 1)));

        mockMvc.perform(post("/api/scores/recalculate/async")
                        .with(csrf())
                        .param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void startAsyncRecalculation_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/scores/recalculate/async")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getRecalculationJobStatus_ReturnsOk() throws Exception {
        when(recalculationJobService.getJobStatus(1L))
                .thenReturn(new Response(true, "Running", Map.of("status", "RUNNING")));

        mockMvc.perform(get("/api/scores/recalculate/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getRecalculationJobStatus_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/scores/recalculate/status/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void retryRecalculationJob_Success_ReturnsOk() throws Exception {
        when(recalculationJobService.retryFailedJob(1L))
                .thenReturn(new Response(true, "Retried", null));

        mockMvc.perform(post("/api/scores/recalculate/retry/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void retryRecalculationJob_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/scores/recalculate/retry/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
