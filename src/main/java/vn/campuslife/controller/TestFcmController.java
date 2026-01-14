package vn.campuslife.controller;
import lombok.RequiredArgsConstructor;
import vn.campuslife.repository.DeviceTokenRepository;
import vn.campuslife.service.FcmService;
import vn.campuslife.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestFcmController {

    private final FcmService fcmService;
    private final DeviceTokenRepository deviceTokenRepository;

    @GetMapping("/push")
    public String testPush() {
        deviceTokenRepository.findAll().forEach(dt -> {
            Map<String, String> data = new HashMap<>();
            data.put("type", "TEST");
            data.put("msg", "hello");

            fcmService.send(
                    dt.getToken(),
                    " Test Push",
                    "Push từ CampusLife BE",
                    data
            );
        });
        return "sent";
    }
}
