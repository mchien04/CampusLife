package vn.campuslife.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.model.*;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.ActivityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySeriesServiceImpl implements ActivitySeriesService {

    private final ActivitySeriesRepository activitySeriesRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;

    @Override
    @Transactional
    public ActivitySeries getSeriesById(Long id) {
        return activitySeriesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuỗi sự kiện"));
    }

    @Override
    @Transactional
    public Response createSeries(CreateActivitySeriesRequest request, String username) {
        try {
            if (request.getName() == null || request.getName().isBlank()) {
                return new Response(false, "Tên chuỗi sự kiện là bắt buộc", null);
            }

            if (request.getStartDate() != null && request.getEndDate() != null &&
                    request.getStartDate().isAfter(request.getEndDate())) {
                return new Response(false, "Ngày bắt đầu phải trước ngày kết thúc", null);
            }

            ActivitySeries series = new ActivitySeries();
            series.setName(request.getName().trim());
            series.setDescription(request.getDescription());
            series.setStartDate(request.getStartDate());
            series.setEndDate(request.getEndDate());
            series.setRequiredParticipationCount(
                    request.getRequiredParticipationCount() != null ? request.getRequiredParticipationCount() : 0
            );
            series.setBonusPoints(
                    request.getBonusPoints() != null ? request.getBonusPoints() : BigDecimal.ZERO
            );

            series.setCreatedBy(username);
            series.setUpdatedBy(username);

            ActivitySeries saved = activitySeriesRepository.save(series);
            return new Response(true, "Tạo chuỗi sự kiện thành công", saved);

        } catch (Exception e) {
            log.error("Lỗi khi tạo chuỗi sự kiện: {}", e.getMessage(), e);
            return new Response(false, "Không thể tạo chuỗi sự kiện: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getMySeries(String username) {
        List<ActivitySeries> seriesList =
                activitySeriesRepository.findAllByCreatedByAndIsDeletedFalseOrderByCreatedAtDesc(username);

        List<ActivitySeriesResponse> dtoList = seriesList.stream()
                .map(s -> new ActivitySeriesResponse(
                        s.getId(),
                        s.getName(),
                        s.getDescription(),
                        s.getStartDate(),
                        s.getEndDate(),
                        s.getRequiredParticipationCount(),
                        s.getBonusPoints(),
                        s.getCreatedBy(),
                        s.getActivities()
                ))
                .toList();

        return new Response(true, "Lấy danh sách thành công", dtoList);
    }

    @Override
    public Response getSeriesEvents(Long id, String username) {
        Optional<ActivitySeries> optSeries = activitySeriesRepository.findById(id);
        if (optSeries.isEmpty()) {
            return new Response(false, "Không tìm thấy chuỗi sự kiện", null);
        }

        ActivitySeries series = optSeries.get();
        if (!series.getCreatedBy().equals(username)) {
            return new Response(false, "Bạn không có quyền xem chuỗi này", null);
        }

        List<Activity> activities = activityRepository.findAllBySeries_IdAndIsDeletedFalse(id);

        List<ActivityResponse> responses = activities.stream().map(a -> {
            ActivityResponse dto = new ActivityResponse();
            dto.setId(a.getId());
            dto.setName(a.getName());
            dto.setType(a.getType());
            dto.setScoreType(a.getScoreType());
            dto.setDescription(a.getDescription());
            dto.setStartDate(a.getStartDate());
            dto.setEndDate(a.getEndDate());
            dto.setLocation(a.getLocation());
            dto.setMaxPoints(a.getMaxPoints());
            dto.setSeriesId(series.getId());
            dto.setSeriesName(series.getName());
            return dto;
        }).toList();

        return new Response(true, "Lấy sự kiện trong chuỗi thành công", responses);
    }

    @Override
    public Response deleteSeries(Long id) {
        try {
            Optional<ActivitySeries> optSeries = activitySeriesRepository.findById(id);
            if (optSeries.isEmpty()) {
                return new Response(false, "Series not found", null);
            }

            ActivitySeries series = optSeries.get();
            List<Activity> activities = activitySeriesRepository.findActivitiesBySeriesId(id);
            for (Activity a : activities) {
                a.setDeleted(true);
            }
            activityRepository.saveAll(activities);

            series.setDeleted(true);
            activitySeriesRepository.save(series);

            return new Response(true, "Series and related activities deleted successfully", null);
        } catch (Exception e) {
            log.error("Failed to delete series: {}", e.getMessage(), e);
            return new Response(false, "Failed to delete series due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateSeries(Long id, CreateActivitySeriesRequest request, String username) {
        try {
            Optional<ActivitySeries> opt = activitySeriesRepository.findById(id);
            if (opt.isEmpty()) {
                return new Response(false, "Không tìm thấy chuỗi sự kiện", null);
            }

            ActivitySeries series = opt.get();
            if (!series.getCreatedBy().equals(username)) {
                return new Response(false, "Bạn không có quyền chỉnh sửa chuỗi sự kiện này", null);
            }

            series.setName(request.getName());
            series.setDescription(request.getDescription());
            series.setStartDate(request.getStartDate());
            series.setEndDate(request.getEndDate());
            series.setRequiredParticipationCount(request.getRequiredParticipationCount());
            series.setBonusPoints(request.getBonusPoints());
            activitySeriesRepository.save(series);

            return new Response(true, "Cập nhật chuỗi sự kiện thành công", null);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật chuỗi sự kiện: {}", e.getMessage(), e);
            return new Response(false, "Lỗi server khi cập nhật chuỗi sự kiện", null);
        }
    }

    @Override
    @Transactional
    public Response deleteEventInSeries(Long seriesId, Long eventId, String username) {
        try {
            Optional<ActivitySeries> optSeries = activitySeriesRepository.findById(seriesId);
            if (optSeries.isEmpty()) {
                return new Response(false, "Không tìm thấy chuỗi sự kiện", null);
            }

            ActivitySeries series = optSeries.get();
            if (!series.getCreatedBy().equals(username)) {
                return new Response(false, "Bạn không có quyền chỉnh sửa chuỗi này", null);
            }

            Optional<Activity> optActivity = activityRepository.findByIdAndSeriesId(eventId, seriesId);
            if (optActivity.isEmpty()) {
                return new Response(false, "Sự kiện này không thuộc chuỗi đã chọn", null);
            }

            Activity activity = optActivity.get();
            activity.setDeleted(true);
            activityRepository.save(activity);

            return new Response(true, "Xóa sự kiện khỏi chuỗi thành công", null);
        } catch (Exception e) {
            log.error("Lỗi khi xóa sự kiện khỏi chuỗi: {}", e.getMessage(), e);
            return new Response(false, "Lỗi server khi xóa sự kiện", null);
        }
    }
    @Override
    public List<ActivitySeries> getAllSeries() {
        return activitySeriesRepository.findAll();
    }


    @Override
    @Transactional
    public Response addEventToSeries(Long seriesId, CreateActivityRequest request, String username) {
        try {
            // chỉ gắn seriesId, không tự thêm vào series nữa
            request.setSeriesId(seriesId);

            // gọi lại createActivity (đã tự xử lý series & minigame)
            Response created = activityService.createActivity(request);
            if (!created.isStatus()) return created;

            log.info("Đã thêm sự kiện vào chuỗi #{} thành công", seriesId);
            return new Response(true, "Thêm sự kiện vào chuỗi thành công", created.getBody());

        } catch (Exception e) {
            log.error("Lỗi khi thêm sự kiện vào chuỗi {}: {}", seriesId, e.getMessage(), e);
            return new Response(false, "Không thể thêm sự kiện vào chuỗi", null);
        }
    }
}
