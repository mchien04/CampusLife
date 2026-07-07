package vn.campuslife.security.department;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.DepartmentType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.repository.StudentRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class DepartmentScopeSpecRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ActivityRegistrationRepository registrationRepository;

    @Autowired
    private EventArticleRepository eventArticleRepository;

    @Test
    void activitySpec_FiltersByOrganizerDepartments() {
        Department managerDept = persistDepartment("Manager Dept");
        Department otherDept = persistDepartment("Other Dept");
        Activity managerActivity = persistActivity("Manager Activity", managerDept);
        persistActivity("Other Activity", otherDept);

        List<Activity> activities = activityRepository.findAll(
                DepartmentScopeSpec.activity(Set.of(managerDept.getId())));

        assertEquals(List.of(managerActivity.getId()), activities.stream().map(Activity::getId).toList());
    }

    @Test
    void studentSpec_FiltersByStudentDepartment() {
        Department managerDept = persistDepartment("Manager Dept");
        Department otherDept = persistDepartment("Other Dept");
        Student managerStudent = persistStudent("manager-student", managerDept);
        persistStudent("other-student", otherDept);

        List<Student> students = studentRepository.findAll(
                DepartmentScopeSpec.student(Set.of(managerDept.getId())));

        assertEquals(List.of(managerStudent.getId()), students.stream().map(Student::getId).toList());
    }

    @Test
    void registrationSpec_UsesSnapshotBeforeCurrentDepartmentFallback() {
        Department managerDept = persistDepartment("Manager Dept");
        Department otherDept = persistDepartment("Other Dept");
        Student movedStudent = persistStudent("moved-student", otherDept);
        Activity activity = persistActivity("Snapshot Activity", managerDept);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setStudent(movedStudent);
        registration.setStudentDepartmentAtRegistration(managerDept);
        registration.setStatus(RegistrationStatus.APPROVED);
        entityManager.persist(registration);
        entityManager.flush();

        List<ActivityRegistration> registrations = registrationRepository.findAll(
                DepartmentScopeSpec.activityRegistration(Set.of(managerDept.getId())));

        assertEquals(List.of(registration.getId()), registrations.stream().map(ActivityRegistration::getId).toList());
    }

    @Test
    void eventArticleSpec_FiltersByOwnerDepartment() {
        Department managerDept = persistDepartment("Manager Dept");
        Department otherDept = persistDepartment("Other Dept");
        EventArticle managerArticle = persistArticle("manager-article", managerDept);
        persistArticle("other-article", otherDept);

        List<EventArticle> articles = eventArticleRepository.findAll(
                DepartmentScopeSpec.eventArticle(Set.of(managerDept.getId())));

        assertEquals(List.of(managerArticle.getId()), articles.stream().map(EventArticle::getId).toList());
    }

    private Department persistDepartment(String name) {
        Department department = new Department();
        department.setName(name);
        department.setType(DepartmentType.KHOA);
        entityManager.persist(department);
        return department;
    }

    private Activity persistActivity(String name, Department department) {
        Activity activity = new Activity();
        activity.setName(name);
        activity.setType(ActivityType.SUKIEN);
        activity.getOrganizers().add(department);
        entityManager.persist(activity);
        return activity;
    }

    private Student persistStudent(String username, Department department) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@campuslife.test");
        user.setPassword("password");
        user.setRole(Role.STUDENT);
        user.setActivated(true);
        entityManager.persist(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(username.toUpperCase());
        student.setDepartment(department);
        entityManager.persist(student);
        return student;
    }

    private EventArticle persistArticle(String slug, Department ownerDepartment) {
        EventArticle article = new EventArticle();
        article.setTitle(slug);
        article.setSlug(slug);
        article.setContent("content");
        article.setOwnerDepartment(ownerDepartment);
        entityManager.persist(article);
        return article;
    }
}
