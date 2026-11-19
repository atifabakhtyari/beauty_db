package com.beautybooking.beauty_booking.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Long serviceId,
        String serviceName,
        BigDecimal price,
        Integer durationMinutes,
        String categoryId,
        String categoryName,
        boolean isActive
) {
}