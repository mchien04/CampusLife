package vn.campuslife.security.department;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.AllocationAdjustmentRequest;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.EmailHistory;
import vn.campuslife.entity.EventArticle;
import vn.campuslife.entity.Expense;
import vn.campuslife.entity.FundAdvance;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.Notification;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentClass;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.TaskSubmission;

import java.util.Set;

public final class DepartmentScopeSpec {

    private DepartmentScopeSpec() {
    }

    public static <T> Specification<T> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T> Specification<T> noRows() {
        return (root, query, cb) -> cb.disjunction();
    }

    public static Specification<Activity> activity(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Activity, Department> organizers = root.join("organizers");
            return cb.and(
                    cb.isFalse(root.get("isDeleted")),
                    organizers.get("id").in(deptIds));
        };
    }

    public static Specification<MiniGame> miniGame(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<MiniGame, Activity> activity = root.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<ActivitySeries> activitySeries(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<ActivitySeries, Department> targetDepartments = root.join("targetDepartments", JoinType.LEFT);

            Subquery<Long> childActivityExists = query.subquery(Long.class);
            Root<Activity> activity = childActivityExists.from(Activity.class);
            Join<Activity, Department> organizers = activity.join("organizers");
            childActivityExists.select(activity.get("id"))
                    .where(
                            cb.equal(activity.get("seriesId"), root.get("id")),
                            cb.isFalse(activity.get("isDeleted")),
                            organizers.get("id").in(deptIds));

            return cb.and(
                    cb.isFalse(root.get("isDeleted")),
                    cb.or(targetDepartments.get("id").in(deptIds), cb.exists(childActivityExists)));
        };
    }

    public static Specification<Student> student(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                root.get("department").get("id").in(deptIds));
    }

    public static Specification<StudentClass> studentClass(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                root.get("department").get("id").in(deptIds));
    }

    public static Specification<ActivityRegistration> activityRegistration(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("activity").get("isDeleted")),
                departmentSnapshotOrCurrentStudent(cb, root, "studentDepartmentAtRegistration", "student", deptIds));
    }

    public static Specification<ActivityParticipation> activityParticipation(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            Join<ActivityParticipation, ActivityRegistration> registration = root.join("registration");
            Join<ActivityParticipation, Department> participationSnapshot =
                    root.join("studentDepartmentAtParticipation", JoinType.LEFT);
            Join<ActivityRegistration, Department> registrationSnapshot =
                    registration.join("studentDepartmentAtRegistration", JoinType.LEFT);
            Join<ActivityRegistration, Student> student = registration.join("student", JoinType.LEFT);
            Join<Student, Department> studentDepartment = student.join("department", JoinType.LEFT);

            Predicate participationDept = participationSnapshot.get("id").in(deptIds);
            Predicate registrationDept = cb.and(
                    cb.isNull(participationSnapshot.get("id")),
                    registrationSnapshot.get("id").in(deptIds));
            Predicate currentDept = cb.and(
                    cb.isNull(participationSnapshot.get("id")),
                    cb.isNull(registrationSnapshot.get("id")),
                    studentDepartment.get("id").in(deptIds));

            return cb.and(
                    cb.isFalse(registration.get("activity").get("isDeleted")),
                    cb.or(participationDept, registrationDept, currentDept));
        };
    }

    public static Specification<ActivityTask> activityTask(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<ActivityTask, Activity> activity = root.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<TaskAssignment> taskAssignment(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<TaskAssignment, ActivityTask> task = root.join("task");
            Join<ActivityTask, Activity> activity = task.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(
                    cb.isFalse(activity.get("isDeleted")),
                    organizers.get("id").in(deptIds),
                    root.get("student").get("department").get("id").in(deptIds));
        };
    }

    public static Specification<TaskSubmission> taskSubmission(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<TaskSubmission, ActivityTask> task = root.join("task");
            Join<ActivityTask, Activity> activity = task.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(
                    cb.isFalse(root.get("isDeleted")),
                    cb.isFalse(activity.get("isDeleted")),
                    organizers.get("id").in(deptIds),
                    root.get("student").get("department").get("id").in(deptIds));
        };
    }

    public static Specification<ScoreEntry> scoreEntry(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> departmentSnapshotOrCurrentStudent(cb, root, "studentDepartmentAtAward", "student", deptIds);
    }

    public static Specification<StudentScore> studentScore(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> departmentSnapshotOrCurrentStudent(cb, root, "studentDepartmentAtAward", "student", deptIds);
    }

    public static Specification<PreparationTask> preparationTask(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<PreparationTask, Activity> activity = root.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<Expense> expense(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Expense, PreparationTask> task = root.join("task");
            Join<PreparationTask, Activity> activity = task.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<FundAdvance> fundAdvance(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<FundAdvance, PreparationTask> task = root.join("task");
            Join<PreparationTask, Activity> activity = task.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<AllocationAdjustmentRequest> allocationAdjustmentRequest(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<AllocationAdjustmentRequest, PreparationTask> task = root.join("task");
            Join<PreparationTask, Activity> activity = task.join("activity");
            Join<Activity, Department> organizers = activity.join("organizers");
            return cb.and(cb.isFalse(activity.get("isDeleted")), organizers.get("id").in(deptIds));
        };
    }

    public static Specification<EventArticle> eventArticle(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> root.get("ownerDepartment").get("id").in(deptIds);
    }

    public static Specification<EmailHistory> emailHistory(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EmailHistory, Department> targetDepartments = root.join("targetDepartments", JoinType.LEFT);
            return cb.or(
                    root.get("senderDepartment").get("id").in(deptIds),
                    root.get("recipientDepartmentAtSend").get("id").in(deptIds),
                    targetDepartments.get("id").in(deptIds));
        };
    }

    public static Specification<Notification> notification(Set<Long> deptIds) {
        if (isEmpty(deptIds)) {
            return noRows();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Notification, Department> targetDepartments = root.join("targetDepartments", JoinType.LEFT);
            return cb.and(
                    cb.isFalse(root.get("isDeleted")),
                    cb.or(
                            root.get("senderDepartment").get("id").in(deptIds),
                            targetDepartments.get("id").in(deptIds)));
        };
    }

    private static boolean isEmpty(Set<Long> deptIds) {
        return deptIds == null || deptIds.isEmpty();
    }

    private static <T> Predicate departmentSnapshotOrCurrentStudent(
            CriteriaBuilder cb,
            Root<T> root,
            String snapshotAttribute,
            String studentAttribute,
            Set<Long> deptIds) {
        Join<T, Department> snapshot = root.join(snapshotAttribute, JoinType.LEFT);
        Join<T, Student> student = root.join(studentAttribute, JoinType.LEFT);
        Join<Student, Department> studentDepartment = student.join("department", JoinType.LEFT);
        return cb.or(
                snapshot.get("id").in(deptIds),
                cb.and(cb.isNull(snapshot.get("id")), studentDepartment.get("id").in(deptIds)));
    }
}
