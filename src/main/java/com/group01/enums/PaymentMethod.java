package com.group01.enums;

/** Database values defined in vehicle_rental_system.sql or proposed shared conventions. */
public enum PaymentMethod {
    ONLINE("ONLINE"),
    AT_PICKUP("AT_PICKUP");

    private final String dbValue;

    PaymentMethod(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static PaymentMethod fromDbValue(String value) {
        for (PaymentMethod item : values()) {
            if (item.dbValue.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentMethod: " + value);
    }
}
