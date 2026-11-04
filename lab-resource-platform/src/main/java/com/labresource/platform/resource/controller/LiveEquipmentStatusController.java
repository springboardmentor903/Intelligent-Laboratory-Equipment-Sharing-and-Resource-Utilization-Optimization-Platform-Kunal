
package com.labresource.platform.resource.controller;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.resource.dto.LiveEquipmentStatusResponse;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources/live-status")
public class LiveEquipmentStatusController {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;

    public LiveEquipmentStatusController(
            ResourceRepository resourceRepository,
            BookingRepository bookingRepository) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<LiveEquipmentStatusResponse> getLiveEquipmentStatus(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination values");
        }

        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<Booking>> activeBookingsByResource =
                bookingRepository.findAllInUseWithResource()
                        .stream()
                        .collect(Collectors.groupingBy(
                                booking -> booking.getResource().getId()));

        List<Resource> resources = resourceRepository.findAll()
                .stream()
                .filter(resource -> Boolean.TRUE.equals(resource.getIsActive()))
                .filter(resource -> category == null || category.isBlank()
                        || equalsIgnoreCase(resource.getCategory(), category))
                .filter(resource -> location == null || location.isBlank()
                        || containsIgnoreCase(resource.getLocation(), location))
                .filter(resource -> status == null || status.isBlank()
                        || equalsIgnoreCase(resource.getStatus(), status))
                .filter(resource -> search == null || search.isBlank()
                        || containsIgnoreCase(resource.getName(), search)
                        || containsIgnoreCase(resource.getDescription(), search))
                .sorted(Comparator.comparing(Resource::getId))
                .toList();

        List<LiveEquipmentStatusResponse> responseList =
                resources.stream()
                        .map(resource -> buildResponse(
                                resource,
                                activeBookingsByResource.getOrDefault(
                                        resource.getId(), List.of()),
                                now))
                        .toList();

        Pageable pageable = PageRequest.of(page, size);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(),
                responseList.size());

        List<LiveEquipmentStatusResponse> pageContent =
                start >= responseList.size()
                        ? List.of()
                        : responseList.subList(start, end);

        return new PageImpl<>(
                pageContent,
                pageable,
                responseList.size());
    }

    private LiveEquipmentStatusResponse buildResponse(
            Resource resource,
            List<Booking> activeBookings,
            LocalDateTime now) {

        Booking currentBooking = activeBookings.stream()
                .min(Comparator.comparing(
                        Booking::getActualStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        LocalDateTime actualStart = currentBooking == null
                ? null : currentBooking.getActualStartTime();

        LocalDateTime scheduledEnd = currentBooking == null
                ? null : currentBooking.getEndTime();

        Long elapsedMinutes = actualStart == null
                ? null
                : Math.max(0, Duration.between(
                actualStart, now).toMinutes());

        Optional<Booking> nextBooking =
                bookingRepository
                        .findFirstByResourceIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                                resource.getId(),
                                "APPROVED",
                                now);

        return new LiveEquipmentStatusResponse(
                resource.getId(),
                resource.getName(),
                resource.getCategory(),
                resource.getLocation(),
                resource.getStatus(),
                resource.getQuantity(),
                resource.getAvailableQuantity(),
                activeBookings.size(),
                actualStart,
                scheduledEnd,
                elapsedMinutes,
                nextBooking.map(Booking::getStartTime).orElse(null),
                nextBooking.map(Booking::getEndTime).orElse(null)
        );
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null
                && first.equalsIgnoreCase(second.trim());
    }

    private boolean containsIgnoreCase(String source, String search) {
        return source != null
                && source.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }
}
