package com.wipro.payroll.repository;

import com.wipro.payroll.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // SELECT u.*
    // FROM "user" u
    // WHERE u.username = ?;
    Optional<User> findByUsername(String username);

    // SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
    // FROM "user" u 
    // WHERE u.username = ?;
    boolean existsByUsername(String username);

    // SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
    // FROM "user" u 
    // WHERE u.email = ?;
    boolean existsByEmail(String email);

    // SELECT u.*
    // FROM "user" u
    // WHERE u.email = ?;
    Optional<User> findByEmail(String email);

    // Scheduled Cleanup Jobs to prevent bot accounts
    // SELECT u.*
    // FROM "user" u
    // WHERE u.verification_code_expiry_time < ?;
    List<User> findByVerificationCodeExpiryTimeBefore(LocalDateTime now);
}
