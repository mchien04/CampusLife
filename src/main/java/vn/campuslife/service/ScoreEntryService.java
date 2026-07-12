package vn.campuslife.service;

import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.score.ScoreEntryCommand;

public interface ScoreEntryService {
    ScoreEntry upsertEntry(ScoreEntryCommand command);
    void reverseEntries(ScoreEntrySourceType sourceType, Long sourceId, String reason, User actor);
    /** Reverse một ScoreEntry ACTIVE theo id (dùng khi duyệt khiếu nại). */
    void reverseEntry(Long scoreEntryId, String reason, User actor);
    void refreshStudentScore(Long studentId, Long semesterId, ScoreType scoreType);
}
