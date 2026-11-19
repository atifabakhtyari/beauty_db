
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.PasswordResetRequest;
import com.beautybooking.beauty_booking.dto.PasswordResetVerifyRequest;
import com.beautybooking.beauty_booking.dto.PasswordResetConfirmRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class PasswordResetService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final PasswordResetRateLimiter rateLimiter;
    private final int codeExpiryMinutes;
    private final int maxCodeAttempts;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            JdbcTemplate jdbcTemplate,
            JavaMailSender mailSender,
            PasswordResetRateLimiter rateLimiter,
            @Value("${spring.mail.username}") String senderEmail,
            @Value("${app.password-reset.code-expiry-minutes:10}")
            int codeExpiryMinutes,
            @Value("${app.password-reset.max-code-attempts:5}")
            int maxCodeAttempts
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
        this.senderEmail = senderEmail;
        this.codeExpiryMinutes = codeExpiryMinutes;
        this.maxCodeAttempts = maxCodeAttempts;
    }

    // مرحله اول: درخواست کد بازیابی
    @Transactional
    public void requestReset(PasswordResetRequest request) {

        String email = normalizeEmail(request.email());

        // محدودیت نرخ برای همه ایمیل‌ها یکسان اعمال می‌شود تا
        // وجود یا عدم وجود حساب افشا نشود.
        rateLimiter.assertRequestAllowed(email);
        rateLimiter.recordRequest(email);

        Long userId = findEligibleUserId(email);

        // برای جلوگیری از شناسایی ایمیل‌های ثبت‌شده،
        // در هر دو حالت پاسخ یکسان به کاربر داده می‌شود.
        if (userId == null) {
            return;
        }

        // تولید کد شش‌رقمی با SecureRandom
        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // رمزنگاری کد
        String codeHash = passwordEncoder.encode(code);

        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusMinutes(codeExpiryMinutes);

        // ذخیره یا جایگزینی درخواست قبلی
        jdbcTemplate.update(
                """
                INSERT INTO public.password_reset_codes
                    (user_id, code_hash, expires_at,
                     attempts, verified, created_at)
                VALUES (?, ?, ?, 0, FALSE, CURRENT_TIMESTAMP)

                ON CONFLICT (user_id)
                DO UPDATE SET
                    code_hash = EXCLUDED.code_hash,
                    expires_at = EXCLUDED.expires_at,
                    attempts = 0,
                    verified = FALSE,
                    created_at = CURRENT_TIMESTAMP
                """,
                userId,
                codeHash,
                Timestamp.from(expiresAt.toInstant())
        );

        sendResetEmail(email, code);
    }

    private Long findEligibleUserId(String email) {
        return jdbcTemplate.query(
                """
                SELECT user_id
                FROM public.users
                WHERE LOWER(email) = ?
                  AND is_active = TRUE
                  AND email_verified = TRUE
                """,
                rs -> rs.next() ? rs.getLong("user_id") : null,
                email
        );
    }

    // ارسال ایمیل بازیابی رمز
    private void sendResetEmail(String email, String code) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("بازیابی رمز عبور نوبتیک");

            String html =
                    """
                    <div style="font-family:Arial;
                                direction:rtl;
                                text-align:center;
                                padding:30px;">

                        <h2>بازیابی رمز عبور نوبتیک</h2>

                        <p>کد بازیابی رمز عبور شما:</p>

                        <h1 style="color:#6c4ab6;
                                   letter-spacing:8px;">
                    """ + code + """
                        </h1>

                        <p>این کد تا """ + codeExpiryMinutes + """
                         دقیقه اعتبار دارد.</p>

                        <p>اگر شما درخواست بازیابی نداده‌اید،
                        این ایمیل را نادیده بگیرید.</p>

                    </div>
                    """;

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {

            // هیچ جزئیات حساسی (کد، اعتبارنامه SMTP) افشا نمی‌شود.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ارسال ایمیل بازیابی در حال حاضر امکان‌پذیر نیست.",
                    e
            );
        }
    }

    // مرحله دوم: تأیید کد شش‌رقمی
    @Transactional
    public boolean verifyCode(PasswordResetVerifyRequest request) {

        String email = normalizeEmail(request.email());

        rateLimiter.assertVerifyAllowed(email);

        ResetRecord record = jdbcTemplate.query(
                """
                SELECT pr.user_id,
                       pr.code_hash,
                       pr.expires_at,
                       pr.attempts,
                       pr.verified
                FROM public.password_reset_codes pr
                JOIN public.users u
                    ON u.user_id = pr.user_id
                WHERE LOWER(u.email) = ?
                FOR UPDATE OF pr
                """,
                rs -> rs.next()
                        ? new ResetRecord(
                        rs.getLong("user_id"),
                        rs.getString("code_hash"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getInt("attempts"),
                        rs.getBoolean("verified")
                )
                        : null,
                email
        );

        if (record == null) {
            rateLimiter.recordVerifyFailure(email);
            return false;
        }

        // کد تأییدشده قبلاً استفاده شده و قابل استفاده مجدد نیست.
        if (record.verified()) {
            rateLimiter.recordVerifyFailure(email);
            return false;
        }

        // حداکثر تلاش مجاز برای هر کد
        if (record.attempts() >= maxCodeAttempts) {
            rateLimiter.recordVerifyFailure(email);
            return false;
        }

        // بررسی انقضای کد
        if (!record.expiresAt().isAfter(Instant.now())) {
            rateLimiter.recordVerifyFailure(email);
            return false;
        }

        // بررسی کد
        if (!passwordEncoder.matches(
                request.code(),
                record.codeHash()
        )) {

            jdbcTemplate.update(
                    """
                    UPDATE public.password_reset_codes
                    SET attempts = attempts + 1
                    WHERE user_id = ?
                    """,
                    record.userId()
            );

            rateLimiter.recordVerifyFailure(email);
            return false;
        }

        // تأیید موفق کد
        jdbcTemplate.update(
                """
                UPDATE public.password_reset_codes
                SET verified = TRUE
                WHERE user_id = ?
                """,
                record.userId()
        );

        rateLimiter.recordVerifySuccess(email);
        return true;
    }

    // مرحله سوم: ثبت رمز عبور جدید
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {

        String email = normalizeEmail(request.email());

        Long userId = jdbcTemplate.query(
                """
                SELECT pr.user_id
                FROM public.password_reset_codes pr
                JOIN public.users u
                    ON u.user_id = pr.user_id
                WHERE LOWER(u.email) = ?
                  AND pr.verified = TRUE
                  AND pr.expires_at > CURRENT_TIMESTAMP
                  AND pr.attempts < ?
                  AND u.is_active = TRUE
                FOR UPDATE OF pr
                """,
                rs -> rs.next() ? rs.getLong("user_id") : null,
                email,
                maxCodeAttempts
        );

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "درخواست بازیابی معتبر نیست یا کد تأیید نشده است."
            );
        }

        // رمزنگاری رمز جدید
        String newPasswordHash =
                passwordEncoder.encode(request.newPassword());

        // بروزرسانی رمز عبور و ثبت زمان تغییر رمز.
        // توکن‌های صادرشده قبل از این زمان توسط فیلتر JWT رد می‌شوند.
        jdbcTemplate.update(
                """
                UPDATE public.users
                SET password_hash = ?,
                    password_changed_at = clock_timestamp(),
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """,
                newPasswordHash,
                userId
        );

        // حذف کد بازیابی پس از موفقیت (جلوگیری از استفاده مجدد)
        jdbcTemplate.update(
                """
                DELETE FROM public.password_reset_codes
                WHERE user_id = ?
                """,
                userId
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record ResetRecord(
            Long userId,
            String codeHash,
            Instant expiresAt,
            int attempts,
            boolean verified
    ) {
    }
}
