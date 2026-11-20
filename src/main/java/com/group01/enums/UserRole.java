package com.group01.enums;

/** Database values defined in vehicle_rental_system.sql or proposed shared conventions. */
public enum UserRole {
    MEMBER("MEMBER"),
    ADMIN("ADMIN");

    private final String dbValue;

    UserRole(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserRole fromDbValue(String value) {
        for (UserRole item : values()) {
            if (item.dbValue.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown UserRole: " + value);
    }
}
