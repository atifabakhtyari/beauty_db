package com.beautybooking.beauty_booking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private final String senderEmail;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            JdbcTemplate jdbcTemplate,
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    /**
     * تولید، ذخیره و ارسال کد تأیید ایمیل
     */
    @Transactional
    public void sendVerificationCode(String email) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        // پیدا کردن شناسه کاربر
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

        // بررسی تأیید شدن ایمیل
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

        // تولید کد تصادفی ۶ رقمی
        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // Hash کردن کد برای ذخیره امن در دیتابیس
        String codeHash = passwordEncoder.encode(code);

        // اعتبار کد: ۱۰ دقیقه
        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusMinutes(10);

        // ذخیره کد در دیتابیس
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

        // ارسال ایمیل از طریق Gmail SMTP
        sendEmail(normalizedEmail, code);
    }

    /**
     * ارسال ایمیل HTML با Gmail SMTP
     */
    private void sendEmail(String recipientEmail, String code) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("کد تأیید ایمیل نوبتیک");

            String htmlContent =
                    """
                    <div style="
                        font-family: Arial, sans-serif;
                        direction: rtl;
                        text-align: center;
                        padding: 25px;
                        background-color: #f7f7f7;
                    ">
                        <div style="
                            background-color: white;
                            padding: 30px;
                            border-radius: 12px;
                        ">
                            <h2>تأیید ایمیل نوبتیک</h2>

                            <p>کد تأیید ایمیل شما:</p>

                            <h1 style="
                                letter-spacing: 8px;
                                color: #6c4ab6;
                                font-size: 32px;
                            ">
                                """ + code + """
                            </h1>

                            <p>این کد تا ۱۰ دقیقه اعتبار دارد.</p>

                            <p>
                                اگر این درخواست را انجام نداده‌اید،
                                این پیام را نادیده بگیرید.
                            </p>
                        </div>
                    </div>
                    """;

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {

            // هرگز کد تأیید یا رمز ایمیل را در لاگ ثبت نکن
            throw new IllegalStateException(
                    "ارسال ایمیل تأیید از طریق Gmail ناموفق بود.",
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

        // اگر ایمیل قبلاً تأیید شده باشد
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

        // حداکثر ۵ بار تلاش
        if (record.attempts() >= 5) {
            return false;
        }

        // بررسی انقضای کد
        if (!record.expiresAt().isAfter(Instant.now())) {
            return false;
        }

        // بررسی درستی کد
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

        // تأیید ایمیل و فعال‌سازی کاربر
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

        // حذف کد پس از تأیید موفق
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