package com.group01.controller;

import com.group01.dto.CurrentUserResponse;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Small endpoints that show who is logged in and prove the role rules of SecurityConfig. */
@RestController
public class AccountController {

    @GetMapping("/api/me")
    public CurrentUserResponse me(Authentication authentication) {
        return new CurrentUserResponse(authentication.getName(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }

    @GetMapping("/api/member/ping")
    public Map<String, String> memberPing(Authentication authentication) {
        return Map.of("message", "Hello member", "email", authentication.getName());
    }

    @GetMapping("/api/admin/ping")
    public Map<String, String> adminPing(Authentication authentication) {
        return Map.of("message", "Hello administrator", "email", authentication.getName());
    }
}
