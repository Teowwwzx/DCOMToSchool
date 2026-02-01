package com.wipro.payroll.common;

import java.io.Serializable;

public enum UserStatus implements Serializable {
    ACTIVE,
    INACTIVE,
    PENDING_VERIFICATION,
    FROZEN
}
