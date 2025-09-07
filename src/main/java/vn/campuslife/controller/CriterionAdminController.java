package vn.campuslife.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.CriterionGroupRequest;
import vn.campuslife.model.CriterionRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.CriterionService;

@RestController
@RequestMapping("/api/admin/criteria")
public class CriterionAdminController {

    private final CriterionService criterionService;

    public CriterionAdminController(CriterionService criterionService) {
        this.criterionService = criterionService;
    }

    // Groups
    @GetMapping("/groups")
    public ResponseEntity<Response> getGroups() {
        return ResponseEntity.ok(criterionService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Response> getGroup(@PathVariable Long id) {
        Response r = criterionService.getGroup(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/groups")
    public ResponseEntity<Response> createGroup(@RequestBody CriterionGroupRequest request) {
        return ResponseEntity.ok(criterionService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<Response> updateGroup(@PathVariable Long id, @RequestBody CriterionGroupRequest request) {
        Response r = criterionService.updateGroup(id, request);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Response> deleteGroup(@PathVariable Long id) {
        Response r = criterionService.deleteGroup(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    // Criteria
    @GetMapping("/groups/{groupId}/items")
    public ResponseEntity<Response> getCriteriaByGroup(@PathVariable Long groupId) {
        Response r = criterionService.getCriteriaByGroup(groupId);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<Response> getCriterion(@PathVariable Long id) {
        Response r = criterionService.getCriterion(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/items")
    public ResponseEntity<Response> createCriterion(@RequestBody CriterionRequest request) {
        return ResponseEntity.ok(criterionService.createCriterion(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Response> updateCriterion(@PathVariable Long id, @RequestBody CriterionRequest request) {
        Response r = criterionService.updateCriterion(id, request);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Response> deleteCriterion(@PathVariable Long id) {
        Response r = criterionService.deleteCriterion(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }
}
