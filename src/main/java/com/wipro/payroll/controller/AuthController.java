package com.wipro.payroll.controller;

import com.wipro.payroll.dto.LoginRequest;
import com.wipro.payroll.dto.UserDTO;
import com.wipro.payroll.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow frontend access
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody com.wipro.payroll.dto.SignupRequest signUpRequest) {
        try {
            authService.register(signUpRequest);
            return ResponseEntity.ok("User registered successfully! Please check your email to verify your account.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequest loginRequest) {
        UserDTO user = authService.login(loginRequest);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String code) {
        try {
            authService.verifyEmail(email, code);
            return ResponseEntity.ok("Verification successful! You can now login.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
