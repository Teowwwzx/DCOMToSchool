package com.wipro.payroll.service;

import com.wipro.payroll.dto.LoginRequest;
import com.wipro.payroll.dto.UserDTO;
import com.wipro.payroll.entity.User;
import com.wipro.payroll.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserDTO login(LoginRequest request) {
        // 1. Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials")); // Generic error for security

        // 2. Verify password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 3. Verify Email Status
        if (!user.isVerified()) {
            throw new RuntimeException("Account not verified. Please check your email.");
        }

        // 4. Convert to DTO
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .token("dummy-token-phase-1") // Placeholder
                .build();
    }

    @Autowired
    private com.wipro.payroll.service.EmailService emailService;

    public void register(com.wipro.payroll.dto.SignupRequest request) {
        // 1. Check if user exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // 2. Generate Verification Code
        String verificationCode = java.util.UUID.randomUUID().toString().substring(0, 6); // Simple 6-char code
        java.time.LocalDateTime expiry = java.time.LocalDateTime.now().plusHours(24);

        // 3. Create User
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .ic(request.getIc())
                .role(com.wipro.payroll.entity.Role.EMPLOYEE) // Default role
                .status(com.wipro.payroll.entity.UserStatus.PENDING_VERIFICATION)
                .isVerified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiryTime(expiry)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        userRepository.save(user);

        // 4. Send Verification Email
        String verifyLink = "http://localhost:8082/api/auth/verify?email=" + user.getEmail() + "&code="
                + verificationCode;
        String emailBody = "Welcome " + user.getFirstName() + "!\n\n" +
                "Please verify your account by clicking the link below:\n" +
                verifyLink + "\n\n" +
                "This link expires in 24 hours.";

        emailService.sendEmail(user.getEmail(), "Verify your DCOMToSchool Account", emailBody);
    }

    public void verifyEmail(String email, String verificationCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            throw new RuntimeException("Account already verified");
        }

        // 1. Check if token matches (use !equals)
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(verificationCode)) {
            throw new RuntimeException("Invalid verification code");
        }

        // 2. Check if token is expired
        if (java.time.LocalDateTime.now().isAfter(user.getVerificationCodeExpiryTime())) {
            throw new RuntimeException("Verification code has expired");
        }

        // 3. Success: Verify user and clear code
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiryTime(null);
        userRepository.save(user);
    }
}
