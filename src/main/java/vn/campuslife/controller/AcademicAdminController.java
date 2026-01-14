package vn.campuslife.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.AcademicYearRequest;
import vn.campuslife.model.Response;
import vn.campuslife.model.SemesterRequest;
import vn.campuslife.service.AcademicService;

@RestController
@RequestMapping("/api/admin/academics")
public class AcademicAdminController {

    private final AcademicService academicService;

    public AcademicAdminController(AcademicService academicService) {
        this.academicService = academicService;
    }

    // Years
    @GetMapping("/years")
    public ResponseEntity<Response> getYears() {
        return ResponseEntity.ok(academicService.getYears());
    }

    @GetMapping("/years/{id}")
    public ResponseEntity<Response> getYear(@PathVariable Long id) {
        Response r = academicService.getYear(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/years")
    public ResponseEntity<Response> createYear(@RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(academicService.createYear(request));
    }

    @PutMapping("/years/{id}")
    public ResponseEntity<Response> updateYear(@PathVariable Long id, @RequestBody AcademicYearRequest request) {
        Response r = academicService.updateYear(id, request);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @DeleteMapping("/years/{id}")
    public ResponseEntity<Response> deleteYear(@PathVariable Long id) {
        Response r = academicService.deleteYear(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    // Semesters
    @GetMapping("/years/{yearId}/semesters")
    public ResponseEntity<Response> getSemestersByYear(@PathVariable Long yearId) {
        Response r = academicService.getSemestersByYear(yearId);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @GetMapping("/semesters/{id}")
    public ResponseEntity<Response> getSemester(@PathVariable Long id) {
        Response r = academicService.getSemester(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/semesters")
    public ResponseEntity<Response> createSemester(@RequestBody SemesterRequest request) {
        return ResponseEntity.ok(academicService.createSemester(request));
    }

    @PutMapping("/semesters/{id}")
    public ResponseEntity<Response> updateSemester(@PathVariable Long id, @RequestBody SemesterRequest request) {
        Response r = academicService.updateSemester(id, request);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @DeleteMapping("/semesters/{id}")
    public ResponseEntity<Response> deleteSemester(@PathVariable Long id) {
        Response r = academicService.deleteSemester(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/semesters/{id}/toggle")
    public ResponseEntity<Response> toggleSemester(@PathVariable Long id, @RequestParam("open") boolean open) {
        Response r = academicService.toggleSemesterOpen(id, open);
        return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
    }

    @PostMapping("/semesters/{id}/initialize-scores")
    public ResponseEntity<Response> initializeScoresForSemester(@PathVariable Long id) {
        Response r = academicService.initializeScoresForSemester(id);
        return ResponseEntity.status(r.isStatus() ? 200 : 500).body(r);
    }
}
