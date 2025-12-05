package vn.campuslife.enumeration;

public enum RecipientType {
    BULK, // Gửi theo danh sách user IDs (có thể 1 hoặc nhiều)
    ACTIVITY_REGISTRATIONS, // Danh sách đăng ký activity
    SERIES_REGISTRATIONS, // Danh sách đăng ký series
    ALL_STUDENTS, // Tất cả sinh viên
    BY_CLASS, // Sinh viên theo lớp
    BY_DEPARTMENT // Sinh viên theo khoa
}
