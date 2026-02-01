package com.wipro.payroll.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String ic;
}
