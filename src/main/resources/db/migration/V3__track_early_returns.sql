ALTER TABLE bookings
    ADD COLUMN requested_return_date date,
    ADD COLUMN actual_return_date date;
