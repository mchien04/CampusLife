package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.campuslife.entity.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.StudentClassService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassServiceImpl implements StudentClassService {

    private static final Logger logger = LoggerFactory.getLogger(StudentClassServiceImpl.class);

    private final StudentClassRepository studentClassRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public Response createClass(String className, String description, Long departmentId) {
        try {
            // Validate department exists
            Optional<Department> departmentOpt = departmentRepository.findById(departmentId);
            if (departmentOpt.isEmpty()) {
                return new Response(false, "Department not found", null);
            }

            // Check if class name already exists
            Optional<StudentClass> existingClass = studentClassRepository.findByClassNameAndIsDeletedFalse(className);
            if (existingClass.isPresent()) {
                return new Response(false, "Class name already exists", null);
            }

            // Create new class
            StudentClass studentClass = new StudentClass();
            studentClass.setClassName(className);
            studentClass.setDescription(description);
            studentClass.setDepartment(departmentOpt.get());

            StudentClass savedClass = studentClassRepository.save(studentClass);
            return new Response(true, "Class created successfully", savedClass);
        } catch (Exception e) {
            logger.error("Failed to create class: {}", e.getMessage(), e);
            return new Response(false, "Failed to create class: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response updateClass(Long classId, String className, String description) {
        try {
            Optional<StudentClass> classOpt = studentClassRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            StudentClass studentClass = classOpt.get();

            // Check if new class name already exists (excluding current class)
            if (!studentClass.getClassName().equals(className)) {
                Optional<StudentClass> existingClass = studentClassRepository
                        .findByClassNameAndIsDeletedFalse(className);
                if (existingClass.isPresent() && !existingClass.get().getId().equals(classId)) {
                    return new Response(false, "Class name already exists", null);
                }
            }

            studentClass.setClassName(className);
            studentClass.setDescription(description);

            StudentClass savedClass = studentClassRepository.save(studentClass);
            return new Response(true, "Class updated successfully", savedClass);
        } catch (Exception e) {
            logger.error("Failed to update class: {}", e.getMessage(), e);
            return new Response(false, "Failed to update class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getAllClasses() {
        try {
            List<StudentClass> classes = studentClassRepository.findByIsDeletedFalse();
            return new Response(true, "Classes retrieved successfully", classes);
        } catch (Exception e) {
            logger.error("Failed to get all classes: {}", e.getMessage(), e);
            return new Response(false, "Failed to get classes: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getClassesByDepartment(Long departmentId) {
        try {
            List<StudentClass> classes = studentClassRepository.findByDepartmentIdAndIsDeletedFalse(departmentId);
            return new Response(true, "Classes retrieved successfully", classes);
        } catch (Exception e) {
            logger.error("Failed to get classes by department: {}", e.getMessage(), e);
            return new Response(false, "Failed to get classes: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getClassById(Long classId) {
        try {
            Optional<StudentClass> classOpt = studentClassRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            return new Response(true, "Class retrieved successfully", classOpt.get());
        } catch (Exception e) {
            logger.error("Failed to get class by ID: {}", e.getMessage(), e);
            return new Response(false, "Failed to get class: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response deleteClass(Long classId) {
        try {
            Optional<StudentClass> classOpt = studentClassRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            StudentClass studentClass = classOpt.get();
            studentClass.setDeleted(true);
            studentClassRepository.save(studentClass);

            return new Response(true, "Class deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete class: {}", e.getMessage(), e);
            return new Response(false, "Failed to delete class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsInClass(Long classId) {
        try {
            // Validate class exists
            Optional<StudentClass> classOpt = studentClassRepository.findByIdAndIsDeletedFalse(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            List<Student> students = studentRepository.findByClassId(classId);

            // Convert to DTO to avoid circular reference
            List<StudentResponse> studentResponses = students.stream()
                    .map(StudentResponse::fromEntity)
                    .collect(Collectors.toList());

            return new Response(true, "Students retrieved successfully", studentResponses);
        } catch (Exception e) {
            logger.error("Failed to get students in class: {}", e.getMessage(), e);
            return new Response(false, "Failed to get students: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentsInClass(Long classId, Pageable pageable) {
        try {
            // Validate class exists
            Optional<StudentClass> classOpt = studentClassRepository.findByIdAndIsDeletedFalse(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            Page<Student> students = studentRepository.findByStudentClassIdAndIsDeletedFalse(classId, pageable);

            // Convert to DTO
            Page<StudentResponse> studentResponses = students.map(StudentResponse::fromEntity);

            Map<String, Object> result = new HashMap<>();
            result.put("content", studentResponses.getContent());
            result.put("totalElements", studentResponses.getTotalElements());
            result.put("totalPages", studentResponses.getTotalPages());
            result.put("size", studentResponses.getSize());
            result.put("number", studentResponses.getNumber());
            result.put("first", studentResponses.isFirst());
            result.put("last", studentResponses.isLast());

            return new Response(true, "Students retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to get students in class: {}", e.getMessage(), e);
            return new Response(false, "Failed to get students: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response addStudentToClass(Long classId, Long studentId) {
        try {
            // Validate class exists
            Optional<StudentClass> classOpt = studentClassRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            Student student = studentOpt.get();
            student.setStudentClass(classOpt.get());
            studentRepository.save(student);

            return new Response(true, "Student added to class successfully", null);
        } catch (Exception e) {
            logger.error("Failed to add student to class: {}", e.getMessage(), e);
            return new Response(false, "Failed to add student to class: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response removeStudentFromClass(Long classId, Long studentId) {
        try {
            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            Student student = studentOpt.get();
            if (student.getStudentClass() == null || !student.getStudentClass().getId().equals(classId)) {
                return new Response(false, "Student is not in this class", null);
            }

            student.setStudentClass(null);
            studentRepository.save(student);

            return new Response(true, "Student removed from class successfully", null);
        } catch (Exception e) {
            logger.error("Failed to remove student from class: {}", e.getMessage(), e);
            return new Response(false, "Failed to remove student from class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getClassByName(String className) {
        try {
            Optional<StudentClass> classOpt = studentClassRepository.findByClassNameAndIsDeletedFalse(className);
            if (classOpt.isEmpty()) {
                return new Response(false, "Class not found", null);
            }

            return new Response(true, "Class retrieved successfully", classOpt.get());
        } catch (Exception e) {
            logger.error("Failed to get class by name: {}", e.getMessage(), e);
            return new Response(false, "Failed to get class: " + e.getMessage(), null);
        }
    }

}
