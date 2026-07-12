package vn.campuslife.service;

import vn.campuslife.entity.User;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.BulkManualScoreRequest;
import vn.campuslife.model.score.ManualScoreRequest;
import vn.campuslife.security.department.DepartmentScope;

public interface ManualScoreService {

    Response createManualAdjustment(ManualScoreRequest request, User actor, DepartmentScope scope);

    Response createBulkManualAdjustments(BulkManualScoreRequest request, User actor, DepartmentScope scope);

    Response reverseManualAdjustment(Long adjustmentId, String reason, User actor, DepartmentScope scope);
}
