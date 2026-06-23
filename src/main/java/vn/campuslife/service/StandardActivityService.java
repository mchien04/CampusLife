package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;

public interface StandardActivityService {
    Response createActivity(StandardActivityCreateRequest request);
    Response updateActivity(Long id, StandardActivityUpdateRequest request);
    Response getActivity(Long id);
}
