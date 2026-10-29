
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.ProviderApprovalStatus;
import com.beautybooking.beauty_booking.entity.ProviderType;

public record ProviderProfileResponse(
        Long providerId,
        Long userId,
        String providerName,
        ProviderType providerType,
        String city,
        String neighborhood,
        String address,
        String contactPhone,
        String introduction,
        String instagramUrl,
        boolean active,
        ProviderApprovalStatus approvalStatus
) {}
