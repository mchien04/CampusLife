package vn.campuslife.service;

import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.util.UserSoftDeleteSupport;

/**
 * Ensures username/email/studentCode checks ignore soft-deleted records and
 * reclaims identifiers still held by legacy soft-deleted rows.
 */
public final class UserUniquenessHelper {

    private UserUniquenessHelper() {
    }

    public static void reclaimDeletedIdentifiers(UserRepository userRepository, String username, String email) {
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username.trim()).ifPresent(user -> {
                if (user.isDeleted()) {
                    UserSoftDeleteSupport.releaseUserUniqueFields(user);
                    userRepository.save(user);
                }
            });
        }
        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email.trim()).ifPresent(user -> {
                if (user.isDeleted()) {
                    UserSoftDeleteSupport.releaseUserUniqueFields(user);
                    userRepository.save(user);
                }
            });
        }
    }

    public static void reclaimDeletedStudentCode(StudentRepository studentRepository,
            UserRepository userRepository, String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            return;
        }
        studentRepository.findByStudentCode(studentCode.trim()).ifPresent(student -> {
            if (!student.isDeleted()) {
                return;
            }
            UserSoftDeleteSupport.releaseStudentCode(student);
            studentRepository.save(student);
            if (student.getUser() != null && student.getUser().isDeleted()) {
                UserSoftDeleteSupport.releaseUserUniqueFields(student.getUser());
                userRepository.save(student.getUser());
            }
        });
    }
}
