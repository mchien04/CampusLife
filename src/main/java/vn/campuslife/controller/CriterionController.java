package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.repository.CriterionGroupRepository;
import vn.campuslife.repository.CriterionRepository;

@RestController
@RequestMapping("/api/criteria")
@RequiredArgsConstructor
public class CriterionController {

    private final CriterionGroupRepository groupRepo;
    private final CriterionRepository criterionRepo;

    @GetMapping("/groups")
    public ResponseEntity<Response> getGroups() {
        return ResponseEntity.ok(new Response(true, "Groups retrieved", groupRepo.findByIsDeletedFalse()));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<Response> getGroup(@PathVariable Long groupId) {
        return groupRepo.findById(groupId)
                .map(g -> ResponseEntity.ok(new Response(true, "Group retrieved", g)))
                .orElse(ResponseEntity.ok(new Response(false, "Group not found", null)));
    }

    @GetMapping("/groups/{groupId}/list")
    public ResponseEntity<Response> getCriteriaByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(new Response(true, "Criteria retrieved",
                criterionRepo.findByGroupIdAndIsDeletedFalse(groupId)));
    }

    @GetMapping
    public ResponseEntity<Response> getAllCriteria() {
        return ResponseEntity.ok(new Response(true, "Criteria retrieved", criterionRepo.findAll()));
    }
}
