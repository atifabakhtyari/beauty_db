
package com.beautybooking.beauty_booking.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("resourceOwnershipService")
public class ResourceOwnershipService {

    private final JdbcTemplate jdbcTemplate;

    public ResourceOwnershipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // آیا این پروفایل متعلق به این ارائه‌دهنده است؟
    public boolean isProviderOwnerOfProfile(
            Long providerId, String email) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM providers p
                JOIN users u ON u.user_id = p.user_id
                WHERE p.provider_id = ?
                  AND LOWER(u.email) = LOWER(?)
                  AND u.role = 'PROVIDER'
            )
            """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        sql, Boolean.class, providerId, email
                )
        );
    }

    // آیا این خدمت متعلق به این ارائه‌دهنده است؟
    public boolean isProviderOwnerOfService(
            Long serviceId, String email) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM services s
                JOIN providers p ON p.provider_id = s.provider_id
                JOIN users u ON u.user_id = p.user_id
                WHERE s.service_id = ?
                  AND LOWER(u.email) = LOWER(?)
                  AND u.role = 'PROVIDER'
            )
            """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        sql, Boolean.class, serviceId, email
                )
        );
    }

    // آیا این نوبت متعلق به این مشتری است؟
    public boolean isCustomerOwnerOfAppointment(
            Long appointmentId, String email) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM appointments a
                JOIN users u ON u.user_id = a.customer_id
                WHERE a.appointment_id = ?
                  AND LOWER(u.email) = LOWER(?)
                  AND u.role = 'CUSTOMER'
            )
            """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        sql, Boolean.class, appointmentId, email
                )
        );
    }

    // آیا این نوبت متعلق به ارائه‌دهنده فعلی است؟
    public boolean isProviderOwnerOfAppointment(
            Long appointmentId, String email) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM appointments a
                JOIN providers p ON p.provider_id = a.provider_id
                JOIN users u ON u.user_id = p.user_id
                WHERE a.appointment_id = ?
                  AND LOWER(u.email) = LOWER(?)
                  AND u.role = 'PROVIDER'
            )
            """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        sql, Boolean.class, appointmentId, email
                )
        );
    }
}
