
package com.beautybooking.beauty_booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "رمز عبور باید حداقل ۸ کاراکتر باشد")
        String newPassword

) {
}
