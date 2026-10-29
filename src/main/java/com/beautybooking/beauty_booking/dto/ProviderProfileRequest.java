
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderProfileRequest(

        @NotBlank(message = "نام کسب‌وکار الزامی است")
        @Size(max = 150, message = "نام کسب‌وکار حداکثر 150 کاراکتر است")
        String providerName,

        @NotNull(message = "نوع کسب‌وکار الزامی است")
        ProviderType providerType,

        @NotBlank(message = "شهر الزامی است")
        @Size(max = 100)
        String city,

        @NotBlank(message = "محله الزامی است")
        @Size(max = 100)
        String neighborhood,

        @NotBlank(message = "آدرس الزامی است")
        @Size(max = 255)
        String address,

        @NotBlank(message = "شماره تماس الزامی است")
        @Pattern(
                regexp = "^[0-9+()\\- ]{7,20}$",
                message = "فرمت شماره تماس معتبر نیست"
        )
        String contactPhone,

        String introduction,

        @Size(max = 255)
        String instagramUrl
) {}
