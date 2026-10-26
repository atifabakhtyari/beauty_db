
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.LoginRequest;
import com.beautybooking.beauty_booking.dto.LoginResponse;
import com.beautybooking.beauty_booking.dto.RegisterRequest;
import com.beautybooking.beauty_booking.dto.RegisterResponse;
import com.beautybooking.beauty_booking.service.AuthService;
import com.beautybooking.beauty_booking.service.LoginService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;

    public AuthController(
            AuthService authService,
            LoginService loginService) {
        this.authService = authService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = loginService.login(request);

        return ResponseEntity.ok(response);
    }
}
