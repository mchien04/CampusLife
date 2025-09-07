package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Criterion;
import vn.campuslife.entity.CriterionGroup;
import vn.campuslife.entity.Department;
import vn.campuslife.model.CriterionGroupRequest;
import vn.campuslife.model.CriterionRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.CriterionGroupRepository;
import vn.campuslife.repository.CriterionRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.service.CriterionService;

import java.util.List;

@Service
public class CriterionServiceImpl implements CriterionService {

    private final CriterionGroupRepository groupRepo;
    private final CriterionRepository criterionRepo;
    private final DepartmentRepository departmentRepo;

    public CriterionServiceImpl(CriterionGroupRepository groupRepo, CriterionRepository criterionRepo,
            DepartmentRepository departmentRepo) {
        this.groupRepo = groupRepo;
        this.criterionRepo = criterionRepo;
        this.departmentRepo = departmentRepo;
    }

    // Groups
    @Override
    public Response getGroups() {
        List<CriterionGroup> list = groupRepo.findAll();
        return new Response(true, "Groups retrieved", list);
    }

    @Override
    public Response getGroup(Long id) {
        return groupRepo.findById(id)
                .map(g -> new Response(true, "Group found", g))
                .orElseGet(() -> new Response(false, "Group not found", null));
    }

    @Override
    @Transactional
    public Response createGroup(CriterionGroupRequest request) {
        CriterionGroup g = new CriterionGroup();
        g.setName(request.getName());
        g.setDescription(request.getDescription());
        CriterionGroup saved = groupRepo.save(g);
        return new Response(true, "Group created", saved);
    }

    @Override
    @Transactional
    public Response updateGroup(Long id, CriterionGroupRequest request) {
        return groupRepo.findById(id).map(g -> {
            g.setName(request.getName());
            g.setDescription(request.getDescription());
            CriterionGroup saved = groupRepo.save(g);
            return new Response(true, "Group updated", saved);
        }).orElseGet(() -> new Response(false, "Group not found", null));
    }

    @Override
    @Transactional
    public Response deleteGroup(Long id) {
        return groupRepo.findById(id).map(g -> {
            groupRepo.delete(g);
            return new Response(true, "Group removed", null);
        }).orElseGet(() -> new Response(false, "Group not found", null));
    }

    // Criteria
    @Override
    public Response getCriteriaByGroup(Long groupId) {
        return groupRepo.findById(groupId).map(g -> {
            List<Criterion> list = criterionRepo.findAll().stream().filter(c -> c.getGroup().getId().equals(groupId))
                    .toList();
            return new Response(true, "Criteria retrieved", list);
        }).orElseGet(() -> new Response(false, "Group not found", null));
    }

    @Override
    public Response getCriterion(Long id) {
        return criterionRepo.findById(id)
                .map(c -> new Response(true, "Criterion found", c))
                .orElseGet(() -> new Response(false, "Criterion not found", null));
    }

    @Override
    @Transactional
    public Response createCriterion(CriterionRequest request) {
        CriterionGroup group = groupRepo.findById(request.getGroupId()).orElse(null);
        if (group == null)
            return new Response(false, "Group not found", null);
        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepo.findById(request.getDepartmentId()).orElse(null);
            if (dept == null)
                return new Response(false, "Department not found", null);
        }
        Criterion c = new Criterion();
        c.setGroup(group);
        c.setName(request.getName());
        c.setMaxScore(request.getMaxScore());
        c.setMinScore(request.getMinScore());
        c.setDepartment(dept);
        c.setDescription(request.getDescription());
        Criterion saved = criterionRepo.save(c);
        return new Response(true, "Criterion created", saved);
    }

    @Override
    @Transactional
    public Response updateCriterion(Long id, CriterionRequest request) {
        return criterionRepo.findById(id).map(c -> {
            CriterionGroup group = groupRepo.findById(request.getGroupId()).orElse(null);
            if (group == null)
                return new Response(false, "Group not found", null);
            Department dept = null;
            if (request.getDepartmentId() != null) {
                dept = departmentRepo.findById(request.getDepartmentId()).orElse(null);
                if (dept == null)
                    return new Response(false, "Department not found", null);
            }
            c.setGroup(group);
            c.setName(request.getName());
            c.setMaxScore(request.getMaxScore());
            c.setMinScore(request.getMinScore());
            c.setDepartment(dept);
            c.setDescription(request.getDescription());
            Criterion saved = criterionRepo.save(c);
            return new Response(true, "Criterion updated", saved);
        }).orElseGet(() -> new Response(false, "Criterion not found", null));
    }

    @Override
    @Transactional
    public Response deleteCriterion(Long id) {
        return criterionRepo.findById(id).map(c -> {
            criterionRepo.delete(c);
            return new Response(true, "Criterion removed", null);
        }).orElseGet(() -> new Response(false, "Criterion not found", null));
    }
}
