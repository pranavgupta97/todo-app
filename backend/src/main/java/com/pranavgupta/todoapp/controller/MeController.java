package com.pranavgupta.todoapp.controller;

import com.pranavgupta.todoapp.dto.UserResponse;
import com.pranavgupta.todoapp.security.AppOidcUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints concerning the current authenticated user.
 *
 * <p>Sign-out is handled by Spring Security's built-in {@code POST /logout}
 * (configured in {@code SecurityConfig}) — no controller method needed.</p>
 */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppOidcUser principal) {
        return new UserResponse(
                principal.getAppUserId(),
                principal.getEmail(),
                principal.getFullName()
        );
    }
}
