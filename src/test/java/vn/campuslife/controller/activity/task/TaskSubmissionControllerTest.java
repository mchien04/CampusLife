package vn.campuslife.controller.activity.task;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.campuslife.model.Response;
import vn.campuslife.service.StudentService;
import vn.campuslife.service.TaskSubmissionService;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.entity.User;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class TaskSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskSubmissionService taskSubmissionService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void submitTask_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(taskSubmissionService.submitTask(eq(1L), eq(10L), any(), any(), any()))
                .thenReturn(new Response(true, "Task submitted", null));

        MockMultipartFile file = new MockMultipartFile("files", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/submissions/task/1")
                        .file(file)
                        .with(csrf())
                        .param("content", "My submission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void submitTask_StudentNotFound_ReturnsBadRequest() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);

        mockMvc.perform(multipart("/api/submissions/task/1")
                        .with(csrf())
                        .param("content", "My submission"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void submitTask_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/submissions/task/1")
                        .with(csrf())
                        .param("content", "My submission"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitTask_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(multipart("/api/submissions/task/1")
                        .with(csrf())
                        .param("content", "My submission"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateSubmission_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(taskSubmissionService.updateSubmission(eq(1L), eq(10L), any(), any(), any()))
                .thenReturn(new Response(true, "Updated", null));

        MockMultipartFile file = new MockMultipartFile("files", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/submissions/1")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .param("content", "Updated content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSubmission_AdminRole_ReturnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/submissions/1")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(csrf())
                        .param("content", "Updated content"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMySubmissions_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(taskSubmissionService.getStudentSubmissions(1L, 10L))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/submissions/task/1/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getMySubmissions_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/submissions/task/1/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getTaskSubmissions_Admin_ReturnsOk() throws Exception {
        when(taskSubmissionService.getTaskSubmissions(1L))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/submissions/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getTaskSubmissions_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/submissions/task/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void gradeSubmission_Admin_ReturnsOk() throws Exception {
        User user = new User();
        user.setId(5L);
        when(userRepository.findByUsernameAndIsDeletedFalse(any())).thenReturn(Optional.of(user));
        when(taskSubmissionService.gradeSubmission(eq(1L), eq(5L), eq(true), eq("Good job")))
                .thenReturn(new Response(true, "Graded", null));

        mockMvc.perform(put("/api/submissions/1/grade")
                        .with(csrf())
                        .param("isCompleted", "true")
                        .param("feedback", "Good job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void gradeSubmission_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/submissions/1/grade")
                        .with(csrf())
                        .param("isCompleted", "true")
                        .param("feedback", "Good job"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER", "STUDENT"})
    void getSubmissionDetails_ReturnsOk() throws Exception {
        when(taskSubmissionService.getSubmissionDetails(1L))
                .thenReturn(new Response(true, "Details", null));

        mockMvc.perform(get("/api/submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void deleteSubmission_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(taskSubmissionService.deleteSubmission(1L, 10L))
                .thenReturn(new Response(true, "Deleted", null));

        mockMvc.perform(delete("/api/submissions/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteSubmission_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/submissions/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER", "STUDENT"})
    void getSubmissionFiles_ReturnsOk() throws Exception {
        when(taskSubmissionService.getSubmissionFiles(1L))
                .thenReturn(new Response(true, "Files", null));

        mockMvc.perform(get("/api/submissions/1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}
