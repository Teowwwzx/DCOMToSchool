package com.wipro.payroll.dto;

import com.wipro.payroll.entity.Role;
import com.wipro.payroll.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private Integer id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UserStatus status;
    private String token; // For future JWT
}
