package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;

import java.util.List;

public interface ActivityPhotoService {
    /**
     * Upload photos for an activity (only after activity ends)
     * @param activityId ID của sự kiện
     * @param files Danh sách file ảnh
     * @param captions Danh sách caption tương ứng (có thể null hoặc ít hơn số file)
     * @param uploadedBy Username của người upload
     * @return Response với danh sách ảnh đã upload
     */
    Response uploadPhotos(Long activityId, List<MultipartFile> files, List<String> captions, String uploadedBy);

    /**
     * Lấy danh sách ảnh của một sự kiện
     * @param activityId ID của sự kiện
     * @return Response với danh sách ảnh
     */
    Response getActivityPhotos(Long activityId);

    /**
     * Xóa một ảnh (soft delete)
     * @param photoId ID của ảnh
     * @param username Username của người xóa (để kiểm tra quyền)
     * @return Response
     */
    Response deletePhoto(Long photoId, String username);

    /**
     * Cập nhật thứ tự hiển thị của ảnh
     * @param photoId ID của ảnh
     * @param newOrder Thứ tự mới
     * @param username Username của người cập nhật
     * @return Response
     */
    Response updatePhotoOrder(Long photoId, Integer newOrder, String username);
}

