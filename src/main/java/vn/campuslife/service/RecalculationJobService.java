package vn.campuslife.service;

import vn.campuslife.model.Response;

public interface RecalculationJobService {
    Response startAsyncRecalculation(Long semesterId, Long createdBy);
    Response getJobStatus(Long jobId);
    Response retryFailedJob(Long jobId);
    void cleanupStaleJobs();
}
