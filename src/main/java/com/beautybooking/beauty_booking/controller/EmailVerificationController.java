
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.VerifyEmailRequest;
import com.beautybooking.beauty_booking.dto.ResendVerificationRequest;
import com.beautybooking.beauty_booking.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    public EmailVerificationController(
            EmailVerificationService verificationService
    ) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        boolean verified = verificationService.verifyCode(
                request.email(),
                request.code()
        );

        if (!verified) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "کد تأیید نامعتبر است، منقضی شده یا قابل استفاده نیست."
            );
        }

        return Map.of(
                "message",
                "ایمیل با موفقیت تأیید شد. اکنون می‌توانید وارد حساب شوید."
        );
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        verificationService.sendVerificationCode(request.email());

        return Map.of(
                "message",
                "اگر ایمیل ثبت شده و هنوز تأیید نشده باشد، کد ارسال خواهد شد."
        );
    }
}
