
package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository
        extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByTokenHash(String tokenHash);
}
