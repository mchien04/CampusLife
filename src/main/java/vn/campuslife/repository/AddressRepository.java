package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Address;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByStudentIdAndIsDeletedFalse(Long studentId);

    List<Address> findByProvinceCodeAndIsDeletedFalse(Integer provinceCode);

    List<Address> findByWardCodeAndIsDeletedFalse(Integer wardCode);

    @Query("SELECT DISTINCT a.provinceCode, a.provinceName FROM Address a WHERE a.isDeleted = false ORDER BY a.provinceName")
    List<Object[]> findDistinctProvinces();

    @Query("SELECT DISTINCT a.wardCode, a.wardName FROM Address a WHERE a.provinceCode = :provinceCode AND a.isDeleted = false ORDER BY a.wardName")
    List<Object[]> findWardsByProvince(@Param("provinceCode") Integer provinceCode);
}
