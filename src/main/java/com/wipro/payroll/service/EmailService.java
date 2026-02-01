package com.wipro.payroll.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@dcomtoschool.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        System.out.println("📧 Sending email to: " + to);
        mailSender.send(message);
        System.out.println("✅ Email sent successfully!");
    }
}
