package vn.campuslife.controller.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.service.StandardActivityService;

@RestController
@RequestMapping("/api/activities/standard")
@RequiredArgsConstructor
public class StandardActivityController {

    private final StandardActivityService standardActivityService;

    @PostMapping
    public ResponseEntity<Response> createStandardActivity(@RequestBody StandardActivityCreateRequest request) {
        Response response = standardActivityService.createActivity(request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateStandardActivity(@PathVariable Long id, @RequestBody StandardActivityUpdateRequest request) {
        Response response = standardActivityService.updateActivity(id, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getStandardActivity(@PathVariable Long id) {
        Response response = standardActivityService.getActivity(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
