package vn.campuslife.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Semester;
import vn.campuslife.repository.SemesterRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterHelperService {

    private final SemesterRepository semesterRepository;

    /**
     * Tìm semester dựa vào thời gian diễn ra activity
     * Logic: Dùng startDate của activity để xác định semester
     * Nếu không tìm thấy, fallback về semester đang mở
     */
    public Semester getSemesterForActivity(Activity activity) {
        if (activity == null) {
            return getCurrentOpenSemester();
        }

        // Ưu tiên: Dùng startDate của activity
        LocalDateTime activityStartDate = activity.getStartDate();
        if (activityStartDate != null) {
            Optional<Semester> semesterOpt = semesterRepository.findByDateTime(activityStartDate);
            if (semesterOpt.isPresent()) {
                log.debug("Found semester {} for activity {} based on startDate {}",
                        semesterOpt.get().getId(), activity.getId(), activityStartDate);
                return semesterOpt.get();
            }
        }

        // Fallback: Dùng endDate nếu startDate không tìm thấy
        LocalDateTime activityEndDate = activity.getEndDate();
        if (activityEndDate != null) {
            Optional<Semester> semesterOpt = semesterRepository.findByDateTime(activityEndDate);
            if (semesterOpt.isPresent()) {
                log.debug("Found semester {} for activity {} based on endDate {}",
                        semesterOpt.get().getId(), activity.getId(), activityEndDate);
                return semesterOpt.get();
            }
        }

        // Fallback cuối cùng: Dùng semester đang mở
        log.warn("Could not find semester for activity {} (startDate: {}, endDate: {}). " +
                "Using current open semester as fallback.",
                activity.getId(), activityStartDate, activityEndDate);
        return getCurrentOpenSemester();
    }

    /**
     * Tìm semester dựa vào một ngày cụ thể
     */
    public Semester getSemesterForDate(LocalDate date) {
        if (date == null) {
            return getCurrentOpenSemester();
        }

        Optional<Semester> semesterOpt = semesterRepository.findByDate(date);
        if (semesterOpt.isPresent()) {
            return semesterOpt.get();
        }

        log.warn("Could not find semester for date {}. Using current open semester as fallback.", date);
        return getCurrentOpenSemester();
    }

    /**
     * Lấy semester đang mở (fallback)
     */
    private Semester getCurrentOpenSemester() {
        return semesterRepository.findAll().stream()
                .filter(Semester::isOpen)
                .findFirst()
                .orElse(semesterRepository.findAll().stream()
                        .findFirst()
                        .orElse(null));
    }
}

