
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.entity.BlacklistedToken;
import com.beautybooking.beauty_booking.repository.BlacklistedTokenRepository;
import com.beautybooking.beauty_booking.security.JwtService;

import io.jsonwebtoken.Claims;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class LogoutService {

    private final BlacklistedTokenRepository tokenRepository;
    private final JwtService jwtService;

    public LogoutService(
            BlacklistedTokenRepository tokenRepository,
            JwtService jwtService) {
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void logout(String token) {

        Claims claims = jwtService.extractClaims(token);

        String tokenHash = hashToken(token);

        if (!tokenRepository.existsByTokenHash(tokenHash)) {

            BlacklistedToken blacklistedToken =
                    new BlacklistedToken(
                            tokenHash,
                            claims.getExpiration().toInstant()
                                    .atOffset(java.time.ZoneOffset.UTC)
                    );

            tokenRepository.save(blacklistedToken);
        }
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available", ex
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String tokenHash = hashToken(token);
        return tokenRepository.existsByTokenHash(tokenHash);
    }

}
