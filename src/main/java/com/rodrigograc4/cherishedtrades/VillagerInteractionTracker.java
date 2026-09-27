package com.rodrigograc4.cherishedtrades;

import java.util.UUID;

public class VillagerInteractionTracker {

    private static final long MAX_AGE_NANOS = 10_000_000_000L;

    private static UUID pendingId;
    private static long pendingTime;

    // Remembers the merchant the player just right-clicked, so the next trade screen is tied to it.
    public static void record(UUID entityId) {
        pendingId = entityId;
        pendingTime = System.nanoTime();
    }

    // Returns and clears the last right-clicked merchant, or null if the click is too old to be related.
    public static UUID consume() {
        UUID id = pendingId;
        pendingId = null;
        if (id == null || System.nanoTime() - pendingTime > MAX_AGE_NANOS) return null;
        return id;
    }
}
