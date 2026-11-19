
package com.beautybooking.beauty_booking.config;

import com.beautybooking.beauty_booking.dto.ApiErrorResponse;
import com.beautybooking.beauty_booking.repository.UserRepository;
import com.beautybooking.beauty_booking.security.JwtAuthenticationFilter;
import com.beautybooking.beauty_booking.security.JwtService;
import com.beautybooking.beauty_booking.service.LogoutService;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // پاسخ‌های خطای امنیتی باید همان قالب و فرمت زمانی بقیه API را داشته باشند.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(
                    com.fasterxml.jackson.databind
                            .SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
            );

    // احراز هویت فقط با JWT انجام می‌شود. این Bean عمداً خالی است تا
    // Spring Boot کاربر پیش‌فرض «user» با رمز تصادفی نسازد و آن رمز را در
    // لاگ‌ها چاپ نکند. هیچ مسیری از ورود با نام کاربری/رمز عبور استفاده نمی‌کند.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Username/password authentication is not enabled"
            );
        };
    }

    // ساخت فیلتر JWT
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            LogoutService logoutService
    ) {
        return new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                logoutService
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // غیرفعال کردن Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // مدیریت خطاهای امنیتی
                .exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint((request, response, ex) ->
                                writeSecurityError(
                                        response,
                                        request,
                                        HttpStatus.UNAUTHORIZED,
                                        "Authentication is required. Please log in."
                                )
                        )

                        .accessDeniedHandler((request, response, ex) ->
                                writeSecurityError(
                                        response,
                                        request,
                                        HttpStatus.FORBIDDEN,
                                        "You do not have permission to access this resource."
                                )
                        )
                )

                // تعیین دسترسی مسیرهای API
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // مسیرهای عمومی احراز هویت
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/verify",
                                "/api/auth/password-reset/confirm"
                        ).permitAll()

                        // مستندات Swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // نمایش عکس پروفایل برای همه کاربران
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/public/profile-images/**"
                        ).permitAll()

                        // پروفایل عمومی سالن و تصاویر آن
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/public/providers/**",
                                "/api/public/provider-images/**"
                        ).permitAll()

                        // مسیرهای مخصوص نقش‌ها
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/v1/provider/**")
                        .hasRole("PROVIDER")

                        .requestMatchers("/api/provider/**")
                        .hasRole("PROVIDER")

                        .requestMatchers("/api/customer/**")
                        .hasRole("CUSTOMER")

                        // سایر مسیرها نیازمند احراز هویت هستند
                        .anyRequest().authenticated()
                )

                // اضافه کردن فیلتر JWT
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // ساخت پاسخ خطای امنیتی
    private void writeSecurityError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
