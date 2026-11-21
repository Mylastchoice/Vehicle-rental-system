package com.group01.entity.converter;

import com.group01.enums.VehicleStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link VehicleStatus} to the exact text stored in the database (see {@code VehicleStatus.getDbValue()}). */
@Converter(autoApply = true)
public class VehicleStatusConverter implements AttributeConverter<VehicleStatus, String> {

    @Override
    public String convertToDatabaseColumn(VehicleStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public VehicleStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VehicleStatus.fromDbValue(dbData);
    }
}
