package vn.campuslife.enumeration;

public enum ScoreType {
    REN_LUYEN(false),         // Điểm rèn luyện
    CONG_TAC_XA_HOI(true),    // Điểm công tác xã hội (tích lũy suốt 4 năm)
    CHUYEN_DE(true);          // Điểm chuyên đề doanh nghiệp (tích lũy suốt 4 năm)

    private final boolean cumulative;

    ScoreType(boolean cumulative) {
        this.cumulative = cumulative;
    }

    public boolean isCumulative() {
        return cumulative;
    }
}