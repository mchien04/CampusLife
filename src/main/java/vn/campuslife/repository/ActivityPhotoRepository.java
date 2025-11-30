package vn.campuslife.repository;

import vn.campuslife.entity.ActivityPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

    /**
     * Lấy danh sách ảnh theo activity ID, sắp xếp theo displayOrder
     */
    @Query("SELECT ap FROM ActivityPhoto ap " +
            "WHERE ap.activity.id = :activityId " +
            "AND ap.isDeleted = false " +
            "ORDER BY ap.displayOrder ASC, ap.createdAt ASC")
    List<ActivityPhoto> findByActivityIdAndIsDeletedFalseOrderByDisplayOrderAsc(@Param("activityId") Long activityId);

    /**
     * Đếm số lượng ảnh của một activity (chưa bị xóa)
     */
    @Query("SELECT COUNT(ap) FROM ActivityPhoto ap " +
            "WHERE ap.activity.id = :activityId " +
            "AND ap.isDeleted = false")
    Long countByActivityIdAndIsDeletedFalse(@Param("activityId") Long activityId);
    //hien tat ca anh
    List<ActivityPhoto> findByIsDeletedFalseOrderByCreatedAtDesc();

}

