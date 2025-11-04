package vn.campuslife.service;


import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.model.ActivitySeriesResponse;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.CreateActivitySeriesRequest;
import vn.campuslife.model.Response;

import java.util.List;

public interface ActivitySeriesService {
    Response createSeries(CreateActivitySeriesRequest request,String username);

    List<ActivitySeries> getAllSeries();

    Response getMySeries(String username);
    Response getSeriesEvents(Long id, String username);
    Response deleteSeries(Long id);
    Response updateSeries(Long id,CreateActivitySeriesRequest request, String username);
    Response deleteEventInSeries(Long seriesId, Long eventId, String username);
    ActivitySeries getSeriesById(Long id);
    Response addEventToSeries(Long seriesId, CreateActivityRequest request, String username);

}