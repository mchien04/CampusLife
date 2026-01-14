package vn.campuslife.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.AcademicYear;
import vn.campuslife.entity.Semester;
import vn.campuslife.model.AcademicYearRequest;
import vn.campuslife.model.Response;
import vn.campuslife.model.SemesterRequest;
import vn.campuslife.repository.AcademicYearRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.service.AcademicService;
import vn.campuslife.service.StudentScoreInitService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AcademicServiceImpl implements AcademicService {

    private final AcademicYearRepository yearRepo;
    private final SemesterRepository semRepo;
    private final StudentScoreInitService studentScoreInitService;

    public AcademicServiceImpl(
            AcademicYearRepository yearRepo,
            SemesterRepository semRepo,
            StudentScoreInitService studentScoreInitService) {
        this.yearRepo = yearRepo;
        this.semRepo = semRepo;
        this.studentScoreInitService = studentScoreInitService;
    }

    @Override
    public Response getYears() {
        List<AcademicYear> list = yearRepo.findAll();
        return new Response(true, "Years retrieved", list);
    }

    @Override
    public Response getYear(Long id) {
        return yearRepo.findById(id)
                .map(y -> new Response(true, "Year found", y))
                .orElseGet(() -> new Response(false, "Year not found", null));
    }

    @Override
    @Transactional
    public Response createYear(AcademicYearRequest request) {
        AcademicYear y = new AcademicYear();
        y.setName(request.getName());
        y.setStartDate(request.getStartDate());
        y.setEndDate(request.getEndDate());
        AcademicYear saved = yearRepo.save(y);
        return new Response(true, "Year created", saved);
    }

    @Override
    @Transactional
    public Response updateYear(Long id, AcademicYearRequest request) {
        return yearRepo.findById(id).map(y -> {
            y.setName(request.getName());
            y.setStartDate(request.getStartDate());
            y.setEndDate(request.getEndDate());
            AcademicYear saved = yearRepo.save(y);
            return new Response(true, "Year updated", saved);
        }).orElseGet(() -> new Response(false, "Year not found", null));
    }

    @Override
    @Transactional
    public Response deleteYear(Long id) {
        return yearRepo.findById(id).map(y -> {
            yearRepo.delete(y);
            return new Response(true, "Year removed", null);
        }).orElseGet(() -> new Response(false, "Year not found", null));
    }

    @Override
    public Response getSemestersByYear(Long yearId) {
        return yearRepo.findById(yearId).map(y -> {
            List<Semester> list = y.getClass() != null
                    ? semRepo.findAll().stream().filter(s -> s.getYear().getId().equals(yearId)).toList()
                    : List.of();
            return new Response(true, "Semesters retrieved", list);
        }).orElseGet(() -> new Response(false, "Year not found", null));
    }

    @Override
    public Response getSemester(Long id) {
        return semRepo.findById(id)
                .map(s -> new Response(true, "Semester found", s))
                .orElseGet(() -> new Response(false, "Semester not found", null));
    }

    @Override
    @Transactional
    public Response createSemester(SemesterRequest request) {
        AcademicYear year = yearRepo.findById(request.getYearId()).orElse(null);
        if (year == null)
            return new Response(false, "Year not found", null);
        Semester s = new Semester();
        s.setYear(year);
        s.setName(request.getName());
        s.setStartDate(request.getStartDate());
        s.setEndDate(request.getEndDate());
        if (request.getOpen() != null)
            s.setOpen(request.getOpen());
        Semester saved = semRepo.save(s);

        // Auto-initialize scores for all students if semester is opened
        if (saved.isOpen()) {
            try {
                studentScoreInitService.initializeScoresForAllStudents(saved);
                log.info("Auto-initialized scores for all students in new semester {}", saved.getId());
            } catch (Exception e) {
                // Log error but don't fail semester creation
                // Admin can call manual API if needed
                log.error("Failed to auto-initialize scores for new semester {}: {}",
                        saved.getId(), e.getMessage(), e);
            }
        }

        return new Response(true, "Semester created", saved);
    }

    @Override
    @Transactional
    public Response updateSemester(Long id, SemesterRequest request) {
        return semRepo.findById(id).map(s -> {
            AcademicYear year = yearRepo.findById(request.getYearId()).orElse(null);
            if (year == null)
                return new Response(false, "Year not found", null);
            s.setYear(year);
            s.setName(request.getName());
            s.setStartDate(request.getStartDate());
            s.setEndDate(request.getEndDate());
            if (request.getOpen() != null)
                s.setOpen(request.getOpen());
            Semester saved = semRepo.save(s);
            return new Response(true, "Semester updated", saved);
        }).orElseGet(() -> new Response(false, "Semester not found", null));
    }

    @Override
    @Transactional
    public Response deleteSemester(Long id) {
        return semRepo.findById(id).map(s -> {
            semRepo.delete(s);
            return new Response(true, "Semester removed", null);
        }).orElseGet(() -> new Response(false, "Semester not found", null));
    }

    @Override
    @Transactional
    public Response toggleSemesterOpen(Long id, boolean open) {
        return semRepo.findById(id).map(s -> {
            s.setOpen(open);
            Semester saved = semRepo.save(s);
            return new Response(true, open ? "Semester opened" : "Semester closed", saved);
        }).orElseGet(() -> new Response(false, "Semester not found", null));
    }

    @Override
    @Transactional
    public Response initializeScoresForSemester(Long semesterId) {
        Optional<Semester> semesterOpt = semRepo.findById(semesterId);
        if (semesterOpt.isEmpty()) {
            return new Response(false, "Semester not found", null);
        }

        Semester semester = semesterOpt.get();

        try {
            studentScoreInitService.initializeScoresForAllStudents(semester);

            Map<String, Object> result = new HashMap<>();
            result.put("semesterId", semester.getId());
            result.put("semesterName", semester.getName());
            result.put("message", "Scores initialized successfully for all students");

            return new Response(true, "Scores initialized successfully", result);
        } catch (Exception e) {
            log.error("Failed to initialize scores for semester {}: {}",
                    semesterId, e.getMessage(), e);
            return new Response(false, "Failed to initialize scores: " + e.getMessage(), null);
        }
    }
}
