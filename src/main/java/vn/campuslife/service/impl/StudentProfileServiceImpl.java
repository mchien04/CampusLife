package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.StudentProfileService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final Logger logger = LoggerFactory.getLogger(StudentProfileServiceImpl.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public Response createStudentProfile(Long userId) {
        try {
            // Check if student profile already exists
            Optional<Student> existingStudent = studentRepository.findByUserIdAndIsDeletedFalse(userId);
            if (existingStudent.isPresent()) {
                return new Response(true, "Student profile already exists", null);
            }

            // Get user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response(false, "User not found", null);
            }

            User user = userOpt.get();

            // Check if user has STUDENT role
            if (user.getRole() != vn.campuslife.enumeration.Role.STUDENT) {
                return new Response(false, "User is not a student", null);
            }

            // Create student profile with only user_id linked
            Student student = new Student();
            student.setUser(user);
            // All other fields remain null - student will fill them later

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

            // Update student information
            student.setStudentCode(request.getStudentCode());
            student.setFullName(request.getFullName());
            // className is now handled through StudentClass entity
            // student.setClassName(request.getClassName());
            student.setPhone(request.getPhone());
            // Address is now handled separately through Address entity
            student.setDob(request.getDob());
            student.setAvatarUrl(request.getAvatarUrl());
            student.setGender(request.getGender());

            // Update department if provided
            if (request.getDepartmentId() != null) {
                Optional<Department> deptOpt = departmentRepository.findById(request.getDepartmentId());
                if (deptOpt.isPresent()) {
                    student.setDepartment(deptOpt.get());
                } else {
                    return new Response(false, "Department not found", null);
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

    private StudentProfileResponse toProfileResponse(Student student) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setId(student.getId());
        response.setUserId(student.getUser().getId());
        response.setUsername(student.getUser().getUsername());
        response.setEmail(student.getUser().getEmail());
        response.setStudentCode(student.getStudentCode());
        response.setFullName(student.getFullName());

        // Set class info if exists
        if (student.getStudentClass() != null) {
            response.setClassId(student.getStudentClass().getId());
            response.setClassName(student.getStudentClass().getClassName());
        }

        response.setPhone(student.getPhone());

        // Set address info if exists
        if (student.getAddress() != null) {
            Address address = student.getAddress();
            String fullAddress = buildFullAddress(address);
            response.setAddress(fullAddress);
        }

        response.setDob(student.getDob());
        response.setAvatarUrl(student.getAvatarUrl());
        response.setGender(student.getGender());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());

        // Set department info if exists
        if (student.getDepartment() != null) {
            response.setDepartmentId(student.getDepartment().getId());
            response.setDepartmentName(student.getDepartment().getName());
        }

        // Check if profile is complete
        boolean isComplete = student.getStudentCode() != null &&
                student.getFullName() != null &&
                student.getDepartment() != null;
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
