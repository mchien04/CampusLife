# Plan — Sequence Diagram V2 for CampusLife

## Objective
Tổng hợp và vẽ toàn bộ sequence diagram (cả 34 sequence cũ + 56 sequence mới) cho hệ thống CampusLife, phân nhóm theo chức năng, lưu vào `docs/sequence-diagramV2/`.

## Format mẫu (tham khảo)
- Mỗi file markdown chứa 1 nhóm chức năng
- Sử dụng Mermaid `sequenceDiagram`
- Các participant: `UserRole`, `Client/Frontend`, `Controller`, `Service`, `Repository`, `Database` (và các thành phần khác nếu cần: `EmailService`, `NotificationService`, `FileStorage`, `AI Service`, v.v.)
- Có `Note over` để mô tả luồng
- Request: `User->>Client->>Controller->>Service->>Repository->>Database`
- Response: `Database-->>Repository-->>Service-->>Controller-->>Client-->>User`
- Mỗi sequence có mô tả, các bước chi tiết, và ghi chú đặc điểm

## Cấu trúc file (19 file)

| # | File | Nhóm | Sequence bao gồm |
|---|------|------|-----------------|
| 1 | `sequence-diagram-auth.md` | Auth | 3.3.1 Login, 3.3.2 Logout, 3.3.3 Đổi mật khẩu, 3.3.4 Đăng ký, A.1 Self-register, A.2 Forgot-password, A.3 Verify email |
| 2 | `sequence-diagram-academic.md` | Academic | B.4 CRUD AcademicYear, B.5 CRUD Semester, B.6 Toggle Semester, B.7 Initialize scores |
| 3 | `sequence-diagram-department.md` | Department | C.8 CRUD Department, C.9 Get Departments |
| 4 | `sequence-diagram-student-class.md` | Student & Class | D.10 Upload Excel, D.11 CRUD Class, D.12 Class membership, D.13 Search student, D.14 Send credentials, D.15 Address management |
| 5 | `sequence-diagram-activity.md` | Activity | 3.3.14-17 (CRUD + List), E.16 Publish/Unpublish, E.17 Copy, E.18 Preset preview, E.19 Image management |
| 6 | `sequence-diagram-activity-series.md` | Activity Series | F.20 CRUD Series, F.21 Attach activity, F.22 Register series, F.23 Progress, F.24 Overview, F.25 Milestone |
| 7 | `sequence-diagram-minigame-quiz.md` | Minigame/Quiz | G.26 Create, G.27 Start, G.28 Submit, G.29 History |
| 8 | `sequence-diagram-articles.md` | Event Articles | H.30 Create/Publish, H.31 View/Search, H.32 Comment, H.33 Wishlist, H.34 Track views |
| 9 | `sequence-diagram-preparation.md` | Preparation | I.35 Toggle prep, I.36 Organizers, I.37 Task assignment, I.38 Budget, I.39 Fund advance, I.40 Expense |
| 10 | `sequence-diagram-preparation-export.md` | Prep Export | J.41 Financial export, J.42 Operational export |
| 11 | `sequence-diagram-statistics.md` | Statistics | K.43 Dashboard, K.44 Activity/Student stats, K.45 Score breakdown |
| 12 | `sequence-diagram-score.md` | Score & Submission | 3.3.9 Chấm điểm, 3.3.10 Nộp bài, 3.3.11 Sửa bài, 3.3.24 Xem điểm chi tiết, 3.3.25 Xem tổng điểm, 3.3.26 Xếp hạng, L.46 History, L.47 Recalculate |
| 13 | `sequence-diagram-notification.md` | Notification | 3.3.7 Nhắc nhở, M.48 Notifications, M.49 Bulk send, M.50 Send email, M.51 Device token |
| 14 | `sequence-diagram-registration.md` | Registration | 3.3.5 Hủy ĐK, 3.3.6 Phê duyệt, 3.3.8 Lịch sử, 3.3.12 Check-in, 3.3.13 Check-out, 3.3.22 Xem vé, 3.3.23 Kiểm tra vé, N.52 QR check-in, N.53 Validate ticket, N.54 Backfill |
| 15 | `sequence-diagram-recommendation.md` | Recommendation | O.55 Recommendations |
| 16 | `sequence-diagram-chatbot.md` | Chatbot | P.56 Chat with AI |
| 17 | `sequence-diagram-user-management.md` | User Mgmt | 3.3.18 Tạo TK, 3.3.19 Xem DS, 3.3.20 Sửa, 3.3.21 Xóa |
| 18 | `sequence-diagram-profile.md` | Profile | 3.3.27 Xem info (Admin/Manager), 3.3.26 Xem info (Student) |
| 19 | `sequence-diagram-task-management.md` | Task Mgmt | 3.3.28-31 CRUD Task, 3.3.32 Xem phân công, 3.3.33 Phân công, 3.3.34 Hủy phân công |

## Execution
- Stage 1: Tạo cấu trúc thư mục ✅
- Stage 2: Dispatch sub-agent song song để vẽ từng file (batch theo nhóm)
- Stage 3: Kiểm tra và xác nhận hoàn thành
