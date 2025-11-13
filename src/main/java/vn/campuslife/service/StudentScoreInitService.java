package vn.campuslife.service;

import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentScoreInitService {

    private final StudentScoreRepository studentScoreRepository;
    private final SemesterRepository semesterRepository;

    /**
     * Initialize 3 score records (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE) for a
     * student in a semester
     */
    @Transactional
    public void initializeStudentScores(Student student, Semester semester) {
        try {
            log.info("Initializing scores for student {} in semester {}", student.getId(), semester.getId());

            // Get the 3 score types
            ScoreType[] scoreTypes = new ScoreType[] {
                    ScoreType.REN_LUYEN,
                    ScoreType.CONG_TAC_XA_HOI,
                    ScoreType.CHUYEN_DE
            };

            for (ScoreType scoreType : scoreTypes) {
                // Check if already exists
                Optional<StudentScore> existing = studentScoreRepository
                        .findByStudentIdAndSemesterIdAndScoreType(student.getId(), semester.getId(), scoreType);

                if (existing.isPresent()) {
                    log.info("Score {} already exists for student {} in semester {}",
                            scoreType, student.getId(), semester.getId());
                    continue;
                }

                // Create new score record
                StudentScore score = new StudentScore();
                score.setStudent(student);
                score.setSemester(semester);
                score.setScoreType(scoreType);
                score.setScore(BigDecimal.ZERO);
                score.setActivityIds("[]"); // Empty JSON array
                score.setCriterion(null);
                score.setNotes(null);

                studentScoreRepository.save(score);
                log.info("Created {} score for student {} in semester {}",
                        scoreType, student.getId(), semester.getId());
            }

        } catch (Exception e) {
            log.error("Failed to initialize student scores: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize student scores", e);
        }
    }

    /**
     * Initialize scores for current open semester
     */
    @Transactional
    public void initializeStudentScoresForCurrentSemester(Student student) {
        Optional<Semester> currentSemester = semesterRepository.findAll().stream()
                .filter(Semester::isOpen)
                .findFirst();

        if (currentSemester.isEmpty()) {
            log.warn("No open semester found for student {} score initialization", student.getId());
            return;
        }

        initializeStudentScores(student, currentSemester.get());
    }
}
