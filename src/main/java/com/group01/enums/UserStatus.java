package com.group01.enums;

/** Database values defined in vehicle_rental_system.sql or proposed shared conventions. */
public enum UserStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String dbValue;

    UserStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserStatus fromDbValue(String value) {
        for (UserStatus item : values()) {
            if (item.dbValue.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus: " + value);
    }
}
