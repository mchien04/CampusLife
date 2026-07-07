package vn.campuslife.util;

import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;

/**
 * Releases unique {@code users}/{@code students} columns when soft-deleting so
 * email, username and student code can be reused for new accounts.
 */
public final class UserSoftDeleteSupport {

    private static final String DELETED_MARKER = "__deleted__";
    private static final int USERNAME_MAX = 255;
    private static final int EMAIL_MAX = 255;
    private static final int STUDENT_CODE_MAX = 255;

    private UserSoftDeleteSupport() {
    }

    public static void softDelete(User user) {
        releaseUserUniqueFields(user);
        user.setDeleted(true);
    }

    public static void softDelete(Student student) {
        releaseStudentCode(student);
        student.setDeleted(true);
        if (student.getUser() != null) {
            softDelete(student.getUser());
        }
    }

    public static void releaseUserUniqueFields(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String suffix = DELETED_MARKER + user.getId();
        if (user.getUsername() != null && !user.getUsername().contains(DELETED_MARKER)) {
            user.setUsername(truncate(user.getUsername() + suffix, USERNAME_MAX));
        }
        if (user.getEmail() != null && !user.getEmail().contains(DELETED_MARKER)) {
            user.setEmail(truncate(releaseEmail(user.getEmail(), user.getId()), EMAIL_MAX));
        }
    }

    public static void releaseStudentCode(Student student) {
        if (student == null || student.getId() == null || student.getStudentCode() == null) {
            return;
        }
        String suffix = DELETED_MARKER + student.getId();
        if (!student.getStudentCode().contains(DELETED_MARKER)) {
            student.setStudentCode(truncate(student.getStudentCode() + suffix, STUDENT_CODE_MAX));
        }
    }

    private static String releaseEmail(String email, Long userId) {
        int at = email.indexOf('@');
        if (at > 0) {
            return email.substring(0, at) + "+deleted" + userId + email.substring(at);
        }
        return "deleted+" + userId + "+" + email;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
