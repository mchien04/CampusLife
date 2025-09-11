package vn.campuslife.controller;

import vn.campuslife.model.DepartmentRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/departments")
public class DepartmentAdminController {

    private final DepartmentService departmentService;

    public DepartmentAdminController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<Response> getAll() {
        return ResponseEntity.ok(departmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getOne(@PathVariable Long id) {
        Response r = departmentService.getById(id);
        if (r.isStatus())
            return ResponseEntity.ok(r);
        return ResponseEntity.status(404).body(r);
    }

    @PostMapping
    public ResponseEntity<Response> create(@RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable Long id, @RequestBody DepartmentRequest request) {
        Response r = departmentService.update(id, request);
        if (r.isStatus())
            return ResponseEntity.ok(r);
        return ResponseEntity.status(404).body(r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable Long id) {
        Response r = departmentService.delete(id);
        if (r.isStatus())
            return ResponseEntity.ok(r);
        return ResponseEntity.status(404).body(r);
    }
}
