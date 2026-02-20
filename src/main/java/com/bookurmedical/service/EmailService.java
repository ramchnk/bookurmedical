
package com.bookurmedical.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to BookUrMedical!");
        message.setText("Dear " + firstName + ",\n\n" +
                "Welcome to BookUrMedical! We are thrilled to have you on board.\n\n" +
                "You can now login to your account and start booking your medical appointments.\n\n" +
                "Best regards,\n" +
                "The BookUrMedical Team");

        mailSender.send(message);
    }
<<<<<<< HEAD

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, please click the link below:\n" +
                "http://localhost:9002/reset-password?token=" + token);

        mailSender.send(message);
    }
=======
>>>>>>> 96d0f91b3637f55db93cce76dd31b9df811f1d68
}
