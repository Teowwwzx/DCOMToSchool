package com.wipro.payroll.controller;

import com.wipro.payroll.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/api/test-email")
    public String sendTestEmail(@RequestParam String email) {
        emailService.sendEmail(email, "Test Email from DCOMToSchool",
                "Hello! This is a test email from your local Spring Boot app using MailHog.");
        return "Email sent to " + email + ". Check localhost:8025!";
    }
}
