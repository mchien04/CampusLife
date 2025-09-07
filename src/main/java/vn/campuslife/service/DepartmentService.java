package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.DepartmentRequest;

public interface DepartmentService {
    Response getAll();

    Response getById(Long id);

    Response create(DepartmentRequest request);

    Response update(Long id, DepartmentRequest request);

    Response delete(Long id);
}
