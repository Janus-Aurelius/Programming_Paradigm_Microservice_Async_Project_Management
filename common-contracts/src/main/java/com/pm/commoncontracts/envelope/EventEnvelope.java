package com.pm.commoncontracts.envelope;

import java.time.Instant;
import java.util.UUID;

// Make sure this class is public
public record EventEnvelope<T>(
        UUID eventId,         // Unique ID for this specific event instance
        String correlationId, // ID to trace the request across services
        String eventType,     // Discriminator, e.g., "PROJECT_CREATED", "TASK_STATUS_CHANGED"
        String sourceService, // Name of the service that published the event
        Instant timestamp,      // When the event was generated
        int version,          // Schema version for the payload
        T payload             // The actual event data
) {
    // Optional: Convenience constructor setting defaults
    public EventEnvelope(String correlationId, String eventType, String sourceService, T payload) {
        this(UUID.randomUUID(), correlationId, eventType, sourceService, Instant.now(), 1, payload); // Default version 1
    }
}