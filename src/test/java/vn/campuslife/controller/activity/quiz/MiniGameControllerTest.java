package vn.campuslife.controller.activity.quiz;

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
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;
import vn.campuslife.model.activity.quiz.UpdateMiniGameRequest;
import vn.campuslife.service.MiniGameService;
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
public class MiniGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MiniGameService miniGameService;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateMiniGameRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateMiniGameRequest();
        createRequest.setTitle("Quiz Test");
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void createMiniGame_Valid_ReturnsOk() throws Exception {
        when(miniGameService.createMiniGame(any(CreateMiniGameRequest.class)))
                .thenReturn(new Response(true, "Created", null));

        mockMvc.perform(post("/api/minigames")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void createMiniGame_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/minigames")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getMiniGameByActivity_ReturnsOk() throws Exception {
        when(miniGameService.getMiniGameByActivity(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/minigames/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void startAttempt_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(miniGameService.startAttempt(1L, 10L))
                .thenReturn(new Response(true, "Attempt started", null));

        mockMvc.perform(post("/api/minigames/1/start")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void startAttempt_StudentNotFound_ReturnsBadRequest() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);

        mockMvc.perform(post("/api/minigames/1/start")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void startAttempt_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/minigames/1/start")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void submitAttempt_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(miniGameService.submitAttempt(eq(1L), eq(10L), anyMap()))
                .thenReturn(new Response(true, "Submitted", null));

        Map<String, Object> requestBody = Map.of("answers", Map.of("1", 11, "2", 12));

        mockMvc.perform(post("/api/minigames/attempts/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void submitAttempt_AdminRole_ReturnsForbidden() throws Exception {
        Map<String, Object> requestBody = Map.of("answers", Map.of("1", 11));

        mockMvc.perform(post("/api/minigames/attempts/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMyAttempts_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(miniGameService.getStudentAttempts(10L, 1L))
                .thenReturn(new Response(true, "Attempts", null));

        mockMvc.perform(get("/api/minigames/1/attempts/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void getQuestions_ReturnsOk() throws Exception {
        when(miniGameService.getQuestions(1L))
                .thenReturn(new Response(true, "Questions", null));

        mockMvc.perform(get("/api/minigames/1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getAttemptDetail_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(miniGameService.getAttemptDetail(1L, 10L))
                .thenReturn(new Response(true, "Detail", null));

        mockMvc.perform(get("/api/minigames/attempts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateMiniGame_Valid_ReturnsOk() throws Exception {
        UpdateMiniGameRequest request = new UpdateMiniGameRequest();
        request.setTitle("Updated Quiz");

        when(miniGameService.updateMiniGame(eq(1L), any(UpdateMiniGameRequest.class)))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(put("/api/minigames/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateMiniGame_StudentRole_ReturnsForbidden() throws Exception {
        UpdateMiniGameRequest request = new UpdateMiniGameRequest();

        mockMvc.perform(put("/api/minigames/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void deleteMiniGame_Success_ReturnsOk() throws Exception {
        when(miniGameService.deleteMiniGame(1L))
                .thenReturn(new Response(true, "Deleted", null));

        mockMvc.perform(delete("/api/minigames/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void deleteMiniGame_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/minigames/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getAllMiniGames_ReturnsList() throws Exception {
        when(miniGameService.getAllMiniGames())
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/minigames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getAllMiniGames_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/minigames"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void checkActivityHasQuiz_ReturnsResult() throws Exception {
        when(miniGameService.checkActivityHasQuiz(1L))
                .thenReturn(new Response(true, "Has quiz", true));

        mockMvc.perform(get("/api/minigames/activity/1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getQuestionsForEdit_ReturnsOk() throws Exception {
        when(miniGameService.getQuestionsForEdit(1L))
                .thenReturn(new Response(true, "Questions", null));

        mockMvc.perform(get("/api/minigames/1/questions/edit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getQuestionsForEdit_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/minigames/1/questions/edit"))
                .andExpect(status().isForbidden());
    }
}
