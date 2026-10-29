
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.RegisterRequest;
import com.beautybooking.beauty_booking.dto.RegisterResponse;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.entity.UserRole;
import com.beautybooking.beauty_booking.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (request.role() != UserRole.CUSTOMER
                && request.role() != UserRole.PROVIDER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نقش انتخاب‌شده برای ثبت‌نام عمومی مجاز نیست"
            );
        }

        if (request.role() == UserRole.CUSTOMER
                && (request.age() == null || request.age() <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "سن معتبر برای مشتری الزامی است"
            );
        }

        if (request.role() == UserRole.PROVIDER
                && request.age() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "برای ارائه‌دهنده نباید سن در درخواست ثبت‌نام ارسال شود"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "این ایمیل قبلاً ثبت شده است"
            );
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setRole(request.role());

        // مشتری سن دارد؛ برای ارائه‌دهنده مقدار سن null است.
        user.setAge(
                request.role() == UserRole.CUSTOMER
                        ? request.age()
                        : null
        );

        // تا زمان تأیید کد ایمیل، حساب فعال نیست.
        user.setEmailVerified(false);
        user.setActive(false);

        User savedUser = userRepository.saveAndFlush(user);

        // ارسال کد تأیید به ایمیل ثبت‌شده
        emailVerificationService.sendVerificationCode(
                savedUser.getEmail()
        );

        return new RegisterResponse(
                savedUser.getUserId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getAge()
        );
    }
}
