package com.wipro.payroll.config;

import com.wipro.payroll.entity.Role;
import com.wipro.payroll.entity.User;
import com.wipro.payroll.entity.UserStatus;
import com.wipro.payroll.repository.UserRepository;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeederConfig {

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                System.out.println("🌱 Seeding initial users using BCrypt...");

                String defaultPasswordHash = passwordEncoder.encode("123123");

                // Create HR
                User hr = User.builder()
                        .username("hr")
                        .firstName("Tan")
                        .lastName("ZX")
                        .email("hr@gmail.com")
                        .password(defaultPasswordHash)
                        .role(Role.HR)
                        .status(UserStatus.ACTIVE)
                        .isVerified(true)
                        .phone("012-2222222")
                        .verificationCode(null)
                        .verificationCodeExpiryTime(null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                // Create Manager
                User manager = User.builder()
                        .username("mng")
                        .firstName("Tan")
                        .lastName("WH")
                        .email("mng@gmail.com")
                        .password(defaultPasswordHash)
                        .role(Role.MANAGER)
                        .status(UserStatus.ACTIVE)
                        .isVerified(true)
                        .phone("011-11111111")
                        .verificationCode(null)
                        .verificationCodeExpiryTime(null)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                userRepository.save(hr);
                userRepository.save(manager);

                System.out.println("✅ Data seeding complete. Default password: 123123123");
            }
        };
    }
}
