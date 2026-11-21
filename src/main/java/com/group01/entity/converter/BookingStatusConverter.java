package com.group01.entity.converter;

import com.group01.enums.BookingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link BookingStatus} to the exact text stored in the database (see {@code BookingStatus.getDbValue()}). */
@Converter(autoApply = true)
public class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(BookingStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public BookingStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BookingStatus.fromDbValue(dbData);
    }
}
