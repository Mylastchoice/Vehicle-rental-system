package com.morrow.vehiclerental.service;

import com.morrow.vehiclerental.model.*;
import com.morrow.vehiclerental.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final VehicleRepository vehicles;
    private final CategoryRepository categories;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AdminService(VehicleRepository vehicles, CategoryRepository categories,
                        AppUserRepository users, PasswordEncoder encoder) {
        this.vehicles = vehicles;
        this.categories = categories;
        this.users = users;
        this.encoder = encoder;
    }

    public Iterable<Vehicle> allVehicles() { return vehicles.findAll(); }
    public Iterable<Category> allCategories() { return categories.findAll(); }

    @Transactional
    public void saveVehicle(Vehicle form, Long categoryId) {
        if (form.getVehicleCode() == null || form.getVehicleCode().isBlank()) {
            throw new IllegalArgumentException("Mã xe không được để trống.");
        }
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("Tên xe không được để trống.");
        }
        if (form.getPricePerDay() == null || form.getPricePerDay().signum() < 0) {
            throw new IllegalArgumentException("Giá thuê phải lớn hơn hoặc bằng 0.");
        }

        boolean duplicate;
        if (form.getId() == null) {
            duplicate = vehicles.existsByVehicleCode(form.getVehicleCode().trim());
        } else {
            duplicate = vehicles.existsByVehicleCodeAndIdNot(
                    form.getVehicleCode().trim(), form.getId());
        }
        if (duplicate) {
            throw new IllegalArgumentException("Mã xe đã tồn tại.");
        }

        form.setVehicleCode(form.getVehicleCode().trim());
        if (categoryId != null) {
            form.setCategory(categories.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại.")));
        } else {
            form.setCategory(null);
        }
        vehicles.save(form);
    }

    public Vehicle findVehicle(Long id) {
        return vehicles.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe."));
    }

    // Tạm ẩn xe thay vì xóa cứng để hạn chế mất dữ liệu liên quan.
    // Khi có Booking entity cần kiểm tra booking đang hoạt động trước thao tác này.
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findVehicle(id);
        vehicle.setActive(false);
        vehicles.save(vehicle);
    }

    public Iterable<AppUser> allUsers() { return users.findAll(); }

    public AppUser findUser(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    @Transactional
    public void saveUser(AppUser form, String rawPassword) {
        if (form.getUsername() == null || form.getUsername().isBlank()
                || form.getEmail() == null || form.getEmail().isBlank()
                || form.getFullName() == null || form.getFullName().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin người dùng.");
        }

        boolean duplicateUsername;
        boolean duplicateEmail;
        if (form.getId() == null) {
            duplicateUsername = users.existsByUsername(form.getUsername().trim());
            duplicateEmail = users.existsByEmail(form.getEmail().trim());
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("Mật khẩu không được để trống.");
            }
            form.setPasswordHash(encoder.encode(rawPassword));
        } else {
            AppUser old = findUser(form.getId());
            duplicateUsername = users.existsByUsernameAndIdNot(form.getUsername().trim(), form.getId());
            duplicateEmail = users.existsByEmailAndIdNot(form.getEmail().trim(), form.getId());
            form.setPasswordHash(old.getPasswordHash());
            form.setLocked(old.isLocked());
        }

        if (duplicateUsername) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        if (duplicateEmail) throw new IllegalArgumentException("Email đã được sử dụng.");

        form.setUsername(form.getUsername().trim());
        form.setEmail(form.getEmail().trim());
        users.save(form);
    }

    @Transactional
    public void setLocked(Long id, boolean locked) {
        AppUser user = findUser(id);
        user.setLocked(locked);
        users.save(user);
    }
}
