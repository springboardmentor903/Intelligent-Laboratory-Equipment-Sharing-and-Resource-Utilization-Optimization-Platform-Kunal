
package com.labresource.platform.usage.dto;

import java.math.BigDecimal;
import java.util.List;

public class UsageChargePageResponse {

    private List<UsageChargeResponse> charges;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private BigDecimal totalAmount;

    public UsageChargePageResponse(
            List<UsageChargeResponse> charges,
            long totalElements,
            int totalPages,
            int currentPage,
            BigDecimal totalAmount) {

        this.charges = charges;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.totalAmount = totalAmount;
    }

    public List<UsageChargeResponse> getCharges() { return charges; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
