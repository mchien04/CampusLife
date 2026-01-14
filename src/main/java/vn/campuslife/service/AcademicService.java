package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.AcademicYearRequest;
import vn.campuslife.model.SemesterRequest;

public interface AcademicService {
    // Years
    Response getYears();

    Response getYear(Long id);

    Response createYear(AcademicYearRequest request);

    Response updateYear(Long id, AcademicYearRequest request);

    Response deleteYear(Long id);

    // Semesters
    Response getSemestersByYear(Long yearId);

    Response getSemester(Long id);

    Response createSemester(SemesterRequest request);

    Response updateSemester(Long id, SemesterRequest request);

    Response deleteSemester(Long id);

    Response toggleSemesterOpen(Long id, boolean open);

    /**
     * Initialize scores for all students in a semester (manual trigger)
     */
    Response initializeScoresForSemester(Long semesterId);
}
