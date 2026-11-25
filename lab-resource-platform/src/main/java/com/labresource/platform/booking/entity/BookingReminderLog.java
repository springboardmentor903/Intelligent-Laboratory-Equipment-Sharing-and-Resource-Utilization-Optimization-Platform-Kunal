package com.labresource.platform.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_reminder_logs", uniqueConstraints =
@UniqueConstraint(name = "uk_booking_reminder_booking", columnNames = "booking_id"))
public class BookingReminderLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
    protected BookingReminderLog() {}
    public BookingReminderLog(Long bookingId) { this.bookingId = bookingId; this.sentAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public LocalDateTime getSentAt() { return sentAt; }
}
