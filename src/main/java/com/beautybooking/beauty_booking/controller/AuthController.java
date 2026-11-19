
package com.beautybooking.beauty_booking.controller;

// DTO imports
import com.beautybooking.beauty_booking.dto.LoginRequest;
import com.beautybooking.beauty_booking.dto.LoginResponse;
import com.beautybooking.beauty_booking.dto.RegisterRequest;
import com.beautybooking.beauty_booking.dto.RegisterResponse;
import com.beautybooking.beauty_booking.dto.PasswordResetRequest;
import com.beautybooking.beauty_booking.dto.PasswordResetVerifyRequest;
import com.beautybooking.beauty_booking.dto.PasswordResetConfirmRequest;

// Service imports
import com.beautybooking.beauty_booking.service.AuthService;
import com.beautybooking.beauty_booking.service.LoginService;
import com.beautybooking.beauty_booking.service.PasswordResetService;
import com.beautybooking.beauty_booking.service.LogoutService;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;
    private final LogoutService logoutService;

    // Constructor
    public AuthController(
            AuthService authService,
            LoginService loginService,
            PasswordResetService passwordResetService,
            LogoutService logoutService
    ) {
        this.authService = authService;
        this.loginService = loginService;
        this.passwordResetService = passwordResetService;
        this.logoutService = logoutService;
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = loginService.login(request);

        return ResponseEntity.ok(response);
    }

    // Logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {

        String token = authorizationHeader.substring(7).trim();

        logoutService.logout(token);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "با موفقیت از حساب خود خارج شدید."
                )
        );
    }

    // Password Reset - Request Code
    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {

        passwordResetService.requestReset(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "اگر حساب فعالی با این ایمیل وجود داشته باشد، کد بازیابی ارسال می‌شود."
                )
        );
    }

    // Password Reset - Verify Code
    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> verifyPasswordReset(
            @Valid @RequestBody PasswordResetVerifyRequest request
    ) {

        boolean verified =
                passwordResetService.verifyCode(request);

        if (!verified) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "کد واردشده نامعتبر، منقضی یا تعداد تلاش‌ها بیش از حد مجاز است."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "کد با موفقیت تأیید شد."
                )
        );
    }

    // Password Reset - Confirm New Password
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {

        passwordResetService.confirmReset(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "رمز عبور شما با موفقیت تغییر کرد."
                )
        );
    }
}
