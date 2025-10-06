package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.repository.AcademicYearRepository;
import vn.campuslife.repository.SemesterRepository;

@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class AcademicPublicController {

    private final AcademicYearRepository yearRepo;
    private final SemesterRepository semRepo;

    @GetMapping("/years")
    public ResponseEntity<Response> listYears() {
        return ResponseEntity.ok(new Response(true, "Years retrieved", yearRepo.findAll()));
    }

    @GetMapping("/years/{yearId}/semesters")
    public ResponseEntity<Response> listSemesters(@PathVariable Long yearId) {
        return yearRepo.findById(yearId)
                .map(y -> ResponseEntity.ok(new Response(true, "Semesters retrieved",
                        semRepo.findAll().stream().filter(s -> s.getYear().getId().equals(yearId)).toList())))
                .orElse(ResponseEntity.ok(new Response(false, "Year not found", null)));
    }

    @GetMapping("/semesters")
    public ResponseEntity<Response> getSemesters() {
        return ResponseEntity.ok(new Response(true, "Semesters retrieved", semRepo.findAll()));
    }
}
