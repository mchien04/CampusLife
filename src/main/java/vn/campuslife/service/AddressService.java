package vn.campuslife.service;

import vn.campuslife.model.Response;

import java.util.List;
import java.util.Map;

public interface AddressService {

    // Lấy danh sách tỉnh/thành phố
    Response getProvinces();

    // Lấy danh sách phường/xã theo mã tỉnh
    Response getWardsByProvince(Integer provinceCode);

    // Lấy thông tin địa chỉ của student
    Response getStudentAddress(Long studentId);

    // Cập nhật địa chỉ của student
    Response updateStudentAddress(Long studentId, Integer provinceCode, String provinceName,
            Integer wardCode, String wardName, String street, String note);

    // Tạo địa chỉ mới cho student
    Response createStudentAddress(Long studentId, Integer provinceCode, String provinceName,
            Integer wardCode, String wardName, String street, String note);

    // Xóa địa chỉ của student
    Response deleteStudentAddress(Long studentId);

    // Tìm kiếm địa chỉ theo từ khóa
    Response searchAddresses(String keyword);

    // Lấy dữ liệu JSON từ GitHub
    Response loadProvinceDataFromGitHub();
}
