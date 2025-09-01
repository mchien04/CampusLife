package vn.campuslife.service;

import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;

import java.util.List;

public interface ActivityService {
    Response createActivity(CreateActivityRequest request);

    Response getAllActivities();

    Response getActivityById(Long id);

    Response updateActivity(Long id, CreateActivityRequest request);

    Response deleteActivity(Long id);
}