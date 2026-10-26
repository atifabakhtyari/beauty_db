
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "نام الزامی است")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "نام خانوادگی الزامی است")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "فرمت ایمیل معتبر نیست")
        @Size(max = 255)
        String email,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(min = 8, max = 72,
                message = "رمز عبور باید بین 8 تا 72 کاراکتر باشد")
        String password,

        @NotNull(message = "نقش کاربر الزامی است")
        UserRole role,

        @Positive(message = "سن باید عددی مثبت باشد")
        Integer age
) {
}
