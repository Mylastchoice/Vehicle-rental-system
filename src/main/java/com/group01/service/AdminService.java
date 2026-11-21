package com.group01.service;

import com.group01.entity.Category;
import com.group01.entity.User;
import com.group01.entity.Vehicle;
import com.group01.entity.Branch;
import com.group01.enums.UserRole;
import com.group01.enums.UserStatus;
import com.group01.enums.VehicleStatus;
import com.group01.exception.BadRequestException;
import com.group01.exception.ConflictException;
import com.group01.exception.ResourceNotFoundException;
import com.group01.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final UserRepository users;
    private final AdministratorRepository administrators;
    private final VehicleRepository vehicles;
    private final CategoryRepository categories;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;

    public AdminService(UserRepository users, AdministratorRepository administrators,
                        VehicleRepository vehicles, CategoryRepository categories,
                        BranchRepository branches, PasswordEncoder encoder) {
        this.users = users; this.administrators = administrators;
        this.vehicles = vehicles; this.categories = categories;
        this.branches = branches; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> listUsers() {
        return users.findAll().stream().map(u -> Map.<String,Object>of(
                "userId", u.getUserId(), "fullName", u.getFullName(), "email", u.getEmail(),
                "phone", u.getPhone(), "role", u.getRole().getDbValue(), "status", u.getStatus().getDbValue())).toList();
    }

    public Map<String,Object> createUser(UserInput in) {
        validateUser(in, true);
        String email = in.email().trim();
        if (users.existsByEmailIgnoreCase(email) || administrators.findByEmailIgnoreCase(email).isPresent())
            throw new ConflictException("Email already exists");
        User u = new User();
        u.setFullName(in.fullName().trim()); u.setEmail(email);
        u.setPasswordHash(encoder.encode(in.password()));
        u.setPhone(in.phone().trim()); u.setRole(UserRole.MEMBER); u.setStatus(UserStatus.ACTIVE);
        users.save(u);
        return Map.of("userId", u.getUserId(), "email", u.getEmail(), "status", u.getStatus().getDbValue());
    }

    public Map<String,Object> updateUserStatus(Long id, String status) {
        User u = users.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
        if (u.getRole() == UserRole.ADMIN) throw new BadRequestException("Cannot change status of an administrator here");
        try { u.setStatus(UserStatus.valueOf(status.trim().toUpperCase())); }
        catch (Exception ex) { throw new BadRequestException("Status must be ACTIVE or INACTIVE"); }
        return Map.of("userId", u.getUserId(), "status", u.getStatus().getDbValue());
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> listVehicles() {
        return vehicles.findAll().stream().map(v -> vehicleMap(v)).toList();
    }

    public Map<String,Object> saveVehicle(String code, VehicleInput in) {
        boolean creating = code == null;
        if (in == null) throw new BadRequestException("Vehicle data is required");
        String vehicleCode = creating ? clean(in.vehicleCode()) : code;
        if (vehicleCode == null || vehicleCode.isBlank()) throw new BadRequestException("Vehicle code is required");
        Vehicle v = creating ? new Vehicle() : vehicles.findById(vehicleCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleCode));
        if (creating) v.setVehicleCode(vehicleCode);
        if (in.name() != null && !in.name().isBlank()) v.setName(in.name().trim());
        else if (creating) throw new BadRequestException("Vehicle name is required");
        if (in.categoryId() != null) v.setCategory(categories.findById(in.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", in.categoryId())));
        else if (creating) throw new BadRequestException("Category is required");
        if (in.branchId() != null) v.setBranch(branches.findById(in.branchId())
                .orElseThrow(() -> ResourceNotFoundException.of("Branch", in.branchId())));
        else if (creating) throw new BadRequestException("Branch is required");
        if (in.dailyPrice() != null) {
            if (in.dailyPrice().compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Daily price must be positive");
            v.setDailyPrice(in.dailyPrice());
        } else if (creating) throw new BadRequestException("Daily price is required");
        if (in.discountRate() != null) {
            if (in.discountRate().compareTo(BigDecimal.ZERO) < 0 || in.discountRate().compareTo(BigDecimal.ONE) > 0)
                throw new BadRequestException("Discount rate must be between 0 and 1");
            v.setDiscountRate(in.discountRate());
        }
        if (in.status() != null) {
            String requested = in.status().trim();
            VehicleStatus parsed = null;
            for (VehicleStatus value : VehicleStatus.values()) {
                if (value.name().equalsIgnoreCase(requested) || value.getDbValue().equalsIgnoreCase(requested)) {
                    parsed = value; break;
                }
            }
            if (parsed == null) throw new BadRequestException("Status must be Available, Rented or Maintenance");
            v.setStatus(parsed);
        }
        if (in.licensePlate() != null) v.setLicensePlate(in.licensePlate().trim());
        if (in.imageUrl() != null) v.setImageUrl(in.imageUrl().trim());
        if (in.shortDescription() != null) v.setShortDescription(in.shortDescription());
        if (in.detailedDescription() != null) v.setDetailedDescription(in.detailedDescription());
        vehicles.save(v);
        return vehicleMap(v);
    }

    public void deleteVehicle(String code) {
        Vehicle v = vehicles.findById(code).orElseThrow(() -> ResourceNotFoundException.of("Vehicle", code));
        try { vehicles.delete(v); vehicles.flush(); }
        catch (DataIntegrityViolationException ex) { throw new ConflictException("Vehicle is referenced by booking records and cannot be deleted"); }
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories() { return categories.findAll(); }

    public Category saveCategory(Long id, CategoryInput in) {
        if (in == null || in.categoryName() == null || in.categoryName().isBlank())
            throw new BadRequestException("Category name is required");
        Category c = id == null ? new Category() : categories.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        c.setCategoryName(in.categoryName().trim());
        c.setDescription(in.description());
        try { return categories.saveAndFlush(c); }
        catch (DataIntegrityViolationException ex) { throw new ConflictException("Category name already exists"); }
    }

    public void deleteCategory(Long id) {
        Category c = categories.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        try { categories.delete(c); categories.flush(); }
        catch (DataIntegrityViolationException ex) { throw new ConflictException("Category is used by vehicles and cannot be deleted"); }
    }

    private static void validateUser(UserInput in, boolean passwordRequired) {
        if (in == null || in.fullName() == null || in.fullName().isBlank()
                || in.email() == null || !EMAIL.matcher(in.email().trim()).matches()
                || in.phone() == null || in.phone().isBlank())
            throw new BadRequestException("Full name, valid email and phone are required");
        if (passwordRequired && (in.password() == null || in.password().length() < 8))
            throw new BadRequestException("Password must contain at least 8 characters");
    }
    private static String clean(String s) { return s == null ? null : s.trim(); }
    private static Map<String,Object> vehicleMap(Vehicle v) {
        return Map.of("vehicleCode", v.getVehicleCode(), "name", v.getName(),
                "categoryId", v.getCategory().getCategoryId(), "branchId", v.getBranch().getBranchId(),
                "dailyPrice", v.getDailyPrice(), "discountRate", v.getDiscountRate(),
                "status", v.getStatus().getDbValue(), "viewCount", v.getViewCount(), "rentalCount", v.getRentalCount());
    }

    public record UserInput(String fullName, String email, String password, String phone) {}
    public record VehicleInput(String vehicleCode, String name, Long categoryId, Integer branchId,
                               String licensePlate, String imageUrl, String shortDescription,
                               String detailedDescription, BigDecimal dailyPrice, BigDecimal discountRate,
                               String status) {}
    public record CategoryInput(String categoryName, String description) {}
}
