package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

@Entity
@Table(name = "activity_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    @Comment("Sự kiện")
    private Activity activity;

    @Column(nullable = false, length = 500)
    @Comment("Đường dẫn ảnh")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    @Comment("Mô tả ảnh (tùy chọn)")
    private String caption;

    @Column(nullable = false)
    @Comment("Thứ tự hiển thị")
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 100)
    @Comment("Người upload (username)")
    private String uploadedBy;

    @Column(nullable = false)
    @Comment("Cờ xóa mềm")
    private boolean isDeleted = false;

    @CreatedDate
    @Comment("Ngày tạo")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Comment("Ngày cập nhật")
    private LocalDateTime updatedAt;
}

