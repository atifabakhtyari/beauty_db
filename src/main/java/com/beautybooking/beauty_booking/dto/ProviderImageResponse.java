
package com.beautybooking.beauty_booking.dto;

import java.time.OffsetDateTime;

public record ProviderImageResponse(
        Long imageId,
        String imageUrl,
        String imageType,
        Integer displayOrder,
        OffsetDateTime createdAt
) {
}
