package com.wipro.payroll.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Role  implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name; // e.g., "EMPLOYEE", "HR", "MANAGER"
    private LocalDateTime createdAt;


    public Role() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
