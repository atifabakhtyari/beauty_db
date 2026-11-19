package com.beautybooking.beauty_booking.dto;

import java.util.List;

public record ServiceCategoryListResponse(
        List<ServiceCategoryResponse> items
) {
}