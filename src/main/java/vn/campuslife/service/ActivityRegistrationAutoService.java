package vn.campuslife.service;

import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;

import java.util.Set;

/**
 * Auto-registers eligible students for a (non-draft) activity, based on
 * {@code isImportant} and {@code mandatoryForFacultyStudents} flags, then sends
 * notifications. Idempotent — students who already have a registration are
 * skipped.
 *
 * <p>Extracted from {@code ActivityServiceImpl} so the same logic can be reused
 * by {@code StandardActivityServiceImpl}, {@code MinigameActivityServiceImpl},
 * and any future activity-creation flow without duplication.
 */
public interface ActivityRegistrationAutoService {

    /**
     * Auto-register students based on the activity's flags.
     *
     * <p>No-op when the activity is a draft, or when neither
     * {@code isImportant} nor {@code mandatoryForFacultyStudents} is set, or
     * when all eligible students already have a registration for this activity.
     * Never throws — failures are logged and swallowed so that activity
     * create/update cannot fail because of auto-registration.
     *
     * @param activity the activity (must be a managed/persisted entity; the
     *                 implementations reads flags, organizers, and seriesId)
     */
    void autoRegisterStudents(Activity activity);

    /**
     * Same as {@link #autoRegisterStudents(Activity)} but lets the caller override
     * the auto-register flags and organizer departments, instead of reading them
     * from the activity entity. Used by series create/update to register students
     * based on the series' own {@code isImportant} / {@code mandatoryForFacultyStudents}
     * flags and {@code targetDepartments}, rather than the per-activity flags
     * (which are always {@code false} on series child activities).
     *
     * @param activity              the activity to register students for
     * @param isImportant           if true, register every active student
     * @param mandatoryForFaculty   if true, register students of the given departments
     * @param organizerDepartments  departments whose students are mandatory (ignored when
     *                              mandatoryForFaculty is false)
     */
    void autoRegisterStudents(Activity activity, boolean isImportant, boolean mandatoryForFaculty,
            Set<Department> organizerDepartments);
}