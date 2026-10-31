
package com.beautybooking.beauty_booking.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailVerificationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Resend resend;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            JdbcTemplate jdbcTemplate,
            @Value("${resend.api-key}") String apiKey
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resend = new Resend(apiKey);
    }

    /**
     * تولید و ارسال کد تأیید ایمیل با Resend
     */
    @Transactional
    public void sendVerificationCode(String email) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        Long userId = jdbcTemplate.query(
                """
                SELECT user_id
                FROM public.users
                WHERE LOWER(email) = ?
                """,
                rs -> rs.next() ? rs.getLong("user_id") : null,
                normalizedEmail
        );

        if (userId == null) {
            return;
        }

        Boolean verified = jdbcTemplate.query(
                """
                SELECT email_verified
                FROM public.users
                WHERE user_id = ?
                """,
                rs -> rs.next() && rs.getBoolean("email_verified"),
                userId
        );

        if (Boolean.TRUE.equals(verified)) {
            return;
        }

        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String codeHash = passwordEncoder.encode(code);

        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusMinutes(10);

        jdbcTemplate.update(
                """
                INSERT INTO public.email_verification_codes
                    (user_id, code_hash, expires_at, attempts, created_at)
                VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id)
                DO UPDATE SET
                    code_hash = EXCLUDED.code_hash,
                    expires_at = EXCLUDED.expires_at,
                    attempts = 0,
                    created_at = CURRENT_TIMESTAMP
                """,
                userId,
                codeHash,
                Timestamp.from(expiresAt.toInstant())
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev")
                .to(normalizedEmail)
                .subject("کد تأیید ایمیل نوبتیک")
                .html(
                        "<div style='font-family:Arial,sans-serif;"
                                + "direction:rtl;text-align:center;'>"
                                + "<h2>تأیید ایمیل نوبتیک</h2>"
                                + "<p>کد تأیید ایمیل شما:</p>"
                                + "<h1 style='letter-spacing:8px;'>"
                                + code
                                + "</h1>"
                                + "<p>این کد تا ۱۰ دقیقه اعتبار دارد.</p>"
                                + "<p>اگر این درخواست را انجام نداده‌اید،"
                                + " این پیام را نادیده بگیرید.</p>"
                                + "</div>"
                )
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            // کد تأیید نباید در لاگ ثبت شود.
            logger.error("Resend email sending failed", e);

            throw new IllegalStateException(
                    "ارسال ایمیل تأیید از طریق Resend ناموفق بود.",
                    e
            );
        }
    }

    /**
     * بررسی کد تأیید ایمیل
     */
    @Transactional
    public boolean verifyCode(String email, String code) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }

        List<VerificationRecord> records = jdbcTemplate.query(
                """
                SELECT ev.verification_id,
                       ev.user_id,
                       ev.code_hash,
                       ev.expires_at,
                       ev.attempts,
                       u.email_verified
                FROM public.email_verification_codes ev
                JOIN public.users u
                    ON u.user_id = ev.user_id
                WHERE LOWER(u.email) = ?
                FOR UPDATE OF ev
                """,
                (rs, rowNum) -> new VerificationRecord(
                        rs.getLong("verification_id"),
                        rs.getLong("user_id"),
                        rs.getString("code_hash"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getInt("attempts"),
                        rs.getBoolean("email_verified")
                ),
                normalizedEmail
        );

        if (records.isEmpty()) {
            return false;
        }

        VerificationRecord record = records.get(0);

        if (record.emailVerified()) {
            jdbcTemplate.update(
                    """
                    DELETE FROM public.email_verification_codes
                    WHERE verification_id = ?
                    """,
                    record.verificationId()
            );
            return true;
        }

        if (record.attempts() >= 5) {
            return false;
        }

        if (!record.expiresAt().isAfter(Instant.now())) {
            return false;
        }

        if (!passwordEncoder.matches(code, record.codeHash())) {
            jdbcTemplate.update(
                    """
                    UPDATE public.email_verification_codes
                    SET attempts = attempts + 1
                    WHERE verification_id = ?
                    """,
                    record.verificationId()
            );

            return false;
        }

        jdbcTemplate.update(
                """
                UPDATE public.users
                SET email_verified = TRUE,
                    is_active = TRUE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """,
                record.userId()
        );

        jdbcTemplate.update(
                """
                DELETE FROM public.email_verification_codes
                WHERE verification_id = ?
                """,
                record.verificationId()
        );

        return true;
    }

    private record VerificationRecord(
            Long verificationId,
            Long userId,
            String codeHash,
            Instant expiresAt,
            int attempts,
            boolean emailVerified
    ) {
    }
}
