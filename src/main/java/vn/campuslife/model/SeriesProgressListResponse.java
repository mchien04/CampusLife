package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesProgressListResponse {
    private Long seriesId;
    private String seriesName;
    private Integer totalActivities;
    private Long totalRegistered; // tổng số SV đã đăng ký
    private List<SeriesProgressItemResponse> progressList;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Long totalElements;
}

