
package com.beautybooking.beauty_booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateServiceRequest(

        @Size(max = 150,
                message = "نام خدمت نباید بیشتر از 150 کاراکتر باشد")
        String serviceName,

        @DecimalMin(value = "0.0", inclusive = false,
                message = "قیمت باید بیشتر از صفر باشد")
        BigDecimal price,

        @Min(value = 5, message = "مدت زمان حداقل 5 دقیقه است")
        @Max(value = 600, message = "مدت زمان حداکثر 600 دقیقه است")
        Integer durationMinutes,

        @Size(max = 50, message = "شناسه دسته‌بندی نامعتبر است")
        @Pattern(regexp = ".*\\S.*",
                message = "شناسه دسته‌بندی نمی‌تواند خالی باشد")
        String categoryId

) {
}
