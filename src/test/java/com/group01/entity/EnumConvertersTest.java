package com.group01.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group01.entity.converter.BookingStatusConverter;
import com.group01.entity.converter.PaymentMethodConverter;
import com.group01.entity.converter.UserRoleConverter;
import com.group01.entity.converter.UserStatusConverter;
import com.group01.entity.converter.VehicleStatusConverter;
import com.group01.enums.BookingStatus;
import com.group01.enums.PaymentMethod;
import com.group01.enums.UserRole;
import com.group01.enums.UserStatus;
import com.group01.enums.VehicleStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** No database needed: the converters must produce exactly the strings allowed by the CHECK constraints. */
class EnumConvertersTest {

    @Test
    void bookingStatusUsesDatabaseSpelling() {
        BookingStatusConverter c = new BookingStatusConverter();
        assertEquals("Picked up", c.convertToDatabaseColumn(BookingStatus.PICKED_UP));
        assertEquals(BookingStatus.PICKED_UP, c.convertToEntityAttribute("Picked up"));
        for (BookingStatus s : BookingStatus.values()) {
            assertEquals(s, c.convertToEntityAttribute(c.convertToDatabaseColumn(s)));
        }
    }

    @Test
    void vehicleStatusUsesDatabaseSpelling() {
        VehicleStatusConverter c = new VehicleStatusConverter();
        assertEquals("Available", c.convertToDatabaseColumn(VehicleStatus.AVAILABLE));
        assertEquals(VehicleStatus.MAINTENANCE, c.convertToEntityAttribute("Maintenance"));
    }

    @Test
    void otherEnumsRoundTrip() {
        assertEquals("AT_PICKUP", new PaymentMethodConverter().convertToDatabaseColumn(PaymentMethod.AT_PICKUP));
        assertEquals(UserRole.ADMIN, new UserRoleConverter().convertToEntityAttribute("ADMIN"));
        assertEquals(UserStatus.INACTIVE, new UserStatusConverter().convertToEntityAttribute("INACTIVE"));
    }

    @Test
    void nullStaysNullAndUnknownValueIsRejected() {
        assertNull(new BookingStatusConverter().convertToDatabaseColumn(null));
        assertNull(new BookingStatusConverter().convertToEntityAttribute(null));
        assertThrows(IllegalArgumentException.class, () -> new BookingStatusConverter().convertToEntityAttribute("Done"));
    }

    @Test
    void finalDailyPriceAppliesDiscount() {
        Vehicle v = new Vehicle();
        v.setDailyPrice(new BigDecimal("700000.00"));
        v.setDiscountRate(new BigDecimal("0.1000"));
        assertEquals(new BigDecimal("630000.00"), v.getFinalDailyPrice());
    }
}
