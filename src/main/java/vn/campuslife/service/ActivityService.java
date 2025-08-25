package vn.campuslife.service;

import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;

public interface ActivityService {
    Response createActivity(CreateActivityRequest request);
}