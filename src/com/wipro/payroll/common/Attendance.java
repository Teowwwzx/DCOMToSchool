package com.wipro.payroll.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Attendance implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private boolean isApproved;
    private int approvedById;

    public Attendance() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDateTime getClockIn() { return clockIn; }
    public void setClockIn(LocalDateTime clockIn) { this.clockIn = clockIn; }
    public LocalDateTime getClockOut() { return clockOut; }
    public void setClockOut(LocalDateTime clockOut) { this.clockOut = clockOut; }
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }
    public int getApprovedById() { return approvedById; }
    public void setApprovedById(int approvedById) { this.approvedById = approvedById; }
}
