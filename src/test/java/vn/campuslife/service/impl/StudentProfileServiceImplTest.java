package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentProfileResponse;
import vn.campuslife.model.StudentProfileUpdateRequest;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentClassRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.UploadStorageService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceImplTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private StudentClassRepository studentClassRepository;
    @Mock
    private UploadProperties uploadProperties;
    @Mock
    private UploadStorageService uploadStorageService;

    @InjectMocks
    private StudentProfileServiceImpl studentProfileService;

    private Student student;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("sv01");
        user.setEmail("sv01@campuslife.vn");
        user.setRole(Role.STUDENT);

        student = new Student();
        student.setId(10L);
        student.setUser(user);
        student.setAvatarUrl("/uploads/avatars/old.jpg");

        UploadProperties.Paths paths = new UploadProperties.Paths();
        paths.setAvatars("avatars");
        org.mockito.Mockito.lenient().when(uploadProperties.getPaths()).thenReturn(paths);
    }

    @Test
    void uploadStudentAvatar_StoresUnderAvatarsAndReplacesOldFile() throws Exception {
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(uploadStorageService.store(any(), eq("avatars"), eq(true)))
                .thenReturn("/uploads/avatars/new.jpg");
        when(uploadStorageService.extractRelativePath("/uploads/avatars/old.jpg"))
                .thenReturn("/uploads/avatars/old.jpg");
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(uploadStorageService.toPublicUrl("/uploads/avatars/new.jpg"))
                .thenReturn("http://localhost:8080/uploads/avatars/new.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "img".getBytes());

        Response response = studentProfileService.uploadStudentAvatar(10L, file);

        assertTrue(response.isStatus());
        StudentProfileResponse body = (StudentProfileResponse) response.getBody();
        assertEquals("http://localhost:8080/uploads/avatars/new.jpg", body.getAvatarUrl());
        assertEquals("/uploads/avatars/new.jpg", student.getAvatarUrl());
        verify(uploadStorageService).delete("/uploads/avatars/old.jpg");
    }

    @Test
    void updateStudentProfile_NormalizesAvatarUrlBeforeSave() throws Exception {
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(uploadStorageService.extractRelativePath("http://localhost:8080/uploads/avatars/new.jpg"))
                .thenReturn("/uploads/avatars/new.jpg");
        when(uploadStorageService.extractRelativePath("/uploads/avatars/old.jpg"))
                .thenReturn("/uploads/avatars/old.jpg");
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(uploadStorageService.toPublicUrl("/uploads/avatars/new.jpg"))
                .thenReturn("http://localhost:8080/uploads/avatars/new.jpg");

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest();
        request.setFullName("Nguyen Van A");
        request.setAvatarUrl("http://localhost:8080/uploads/avatars/new.jpg");

        Response response = studentProfileService.updateStudentProfile(10L, request);

        assertTrue(response.isStatus());
        assertEquals("/uploads/avatars/new.jpg", student.getAvatarUrl());
        verify(uploadStorageService).delete("/uploads/avatars/old.jpg");
    }

    @Test
    void updateStudentProfile_NullAvatarUrl_DoesNotChangeAvatar() throws Exception {
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(uploadStorageService.toPublicUrl("/uploads/avatars/old.jpg"))
                .thenReturn("http://localhost:8080/uploads/avatars/old.jpg");

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest();
        request.setFullName("Nguyen Van A");

        Response response = studentProfileService.updateStudentProfile(10L, request);

        assertTrue(response.isStatus());
        assertEquals("/uploads/avatars/old.jpg", student.getAvatarUrl());
        verify(uploadStorageService, never()).delete(any());
    }
}
