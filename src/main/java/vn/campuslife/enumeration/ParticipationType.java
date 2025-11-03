package vn.campuslife.enumeration;

public enum ParticipationType {
    REGISTERED, // Đã đăng ký
    CHECKED_IN, // Đã check-in (lần 1)
    CHECKED_OUT, // Đã check-out (lần 2)
    ATTENDED, // Hoàn thành cả 2 lần check
    COMPLETED // Đã chấm điểm (đạt hoặc không đạt)
}
