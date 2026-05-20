package vn.campuslife.model.recommend;

import java.util.List;

public class PythonRecommendRequest {
    private String userProfile;
    private List<PythonActivityItem> activities;

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    public List<PythonActivityItem> getActivities() {
        return activities;
    }

    public void setActivities(List<PythonActivityItem> activities) {
        this.activities = activities;
    }
}
