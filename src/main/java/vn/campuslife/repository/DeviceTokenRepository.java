package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.model.DeviceToken;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository
        extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findAllByUserId(Long userId);

}
