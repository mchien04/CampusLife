package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.CriterionGroupRequest;
import vn.campuslife.model.CriterionRequest;

public interface CriterionService {
    // Groups
    Response getGroups();

    Response getGroup(Long id);

    Response createGroup(CriterionGroupRequest request);

    Response updateGroup(Long id, CriterionGroupRequest request);

    Response deleteGroup(Long id);

    // Criteria
    Response getCriteriaByGroup(Long groupId);

    Response getCriterion(Long id);

    Response createCriterion(CriterionRequest request);

    Response updateCriterion(Long id, CriterionRequest request);

    Response deleteCriterion(Long id);
}
