# M4 – Booking Management (newly reconstructed implementation)

**Important:** This M4 module was created when cleaning the Group 01 submission ZIP.
It is **not** claimed to be the original contributor's code on the GitHub M4 branch.
The original 19 M4 test-case results in `../Testing_Document.xlsx` predate this integration;
they must be **rerun** before calling this build verified.

## Files added

- `controller/BookingController.java` — member web pages `/bookings`, `/bookings/{id}`, cancel and JSON API `/api/bookings/**`.
- `controller/BookingAdminController.java` — admin web pages `/admin/bookings`, filters, details and admin state changes; `/api/admin/bookings/**` JSON API.
- `service/BookingService.java` — ownership checks, status transition rules, cancellation rules, confirmation conflict detection.
- `service/BookingNotificationService.java` — optional mail after transaction commit.
- `repository/BookingManagementRepository.java` — PostgreSQL JDBC queries on existing `bookings`, `booking_details`, `payments`, `users`, and `vehicles` tables.
- `dto/Booking*Response.java` — response records.
- `templates/booking/` — member and administrator pages; empty list says "No bookings yet".
- `src/test/java/com/group01/service/BookingServiceTest.java` — seven new isolated unit tests for the M4 rules.

## Business rules and limitations

- Members see their own bookings only; booking details and cancellation never trust user IDs from the browser.
- Administrators may filter by status and pickup date, then move Pending → Confirmed → Picked up → Completed, or cancel from Pending / Confirmed.
- Confirmation checks overlapping vehicles (a Pending/Confirmed/Picked up booking blocks the dates). Database rows for vehicles are locked in sorted order before the check, consistent with M3 checkout.
- Member cancellation requires Pending/Confirmed and a future pickup; at least 24 hours before pickup requests a full deposit refund. Admin cancellation requests full deposit refund.
- **The database stores pickup dates, not pickup times**. The 24-hour calculation assumes local midnight at the pickup date; precise time-based policies require an additional time field and are not reproduced here.
- **A REFUND row is created with `status = Pending`**. Refunds require manual processing. Stripe sandbox handles online deposits.
- A member with a picked-up booking can request an earlier return date once. When an administrator records the actual return, the scheduled and requested dates are preserved and the booking is completed. Early returns use the original booked price.
- Status transitions write **audit messages to server logs only**. The existing schema has no booking status-history table, so audit history is not durable and is not displayed on a dedicated page.
- Email notifications run only when `MAIL_USERNAME` is configured and an SMTP sender is available; mail failure is logged and does not cancel the database transaction.
- The existing schema and Figma/M3 pages were not modified. CSRF remains disabled in the original security configuration, so **do not publish a public booking-admin demo without a security review**.

## Run/verify

Use Maven Java 21 in `SourceCode/`: `mvn clean test` then `mvn package`.
Use a disposable test database with a seeded active Member and Admin.
Test the flows manually at `/bookings` and `/admin/bookings`, including ownership, overlap, refund records and invalid transitions. Docker uses host Postgres port 5433 and web port 8080.
