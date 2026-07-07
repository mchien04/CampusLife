package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Student;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentListResponse;
import vn.campuslife.model.StudentResponse;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.StudentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentAuthorizationService departmentAuthorizationService;

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
        return getAllStudents(pageable, null);
    }

    @Override
    public Response getAllStudents(Pageable pageable, DepartmentScope scope) {
        try {
            Page<Student> students = scope != null && scope.manager()
                    ? studentRepository.findAll(DepartmentScopeSpec.student(scope.departmentIds()), pageable)
                    : studentRepository.findByIsDeletedFalse(pageable);

            Map<String, Object> result = pageResult(students);

            return new Response(true, "Students retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students: " + e.getMessage(), null);
        }
    }

    @Override
    public Response searchStudents(String keyword, Pageable pageable) {
        return searchStudents(keyword, pageable, null);
    }

    @Override
    public Response searchStudents(String keyword, Pageable pageable, DepartmentScope scope) {
        try {
            // Tìm kiếm theo cả tên và mã sinh viên
            Page<Student> students;
            if (scope != null && scope.manager()) {
                Specification<Student> spec = DepartmentScopeSpec.student(scope.departmentIds())
                        .and((root, query, cb) -> cb.or(
                                cb.like(cb.lower(root.get("fullName")), "%" + keyword.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("studentCode")), "%" + keyword.toLowerCase() + "%")));
                students = studentRepository.findAll(spec, pageable);
            } else {
                students = studentRepository.searchByFullNameOrStudentCode(keyword, pageable);
            }

            Map<String, Object> result = pageResult(students);

            return new Response(true, "Search completed successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to search students: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsWithoutClass(Pageable pageable) {
        return getStudentsWithoutClass(pageable, null);
    }

    @Override
    public Response getStudentsWithoutClass(Pageable pageable, DepartmentScope scope) {
        try {
            Page<Student> students;
            if (scope != null && scope.manager()) {
                students = studentRepository.findAll(
                        DepartmentScopeSpec.student(scope.departmentIds())
                                .and((root, query, cb) -> cb.isNull(root.get("studentClass"))),
                        pageable);
            } else {
                students = studentRepository.findByStudentClassIsNullAndIsDeletedFalse(pageable);
            }

            Map<String, Object> result = pageResult(students);

            return new Response(true, "Students without class retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students without class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsByDepartment(Long departmentId, Pageable pageable) {
        return getStudentsByDepartment(departmentId, pageable, null);
    }

    @Override
    public Response getStudentsByDepartment(Long departmentId, Pageable pageable, DepartmentScope scope) {
        try {
            Set<Long> departmentFilter = scope != null && scope.manager()
                    ? departmentAuthorizationService.managerDepartmentFilter(scope, departmentId)
                    : Set.of(departmentId);
            Page<Student> students = studentRepository.findAll(DepartmentScopeSpec.student(departmentFilter), pageable);

            // Convert to StudentResponse
            List<StudentResponse> studentResponses = students.getContent().stream()
                    .map(StudentResponse::fromEntity)
                    .collect(Collectors.toList());

            StudentListResponse result = new StudentListResponse();
            result.setContent(studentResponses);
            result.setTotalElements(students.getTotalElements());
            result.setTotalPages(students.getTotalPages());
            result.setSize(students.getSize());
            result.setNumber(students.getNumber());
            result.setFirst(students.isFirst());
            result.setLast(students.isLast());

            return new Response(true, "Students by department retrieved successfully", result);
        } catch (Exception e) {
            return new Response(false, "Failed to get students by department: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentById(Long studentId) {
        return getStudentById(studentId, null);
    }

    @Override
    public Response getStudentById(Long studentId, DepartmentScope scope) {
        try {
            if (scope != null) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }
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
        return getStudentByUsername(username, null);
    }

    @Override
    public Response getStudentByUsername(String username, DepartmentScope scope) {
        try {
            Optional<Student> studentOpt = studentRepository.findByUserUsernameAndIsDeletedFalse(username);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            if (scope != null) {
                departmentAuthorizationService.requireStudentAccess(studentOpt.get().getId(), scope);
            }

            return new Response(true, "Student retrieved successfully", studentOpt.get());
        } catch (Exception e) {
            return new Response(false, "Failed to get student: " + e.getMessage(), null);
        }
    }

    private Map<String, Object> pageResult(Page<Student> students) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", students.getContent());
        result.put("totalElements", students.getTotalElements());
        result.put("totalPages", students.getTotalPages());
        result.put("size", students.getSize());
        result.put("number", students.getNumber());
        result.put("first", students.isFirst());
        result.put("last", students.isLast());
        return result;
    }
}