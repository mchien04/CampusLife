package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Department;
import vn.campuslife.model.DepartmentRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.service.DepartmentService;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Response getAll() {
        List<Department> list = departmentRepository.findAll();
        return new Response(true, "Departments retrieved", list);
    }

    @Override
    public Response getById(Long id) {
        return departmentRepository.findById(id)
                .map(d -> new Response(true, "Department found", d))
                .orElseGet(() -> new Response(false, "Department not found", null));
    }

    @Override
    @Transactional
    public Response create(DepartmentRequest request) {
        Department d = new Department();
        d.setName(request.getName());
        d.setType(request.getType());
        d.setDescription(request.getDescription());
        Department saved = departmentRepository.save(d);
        return new Response(true, "Department created", saved);
    }

    @Override
    @Transactional
    public Response update(Long id, DepartmentRequest request) {
        return departmentRepository.findById(id).map(existing -> {
            existing.setName(request.getName());
            existing.setType(request.getType());
            existing.setDescription(request.getDescription());
            Department saved = departmentRepository.save(existing);
            return new Response(true, "Department updated", saved);
        }).orElseGet(() -> new Response(false, "Department not found", null));
    }

    @Override
    @Transactional
    public Response delete(Long id) {
        return departmentRepository.findById(id).map(existing -> {
            try {
                java.lang.reflect.Field deletedField = existing.getClass().getDeclaredField("isDeleted");
                deletedField.setAccessible(true);
                deletedField.set(existing, true);
                Department saved = departmentRepository.save(existing);
                return new Response(true, "Department deleted", saved);
            } catch (Exception ignored) {
                departmentRepository.delete(existing);
                return new Response(true, "Department removed", null);
            }
        }).orElseGet(() -> new Response(false, "Department not found", null));
    }
}
