package com.wipro.payroll.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"user\"") // "user" is a reserved keyword in Postgres, so we must quote it
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_title_id")
    private Integer jobTitleId;

    @Column(name = "emp_type_id")
    private Integer empTypeId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "f_name", nullable = false)
    private String firstName;

    @Column(name = "l_name", nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "ic", unique = true)
    private String ic;

    @Column(name = "pwd_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_verified", nullable = false)
    @lombok.Builder.Default
    private boolean isVerified = false; // Default to false

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expiry_time")
    private LocalDateTime verificationCodeExpiryTime;

    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
}
