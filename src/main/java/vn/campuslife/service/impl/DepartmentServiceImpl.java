package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Department;
import vn.campuslife.model.Response;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.service.DepartmentService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repo;

    @Override
    public Optional<Department> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<Department> findAll() {
        return repo.findAll();
    }
}
