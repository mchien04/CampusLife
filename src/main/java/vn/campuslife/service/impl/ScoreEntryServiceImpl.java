package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.score.ScoreEntryCommand;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivityScoreRuleRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.StudentScoreRepository;
import vn.campuslife.service.ScoreEntryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScoreEntryServiceImpl implements ScoreEntryService {

    private final ScoreEntryRepository scoreEntryRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ActivityRepository activityRepository;
    private final ActivityScoreRuleRepository ruleRepository;

    @Override
    @Transactional
    public ScoreEntry upsertEntry(ScoreEntryCommand command) {
        Optional<ScoreEntry> existing = scoreEntryRepository.findByStudentIdAndSourceTypeAndSourceIdAndRuleIdAndStatus(
                command.getStudentId(), command.getSourceType(), command.getSourceId(), 
                command.getRuleId(), ScoreEntryStatus.ACTIVE);

        if (existing.isPresent()) {
            ScoreEntry entry = existing.get();
            if (entry.getPoints().compareTo(command.getPoints()) != 0) {
                entry.setPoints(command.getPoints());
                entry.setReason(command.getReason());
                scoreEntryRepository.save(entry);
                refreshStudentScore(command.getStudentId(), command.getSemesterId(), command.getScoreType());
            }
            return entry;
        }

        ScoreEntry newEntry = new ScoreEntry();
        newEntry.setStudent(studentRepository.getReferenceById(command.getStudentId()));
        newEntry.setSemester(semesterRepository.getReferenceById(command.getSemesterId()));
        newEntry.setScoreType(command.getScoreType());
        if (command.getActivityId() != null) {
            newEntry.setActivity(activityRepository.getReferenceById(command.getActivityId()));
        }
        if (command.getRuleId() != null) {
            newEntry.setRule(ruleRepository.getReferenceById(command.getRuleId()));
        }
        newEntry.setSourceType(command.getSourceType());
        newEntry.setSourceId(command.getSourceId());
        newEntry.setPoints(command.getPoints());
        newEntry.setStatus(ScoreEntryStatus.ACTIVE);
        newEntry.setReason(command.getReason());
        newEntry.setCreatedBy(command.getActor());

        scoreEntryRepository.save(newEntry);
        refreshStudentScore(command.getStudentId(), command.getSemesterId(), command.getScoreType());
        return newEntry;
    }

    @Override
    @Transactional
    public void reverseEntries(ScoreEntrySourceType sourceType, Long sourceId, String reason, User actor) {
        List<ScoreEntry> entries = scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(sourceType, sourceId, ScoreEntryStatus.ACTIVE);
        for (ScoreEntry entry : entries) {
            entry.setStatus(ScoreEntryStatus.REVERSED);
            entry.setReason(reason);
            entry.setCreatedBy(actor);
            scoreEntryRepository.save(entry);
            refreshStudentScore(entry.getStudent().getId(), entry.getSemester().getId(), entry.getScoreType());
        }
    }

    @Override
    @Transactional
    public void refreshStudentScore(Long studentId, Long semesterId, ScoreType scoreType) {
        BigDecimal total = scoreEntryRepository.sumPointsByStudentAndSemesterAndScoreTypeAndStatus(
                studentId, semesterId, scoreType, ScoreEntryStatus.ACTIVE);
        if (total == null) total = BigDecimal.ZERO;

        Optional<StudentScore> optScore = studentScoreRepository.findByStudentIdAndSemesterIdAndScoreType(studentId, semesterId, scoreType);
        if (optScore.isPresent()) {
            StudentScore studentScore = optScore.get();
            studentScore.setScore(total);
            studentScoreRepository.save(studentScore);
        } else {
            StudentScore newScore = new StudentScore();
            newScore.setStudent(studentRepository.getReferenceById(studentId));
            newScore.setSemester(semesterRepository.getReferenceById(semesterId));
            newScore.setScoreType(scoreType);
            newScore.setScore(total);
            studentScoreRepository.save(newScore);
        }
    }
}
