
package com.labresource.platform.booking.repository;

import com.labresource.platform.booking.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingStatusHistoryRepository
        extends JpaRepository<BookingStatusHistory, Long> {

    @Query("""
        SELECT h FROM BookingStatusHistory h
        JOIN FETCH h.booking
        WHERE h.booking.id = :bookingId
        ORDER BY h.changedAt DESC
    """)
    List<BookingStatusHistory> findHistoryWithBooking(
            @Param("bookingId") Long bookingId);
}
