package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRegistrationServiceImpl implements ActivityRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRegistrationServiceImpl.class);

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivityParticipationRepository participationRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public Response registerForActivity(ActivityRegistrationRequest request, Long studentId) {
        try {
            // Validate activity exists and not deleted
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            Activity activity = activityOpt.get();

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            Student student = studentOpt.get();

            // Check if already registered
            if (registrationRepository.existsByActivityIdAndStudentId(request.getActivityId(), studentId)) {
                return new Response(false, "Already registered for this activity", null);
            }

            // Check registration deadline
            if (activity.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(activity.getRegistrationDeadline().atStartOfDay())) {
                return new Response(false, "Registration deadline has passed", null);
            }

            // Check if registration is open
            if (activity.getRegistrationStartDate() != null &&
                    LocalDateTime.now().isBefore(activity.getRegistrationStartDate().atStartOfDay())) {
                return new Response(false, "Registration is not yet open", null);
            }

            // Check ticket quantity
            if (activity.getTicketQuantity() != null) {
                Long currentRegistrations = registrationRepository.countByActivityIdAndStatus(
                        request.getActivityId(), RegistrationStatus.APPROVED);
                if (currentRegistrations >= activity.getTicketQuantity()) {
                    return new Response(false, "Activity is full", null);
                }
            }

            // Create registration
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivity(activity);
            registration.setStudent(student);
            registration.setRegisteredDate(LocalDateTime.now());
            registration.setStatus(RegistrationStatus.PENDING);
            registration.setFeedback(request.getFeedback());

            ActivityRegistration savedRegistration = registrationRepository.save(registration);
            ActivityRegistrationResponse response = toRegistrationResponse(savedRegistration);

            return new Response(true, "Successfully registered for activity", response);
        } catch (Exception e) {
            logger.error("Failed to register for activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to register due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response cancelRegistration(Long activityId, Long studentId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activityId, studentId);

            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistration registration = registrationOpt.get();

            // Check if can cancel (only PENDING can be cancelled, APPROVED cannot be
            // cancelled)
            if (registration.getStatus() == RegistrationStatus.CANCELLED) {
                return new Response(false, "Registration already cancelled", null);
            }

            if (registration.getStatus() == RegistrationStatus.APPROVED) {
                return new Response(false,
                        "Cannot cancel approved registration. This is an auto-approved registration.", null);
            }

            registration.setStatus(RegistrationStatus.CANCELLED);
            registrationRepository.save(registration);

            return new Response(true, "Registration cancelled successfully", null);
        } catch (Exception e) {
            logger.error("Failed to cancel registration: {}", e.getMessage(), e);
            return new Response(false, "Failed to cancel registration due to server error", null);
        }
    }

    @Override
    public Response getStudentRegistrations(Long studentId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository
                    .findByStudentIdAndStudentIsDeletedFalse(studentId);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Student registrations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve student registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }

    @Override
    public Response getActivityRegistrations(Long activityId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository
                    .findByActivityIdAndActivityIsDeletedFalse(activityId);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Activity registrations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve activity registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateRegistrationStatus(Long registrationId, String status) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository.findById(registrationId);
            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistration registration = registrationOpt.get();
            RegistrationStatus newStatus = RegistrationStatus.valueOf(status.toUpperCase());
            registration.setStatus(newStatus);

            ActivityRegistration savedRegistration = registrationRepository.save(registration);
            ActivityRegistrationResponse response = toRegistrationResponse(savedRegistration);

            return new Response(true, "Registration status updated successfully", response);
        } catch (IllegalArgumentException e) {
            return new Response(false, "Invalid status: " + status, null);
        } catch (Exception e) {
            logger.error("Failed to update registration status: {}", e.getMessage(), e);
            return new Response(false, "Failed to update status due to server error", null);
        }
    }

    @Override
    public Response getRegistrationById(Long registrationId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository.findById(registrationId);
            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistrationResponse response = toRegistrationResponse(registrationOpt.get());
            return new Response(true, "Registration retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to retrieve registration: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registration due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response recordParticipation(ActivityParticipationRequest request, Long studentId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            // Check if student is registered and approved
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(request.getActivityId(), studentId);

            if (registrationOpt.isEmpty() || registrationOpt.get().getStatus() != RegistrationStatus.APPROVED) {
                return new Response(false, "Student is not approved for this activity", null);
            }

            // Create participation record
            ActivityParticipation participation = new ActivityParticipation();
            participation.setActivity(activityOpt.get());
            participation.setStudent(studentOpt.get());
            participation.setParticipationType(request.getParticipationType());
            participation.setPointsEarned(request.getPointsEarned());
            participation.setDate(LocalDateTime.now());

            ActivityParticipation savedParticipation = participationRepository.save(participation);
            ActivityParticipationResponse response = toParticipationResponse(savedParticipation);

            return new Response(true, "Participation recorded successfully", response);
        } catch (Exception e) {
            logger.error("Failed to record participation: {}", e.getMessage(), e);
            return new Response(false, "Failed to record participation due to server error", null);
        }
    }

    @Override
    public Response getStudentParticipations(Long studentId) {
        try {
            List<ActivityParticipation> participations = participationRepository
                    .findByStudentIdAndActivityIsDeletedFalse(studentId);

            List<ActivityParticipationResponse> responses = participations.stream()
                    .map(this::toParticipationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Student participations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve student participations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve participations due to server error", null);
        }
    }

    @Override
    public Response getActivityParticipations(Long activityId) {
        try {
            List<ActivityParticipation> participations = participationRepository
                    .findByActivityIdAndActivityIsDeletedFalse(activityId);

            List<ActivityParticipationResponse> responses = participations.stream()
                    .map(this::toParticipationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Activity participations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve activity participations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve participations due to server error", null);
        }
    }

    @Override
    public Response checkRegistrationStatus(Long activityId, Long studentId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activityId, studentId);

            if (registrationOpt.isEmpty()) {
                return new Response(true, "Not registered", null);
            }

            ActivityRegistrationResponse response = toRegistrationResponse(registrationOpt.get());
            return new Response(true, "Registration status retrieved", response);
        } catch (Exception e) {
            logger.error("Failed to check registration status: {}", e.getMessage(), e);
            return new Response(false, "Failed to check status due to server error", null);
        }
    }

    private ActivityRegistrationResponse toRegistrationResponse(ActivityRegistration registration) {
        ActivityRegistrationResponse response = new ActivityRegistrationResponse();
        response.setId(registration.getId());
        response.setActivityId(registration.getActivity().getId());
        response.setActivityName(registration.getActivity().getName());
        response.setActivityDescription(registration.getActivity().getDescription());
        response.setActivityStartDate(registration.getActivity().getStartDate());
        response.setActivityEndDate(registration.getActivity().getEndDate());
        response.setActivityLocation(registration.getActivity().getLocation());
        response.setStudentId(registration.getStudent().getId());
        response.setStudentName(registration.getStudent().getFullName());
        response.setStudentCode(registration.getStudent().getStudentCode());
        response.setStatus(registration.getStatus());
        response.setFeedback(registration.getFeedback());
        response.setRegisteredDate(registration.getRegisteredDate());
        response.setCreatedAt(registration.getCreatedAt());
        return response;
    }

    private ActivityParticipationResponse toParticipationResponse(ActivityParticipation participation) {
        ActivityParticipationResponse response = new ActivityParticipationResponse();
        response.setId(participation.getId());
        response.setActivityId(participation.getActivity().getId());
        response.setActivityName(participation.getActivity().getName());
        response.setStudentId(participation.getStudent().getId());
        response.setStudentName(participation.getStudent().getFullName());
        response.setStudentCode(participation.getStudent().getStudentCode());
        response.setParticipationType(participation.getParticipationType());
        response.setPointsEarned(participation.getPointsEarned());
        response.setDate(participation.getDate());
        return response;
    }
}
