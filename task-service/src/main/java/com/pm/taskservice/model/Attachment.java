package com.pm.taskservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data // No need for @Document here, it's an embedded object
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attachment {
    // private String id; // If attachments are globally unique and managed elsewhere, keep id.
    // If they are only unique within a task, this might be overkill.
    // A UUID can be generated in the service if needed.
    @NotBlank
    @Size(max = 2048) // URL length
    private String url; // File storage URL

    @NotBlank
    @Size(max = 255)
    private String filename; // Original file name

    private long size; // File size in bytes

    @NotBlank
    private String uploadedBy; // Uploader's user ID

    @NotNull // Should always have an upload timestamp
    private Instant uploadedAt; // Timestamp
}
