package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.model.CreateActivitySeriesRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivitySeriesService;

import java.util.List;

@RestController
@RequestMapping("/api/activity-series")
@RequiredArgsConstructor
public class ActivitySeriesController {

    private final ActivitySeriesService activitySeriesService;
    @GetMapping("/{id}")
    public ResponseEntity<?> getSeriesDetail(@PathVariable Long id) {
        ActivitySeries series = activitySeriesService.getSeriesById(id);
        return ResponseEntity.ok(new Response(
                true,
                "Lấy thông tin chuỗi sự kiện thành công",
                series
        ));
    }
    @PostMapping
    public ResponseEntity<Response> createSeries(
            @RequestBody CreateActivitySeriesRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        Response res = activitySeriesService.createSeries(request, username);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<Response> getMySeries(Authentication authentication) {
        String username = authentication.getName();
        Response res = activitySeriesService.getMySeries(username);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<Response> getSeriesEvents(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        Response response = activitySeriesService.getSeriesEvents(id, username);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteSeries(@PathVariable Long id) {
        Response response = activitySeriesService.deleteSeries(id);

        if (response.isStatus()) {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } else {
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<Response> updateSeries(
            @PathVariable Long id,
            @RequestBody CreateActivitySeriesRequest request,
            Authentication auth) {

        String username = auth.getName();
        Response response = activitySeriesService.updateSeries(id, request, username);

        if (response.isStatus()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    @DeleteMapping("/{seriesId}/event/{eventId}")
    public ResponseEntity<Response> deleteEventFromSeries(
            @PathVariable Long seriesId,
            @PathVariable Long eventId,
            Authentication auth) {

        String username = auth.getName();
        Response response = activitySeriesService.deleteEventInSeries(seriesId, eventId, username);

        if (response.isStatus()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    @PostMapping("/{seriesId}/events")
    public ResponseEntity<Response> addEventToSeries(
            @PathVariable Long seriesId,
            @RequestBody vn.campuslife.model.CreateActivityRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();

        Response response = activitySeriesService.addEventToSeries(seriesId, request, username);

        if (response.isStatus()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }



}
