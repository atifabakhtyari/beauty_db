package com.beautybooking.beauty_booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String recipientEmail, String code) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Nobatik - Email Verification");

        message.setText(
                "Hello,\n\n" +
                        "Your Nobatik verification code is: " + code + "\n\n" +
                        "This code expires in 10 minutes.\n\n" +
                        "If you did not request this code, please ignore this email."
        );

        mailSender.send(message);
    }
}