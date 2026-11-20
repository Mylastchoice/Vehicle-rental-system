package com.group01.enums;

/** Database values defined in vehicle_rental_system.sql or proposed shared conventions. */
public enum BookingStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PICKED_UP("Picked up"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String dbValue;

    BookingStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static BookingStatus fromDbValue(String value) {
        for (BookingStatus item : values()) {
            if (item.dbValue.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown BookingStatus: " + value);
    }
}
