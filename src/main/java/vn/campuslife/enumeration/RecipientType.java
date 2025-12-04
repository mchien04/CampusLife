package vn.campuslife.enumeration;

public enum RecipientType {
    INDIVIDUAL,              // Gửi cá nhân
    BULK,                    // Gửi bulk (nhiều người)
    ACTIVITY_REGISTRATIONS,  // Danh sách đăng ký activity
    SERIES_REGISTRATIONS,    // Danh sách đăng ký series
    ALL_STUDENTS,            // Tất cả sinh viên
    BY_CLASS,                // Sinh viên theo lớp
    BY_DEPARTMENT,           // Sinh viên theo khoa
    CUSTOM_LIST              // Danh sách user IDs tùy chọn
}

