package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.util.TicketCodeUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
            // 1) Kiểm tra activity
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }
            Activity activity = activityOpt.get();

            // 2) Kiểm tra student
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            Student student = studentOpt.get();

            // 3) Đã đăng ký chưa?
            if (registrationRepository.existsByActivityIdAndStudentId(request.getActivityId(), studentId)) {
                return new Response(false, "Already registered for this activity", null);
            }

            // 4) Thời gian mở/đóng đăng ký
            if (activity.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(activity.getRegistrationDeadline().atStartOfDay())) {
                return new Response(false, "Registration deadline has passed", null);
            }
            if (activity.getRegistrationStartDate() != null &&
                    LocalDateTime.now().isBefore(activity.getRegistrationStartDate().atStartOfDay())) {
                return new Response(false, "Registration is not yet open", null);
            }

            // 5) Kiểm tra số lượng vé (nếu giới hạn theo APPROVED)
            if (activity.getTicketQuantity() != null) {
                Long current = registrationRepository
                        .countByActivityIdAndStatus(request.getActivityId(), RegistrationStatus.APPROVED);
                if (current >= activity.getTicketQuantity()) {
                    return new Response(false, "Activity is full", null);
                }
            }

            // 6) Tạo đăng ký + MÃ VÉ
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivity(activity);
            registration.setStudent(student);
            registration.setRegisteredDate(LocalDateTime.now());
            registration.setStatus(RegistrationStatus.PENDING);

            String code;
            int attempts = 0;
            do {
                code = TicketCodeUtils.newTicketCode();
                attempts++;
            } while (registrationRepository.existsByTicketCode(code) && attempts < 3);
            registration.setTicketCode(code);

            ActivityRegistration saved = registrationRepository.save(registration);
            ActivityRegistrationResponse payload = toRegistrationResponse(saved);

            return new Response(true, "Successfully registered for activity", payload);
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

            if (newStatus == RegistrationStatus.APPROVED) {
                boolean exists = participationRepository.existsByRegistration(savedRegistration);
                if (!exists) {
                    ActivityParticipation participation = new ActivityParticipation();
                    participation.setRegistration(savedRegistration);
                    participation.setParticipationType(ParticipationType.REGISTERED);
                    participation.setPointsEarned(BigDecimal.ZERO);
                    participation.setDate(LocalDateTime.now());
                    participationRepository.save(participation);
                }
            }

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

    @Override
    @Transactional
    public Response checkIn(ActivityParticipationRequest request) {
        ActivityRegistration registration;

        // Tìm registration theo ticketCode hoặc studentId
        if (request.getTicketCode() != null && !request.getTicketCode().isBlank()) {
            registration = registrationRepository.findByTicketCode(request.getTicketCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticketCode"));
        } else if (request.getStudentId() != null) {
            registration = registrationRepository.findByStudentIdAndStatus(
                    request.getStudentId(),
                    RegistrationStatus.APPROVED)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký hợp lệ cho sinh viên này"));
        } else {
            return Response.error("Cần cung cấp ticketCode hoặc studentId");
        }

        // Lấy participation đã tạo khi duyệt
        ActivityParticipation participation = participationRepository.findByRegistration(registration)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy participation cho đăng ký này"));

        // Nếu đã check-in rồi thì không cho check-in lại
        if (participation.getParticipationType() == ParticipationType.CHECKED_IN) {
            return Response.error("Sinh viên đã check-in trước đó");
        }

        // Cập nhật trạng thái check-in
        participation.setParticipationType(ParticipationType.CHECKED_IN);
        participation.setPointsEarned(registration.getActivity().getMaxPoints());
        participation.setDate(LocalDateTime.now());
        participationRepository.save(participation);

        // Cập nhật status của registration (ví dụ sang ATTENDED)
        registration.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(registration);

        ActivityParticipationResponse resp = new ActivityParticipationResponse(
                participation.getId(),
                registration.getActivity().getId(),
                registration.getActivity().getName(),
                registration.getStudent().getId(),
                registration.getStudent().getFullName(),
                registration.getStudent().getStudentCode(),
                participation.getParticipationType(),
                participation.getPointsEarned(),
                participation.getDate(),
                request.getNotes());

        return Response.success("Check-in thành công", resp);
    }

    private ActivityRegistrationResponse toRegistrationResponse(ActivityRegistration r) {
        Activity a = r.getActivity();
        Student s = r.getStudent();
        ActivityRegistrationResponse res = new ActivityRegistrationResponse();
        res.setId(r.getId());
        res.setActivityId(a.getId());
        res.setActivityName(a.getName());
        res.setActivityDescription(a.getDescription());
        res.setActivityStartDate(a.getStartDate());
        res.setActivityEndDate(a.getEndDate());
        res.setActivityLocation(a.getLocation());
        res.setStudentId(s.getId());
        res.setStudentName(s.getFullName());
        res.setStudentCode(s.getStudentCode());
        res.setStatus(r.getStatus());
        res.setRegisteredDate(r.getRegisteredDate());
        res.setCreatedAt(r.getCreatedAt());
        res.setTicketCode(r.getTicketCode());
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Response getParticipationReport(Long activityId) {
        // Lấy tất cả registration đã duyệt
        List<ActivityRegistration> approvedRegs = registrationRepository.findByActivityIdAndStatus(activityId,
                RegistrationStatus.APPROVED);

        // Lấy tất cả participation đã CHECKED_IN
        List<ActivityParticipation> checkedInList = participationRepository
                .findByActivityIdAndParticipationType(activityId, ParticipationType.CHECKED_IN);

        Set<Long> checkedInStudentIds = checkedInList.stream()
                .map(ap -> ap.getRegistration().getStudent().getId())
                .collect(Collectors.toSet());

        // Phân loại
        List<StudentResponse> attended = new ArrayList<>();
        List<StudentResponse> notAttended = new ArrayList<>();

        for (ActivityRegistration reg : approvedRegs) {
            Student s = reg.getStudent();
            StudentResponse dto = StudentResponse.fromEntity(s);

            if (checkedInStudentIds.contains(s.getId())) {
                attended.add(dto);
            } else {
                notAttended.add(dto);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attended", attended);
        result.put("notAttended", notAttended);

        return Response.success("Danh sách tham gia", result);
    }

}
