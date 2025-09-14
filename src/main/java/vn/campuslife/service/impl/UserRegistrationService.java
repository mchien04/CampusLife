package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.User;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.Role;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.repository.StudentRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /**
     * Tự động tạo Student profile khi user được tạo với role STUDENT
     */
    @Transactional
    public void handleUserRegistration(User user) {
        if (user.getRole() == Role.STUDENT) {
            // Check if student profile already exists
            Optional<Student> existingStudent = studentRepository.findByUserIdAndIsDeletedFalse(user.getId());
            if (existingStudent.isEmpty()) {
                // Create student profile with only user_id linked
                Student student = new Student();
                student.setUser(user);
                // All other fields remain null - student will fill them later

                studentRepository.save(student);
                System.out.println("Auto-created student profile for user: " + user.getUsername());
            }
        }
    }
}
