package com.labresource.platform.booking.dto;

import com.labresource.platform.booking.entity.BookingStatusHistory;
import java.time.LocalDateTime;

public class BookingHistoryResponse {

    private Long id;
    private Long bookingId;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
    private String remark;

    public BookingHistoryResponse(BookingStatusHistory history) {
        this.id = history.getId();
        this.bookingId = history.getBooking().getId();
        this.oldStatus = history.getOldStatus();
        this.newStatus = history.getNewStatus();
        this.changedBy = history.getChangedBy();
        this.changedAt = history.getChangedAt();
        this.remark = history.getRemark();
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getRemark() { return remark; }
}