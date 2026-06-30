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
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.ActivityParticipationRequest;
import vn.campuslife.model.activity.ActivityRegistrationRequest;
import vn.campuslife.service.ActivityRegistrationService;
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
public class ActivityRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityRegistrationService registrationService;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    private ActivityRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        registrationRequest = new ActivityRegistrationRequest();
        registrationRequest.setActivityId(1L);
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void registerForActivity_Success_Returns201() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.registerForActivity(any(ActivityRegistrationRequest.class), eq(10L)))
                .thenReturn(new Response(true, "Registered", null));

        mockMvc.perform(post("/api/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void registerForActivity_StudentNotFound_ReturnsBadRequest() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);

        mockMvc.perform(post("/api/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void registerForActivity_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerForActivity_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void cancelRegistration_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.cancelRegistration(1L, 10L))
                .thenReturn(new Response(true, "Cancelled", null));

        mockMvc.perform(delete("/api/registrations/activity/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void cancelRegistration_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/registrations/activity/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMyRegistrations_ReturnsList() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.getStudentRegistrations(10L))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/registrations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getMyRegistrations_AdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/registrations/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getActivityRegistrations_Admin_ReturnsOk() throws Exception {
        when(registrationService.getActivityRegistrations(1L))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/registrations/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getActivityRegistrations_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/registrations/activity/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void updateRegistrationStatus_Admin_ReturnsOk() throws Exception {
        when(registrationService.updateRegistrationStatus(1L, "APPROVED"))
                .thenReturn(new Response(true, "Updated", null));

        mockMvc.perform(put("/api/registrations/1/status")
                        .with(csrf())
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void updateRegistrationStatus_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/registrations/1/status")
                        .with(csrf())
                        .param("status", "APPROVED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER", "STUDENT"})
    void getRegistrationById_ReturnsOk() throws Exception {
        when(registrationService.getRegistrationById(1L))
                .thenReturn(new Response(true, "Found", null));

        mockMvc.perform(get("/api/registrations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkRegistrationStatus_ReturnsResult() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.checkRegistrationStatus(1L, 10L))
                .thenReturn(new Response(true, "Registered", true));

        mockMvc.perform(get("/api/registrations/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER", "STUDENT"})
    void checkIn_Success_Returns201() throws Exception {
        ActivityParticipationRequest request = new ActivityParticipationRequest();
        request.setTicketCode("TK001");

        when(registrationService.checkIn(any(ActivityParticipationRequest.class)))
                .thenReturn(new Response(true, "Checked in", null));

        mockMvc.perform(post("/api/registrations/checkin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER", "STUDENT"})
    void checkIn_Failure_Returns400() throws Exception {
        ActivityParticipationRequest request = new ActivityParticipationRequest();
        request.setTicketCode("INVALID");

        when(registrationService.checkIn(any(ActivityParticipationRequest.class)))
                .thenReturn(new Response(false, "Invalid ticket", null));

        mockMvc.perform(post("/api/registrations/checkin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"STUDENT", "ADMIN", "MANAGER"})
    void checkInByQrCode_Success_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.checkInByQrCode("QR001", 10L))
                .thenReturn(new Response(true, "Checked in", null));

        Map<String, String> requestBody = Map.of("checkInCode", "QR001");

        mockMvc.perform(post("/api/registrations/checkin/qr")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkInByQrCode_EmptyCode_ReturnsBadRequest() throws Exception {
        Map<String, String> requestBody = Map.of("checkInCode", "");

        mockMvc.perform(post("/api/registrations/checkin/qr")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void checkInByQrCode_StudentNotFound_ReturnsBadRequest() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(null);

        Map<String, String> requestBody = Map.of("checkInCode", "QR001");

        mockMvc.perform(post("/api/registrations/checkin/qr")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser
    void validateTicketCode_ReturnsResult() throws Exception {
        when(registrationService.validateTicketCode("TK001"))
                .thenReturn(new Response(true, "Valid", Map.of("studentName", "John")));

        mockMvc.perform(get("/api/registrations/checkin/validate")
                        .param("ticketCode", "TK001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void gradeCompletion_Admin_ReturnsOk() throws Exception {
        when(registrationService.gradeCompletion(1L, true, "Good"))
                .thenReturn(new Response(true, "Graded", null));

        mockMvc.perform(put("/api/registrations/participations/1/grade")
                        .with(csrf())
                        .param("isCompleted", "true")
                        .param("notes", "Good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void gradeCompletion_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/registrations/participations/1/grade")
                        .with(csrf())
                        .param("isCompleted", "true")
                        .param("notes", "Good"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void backfillMissingParticipations_Admin_ReturnsOk() throws Exception {
        when(registrationService.backfillMissingParticipations())
                .thenReturn(new Response(true, "Backfilled", null));

        mockMvc.perform(post("/api/registrations/backfill/participations")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void backfillMissingParticipations_StudentRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/registrations/backfill/participations")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void getActivityParticipations_Admin_ReturnsOk() throws Exception {
        when(registrationService.getActivityParticipations(1L))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/registrations/activities/1/participations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getMyRegistrationsByStatus_ReturnsList() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.getStudentRegistrationsStatus(10L, RegistrationStatus.APPROVED))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/registrations/my/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "MANAGER"})
    void searchRegistrations_Admin_ReturnsOk() throws Exception {
        when(registrationService.search(any(), any()))
                .thenReturn(new Response(true, "List", null));

        mockMvc.perform(get("/api/registrations/search")
                        .param("keyword", "test")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    void getStudentJoinedEventDates_ReturnsOk() throws Exception {
        when(studentService.getStudentIdByUsername(any())).thenReturn(10L);
        when(registrationService.getStudentJoinedEventDates(10L))
                .thenReturn(new Response(true, "Dates", null));

        mockMvc.perform(get("/api/registrations/personal-calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}
