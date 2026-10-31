
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.LoginRequest;
import com.beautybooking.beauty_booking.dto.LoginResponse;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.repository.UserRepository;
import com.beautybooking.beauty_booking.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public LoginService(
            UserRepository userRepository,
            JwtService jwtService,
            EmailVerificationService emailVerificationService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // مرحله ۱: پیدا کردن کاربر
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "ایمیل یا رمز عبور اشتباه است"
                ));

        // مرحله ۲: بررسی رمز عبور
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "ایمیل یا رمز عبور اشتباه است"
            );
        }

        // مرحله ۳: بررسی تأیید ایمیل
        // این بررسی باید قبل از بررسی فعال بودن حساب انجام شود.
        if (!user.isEmailVerified()) {

            emailVerificationService.sendVerificationCode(email);

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ایمیل شما تأیید نشده است. کد تأیید ارسال شد؛ لطفاً ابتدا ایمیل خود را تأیید کنید."
            );
        }

        // مرحله ۴: بررسی فعال بودن حساب
        if (!user.isActive()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "حساب کاربری غیرفعال است"
            );
        }

        // مرحله ۵: صدور توکن فقط پس از تأیید ایمیل
        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                86400,
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
