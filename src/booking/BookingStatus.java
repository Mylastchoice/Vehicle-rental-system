package com.morrow.vrs.domain.enums;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    PICKED_UP,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PICKED_UP || target == CANCELLED;
            case PICKED_UP -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}