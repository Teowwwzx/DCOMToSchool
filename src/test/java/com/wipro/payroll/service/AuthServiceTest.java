package com.wipro.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wipro.payroll.dto.LoginRequest;
import com.wipro.payroll.dto.UserDTO;
import com.wipro.payroll.entity.Role;
import com.wipro.payroll.entity.User;
import com.wipro.payroll.entity.UserStatus;
import com.wipro.payroll.repository.UserRepository;

@ExtendWith(MockitoExtension.class) // Enable Mockito magic
public class AuthServiceTest {

    @Mock // Create a FAKE UserRepository (doesn't talk to DB)
    private UserRepository userRepository;

    @Mock // Create a FAKE PasswordEncoder
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Inject the above FAKEs into the real AuthService
    private AuthService authService;

    @Test
    void login_Success() {
        // 1. SETUP (Arrange)
        // Create a dummy user that the Repo will return
        User fakeUser = User.builder()
                .id(1)
                .username("testuser")
                .password("encoded_pass") // The simulated "hashed" password in DB
                .role(Role.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .isVerified(true) // Important! Logic checks this now
                .firstName("Teow")
                .lastName("ZX")
                .email("test@gmail.com")
                .build();

        // Teach the Mock Repo: "When someone asks for 'testuser', give them this
        // fakeUser"
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(fakeUser));

        // Teach the Mock Encoder: "When someone compares 'raw123' with 'encoded_pass',
        // return true"
        when(passwordEncoder.matches("raw123", "encoded_pass")).thenReturn(true);

        // 2. ACT (Run the actual method)
        LoginRequest request = new LoginRequest("testuser", "raw123");
        UserDTO result = authService.login(request);

        // 3. ASSERT (Verify the results)
        assertNotNull(result); // Must not be null
        assertEquals("testuser", result.getUsername()); // Username must match
        assertEquals("Teow", result.getFirstName()); // Name must match
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // 1. SETUP
        // Teach Mock Repo: "When someone asks for 'ghost', return EMPTY"
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // 2. & 3. ACT & ASSERT
        // Verify that calling login throws a RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(new LoginRequest("ghost", "123"));
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        // 1. SETUP
        User fakeUser = User.builder().username("testuser").password("encoded_pass").isVerified(true).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(fakeUser));

        // Teach Encoder: "Password does NOT match"
        when(passwordEncoder.matches("wrong123", "encoded_pass")).thenReturn(false);

        // 2 & 3. Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.login(new LoginRequest("testuser", "wrong123"));
        });
    }

    @Test
    void login_UnverifiedUser_ThrowsException() {
        // 1. SETUP
        User unverifiedUser = User.builder()
                .username("testuser")
                .password("encoded_pass")
                .isVerified(false) // Not verified!
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(unverifiedUser));
        when(passwordEncoder.matches("raw123", "encoded_pass")).thenReturn(true);

        // 2 & 3. Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.login(new LoginRequest("testuser", "raw123"));
        });

        assertEquals("Account not verified. Please check your email.", ex.getMessage());
    }
}
