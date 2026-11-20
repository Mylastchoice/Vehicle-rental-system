-- M1 / Stage 3: Flyway V2
-- Additional indexes for browse, booking conflict checks and checkout.
-- These do not alter the original 10-table Stage 2 data model.

CREATE INDEX IF NOT EXISTS idx_booking_details_vehicle_dates
    ON public.booking_details (vehicle_code, pickup_date, return_date);

CREATE INDEX IF NOT EXISTS idx_bookings_status
    ON public.bookings (status);

CREATE INDEX IF NOT EXISTS idx_vehicles_category_branch
    ON public.vehicles (category_id, branch_id);
