package com.group01.enums;

/** Database values defined in vehicle_rental_system.sql or proposed shared conventions. */
public enum VehicleStatus {
    AVAILABLE("Available"),
    RENTED("Rented"),
    MAINTENANCE("Maintenance");

    private final String dbValue;

    VehicleStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static VehicleStatus fromDbValue(String value) {
        for (VehicleStatus item : values()) {
            if (item.dbValue.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown VehicleStatus: " + value);
    }
}
