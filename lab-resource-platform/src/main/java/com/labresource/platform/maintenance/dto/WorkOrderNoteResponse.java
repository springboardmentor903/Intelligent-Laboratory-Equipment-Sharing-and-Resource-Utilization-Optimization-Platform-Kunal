package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.WorkOrderNote;

import java.time.LocalDateTime;

public record WorkOrderNoteResponse(
        Long id,
        UserSummary author,
        String content,
        LocalDateTime createdAt
) {

    public static WorkOrderNoteResponse fromEntity(WorkOrderNote note) {

        UserSummary author = null;

        if (note.getAuthor() != null) {
            author = new UserSummary(
                    note.getAuthor().getId(),
                    note.getAuthor().getFullName(),
                    note.getAuthor().getEmail()
            );
        }

        return new WorkOrderNoteResponse(
                note.getId(),
                author,
                note.getContent(),
                note.getCreatedAt()
        );
    }

    public record UserSummary(
            Long id,
            String fullName,
            String email
    ) {
    }
}