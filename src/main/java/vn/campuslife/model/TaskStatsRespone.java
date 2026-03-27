package vn.campuslife.model;

public class TaskStatsRespone {
    public long getTotalTasks() {
        return totalTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getPendingTasks() {
        return pendingTasks;
    }

    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;

    public TaskStatsRespone(long totalTasks, long completedTasks, long pendingTasks) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.pendingTasks = pendingTasks;
    }

}
