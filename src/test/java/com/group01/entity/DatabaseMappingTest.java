package com.group01.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group01.enums.BookingStatus;
import com.group01.enums.PaymentMethod;
import com.group01.enums.UserRole;
import com.group01.enums.UserStatus;
import com.group01.enums.VehicleStatus;
import com.group01.repository.AdministratorRepository;
import com.group01.repository.BookingDetailRepository;
import com.group01.repository.BookingRepository;
import com.group01.repository.BranchRepository;
import com.group01.repository.CategoryRepository;
import com.group01.repository.RentalCartRepository;
import com.group01.repository.UserRepository;
import com.group01.repository.VehicleRepository;
import com.group01.support.RequiresPostgres;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entities <-> PostgreSQL. Uses the schema created by Flyway (V1/V2) and rolls back after every test.
 * Needs PostgreSQL, see {@link RequiresPostgres}.
 */
@RequiresPostgres
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseMappingTest {

    @Autowired UserRepository users;
    @Autowired AdministratorRepository admins;
    @Autowired CategoryRepository categories;
    @Autowired BranchRepository branches;
    @Autowired VehicleRepository vehicles;
    @Autowired RentalCartRepository carts;
    @Autowired BookingRepository bookings;
    @Autowired BookingDetailRepository details;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private Category car;
    private Branch branch;
    private Vehicle vehicle;
    private User member;

    @BeforeEach
    void data() {
        car = new Category();
        car.setCategoryName("Car-" + System.nanoTime());
        categories.save(car);

        branch = new Branch();
        branch.setBranchName("Morrow District 1");
        branch.setAddress("1 Le Loi, HCMC");
        branches.save(branch);

        vehicle = new Vehicle();
        vehicle.setVehicleCode("T-VF3-" + System.nanoTime() % 100000);
        vehicle.setCategory(car);
        vehicle.setBranch(branch);
        vehicle.setName("VinFast VF 3");
        vehicle.setDailyPrice(new BigDecimal("500000.00"));
        vehicle.setDiscountRate(new BigDecimal("0.1000"));
        vehicles.save(vehicle);

        member = new User();
        member.setFullName("Minh Nguyen");
        member.setEmail("minh-" + System.nanoTime() + "@mail.com");
        member.setPasswordHash("$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012");
        member.setPhone("0901234567");
        users.save(member);
        em.flush();
        em.clear();
    }

    private Booking newBooking(LocalDate pickup, LocalDate ret) {
        Booking b = new Booking();
        b.setUser(users.getReferenceById(member.getUserId()));
        b.setPickupBranch(branches.getReferenceById(branch.getBranchId()));
        b.setPickupLocation("Morrow District 1");
        b.setPickupDate(pickup);
        b.setReturnDate(ret);
        b.setRenterName("Minh Nguyen");
        b.setRenterPhone("0901234567");
        b.setRenterLicenseNumber("079123456789");
        b.setTotalAmount(new BigDecimal("900000.00"));
        b.setDepositAmount(new BigDecimal("270000.00"));
        b.setPaymentMethod(PaymentMethod.ONLINE);
        return b;
    }

    @Test
    void vehicleRoundTripStoresEnumsWithDatabaseSpelling() {
        Vehicle loaded = vehicles.findById(vehicle.getVehicleCode()).orElseThrow();
        assertEquals(VehicleStatus.AVAILABLE, loaded.getStatus());
        assertEquals("Available", jdbc.queryForObject(
                "select status from vehicles where vehicle_code = ?", String.class, vehicle.getVehicleCode()));
        assertEquals(car.getCategoryId(), loaded.getCategory().getCategoryId());
        assertEquals(0, loaded.getViewCount());
    }

    @Test
    void userDefaultsAndCreatedAtAreFilled() {
        User loaded = users.findByEmailIgnoreCase(member.getEmail().toUpperCase()).orElseThrow();
        assertEquals(UserRole.MEMBER, loaded.getRole());
        assertEquals(UserStatus.ACTIVE, loaded.getStatus());
        assertNotNull(loaded.getCreatedAt());
        assertTrue(users.existsByEmailIgnoreCase(member.getEmail()));
    }

    @Test
    void administratorIsStoredInItsOwnTable() {
        Administrator a = new Administrator();
        a.setFullName("Admin");
        a.setEmail("admin-" + System.nanoTime() + "@morrow.vn");
        a.setPasswordHash("hash");
        admins.saveAndFlush(a);
        assertTrue(admins.findByEmailIgnoreCase(a.getEmail()).isPresent());
        assertEquals("ACTIVE", jdbc.queryForObject(
                "select status from administrators where admin_id = ?", String.class, a.getAdminId()));
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        User other = new User();
        other.setFullName("Copy");
        other.setEmail(member.getEmail());
        other.setPasswordHash("x");
        other.setPhone("0900000000");
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(other));
    }

    @Test
    void cartWithItemsIsSavedThroughCascade() {
        RentalCart cart = new RentalCart();
        cart.setUser(users.getReferenceById(member.getUserId()));
        CartItem item = new CartItem();
        item.setVehicle(vehicles.getReferenceById(vehicle.getVehicleCode()));
        item.setPickupDate(LocalDate.of(2026, 12, 1));
        item.setReturnDate(LocalDate.of(2026, 12, 3));
        cart.addItem(item);
        carts.saveAndFlush(cart);
        em.clear();

        RentalCart loaded = carts.findByUser_UserId(member.getUserId()).orElseThrow();
        assertEquals(1, loaded.getItems().size());
        assertEquals(1, jdbc.queryForObject("select count(*) from cart_items where cart_id = ?", Integer.class, loaded.getCartId()));
    }

    @Test
    void bookingWithDetailAndPaymentIsSavedAndStatusUsesDatabaseSpelling() {
        Booking b = newBooking(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        BookingDetail d = new BookingDetail();
        d.setVehicle(vehicles.getReferenceById(vehicle.getVehicleCode()));
        d.setPickupDate(b.getPickupDate());
        d.setReturnDate(b.getReturnDate());
        d.setDailyPrice(new BigDecimal("500000.00"));
        d.setDiscountRate(new BigDecimal("0.1000"));
        d.setRentalDays(2);
        d.setSubtotal(new BigDecimal("900000.00"));
        b.addDetail(d);
        Payment p = new Payment();
        p.setAmount(new BigDecimal("270000.00"));
        p.setPaymentType("DEPOSIT");
        p.setTransactionRef("TXN-" + System.nanoTime());
        b.addPayment(p);
        bookings.saveAndFlush(b);

        b.setStatus(BookingStatus.PICKED_UP);
        bookings.saveAndFlush(b);
        assertEquals("Picked up", jdbc.queryForObject(
                "select status from bookings where booking_id = ?", String.class, b.getBookingId()));
        em.clear();

        Booking loaded = bookings.findById(b.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.PICKED_UP, loaded.getStatus());
        assertEquals(1, loaded.getDetails().size());
        assertEquals(1, loaded.getPayments().size());
        assertEquals("Pending", loaded.getPayments().get(0).getStatus());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(1, bookings.findByUser_UserIdOrderByCreatedAtDesc(member.getUserId()).size());
    }

    @Test
    void databaseRejectsReturnDateBeforePickupDate() {
        Booking b = newBooking(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 1));
        assertThrows(DataIntegrityViolationException.class, () -> bookings.saveAndFlush(b));
    }

    @Test
    void overlapQueryFollowsTheHalfOpenIntervalRule() {
        Booking b = newBooking(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15));
        BookingDetail d = new BookingDetail();
        d.setVehicle(vehicles.getReferenceById(vehicle.getVehicleCode()));
        d.setPickupDate(b.getPickupDate());
        d.setReturnDate(b.getReturnDate());
        d.setDailyPrice(new BigDecimal("500000.00"));
        d.setRentalDays(5);
        d.setSubtotal(new BigDecimal("2500000.00"));
        b.addDetail(d);
        bookings.saveAndFlush(b);

        List<BookingStatus> active = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.PICKED_UP);
        String code = vehicle.getVehicleCode();
        assertEquals(1, details.countOverlapping(code, LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 20), active));
        assertEquals(1, details.countOverlapping(code, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 11), active));
        assertEquals(0, details.countOverlapping(code, LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 18), active), "back-to-back is allowed");
        assertEquals(0, details.countOverlapping(code, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), active), "back-to-back is allowed");
        assertEquals(0, details.countOverlapping(code, LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14),
                List.of(BookingStatus.CANCELLED)));
        assertFalse(active.isEmpty());
    }
}
