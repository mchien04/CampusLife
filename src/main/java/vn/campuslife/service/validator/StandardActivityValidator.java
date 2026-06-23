package vn.campuslife.service.validator;

import org.springframework.stereotype.Component;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.activity.StandardActivityCreateRequest;

@Component
public class StandardActivityValidator implements ActivityValidator<StandardActivityCreateRequest> {

    @Override
    public void validate(StandardActivityCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Activity name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Activity type is required");
        }
        if (ActivityType.MINIGAME.equals(request.getType())) {
            throw new IllegalArgumentException("MINIGAME type is not allowed for standard activity");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }
        if (request.getOrganizerIds() == null || request.getOrganizerIds().isEmpty()) {
            throw new IllegalArgumentException("At least one organizer is required");
        }
        
        if (request.getRegistrationStartDate() != null && request.getRegistrationDeadline() != null) {
            if (request.getRegistrationStartDate().isAfter(request.getRegistrationDeadline())) {
                throw new IllegalArgumentException("Registration start date must be before deadline");
            }
        }
    }
}
