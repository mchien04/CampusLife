package vn.campuslife.repository;

import vn.campuslife.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByIsDeletedFalse();
    Optional<Activity> findByIdAndIsDeletedFalse(Long id);
}