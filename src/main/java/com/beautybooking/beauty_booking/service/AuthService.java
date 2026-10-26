
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.RegisterRequest;
import com.beautybooking.beauty_booking.dto.RegisterResponse;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.entity.UserRole;
import com.beautybooking.beauty_booking.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // ثبت‌نام عمومی فقط برای مشتری و ارائه‌دهنده است.
        if (request.role() != UserRole.CUSTOMER
                && request.role() != UserRole.PROVIDER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نقش انتخاب‌شده برای ثبت‌نام عمومی مجاز نیست"
            );
        }

        // مشتری باید سن داشته باشد.
        if (request.role() == UserRole.CUSTOMER
                && request.age() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "وارد کردن سن برای مشتری الزامی است"
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

        // هرگز رمز خام را ذخیره نکن.
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setRole(request.role());
        user.setAge(request.age());
        user.setActive(true);

        User savedUser = userRepository.save(user);

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
