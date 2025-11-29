package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.Address;

import java.time.LocalDateTime;

/**
 * Response DTO cho Address
 * Tránh circular reference khi serialize JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private Long studentId;
    private Integer provinceCode;
    private String provinceName;
    private Integer wardCode;
    private String wardName;
    private String street;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddressResponse fromEntity(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        if (address.getStudent() != null) {
            response.setStudentId(address.getStudent().getId());
        }
        response.setProvinceCode(address.getProvinceCode());
        response.setProvinceName(address.getProvinceName());
        response.setWardCode(address.getWardCode());
        response.setWardName(address.getWardName());
        response.setStreet(address.getStreet());
        response.setNote(address.getNote());
        response.setCreatedAt(address.getCreatedAt());
        response.setUpdatedAt(address.getUpdatedAt());
        return response;
    }
}

