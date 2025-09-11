package vn.campuslife.service;

import vn.campuslife.entity.Department;
import vn.campuslife.model.DepartmentRequest;
import vn.campuslife.model.Response;

import java.util.List;
import java.util.Optional;

public interface DepartmentService {

    Optional<Department> findById(Long id);
    List<Department> findAll();


    Response getAll();
    Response getById(Long id);
    Response create(DepartmentRequest request);
    Response update(Long id, DepartmentRequest request);
    Response delete(Long id);
}
