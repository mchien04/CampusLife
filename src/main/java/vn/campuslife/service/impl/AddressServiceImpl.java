package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.campuslife.entity.Address;
import vn.campuslife.entity.Student;
import vn.campuslife.model.Response;
import vn.campuslife.repository.AddressRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.AddressService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);
    private static final String LOCAL_JSON_PATH = "src/main/resources/data/danhmucxaphuong.json";

    private final AddressRepository addressRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache để tránh đọc file nhiều lần
    private List<Map<String, Object>> cachedProvinceData = null;

    @Override
    public Response getProvinces() {
        try {
            Response provinceDataResponse = loadProvinceDataFromGitHub();
            if (!provinceDataResponse.isStatus()) {
                return new Response(false, "Failed to load province data", null);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> provinceData = (List<Map<String, Object>>) provinceDataResponse.getBody();

            List<Map<String, Object>> provinces = new ArrayList<>();
            for (Map<String, Object> province : provinceData) {
                Map<String, Object> provinceInfo = new HashMap<>();
                provinceInfo.put("code", province.get("matinhTMS"));
                provinceInfo.put("name", province.get("tentinhmoi"));
                provinces.add(provinceInfo);
            }

            return new Response(true, "Provinces retrieved successfully", provinces);
        } catch (Exception e) {
            logger.error("Failed to get provinces: {}", e.getMessage(), e);
            return new Response(false, "Failed to get provinces: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getWardsByProvince(Integer provinceCode) {
        try {
            Response provinceDataResponse = loadProvinceDataFromGitHub();
            if (!provinceDataResponse.isStatus()) {
                return new Response(false, "Failed to load province data", null);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> provinceData = (List<Map<String, Object>>) provinceDataResponse.getBody();

            for (Map<String, Object> province : provinceData) {
                if (province.get("matinhTMS").equals(provinceCode)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> wards = (List<Map<String, Object>>) province.get("phuongxa");
                    return new Response(true, "Wards retrieved successfully", wards);
                }
            }

            return new Response(false, "Province not found", null);
        } catch (Exception e) {
            logger.error("Failed to get wards for province {}: {}", provinceCode, e.getMessage(), e);
            return new Response(false, "Failed to get wards: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentAddress(Long studentId) {
        try {
            Optional<Address> addressOpt = addressRepository.findByStudentIdAndIsDeletedFalse(studentId);
            if (addressOpt.isEmpty()) {
                return new Response(false, "Address not found", null);
            }

            Address address = addressOpt.get();
            Map<String, Object> addressInfo = new HashMap<>();
            addressInfo.put("id", address.getId());
            addressInfo.put("provinceCode", address.getProvinceCode());
            addressInfo.put("provinceName", address.getProvinceName());
            addressInfo.put("wardCode", address.getWardCode());
            addressInfo.put("wardName", address.getWardName());
            addressInfo.put("street", address.getStreet());
            addressInfo.put("note", address.getNote());
            addressInfo.put("createdAt", address.getCreatedAt());
            addressInfo.put("updatedAt", address.getUpdatedAt());

            return new Response(true, "Address retrieved successfully", addressInfo);
        } catch (Exception e) {
            logger.error("Failed to get student address: {}", e.getMessage(), e);
            return new Response(false, "Failed to get address: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response updateStudentAddress(Long studentId, Integer provinceCode, String provinceName,
            Integer wardCode, String wardName, String street, String note) {
        try {
            Optional<Address> addressOpt = addressRepository.findByStudentIdAndIsDeletedFalse(studentId);
            if (addressOpt.isEmpty()) {
                return createStudentAddress(studentId, provinceCode, provinceName, wardCode, wardName, street, note);
            }

            Address address = addressOpt.get();
            address.setProvinceCode(provinceCode);
            address.setProvinceName(provinceName);
            address.setWardCode(wardCode);
            address.setWardName(wardName);
            address.setStreet(street);
            address.setNote(note);

            addressRepository.save(address);
            return new Response(true, "Address updated successfully", address);
        } catch (Exception e) {
            logger.error("Failed to update student address: {}", e.getMessage(), e);
            return new Response(false, "Failed to update address: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response createStudentAddress(Long studentId, Integer provinceCode, String provinceName,
            Integer wardCode, String wardName, String street, String note) {
        try {
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            // Check if address already exists
            Optional<Address> existingAddress = addressRepository.findByStudentIdAndIsDeletedFalse(studentId);
            if (existingAddress.isPresent()) {
                return new Response(false, "Address already exists for this student", null);
            }

            Address address = new Address();
            address.setStudent(studentOpt.get());
            address.setProvinceCode(provinceCode);
            address.setProvinceName(provinceName);
            address.setWardCode(wardCode);
            address.setWardName(wardName);
            address.setStreet(street);
            address.setNote(note);

            addressRepository.save(address);
            return new Response(true, "Address created successfully", address);
        } catch (Exception e) {
            logger.error("Failed to create student address: {}", e.getMessage(), e);
            return new Response(false, "Failed to create address: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response deleteStudentAddress(Long studentId) {
        try {
            Optional<Address> addressOpt = addressRepository.findByStudentIdAndIsDeletedFalse(studentId);
            if (addressOpt.isEmpty()) {
                return new Response(false, "Address not found", null);
            }

            Address address = addressOpt.get();
            address.setDeleted(true);
            addressRepository.save(address);

            return new Response(true, "Address deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete student address: {}", e.getMessage(), e);
            return new Response(false, "Failed to delete address: " + e.getMessage(), null);
        }
    }

    @Override
    public Response searchAddresses(String keyword) {
        try {
            // Simple search implementation - can be enhanced with more sophisticated search
            List<Address> addresses = addressRepository.findAll();
            List<Map<String, Object>> results = new ArrayList<>();

            for (Address address : addresses) {
                if (address.getProvinceName().toLowerCase().contains(keyword.toLowerCase()) ||
                        address.getWardName().toLowerCase().contains(keyword.toLowerCase()) ||
                        (address.getStreet() != null
                                && address.getStreet().toLowerCase().contains(keyword.toLowerCase()))) {

                    Map<String, Object> addressInfo = new HashMap<>();
                    addressInfo.put("id", address.getId());
                    addressInfo.put("provinceName", address.getProvinceName());
                    addressInfo.put("wardName", address.getWardName());
                    addressInfo.put("street", address.getStreet());
                    results.add(addressInfo);
                }
            }

            return new Response(true, "Search completed", results);
        } catch (Exception e) {
            logger.error("Failed to search addresses: {}", e.getMessage(), e);
            return new Response(false, "Failed to search addresses: " + e.getMessage(), null);
        }
    }

    @Override
    public Response loadProvinceDataFromGitHub() {
        try {
            // Sử dụng cache nếu đã có dữ liệu
            if (cachedProvinceData != null) {
                return new Response(true, "Province data loaded from cache", cachedProvinceData);
            }

            // Đọc từ file local
            ClassPathResource resource = new ClassPathResource("data/danhmucxaphuong.json");
            List<Map<String, Object>> provinceData = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // Cache dữ liệu
            cachedProvinceData = provinceData;

            return new Response(true, "Province data loaded successfully", provinceData);
        } catch (Exception e) {
            logger.error("Failed to load province data from local file: {}", e.getMessage(), e);
            return new Response(false, "Failed to load province data: " + e.getMessage(), null);
        }
    }

}
