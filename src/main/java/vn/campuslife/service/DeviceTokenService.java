package vn.campuslife.service;

import org.springframework.stereotype.Service;
import vn.campuslife.model.DeviceToken;
import vn.campuslife.repository.DeviceTokenRepository;
import vn.campuslife.repository.UserRepository;

import java.time.LocalDateTime;
@Service
public class DeviceTokenService {

    private final UserRepository userRepo;
    private final DeviceTokenRepository tokenRepo;

    public DeviceTokenService(UserRepository userRepo,
                              DeviceTokenRepository tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }

    public Long getUserIdByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    public void upsert(Long userId, String token) {
        DeviceToken dt = tokenRepo.findAllByUserId(userId)
                .stream()
                .findFirst()
                .orElse(new DeviceToken());

        dt.setUserId(userId);
        dt.setToken(token);
        dt.setUpdatedAt(LocalDateTime.now());

        tokenRepo.save(dt);
    }

}
