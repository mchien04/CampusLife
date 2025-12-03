package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivitySeriesService;

import java.util.Map;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class ActivitySeriesController {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySeriesController.class);

    private final ActivitySeriesService seriesService;
    private final vn.campuslife.service.StudentService studentService;

    /**
     * Tạo chuỗi sự kiện mới
     */
    @PostMapping
    public ResponseEntity<Response> createSeries(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Series name is required", null));
            }

            String description = (String) request.get("description");
            String milestonePoints = request.get("milestonePoints") != null
                    ? request.get("milestonePoints").toString()
                    : null;
            
            String scoreTypeStr = request.get("scoreType") != null
                    ? request.get("scoreType").toString()
                    : "REN_LUYEN";
            vn.campuslife.enumeration.ScoreType scoreType;
            try {
                scoreType = vn.campuslife.enumeration.ScoreType.valueOf(scoreTypeStr);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid ScoreType: {}", scoreTypeStr);
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Invalid ScoreType: " + scoreTypeStr, null));
            }

            Long mainActivityId = null;
            if (request.get("mainActivityId") != null) {
                try {
                    mainActivityId = Long.valueOf(request.get("mainActivityId").toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid mainActivityId: {}", request.get("mainActivityId"));
                }
            }

            java.time.LocalDateTime registrationStartDate = null;
            if (request.get("registrationStartDate") != null) {
                try {
                    registrationStartDate = java.time.LocalDateTime.parse(request.get("registrationStartDate").toString());
                } catch (Exception e) {
                    logger.error("Invalid registrationStartDate format: {}", request.get("registrationStartDate"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid registrationStartDate format", null));
                }
            }
            
            java.time.LocalDateTime registrationDeadline = null;
            if (request.get("registrationDeadline") != null) {
                try {
                    registrationDeadline = java.time.LocalDateTime.parse(request.get("registrationDeadline").toString());
                } catch (Exception e) {
                    logger.error("Invalid registrationDeadline format: {}", request.get("registrationDeadline"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid registrationDeadline format", null));
                }
            }
            
            Boolean requiresApproval = request.get("requiresApproval") != null
                    ? Boolean.valueOf(request.get("requiresApproval").toString())
                    : true;
            Integer ticketQuantity = null;
            if (request.get("ticketQuantity") != null) {
                try {
                    ticketQuantity = Integer.valueOf(request.get("ticketQuantity").toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid ticketQuantity: {}", request.get("ticketQuantity"));
                }
            }

            Response response = seriesService.createSeries(name, description, milestonePoints, scoreType,
                    mainActivityId,
                    registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when creating series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to create series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create series: " + e.getMessage(), null));
        }
    }

    /**
     * Tạo activity trong series với các thuộc tính tối giản
     */
    @PostMapping("/{seriesId}/activities/create")
    public ResponseEntity<Response> createActivityInSeries(
            @PathVariable Long seriesId,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Activity name is required", null));
            }

            String description = (String) request.get("description");
            
            java.time.LocalDateTime startDate = null;
            if (request.get("startDate") != null) {
                try {
                    startDate = java.time.LocalDateTime.parse(request.get("startDate").toString());
                } catch (Exception e) {
                    logger.error("Invalid startDate format: {}", request.get("startDate"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid startDate format", null));
                }
            }
            
            java.time.LocalDateTime endDate = null;
            if (request.get("endDate") != null) {
                try {
                    endDate = java.time.LocalDateTime.parse(request.get("endDate").toString());
                } catch (Exception e) {
                    logger.error("Invalid endDate format: {}", request.get("endDate"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid endDate format", null));
                }
            }
            
            String location = (String) request.get("location");
            Integer order = null;
            if (request.get("order") != null) {
                try {
                    order = Integer.valueOf(request.get("order").toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid order: {}", request.get("order"));
                }
            }

            String shareLink = (String) request.get("shareLink");
            String bannerUrl = (String) request.get("bannerUrl");
            String benefits = (String) request.get("benefits");
            String requirements = (String) request.get("requirements");
            String contactInfo = (String) request.get("contactInfo");
            
            java.util.List<Long> organizerIds = null;
            if (request.get("organizerIds") != null) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> ids = (java.util.List<Object>) request.get("organizerIds");
                    organizerIds = ids.stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(java.util.stream.Collectors.toList());
                } catch (Exception e) {
                    logger.warn("Invalid organizerIds: {}", request.get("organizerIds"), e);
                }
            }

            Response response = seriesService.createActivityInSeries(seriesId, name, description,
                    startDate, endDate, location, order, shareLink, bannerUrl,
                    benefits, requirements, contactInfo, organizerIds);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when creating activity in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to create activity in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create activity in series: " + e.getMessage(), null));
        }
    }

    /**
     * Student đăng ký series (tự động đăng ký tất cả activities trong series)
     */
    @PostMapping("/{seriesId}/register")
    public ResponseEntity<Response> registerForSeries(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            // Get student ID from authentication
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);

            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = seriesService.registerForSeries(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to register for series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to register for series: " + e.getMessage(), null));
        }
    }

    /**
     * Thêm activity vào chuỗi
     */
    @PostMapping("/{seriesId}/activities")
    public ResponseEntity<Response> addActivityToSeries(
            @PathVariable Long seriesId,
            @RequestBody Map<String, Object> request) {
        try {
            Long activityId = Long.valueOf(request.get("activityId").toString());
            Integer order = request.get("order") != null
                    ? Integer.valueOf(request.get("order").toString())
                    : null;

            Response response = seriesService.addActivityToSeries(activityId, seriesId, order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to add activity to series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to add activity to series: " + e.getMessage(), null));
        }
    }

    /**
     * Tính điểm milestone cho student
     */
    @PostMapping("/{seriesId}/students/{studentId}/calculate-milestone")
    public ResponseEntity<Response> calculateMilestone(
            @PathVariable Long seriesId,
            @PathVariable Long studentId) {
        try {
            Response response = seriesService.calculateMilestonePoints(studentId, seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to calculate milestone: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy tất cả chuỗi sự kiện
     */
    @GetMapping
    public ResponseEntity<Response> getAllSeries() {
        try {
            Response response = seriesService.getAllSeries();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get all series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get all series: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy chuỗi sự kiện theo ID
     */
    @GetMapping("/{seriesId}")
    public ResponseEntity<Response> getSeriesById(@PathVariable Long seriesId) {
        try {
            Response response = seriesService.getSeriesById(seriesId);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get series: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách activities trong series
     */
    @GetMapping("/{seriesId}/activities")
    public ResponseEntity<Response> getActivitiesInSeries(@PathVariable Long seriesId) {
        try {
            Response response = seriesService.getActivitiesInSeries(seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get activities in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get activities in series: " + e.getMessage(), null));
        }
    }

    /**
     * Student xem progress của chính mình trong series
     */
    @GetMapping("/{seriesId}/progress/my")
    public ResponseEntity<Response> getMyProgress(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            // Get student ID from authentication
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);

            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = seriesService.getStudentProgress(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get my progress: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get my progress: " + e.getMessage(), null));
        }
    }

    /**
     * Admin/Manager xem progress của student trong series
     */
    @GetMapping("/{seriesId}/students/{studentId}/progress")
    public ResponseEntity<Response> getStudentProgress(
            @PathVariable Long seriesId,
            @PathVariable Long studentId) {
        try {
            Response response = seriesService.getStudentProgress(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get student progress: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get student progress: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật thông tin chuỗi sự kiện
     */
    @PutMapping("/{seriesId}")
    public ResponseEntity<Response> updateSeries(
            @PathVariable Long seriesId,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String milestonePoints = request.get("milestonePoints") != null
                    ? request.get("milestonePoints").toString()
                    : null;
            
            String scoreTypeStr = request.get("scoreType") != null
                    ? request.get("scoreType").toString()
                    : null;
            vn.campuslife.enumeration.ScoreType scoreType = null;
            if (scoreTypeStr != null) {
                try {
                    scoreType = vn.campuslife.enumeration.ScoreType.valueOf(scoreTypeStr);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid ScoreType: {}", scoreTypeStr);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid ScoreType: " + scoreTypeStr, null));
                }
            }

            Long mainActivityId = null;
            if (request.get("mainActivityId") != null) {
                try {
                    Object mainActivityIdObj = request.get("mainActivityId");
                    if (mainActivityIdObj != null && !mainActivityIdObj.toString().equals("null")) {
                        mainActivityId = Long.valueOf(mainActivityIdObj.toString());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid mainActivityId: {}", request.get("mainActivityId"));
                }
            }

            java.time.LocalDateTime registrationStartDate = null;
            if (request.get("registrationStartDate") != null) {
                try {
                    registrationStartDate = java.time.LocalDateTime.parse(request.get("registrationStartDate").toString());
                } catch (Exception e) {
                    logger.error("Invalid registrationStartDate format: {}", request.get("registrationStartDate"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid registrationStartDate format", null));
                }
            }
            
            java.time.LocalDateTime registrationDeadline = null;
            if (request.get("registrationDeadline") != null) {
                try {
                    registrationDeadline = java.time.LocalDateTime.parse(request.get("registrationDeadline").toString());
                } catch (Exception e) {
                    logger.error("Invalid registrationDeadline format: {}", request.get("registrationDeadline"), e);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid registrationDeadline format", null));
                }
            }
            
            Boolean requiresApproval = null;
            if (request.get("requiresApproval") != null) {
                requiresApproval = Boolean.valueOf(request.get("requiresApproval").toString());
            }
            
            Integer ticketQuantity = null;
            if (request.get("ticketQuantity") != null) {
                try {
                    ticketQuantity = Integer.valueOf(request.get("ticketQuantity").toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid ticketQuantity: {}", request.get("ticketQuantity"));
                }
            }

            Response response = seriesService.updateSeries(seriesId, name, description, milestonePoints, scoreType,
                    mainActivityId, registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when updating series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to update series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update series: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa chuỗi sự kiện (soft delete)
     */
    @DeleteMapping("/{seriesId}")
    public ResponseEntity<Response> deleteSeries(@PathVariable Long seriesId) {
        try {
            Response response = seriesService.deleteSeries(seriesId);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Failed to delete series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete series: " + e.getMessage(), null));
        }
    }
}
