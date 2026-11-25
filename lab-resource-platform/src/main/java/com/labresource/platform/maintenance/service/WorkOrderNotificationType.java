package com.labresource.platform.notification.service;

/** Event names used by work-order notification producers. */
public final class WorkOrderNotificationType {

    private WorkOrderNotificationType() {
    }

    public static final String CREATED = "WORK_ORDER_CREATED";
    public static final String ASSIGNED = "WORK_ORDER_ASSIGNED";
    public static final String STARTED = "WORK_ORDER_STARTED";
    public static final String COMPLETED = "WORK_ORDER_COMPLETED";
    public static final String CANCELLED = "WORK_ORDER_CANCELLED";

    public static final String REFERENCE_TYPE = "WORK_ORDER";
}
