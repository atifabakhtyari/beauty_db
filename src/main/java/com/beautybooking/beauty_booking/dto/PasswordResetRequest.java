package com.beautybooking.beauty_booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "فرمت ایمیل معتبر نیست")
        String email

) {
}