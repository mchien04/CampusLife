package vn.campuslife.service;

import vn.campuslife.entity.Department;
import vn.campuslife.model.Response;

import java.util.List;
import java.util.Optional;

public interface DepartmentService {
    Optional<Department> findById(Long id);
    List<Department> findAll();

}
