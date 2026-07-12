package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.CreateScoreAppealRequest;
import vn.campuslife.model.score.ScoreAppealDecisionRequest;
import vn.campuslife.model.score.ScoreAppealMessageRequest;
import vn.campuslife.security.department.DepartmentScope;

import java.util.List;

public interface ScoreAppealService {

    Response uploadEvidence(List<MultipartFile> files, User studentUser);

    Response createAppeal(CreateScoreAppealRequest request, User studentUser);

    Response listMyAppeals(User studentUser);

    Response listAppeals(ScoreAppealStatus status, Long semesterId, Long studentId,
                         int page, int size, DepartmentScope scope);

    Response getAppeal(Long appealId, User actor, DepartmentScope scope);

    Response addMessage(Long appealId, ScoreAppealMessageRequest request, User actor, DepartmentScope scope);

    Response previewDecision(Long appealId, ScoreAppealDecisionRequest request, User actor, DepartmentScope scope);

    Response decide(Long appealId, ScoreAppealDecisionRequest request, User actor, DepartmentScope scope);

    Response close(Long appealId, User actor, DepartmentScope scope);

    Response withdraw(Long appealId, User studentUser);
}
