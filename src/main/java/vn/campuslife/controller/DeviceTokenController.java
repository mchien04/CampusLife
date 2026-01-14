package vn.campuslife.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.RegisterTokenReq;
import vn.campuslife.service.DeviceTokenService;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService service;

    public DeviceTokenController(DeviceTokenService service) {
        this.service = service;
    }

    @PostMapping
    public void register(Authentication authentication,
                         @RequestBody RegisterTokenReq req) {

        String username = authentication.getName(); // email / username
        Long userId = service.getUserIdByUsername(username);

        service.upsert(userId, req.getToken());
    }
}
