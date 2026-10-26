
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.UserRole;

public record RegisterResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        Integer age
) {
}
