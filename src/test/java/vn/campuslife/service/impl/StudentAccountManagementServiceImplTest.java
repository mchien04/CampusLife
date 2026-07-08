package vn.campuslife.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.BulkSendCredentialsRequest;
import vn.campuslife.model.student.CreateStudentRequest;
import vn.campuslife.model.student.StudentAccountResponse;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.StudentScoreInitService;
import vn.campuslife.util.EmailUtil;
import vn.campuslife.util.ExcelParser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAccountManagementServiceImplTest {

    @Mock
    private ExcelParser excelParser;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DepartmentAuthorizationService departmentAuthorizationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StudentScoreInitService studentScoreInitService;
    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private StudentAccountManagementServiceImpl service;

    @Test
    void createStudent_WithDepartmentId_PersistsDepartmentAndReturnsItInResponse() {
        CreateStudentRequest request = new CreateStudentRequest("SV001", "Nguyen Van A", "sv001@test.com", 3L);

        Department department = new Department();
        department.setId(3L);
        department.setName("Khoa CNTT");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("SV001");
        savedUser.setEmail("sv001@test.com");
        savedUser.setRole(Role.STUDENT);
        savedUser.setActivated(true);
        savedUser.setDeleted(false);

        Student savedStudent = new Student();
        savedStudent.setId(20L);
        savedStudent.setUser(savedUser);
        savedStudent.setStudentCode("SV001");
        savedStudent.setFullName("Nguyen Van A");
        savedStudent.setDeleted(false);

        when(userRepository.existsByUsernameAndIsDeletedFalse("SV001")).thenReturn(false);
        when(userRepository.existsByEmailAndIsDeletedFalse("sv001@test.com")).thenReturn(false);
        when(studentRepository.findByStudentCodeAndIsDeletedFalse("SV001")).thenReturn(Optional.empty());
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("SV001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            if (student.getId() == null) {
                student.setId(20L);
            }
            return student;
        });
        when(studentRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(savedStudent));
        when(departmentRepository.existsById(3L)).thenReturn(true);
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

        Response response = service.createStudent(request);

        assertTrue(response.isStatus());
        StudentAccountResponse body = (StudentAccountResponse) response.getBody();
        assertEquals(3L, body.getDepartmentId());
        assertEquals("Khoa CNTT", body.getDepartmentName());

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository, atLeast(2)).save(studentCaptor.capture());
        Student persisted = studentCaptor.getAllValues().get(studentCaptor.getAllValues().size() - 1);
        assertNotNull(persisted.getDepartment());
        assertEquals(3L, persisted.getDepartment().getId());
    }

    @Test
    void createStudent_departmentOutsideManagerScope_doesNotCreateAccount() {
        CreateStudentRequest request = new CreateStudentRequest("SV002", "Test User", "sv002@test.com", 99L);
        DepartmentScope scope = DepartmentScope.manager(Set.of(1L));

        when(departmentRepository.existsById(99L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createStudent(request, scope));

        assertEquals("Department is outside your scope", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void sendCredentials_success_setsCredentialsEmailSentAt() {
        User user = new User();
        user.setId(1L);
        user.setUsername("SV003");
        user.setEmail("sv003@test.com");
        user.setDeleted(false);

        Student student = new Student();
        student.setId(30L);
        student.setUser(user);
        student.setDeleted(false);

        when(studentRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(emailUtil.sendStudentCredentialsEmail(eq("sv003@test.com"), eq("SV003"), anyString())).thenReturn(true);

        Response response = service.sendCredentials(30L);

        assertTrue(response.isStatus());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getCredentialsEmailSentAt());
        assertEquals("hashed", userCaptor.getValue().getPassword());
    }

    @Test
    void sendCredentials_emailFailure_doesNotPersistPasswordOrTimestamp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("SV004");
        user.setEmail("sv004@test.com");
        user.setDeleted(false);

        Student student = new Student();
        student.setId(40L);
        student.setUser(user);
        student.setDeleted(false);

        when(studentRepository.findByIdAndIsDeletedFalse(40L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(emailUtil.sendStudentCredentialsEmail(anyString(), anyString(), anyString())).thenReturn(false);

        Response response = service.sendCredentials(40L);

        assertFalse(response.isStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void bulkSendCredentials_success_setsCredentialsEmailSentAt() {
        User user = new User();
        user.setId(2L);
        user.setUsername("SV005");
        user.setEmail("sv005@test.com");
        user.setDeleted(false);

        Student student = new Student();
        student.setId(50L);
        student.setUser(user);
        student.setDeleted(false);

        when(studentRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(emailUtil.sendStudentCredentialsEmail(eq("sv005@test.com"), eq("SV005"), anyString())).thenReturn(true);

        BulkSendCredentialsRequest request = new BulkSendCredentialsRequest();
        request.setStudentIds(List.of(50L));

        Response response = service.bulkSendCredentials(request);

        assertTrue(response.isStatus());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getCredentialsEmailSentAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPendingAccounts_withCredentialsSentFilter_queriesRepositoryWithSpec() {
        Page<Student> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(studentRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);

        Response response = service.getPendingAccounts(PageRequest.of(0, 20), false);

        assertTrue(response.isStatus());
        verify(studentRepository).findAll(any(Specification.class), any(PageRequest.class));
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(0L, body.get("totalElements"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPendingAccounts_mapsEmailSentFromCredentialsTimestamp() {
        User user = new User();
        user.setId(3L);
        user.setUsername("SV006");
        user.setEmail("sv006@test.com");
        user.setDeleted(false);
        user.setCredentialsEmailSentAt(LocalDateTime.of(2026, 7, 8, 10, 0));
        user.setLastLogin(null);

        Student student = new Student();
        student.setId(60L);
        student.setUser(user);
        student.setDeleted(false);

        Page<Student> page = new PageImpl<>(List.of(student), PageRequest.of(0, 20), 1);
        when(studentRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        Response response = service.getPendingAccounts(PageRequest.of(0, 20), true);

        assertTrue(response.isStatus());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<StudentAccountResponse> content = (List<StudentAccountResponse>) body.get("content");
        assertEquals(1, content.size());
        assertTrue(content.get(0).getEmailSent());
        assertEquals(user.getCredentialsEmailSentAt(), content.get(0).getCredentialsEmailSentAt());
    }
}
