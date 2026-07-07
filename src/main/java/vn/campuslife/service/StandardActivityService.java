package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.security.department.DepartmentScope;

public interface StandardActivityService {
    Response createActivity(StandardActivityCreateRequest request);

    Response createActivity(StandardActivityCreateRequest request, DepartmentScope scope);

    Response updateActivity(Long id, StandardActivityUpdateRequest request);

    Response updateActivity(Long id, StandardActivityUpdateRequest request, DepartmentScope scope);

    Response getActivity(Long id);

    Response getActivity(Long id, DepartmentScope scope);
}
