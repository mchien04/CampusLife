package vn.campuslife.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.campuslife.model.activity.series.SeriesChildActivityCreateRequest;
import vn.campuslife.repository.ActivitySeriesRepository;

@Component
@RequiredArgsConstructor
public class SeriesChildActivityValidator {

    private final ActivitySeriesRepository seriesRepository;

    public void validate(SeriesChildActivityCreateRequest request, Long seriesId) {
        if (seriesId == null) {
            throw new IllegalArgumentException("Series ID is required");
        }
        
        var seriesOpt = seriesRepository.findById(seriesId);
        if (seriesOpt.isEmpty() || seriesOpt.get().isDeleted()) {
            throw new IllegalArgumentException("Series not found or deleted");
        }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Activity name is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
}
