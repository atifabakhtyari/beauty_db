
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
}
