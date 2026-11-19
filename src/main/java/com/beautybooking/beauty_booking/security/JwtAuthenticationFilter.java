
package com.beautybooking.beauty_booking.security;

import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.repository.UserRepository;
import com.beautybooking.beauty_booking.service.LogoutService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final LogoutService logoutService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            LogoutService logoutService) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.logoutService = logoutService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7).trim();

            try {
                // مرحله ۱: بررسی امضا و تاریخ انقضای JWT
                Claims claims = jwtService.extractClaims(token);

                // مرحله ۲: بررسی لیست سیاه
                if (logoutService.isTokenBlacklisted(token)) {

                    SecurityContextHolder.clearContext();

                    filterChain.doFilter(request, response);
                    return;
                }

                String email = claims.getSubject();

                if (email == null || email.isBlank()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // مرحله ۳: دریافت اطلاعات کاربر از دیتابیس
                User user = userRepository
                        .findByEmailIgnoreCase(email)
                        .orElse(null);

                // مرحله ۴: بررسی وجود کاربر
                if (user == null) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // مرحله ۵: بررسی فعال بودن حساب
                if (!user.isActive()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // مرحله ۶: بررسی تأیید ایمیل
                if (!user.isEmailVerified()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // مرحله ۷: رد توکن‌هایی که پیش از آخرین تغییر رمز صادر شده‌اند
                if (isTokenOlderThanPasswordChange(claims, user)) {

                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // مرحله ۸: دریافت نقش کاربر
                String currentRole = user.getRole().name();

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + currentRole
                                        )
                                )
                        );

                // مرحله ۹: ثبت احراز هویت
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException ex) {

                // توکن نامعتبر یا منقضی‌شده
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenOlderThanPasswordChange(
            Claims claims,
            User user
    ) {
        if (claims.getIssuedAt() == null ||
                user.getPasswordChangedAt() == null) {
            return false;
        }

        // JWT زمان صدور را با دقت ثانیه ذخیره می‌کند؛
        // زمان تغییر رمز نیز به ثانیه کوتاه می‌شود.
        return claims.getIssuedAt().toInstant().isBefore(
                user.getPasswordChangedAt()
                        .toInstant()
                        .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        );
    }
}
