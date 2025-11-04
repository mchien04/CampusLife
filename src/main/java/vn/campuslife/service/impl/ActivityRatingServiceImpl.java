package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.ActivityRating;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRatingRepository;
import vn.campuslife.service.ActivityRatingService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRatingServiceImpl implements ActivityRatingService {

    private final ActivityRatingRepository activityRatingRepository;

    @Override
    public List<ActivityRating> getActivityRatingByActivityId(Long activityId) {
        return activityRatingRepository.findByActivity_Id(activityId);
    }

    @Override
    public Optional<ActivityRating> getActivityRatingByActivityIdAndStudent(Long activityId, Long studentId) {
        return activityRatingRepository.findByActivity_IdAndStudent_Id(activityId, studentId);
    }

   @Override
    public Response createActivityRating(Long activityId, Long studentId, float rating, String comment) {

        Optional<ActivityRating> existing = activityRatingRepository
                .findByActivity_IdAndStudent_Id(activityId, studentId);

        if (existing.isPresent()) {
            return new Response(false, "You have already rated this event.", null);
        }


        ActivityRating newRating = new ActivityRating();
        newRating.setRating(rating);
        newRating.setComment(comment);
        newRating.setCreatedAt(java.time.LocalDateTime.now());


        vn.campuslife.entity.Activity activity = new vn.campuslife.entity.Activity();
        activity.setId(activityId);
        newRating.setActivity(activity);

        vn.campuslife.entity.Student student = new vn.campuslife.entity.Student();
        student.setId(studentId);
        newRating.setStudent(student);


        ActivityRating saved = activityRatingRepository.save(newRating);

        Double avg = activityRatingRepository.findAverageRatingByActivityId(activityId);


        return new Response(true, "Rating submitted successfully.", saved);
    }
    @Override
    public Response getActivityRatingStats(Long activityId) {
        List<ActivityRating> ratings = activityRatingRepository.findByActivity_Id(activityId);

        if (ratings.isEmpty()) {
            return new Response(true, "No ratings yet", Map.of(
                    "average", 0,
                    "count", 0,
                    "distribution", Map.of(1, 0, 2, 0, 3, 0, 4, 0, 5, 0),
                    "students", List.of()
            ));
        }

        double avg = ratings.stream()
                .mapToDouble(ActivityRating::getRating)
                .average().orElse(0);

        Map<Integer, Long> distribution = ratings.stream()
                .collect(Collectors.groupingBy(r -> Math.round(r.getRating()), Collectors.counting()));

        List<Map<String, Object>> students = ratings.stream()
                .map(r -> Map.<String, Object>of(
                        "studentName", r.getStudent().getFullName(),
                        "studentCode", r.getStudent().getStudentCode(),
                        "rating", r.getRating(),
                        "comment", r.getComment()
                ))
                .collect(Collectors.toList());

        return new Response(true, "Rating stats retrieved", Map.of(
                "average", avg,
                "count", ratings.size(),
                "distribution", distribution,
                "students", students
        ));
    }



}
