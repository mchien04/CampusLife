package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Department;
import vn.campuslife.model.DepartmentRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.service.DepartmentService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repo;

    // ===== Public APIs =====
    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findById(Long id) {
        return repo.findById(id).filter(d -> !d.isDeleted());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return repo.findAll().stream()
                .filter(d -> !d.isDeleted())
                .sorted(Comparator.comparing(
                        Department::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();
    }

    // ===== Admin APIs (Response wrapper) =====
    @Override
    @Transactional(readOnly = true)
    public Response getAll() {
        var list = findAll(); // tái dùng logic public (lọc isDeleted=false)

        return new Response(true, "Departments retrieved", list);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getById(Long id) {
        return findById(id)
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
        Department saved = repo.save(d);
        return new Response(true, "Department created", saved);
    }

    @Override
    @Transactional
    public Response update(Long id, DepartmentRequest request) {
        return repo.findById(id).map(existing -> {
            if (existing.isDeleted()) {
                return new Response(false, "Department not found", null);
            }
            existing.setName(request.getName());
            existing.setType(request.getType());
            existing.setDescription(request.getDescription());
            Department saved = repo.save(existing);
            return new Response(true, "Department updated", saved);
        }).orElseGet(() -> new Response(false, "Department not found", null));
    }

    @Override
    @Transactional
    public Response delete(Long id) {
        return repo.findById(id).map(existing -> {
            if (existing.isDeleted()) {
                return new Response(false, "Department not found", null);
            }
            existing.setDeleted(true);           // soft delete
            Department saved = repo.save(existing);
            return new Response(true, "Department deleted", saved);
        }).orElseGet(() -> new Response(false, "Department not found", null));
    }
}
