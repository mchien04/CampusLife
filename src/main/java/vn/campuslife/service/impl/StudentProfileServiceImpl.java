package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentProfileResponse;
import vn.campuslife.model.StudentProfileUpdateRequest;
import vn.campuslife.repository.*;
import vn.campuslife.service.StudentProfileService;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final Logger logger = LoggerFactory.getLogger(StudentProfileServiceImpl.class);
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentClassRepository studentClassRepository;
    private final UploadProperties uploadProperties;
    private final UploadStorageService uploadStorageService;

    @Override
    @Transactional
    public Response createStudentProfile(Long userId) {
        try {
            Optional<Student> existingStudent = studentRepository.findByUserIdAndIsDeletedFalse(userId);
            if (existingStudent.isPresent()) {
                return new Response(true, "Student profile already exists", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response(false, "User not found", null);
            }

            User user = userOpt.get();

            if (user.getRole() != vn.campuslife.enumeration.Role.STUDENT) {
                return new Response(false, "User is not a student", null);
            }

            Student student = new Student();
            student.setUser(user);

            Student savedStudent = studentRepository.save(student);
            StudentProfileResponse response = toProfileResponse(savedStudent);

            logger.info("Created student profile for user: {}", user.getUsername());
            return new Response(true, "Student profile created successfully", response);
        } catch (Exception e) {
            logger.error("Failed to create student profile: {}", e.getMessage(), e);
            return new Response(false, "Failed to create student profile due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateStudentProfile(Long studentId, StudentProfileUpdateRequest request) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            Student student = studentOpt.get();

            student.setStudentCode(request.getStudentCode());
            student.setFullName(request.getFullName());
            student.setPhone(request.getPhone());
            student.setDob(request.getDob());
            student.setGender(request.getGender());

            applyAvatarUrlUpdate(student, request.getAvatarUrl());

            if (request.getDepartmentId() != null) {
                Optional<Department> deptOpt = departmentRepository.findById(request.getDepartmentId());
                if (deptOpt.isPresent()) {
                    student.setDepartment(deptOpt.get());
                } else {
                    return new Response(false, "Department not found", null);
                }
            }

            if (request.getClassId() != null) {
                Optional<StudentClass> classOpt = studentClassRepository.findById(request.getClassId());
                if (classOpt.isPresent()) {
                    student.setStudentClass(classOpt.get());
                } else {
                    return new Response(false, "Class not found", null);
                }
            }

            Student savedStudent = studentRepository.save(student);
            StudentProfileResponse response = toProfileResponse(savedStudent);

            return new Response(true, "Student profile updated successfully", response);
        } catch (Exception e) {
            logger.error("Failed to update student profile: {}", e.getMessage(), e);
            return new Response(false, "Failed to update student profile due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response uploadStudentAvatar(Long studentId, MultipartFile file) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            if (file == null || file.isEmpty()) {
                return new Response(false, "Please select an image to upload", null);
            }
            if (file.getSize() > MAX_AVATAR_BYTES) {
                return new Response(false, "File size must be less than 5MB", null);
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Response(false, "Only image files are allowed", null);
            }

            Student student = studentOpt.get();
            String avatarDirectory = uploadProperties.getPaths().getAvatars();
            String storedPath = uploadStorageService.store(file, avatarDirectory, true);

            replaceStoredAvatar(student, storedPath);
            Student savedStudent = studentRepository.save(student);

            return new Response(true, "Avatar uploaded successfully", toProfileResponse(savedStudent));
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to upload student avatar: {}", e.getMessage(), e);
            return new Response(false, "Failed to upload avatar due to server error", null);
        }
    }

    @Override
    public Response getStudentProfile(Long studentId) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            StudentProfileResponse response = toProfileResponse(studentOpt.get());
            return new Response(true, "Student profile retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to retrieve student profile: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve student profile due to server error", null);
        }
    }

    @Override
    public Response getStudentProfileByUsername(String username) {
        try {
            Optional<Student> studentOpt = studentRepository.findByUserUsernameAndIsDeletedFalse(username);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            StudentProfileResponse response = toProfileResponse(studentOpt.get());
            return new Response(true, "Student profile retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to retrieve student profile by username: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve student profile due to server error", null);
        }
    }

    private void applyAvatarUrlUpdate(Student student, String avatarUrl) {
        if (avatarUrl == null) {
            return;
        }

        String trimmed = avatarUrl.trim();
        if (trimmed.isEmpty()) {
            replaceStoredAvatar(student, null);
            return;
        }

        String relativePath = uploadStorageService.extractRelativePath(trimmed);
        replaceStoredAvatar(student, relativePath);
    }

    private void replaceStoredAvatar(Student student, String newRelativePath) {
        String oldPath = student.getAvatarUrl();
        String normalizedNew = (newRelativePath == null || newRelativePath.isBlank()) ? null : newRelativePath.trim();

        if (Objects.equals(oldPath, normalizedNew)) {
            student.setAvatarUrl(normalizedNew);
            return;
        }

        student.setAvatarUrl(normalizedNew);
        deleteAvatarQuietly(oldPath);
    }

    private void deleteAvatarQuietly(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        try {
            String relativePath = uploadStorageService.extractRelativePath(storedPath);
            uploadStorageService.delete(relativePath);
        } catch (IOException e) {
            logger.warn("Failed to delete old avatar {}: {}", storedPath, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Failed to delete old avatar {}: {}", storedPath, e.getMessage());
        }
    }

    private StudentProfileResponse toProfileResponse(Student student) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setId(student.getId());
        response.setUserId(student.getUser().getId());
        response.setUsername(student.getUser().getUsername());
        response.setEmail(student.getUser().getEmail());
        response.setStudentCode(student.getStudentCode());
        response.setFullName(student.getFullName());

        if (student.getStudentClass() != null) {
            response.setClassId(student.getStudentClass().getId());
            response.setClassName(student.getStudentClass().getClassName());
        }

        response.setPhone(student.getPhone());

        if (student.getAddress() != null) {
            Address address = student.getAddress();
            response.setAddress(buildFullAddress(address));
        }

        response.setDob(student.getDob());
        response.setAvatarUrl(uploadStorageService.toPublicUrl(student.getAvatarUrl()));
        response.setGender(student.getGender());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());

        if (student.getDepartment() != null) {
            response.setDepartmentId(student.getDepartment().getId());
            response.setDepartmentName(student.getDepartment().getName());
        }

        boolean isComplete = student.getStudentCode() != null
                && student.getFullName() != null
                && student.getDepartment() != null;
        response.setProfileComplete(isComplete);

        return response;
    }

    private String buildFullAddress(Address address) {
        StringBuilder fullAddress = new StringBuilder();

        if (address.getStreet() != null && !address.getStreet().trim().isEmpty()) {
            fullAddress.append(address.getStreet());
        }

        if (address.getWardName() != null && !address.getWardName().trim().isEmpty()) {
            if (fullAddress.length() > 0) {
                fullAddress.append(", ");
            }
            fullAddress.append(address.getWardName());
        }

        if (address.getProvinceName() != null && !address.getProvinceName().trim().isEmpty()) {
            if (fullAddress.length() > 0) {
                fullAddress.append(", ");
            }
            fullAddress.append(address.getProvinceName());
        }

        return fullAddress.toString();
    }
}
