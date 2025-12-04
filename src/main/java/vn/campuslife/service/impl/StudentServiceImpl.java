package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Student;
import vn.campuslife.model.Response;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.StudentService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public Long getStudentIdByUsername(String username) {
        Optional<Student> studentOpt = studentRepository
                .findByUserUsernameAndIsDeletedFalse(username);
        return studentOpt.map(Student::getId).orElse(null);
    }

    @Override
    public Long getStudentIdByUserId(Long userId) {
        Optional<Student> studentOpt = studentRepository
                .findByUserIdAndIsDeletedFalse(userId);
        return studentOpt.map(Student::getId).orElse(null);
    }

    @Override
    public Response getAllStudents(Pageable pageable) {
        try {
            Page<Student> students = studentRepository.findByIsDeletedFalse(pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("content", students.getContent());
            result.put("totalElements", students.getTotalElements());
            result.put("totalPages", students.getTotalPages());
            result.put("size", students.getSize());
            result.put("number", students.getNumber());
            result.put("first", students.isFirst());
            result.put("last", students.isLast());

            return new Response(true, "Students retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students: " + e.getMessage(), null);
        }
    }

    @Override
    public Response searchStudents(String keyword, Pageable pageable) {
        try {
            // Tìm kiếm theo cả tên và mã sinh viên
            Page<Student> students = studentRepository.searchByFullNameOrStudentCode(keyword, pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("content", students.getContent());
            result.put("totalElements", students.getTotalElements());
            result.put("totalPages", students.getTotalPages());
            result.put("size", students.getSize());
            result.put("number", students.getNumber());
            result.put("first", students.isFirst());
            result.put("last", students.isLast());

            return new Response(true, "Search completed successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to search students: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsWithoutClass(Pageable pageable) {
        try {
            Page<Student> students = studentRepository.findByStudentClassIsNullAndIsDeletedFalse(pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("content", students.getContent());
            result.put("totalElements", students.getTotalElements());
            result.put("totalPages", students.getTotalPages());
            result.put("size", students.getSize());
            result.put("number", students.getNumber());
            result.put("first", students.isFirst());
            result.put("last", students.isLast());

            return new Response(true, "Students without class retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students without class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsByDepartment(Long departmentId, Pageable pageable) {
        try {
            Page<Student> students = studentRepository.findByStudentClassDepartmentIdAndIsDeletedFalse(
                    departmentId, pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("content", students.getContent());
            result.put("totalElements", students.getTotalElements());
            result.put("totalPages", students.getTotalPages());
            result.put("size", students.getSize());
            result.put("number", students.getNumber());
            result.put("first", students.isFirst());
            result.put("last", students.isLast());

            return new Response(true, "Students by department retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students by department: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentById(Long studentId) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            return new Response(true, "Student retrieved successfully", studentOpt.get());
        } catch (Exception e) {
            return new Response(false, "Failed to get student: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentByUsername(String username) {
        try {
            Optional<Student> studentOpt = studentRepository.findByUserUsernameAndIsDeletedFalse(username);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            return new Response(true, "Student retrieved successfully", studentOpt.get());
        } catch (Exception e) {
            return new Response(false, "Failed to get student: " + e.getMessage(), null);
        }
    }
}