package com.group01.dto;

import java.util.List;

/** Booking + line items without exposing sensitive user or payment credentials. */
public record BookingDetailResponse(BookingViewResponse booking,
                                    List<BookingVehicleResponse> vehicles) {
}
