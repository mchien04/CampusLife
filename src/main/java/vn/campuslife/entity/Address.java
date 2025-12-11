package vn.campuslife.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", unique = true, nullable = false)
    @JsonBackReference
    private Student student;

    @Column(nullable = false)
    private Integer provinceCode; // matinhTMS từ JSON

    @Column(nullable = false)
    private String provinceName; // tentinhmoi từ JSON

    @Column(nullable = false)
    private Integer wardCode; // maphuongxa từ JSON

    @Column(nullable = false)
    private String wardName; // tenphuongxa từ JSON

    @Column
    private String street; // Địa chỉ cụ thể (số nhà, tên đường)

    @Column
    private String note; // Ghi chú thêm

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;
}
