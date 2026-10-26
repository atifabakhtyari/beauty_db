
package com.beautybooking.beauty_booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "فرمت ایمیل معتبر نیست")
        String email,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(max = 72, message = "رمز عبور نامعتبر است")
        String password
) {
}
