# Sequence Diagram — Recommendation Module (Gợi ý hoạt động)

## UC-O.55: Gợi ý hoạt động cho sinh viên

```mermaid
sequenceDiagram
    autonumber
    participant S as Student
    participant C as Client (React)
    participant CTL as RecommendationController
    participant SVC as RecommendationService
    participant SR as StudentRepository
    participant RR as RegistrationRepository
    participant AR as ActivityRepository
    participant DB as Database
    participant AI as AI/MLService

    %% ======================== PHASE 1: REQUEST ========================
    Note over S,C: [Phase 1] Student Request Recommendations
    S->>C: 1. Truy cập trang "Gợi ý hoạt động"
    C->>C: 2. Lấy studentId từ auth token / context
    C->>CTL: 3. GET /api/recommendation/students/{id}/recommendations?limit=10
    Note right of C: Headers: Authorization Bearer <token>\nQuery params: limit=10 (default)

    CTL->>CTL: 4. Validate studentId / extract from Principal
    CTL->>SVC: 5. getRecommendations(studentId, limit=10)

    %% ======================== PHASE 2: STUDENT PROFILE ========================
    Note over SVC,SR: [Phase 2] Retrieve Student Profile & Context
    SVC->>SR: 6. findStudentById(studentId)
    SR->>DB: 7. SELECT * FROM students WHERE id = ? AND status = 'ACTIVE'
    DB-->>SR:' "8. Student record (id, name, major, interests, academic_year)"'
    SR-->>SVC: 9. Optional<Student>
    SVC->>SVC: 10. Validate student exists

    SVC->>RR: 11. findRegisteredActivityIdsByStudentId(studentId)
    RR->>DB: 12. SELECT activity_id FROM registrations WHERE student_id = ? AND status IN ('CONFIRMED','PENDING')
    DB-->>RR:' "13. List<activity_id> (registered activity IDs)"'
    RR-->>SVC:' "14. Set<Long> registeredIds"'

    %% ======================== PHASE 3: CANDIDATE ACTIVITIES ========================
    Note over SVC,AR: [Phase 3] Retrieve Candidate Activities
    SVC->>AR: 15. findOpenActivitiesExcluding(studentId, registeredIds)
    AR->>DB: 16. SELECT a.* FROM activities a LEFT JOIN registrations r ON a.id = r.activity_id WHERE a.status = 'OPEN' AND (r.student_id IS NULL OR r.student_id != ?) AND a.deadline > NOW() AND a.remaining_slots > 0
    DB-->>AR:' "17. List<Activity> (open, not registered, has slots, not expired)"'
    AR-->>SVC:' "18. List<Activity> candidateActivities"'

    SVC->>SVC: 19. Validate candidateActivities not empty
    alt candidateActivities is empty
        SVC-->>CTL:' "20. EmptyList<RecommendationDTO>"'
        CTL-->>C:' "21. 200 OK + [] (no recommendations available)"'
        C-->>S:' "22. Hiển thị "Chưa có hoạt động phù hợp""'
    else candidateActivities exists

        %% ======================== PHASE 4: SCORING ========================
        Note over SVC,AI: [Phase 4] Calculate Matching Score
        alt Rule-Based Approach (default)
            Note over SVC: Rule-based scoring logic
            loop For each activity in candidateActivities
                SVC->>SVC: 23a. score += majorRelevance(activity.categories, student.major) * 0.30
                SVC->>SVC: 23b. score += similarPastActivityScore(activity, student.history) * 0.25
                SVC->>SVC: 23c. score += availabilityScore(activity.remaining_slots) * 0.20
                SVC->>SVC: 23d. score += timeFitScore(activity.schedule, student.freeTime) * 0.25
                SVC->>SVC: 23e. normalize score to [0, 100]
                SVC->>SVC: 23f. generateReason(activity, topMatchingFactor)
            end
            SVC->>SVC: 24. Sort candidateActivities by score descending
            SVC->>SVC: 25. Limit to top N (10)

        else AI/ML-Based Approach (enabled via feature flag)
            SVC->>AI: 26. POST /ml/predict-recommendations
            Note right of SVC: Payload: {\n  studentProfile: {major, interests, year, history},\n  candidates: [...]\n}
            AI->>AI: 27. Load ML model (collaborative filtering / content-based hybrid)
            AI->>AI: 28. Vectorize student profile & activity features
            AI->>AI: 29. Predict match scores via trained model
            AI-->>SVC: 30. List<PredictionResult> (activityId, mlScore, confidence)
            SVC->>SVC: 31. Merge ML scores with candidate activities
            SVC->>SVC: 32. Sort by mlScore descending
            SVC->>SVC: 33. Limit to top N (10)
        end

        %% ======================== PHASE 5: RESPONSE ========================
        Note over SVC,CTL: [Phase 5] Build & Return Response
        SVC->>SVC: 34. Map to RecommendationDTO (activityId, name, matchScore, reason, thumbnail, deadline, remainingSlots)
        SVC-->>CTL:' "35. List<RecommendationDTO> topRecommendations"'
        CTL-->>C:' "36. 200 OK + JSON payload"'
        Note right of CTL: Response: {\n  studentId: 123,\n  recommendations: [\n    {activityId, name, matchScore, reason, ...},\n    ...\n  ],\n  generatedAt: "2025-01-15T10:30:00Z"\n}
        C->>C: 37. Parse JSON & render UI components
        C-->>S:' "38. Display recommendation cards (ranked by matchScore)"'
        Note over S: UI shows: Tên hoạt động, Độ phù hợp (%%),\nLý do gợi ý, Slot còn lại, Deadline
    end
```

---

## Tóm tắt thành phần và chức năng

| Thành phần | Vai trò | Chức năng chính trong luồng Recommendation |
|-----------|---------|---------------------------------------------|
| **Student** | Actor | Người dùng cuối — sinh viên truy cập trang gợi ý và nhận danh sách hoạt động được đề xuất. |
| **Client (React)** | Frontend | Điều hướng đến trang gợi ý, lấy `studentId` từ auth context, gửi HTTP GET request, render danh sách kết quả dưới dạng UI cards. |
| **RecommendationController** | Controller (Spring Boot) | Nhận request `GET /api/recommendation/students/{id}/recommendations`, validate đầu vào, gọi `RecommendationService`, trả về HTTP 200 + JSON. |
| **RecommendationService** | Service Layer | Điều phối toàn bộ luồng nghiệp vụ: lấy profile sinh viên, truy vấn hoạt động đang mở, tính điểm matching (rule-based hoặc AI-based), sort, giới hạn top N, và map sang DTO. |
| **StudentRepository** | Repository | Truy vấn thông tin sinh viên (ngành học, sở thích, năm học) từ Database. |
| **RegistrationRepository** | Repository | Truy vấn danh sách hoạt động mà sinh viên đã đăng ký (để loại trừ khỏi danh sách gợi ý). |
| **ActivityRepository** | Repository | Truy vấn các hoạt động đang ở trạng thái `OPEN`, chưa hết hạn, còn slot trống, và chưa được sinh viên đăng ký. |
| **Database** | Persistent Storage | Lưu trữ dữ liệu sinh viên, hoạt động, đăng ký. Thực thi các câu lệnh SQL SELECT. |
| **AI/MLService** | External Service (Optional) | Cung cấp khả năng dự đoán điểm matching thông qua mô hình ML (collaborative filtering / content-based hybrid). Chỉ được gọi khi feature flag `ml.recommendation.enabled=true`. |

### Các yếu tố tính điểm matching (Rule-Based)

| Yếu tố | Trọng số | Mô tả |
|--------|----------|-------|
| **Major Relevance** | 30% | Độ liên quan giữa ngành học của sinh viên và danh mục/lĩnh vực của hoạt động. |
| **Similar Past Activities** | 25% | Sinh viên đã từng tham gia hoạt động tương tự (cùng category, tag, hoặc organizer). |
| **Available Slots** | 20% | Số slot còn trống của hoạt động — ưu tiên hoạt động còn nhiều slot để tăng khả năng đăng ký thành công. |
| **Time Fit** | 25% | Thời gian diễn ra hoạt động có phù hợp với lịch rảnh / thời gian biểu của sinh viên không. |

### Ghi chú kỹ thuật

- **Endpoint:** `GET /api/recommendation/students/{id}/recommendations?limit={n}`
- **Phân quyền:** Sinh viên chỉ có thể xem gợi ý của chính mình (hoặc admin có thể xem của tất cả).
- **Caching:** Kết quả gợi ý có thể được cache (Redis) với TTL ngắn (e.g., 5 phút) để giảm tải tính toán.
- **Fallback:** Nếu AI/MLService không khả dụng hoặc timeout, hệ thống tự động fallback về Rule-Based Approach.
- **Pagination:** Mặc định trả về top 10, có thể điều chỉnh qua query param `limit`.
