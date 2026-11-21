package com.group01.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.group01.enums.BookingStatus;
import com.group01.enums.PaymentMethod;

/** A rental booking made by a member. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_branch_id", nullable = false)
    private Branch pickupBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_branch_id")
    private Branch returnBranch;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "renter_name", nullable = false, length = 150)
    private String renterName;

    @Column(name = "renter_phone", nullable = false, length = 20)
    private String renterPhone;

    @Column(name = "renter_license_number", nullable = false, length = 100)
    private String renterLicenseNumber;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    public Booking() {
        // required by JPA
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Branch getPickupBranch() {
        return pickupBranch;
    }

    public void setPickupBranch(Branch pickupBranch) {
        this.pickupBranch = pickupBranch;
    }

    public Branch getReturnBranch() {
        return returnBranch;
    }

    public void setReturnBranch(Branch returnBranch) {
        this.returnBranch = returnBranch;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getRenterName() {
        return renterName;
    }

    public void setRenterName(String renterName) {
        this.renterName = renterName;
    }

    public String getRenterPhone() {
        return renterPhone;
    }

    public void setRenterPhone(String renterPhone) {
        this.renterPhone = renterPhone;
    }

    public String getRenterLicenseNumber() {
        return renterLicenseNumber;
    }

    public void setRenterLicenseNumber(String renterLicenseNumber) {
        this.renterLicenseNumber = renterLicenseNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<BookingDetail> getDetails() {
        return details;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addDetail(BookingDetail detail) {
        details.add(detail);
        detail.setBooking(this);
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setBooking(this);
    }

}
