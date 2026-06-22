package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityScoreRule;
import vn.campuslife.entity.Semester;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.service.ScoreSemesterResolver;
import vn.campuslife.service.SemesterHelperService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScoreSemesterResolverImpl implements ScoreSemesterResolver {

    private final SemesterHelperService semesterHelperService;
    private final SemesterRepository semesterRepository;

    @Override
    public Semester resolveSemester(Activity activity, ActivityScoreRule rule, LocalDateTime eventTime) {
        switch (rule.getSemesterPolicy()) {
            case EXPLICIT_SEMESTER:
                if (rule.getExplicitSemester() != null) {
                    return rule.getExplicitSemester();
                }
                throw new IllegalStateException("Explicit semester is required");
            case ACTIVITY_SEMESTER:
            default:
                if (eventTime != null) {
                    Semester sem = semesterHelperService.getSemesterForDate(eventTime.toLocalDate());
                    if (sem != null) {
                        return sem;
                    }
                }
                return semesterHelperService.getSemesterForActivity(activity);
        }
    }
}
