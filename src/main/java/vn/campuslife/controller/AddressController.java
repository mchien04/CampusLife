package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.AddressService;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final StudentService studentService;

    /**
     * Lấy danh sách tỉnh/thành phố
     */
    @GetMapping("/provinces")
    public ResponseEntity<Response> getProvinces() {
        try {
            Response response = addressService.getProvinces();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get provinces: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách phường/xã theo mã tỉnh
     */
    @GetMapping("/provinces/{provinceCode}/wards")
    public ResponseEntity<Response> getWardsByProvince(@PathVariable Integer provinceCode) {
        try {
            Response response = addressService.getWardsByProvince(provinceCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get wards: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin địa chỉ của student hiện tại
     */
    @GetMapping("/my")
    public ResponseEntity<Response> getMyAddress(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = addressService.getStudentAddress(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get address: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật địa chỉ của student hiện tại
     */
    @PutMapping("/my")
    public ResponseEntity<Response> updateMyAddress(@RequestParam Integer provinceCode,
            @RequestParam String provinceName,
            @RequestParam Integer wardCode,
            @RequestParam String wardName,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = addressService.updateStudentAddress(studentId, provinceCode, provinceName,
                    wardCode, wardName, street, note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update address: " + e.getMessage(), null));
        }
    }

    /**
     * Tạo địa chỉ mới cho student hiện tại
     */
    @PostMapping("/my")
    public ResponseEntity<Response> createMyAddress(@RequestParam Integer provinceCode,
            @RequestParam String provinceName,
            @RequestParam Integer wardCode,
            @RequestParam String wardName,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = addressService.createStudentAddress(studentId, provinceCode, provinceName,
                    wardCode, wardName, street, note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create address: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa địa chỉ của student hiện tại
     */
    @DeleteMapping("/my")
    public ResponseEntity<Response> deleteMyAddress(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = addressService.deleteStudentAddress(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete address: " + e.getMessage(), null));
        }
    }

    /**
     * Tìm kiếm địa chỉ theo từ khóa
     */
    @GetMapping("/search")
    public ResponseEntity<Response> searchAddresses(@RequestParam String keyword) {
        try {
            Response response = addressService.searchAddresses(keyword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to search addresses: " + e.getMessage(), null));
        }
    }

    /**
     * Tải dữ liệu tỉnh/thành phố từ GitHub
     */
    @PostMapping("/load-data")
    public ResponseEntity<Response> loadProvinceData() {
        try {
            Response response = addressService.loadProvinceDataFromGitHub();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to load province data: " + e.getMessage(), null));
        }
    }

    /**
     * Helper method to get student ID from authentication
     */
    private Long getStudentIdFromAuth(Authentication authentication) {
        try {
            if (authentication == null) {
                return null;
            }
            String username = authentication.getName();
            return studentService.getStudentIdByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }
}
