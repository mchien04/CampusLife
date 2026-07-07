package vn.campuslife.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import vn.campuslife.entity.AcademicYear;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.entity.User;
import vn.campuslife.entity.UserDepartment;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.DepartmentType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class UserDepartmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserDepartmentRepository userDepartmentRepository;

    @Test
    void findActiveDepartmentIdsByUserId_ReturnsOnlyNonDeletedDepartments() {
        User manager = persistUser("manager", Role.MANAGER);
        Department khoa = persistDepartment("Khoa CNTT", DepartmentType.KHOA, false);
        Department phongBan = persistDepartment("Phong CTCT", DepartmentType.PHONG_BAN, false);
        Department deleted = persistDepartment("Deleted", DepartmentType.KHOA, true);

        entityManager.persist(new UserDepartment(manager, khoa, null));
        entityManager.persist(new UserDepartment(manager, phongBan, null));
        entityManager.persist(new UserDepartment(manager, deleted, null));
        entityManager.flush();

        Set<Long> departmentIds = userDepartmentRepository.findActiveDepartmentIdsByUserId(manager.getId());

        assertEquals(Set.of(khoa.getId(), phongBan.getId()), departmentIds);
    }

    @Test
    void persistHistoricalEntities_CapturesStudentDepartmentSnapshots() {
        Department department = persistDepartment("Khoa CNTT", DepartmentType.KHOA, false);
        User studentUser = persistUser("student", Role.STUDENT);
        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentCode("SV001");
        student.setDepartment(department);
        entityManager.persist(student);

        Activity activity = new Activity();
        activity.setName("Department Snapshot Activity");
        activity.setType(ActivityType.SUKIEN);
        entityManager.persist(activity);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setStudent(student);
        registration.setStatus(RegistrationStatus.APPROVED);
        entityManager.persist(registration);

        ActivityParticipation participation = new ActivityParticipation();
        participation.setRegistration(registration);
        participation.setParticipationType(ParticipationType.COMPLETED);
        entityManager.persist(participation);

        Semester semester = persistSemester();

        ScoreEntry scoreEntry = new ScoreEntry();
        scoreEntry.setStudent(student);
        scoreEntry.setSemester(semester);
        scoreEntry.setScoreType(ScoreType.REN_LUYEN);
        scoreEntry.setSourceType(ScoreEntrySourceType.ACTIVITY_PARTICIPATION);
        scoreEntry.setSourceId(1L);
        scoreEntry.setPoints(BigDecimal.TEN);
        scoreEntry.setStatus(ScoreEntryStatus.ACTIVE);
        entityManager.persist(scoreEntry);

        StudentScore studentScore = new StudentScore();
        studentScore.setStudent(student);
        studentScore.setSemester(semester);
        studentScore.setScoreType(ScoreType.REN_LUYEN);
        studentScore.setScore(BigDecimal.TEN);
        entityManager.persist(studentScore);
        entityManager.flush();

        assertEquals(department.getId(), registration.getStudentDepartmentAtRegistration().getId());
        assertEquals(department.getId(), participation.getStudentDepartmentAtParticipation().getId());
        assertEquals(department.getId(), scoreEntry.getStudentDepartmentAtAward().getId());
        assertEquals(department.getId(), studentScore.getStudentDepartmentAtAward().getId());
    }

    private User persistUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@campuslife.test");
        user.setPassword("password");
        user.setRole(role);
        user.setActivated(true);
        entityManager.persist(user);
        return user;
    }

    private Department persistDepartment(String name, DepartmentType type, boolean deleted) {
        Department department = new Department();
        department.setName(name);
        department.setType(type);
        department.setDeleted(deleted);
        entityManager.persist(department);
        return department;
    }

    private Semester persistSemester() {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setName("2025-2026");
        academicYear.setStartDate(LocalDate.of(2025, 9, 1));
        academicYear.setEndDate(LocalDate.of(2026, 8, 31));
        entityManager.persist(academicYear);

        Semester semester = new Semester();
        semester.setYear(academicYear);
        semester.setName("HK1");
        entityManager.persist(semester);
        return semester;
    }
}
