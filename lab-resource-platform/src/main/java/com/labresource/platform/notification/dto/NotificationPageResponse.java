
package com.labresource.platform.notification.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class NotificationPageResponse {

    private List<NotificationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public NotificationPageResponse(
            Page<NotificationResponse> result
    ) {
        this.content = result.getContent();
        this.page = result.getNumber();
        this.size = result.getSize();
        this.totalElements = result.getTotalElements();
        this.totalPages = result.getTotalPages();
        this.last = result.isLast();
    }

    public List<NotificationResponse> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}
