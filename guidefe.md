1) Điều hướng UI (Student-only)
Android app chỉ dành cho STUDENT. Các thao tác ADMIN/MANAGER (bật Preparation, thêm BTC, giao việc, duyệt chi phí) thực hiện trên web/admin.
1.1. Thêm mục “Công tác chuẩn bị” cho Student
Bạn có 2 cách phù hợp với kiến trúc Android hiện có (MVC + Activity/Fragment):

Cách khuyến nghị (ít đụng core): thêm 1 card/nút trong HomeFragment → mở StudentPreparationActivity.
Cách đồng bộ với app nhiều màn: thêm 1 item trong menu (nếu có drawer/menu list) → mở StudentPreparationActivity.
Luồng:

HomeFragment/ Menu → Công tác chuẩn bị
Màn danh sách hiển thị các event mà user là BTC + Preparation đang bật
Click 1 card → StudentPreparationDetailActivity → hiển thị UI “Chuẩn bị sự kiện” (Tasks + Finance)
2) API cần dùng (Student)
Dựa theo module BE hiện tại.

Lưu ý quan trọng về format response:
- Tất cả API trả về wrapper `Response` với 3 field: `status` (boolean), `message` (string), `body` (data).
- Android phải đọc data từ `body` (không phải `data`).

GET /api/student/profile → lấy studentId (đang có trong app)
GET /api/activities → lấy danh sách activity (đang có)
GET /api/preparation/my/activity-ids → lấy danh sách activityId mà user hiện tại thuộc BTC và Preparation đang bật
GET /api/preparation/activities/{activityId}/dashboard
Trả hasPreparation=true/false
Quyền: Organizer hoặc Admin/Manager. Student thường sẽ 403 nếu không phải organizer.
PUT /api/preparation/tasks/{taskId}/status body: { "status": "ACCEPTED" }
GET /api/preparation/activities/{activityId}/expenses?status=ALL|PENDING|APPROVED|REJECTED
Lưu ý: PENDING ở query chính là WAITING_APPROVAL
POST /api/preparation/activities/{activityId}/expenses/evidence (multipart file) → trả {url}
POST /api/preparation/activities/{activityId}/expenses body: {amount, description, evidenceUrl}
Map trạng thái chi phí:

approved = null → WAITING_APPROVAL
approved = true → APPROVED
approved = false → REJECTED
3) Chiến lược lấy danh sách “event có công tác chuẩn bị”
Không dùng `organizerIds` trong `ActivityResponse` để lọc BTC.
- `organizerIds` trong `ActivityResponse` là danh sách Department tổ chức (activity_departments), không phải danh sách Student thuộc BTC (activity_organizers).

Luồng khuyến nghị (ít request, đúng nghiệp vụ BTC):
1) GET /api/preparation/my/activity-ids → trả List<Long> activityId (đã lọc hasPreparation=true)
2) Với mỗi activityId trong list:
   - GET /api/preparation/activities/{activityId}/dashboard để lấy `tasks` + `budget`
   - (tuỳ UI) GET /api/activities/{activityId} để lấy banner/name/time/location

Nếu muốn tối giản call /api/activities/{activityId}:
- Có thể lấy trước list từ GET /api/activities rồi map theo id, nhưng phải join theo activityId lấy từ `my/activity-ids`.

4) Thiết kế màn hình Android
4.1. StudentPreparationActivity (Danh sách)
UI gợi ý

Toolbar title: “Công tác chuẩn bị”
Spinner/Chip filter: ALL | UPCOMING | ONGOING | ENDED (tuỳ bạn)
RecyclerView hiển thị Card:
Banner (Glide)
Tên sự kiện
Thời gian, địa điểm
Badge “BTC”
Quick stats (lấy từ dashboard):
tasks.size, pendingTasksCount
nếu có budget: remainingAmount, không có budget: financeMessage
Khi click card

Intent qua StudentPreparationDetailActivity kèm activityId
4.2. StudentPreparationDetailActivity (Chi tiết công tác chuẩn bị)
Header

Card mô tả ngắn gọn sự kiện: banner + name + time + location
Nội dung Dùng TabLayout + ViewPager2 (hoặc 2 section trong 1 scroll) gồm:

Tab 1: Nhiệm vụ
RecyclerView list PreparationTask
Mỗi item:
title, description, deadline, assigneeName
status chip: PENDING/ACCEPTED/COMPLETED
Nếu assigneeId == myStudentId: cho phép đổi status
UI đơn giản: Spinner 3 giá trị hoặc 2 nút “Nhận việc”, “Hoàn thành”
Gọi PUT /tasks/{id}/status
Tab 2: Tài chính (chỉ hiển thị nếu dashboard có budget != null)
3 card nhỏ: Total / Spent (APPROVED) / Remaining
Filter spinner: ALL | WAITING_APPROVAL | APPROVED | REJECTED
map sang query: ALL | PENDING | APPROVED | REJECTED
RecyclerView list expense:
amount (format VND)
description, createdAt, reportedByName
status chip theo approved
evidence thumbnail (nếu có) → click mở dialog/phóng to (Glide)
Nút “+ Thêm chi phí”

Mở BottomSheetDialog:
amount (EditText)
description (EditText)
nút chọn ảnh/chụp ảnh hóa đơn
preview thumbnail
Flow submit:
Upload evidence (multipart) → evidenceUrl
Create expense (JSON) → status WAITING_APPROVAL
Sau submit: reload expenses + reload dashboard để cập nhật remaining/spent.
5) Retrofit interfaces (gợi ý)
Giả sử app đang có ApiClient, AuthInterceptor, ApiResponse wrapper tương tự mô tả.

Java



Wrapper đúng với BE:

public class ApiResponse<T> {
  public boolean status;
  public String message;
  public T body;
}

public interface PreparationApi {
  @GET("/api/preparation/my/activity-ids")
  Call<ApiResponse<List<Long>>> myPreparationActivityIds();

  @GET("/api/preparation/activities/{activityId}/dashboard")
  Call<ApiResponse<PreparationDashboardDto>> getDashboard(@Path("activityId") long activityId);

  @PUT("/api/preparation/tasks/{taskId}/status")
  Call<ApiResponse<PreparationTaskDto>> updateTaskStatus(
      @Path("taskId") long taskId,
      @Body UpdateTaskStatusRequest body
  );

  @GET("/api/preparation/activities/{activityId}/expenses")
  Call<ApiResponse<List<ExpenseDto>>> listExpenses(
      @Path("activityId") long activityId,
      @Query("status") String status
  );

  @Multipart
  @POST("/api/preparation/activities/{activityId}/expenses/evidence")
  Call<ApiResponse<UploadResultDto>> uploadEvidence(
      @Path("activityId") long activityId,
      @Part MultipartBody.Part file
  );

  @POST("/api/preparation/activities/{activityId}/expenses")
  Call<ApiResponse<ExpenseDto>> createExpense(
      @Path("activityId") long activityId,
      @Body CreateExpenseRequest body
  );
}

DTO tương ứng bám đúng BE. Gợi ý cho `LocalDateTime` (deadline/createdAt): map sang `String` (ISO) để parse thủ công.

6) Ảnh minh chứng (Android)
Chụp ảnh: ActivityResultContracts.TakePicture() (lưu ra file qua FileProvider)
Chọn ảnh: ActivityResultContracts.GetContent()
Nén ảnh trước upload (khuyến nghị):
decode bitmap theo kích thước tối đa (vd 1280px)
compress JPEG quality 70–80
ghi ra file tạm → upload file đó qua Retrofit multipart
Hiển thị ảnh: Glide (trong báo cáo đã dùng Glide)
7) Format tiền và ngày
Tiền VND: NumberFormat.getCurrencyInstance(new Locale("vi","VN"))
Ngày giờ: parse ISO 2026-03-15T00:30:38... bằng java.time (API 26+) hoặc ThreeTenABP nếu minSDK thấp.
8) Danh sách file nên tạo (gợi ý theo cấu trúc báo cáo)
activity/StudentPreparationActivity.java
activity/StudentPreparationDetailActivity.java
fragment/PreparationTasksFragment.java
fragment/PreparationFinanceFragment.java
adapter/PreparationEventAdapter.java
adapter/PreparationTaskAdapter.java
adapter/ExpenseAdapter.java
api/PreparationApi.java
entity/preparation/*Dto.java (PreparationDashboardDto, PreparationTaskDto, BudgetDto, ExpenseDto, UploadResultDto)
utils/ImageCompressUtil.java (nén ảnh)
