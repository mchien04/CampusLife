package vn.campuslife.model.recommend;

public class PythonRecommendResponse {
    private Long activityId;
    private Double score;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
