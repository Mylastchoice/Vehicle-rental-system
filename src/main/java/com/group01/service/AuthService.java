package com.group01.service;

import com.group01.entity.User;
import com.group01.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Lưu tạm số lần đăng nhập sai và thời gian khóa tài khoản (15 phút)
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, LocalDateTime> lockUntilMap = new HashMap<>();

    // Lưu tạm token đặt lại mật khẩu và thời hạn (30 phút)
    private final Map<String, String> resetTokenEmail = new HashMap<>();
    private final Map<String, LocalDateTime> resetTokenExpiry = new HashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String fullName, String phone, String email, String password, String confirmPassword) {
        String name = fullName == null ? "" : fullName.trim();
        String phoneNum = phone == null ? "" : phone.trim();
        String mail = email == null ? "" : email.trim().toLowerCase();

        if (name.isEmpty() || name.length() > 150) {
            throw new IllegalArgumentException("Full name is required (max 150 characters).");
        }
        if (!phoneNum.matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        if (!mail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include letters and numbers.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match.");
        }
        if (userRepository.existsByEmail(mail)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User user = new User();
        user.setFullName(name);
        user.setPhone(phoneNum);
        user.setEmail(mail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("MEMBER");
        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }

    public User login(String email, String password) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (mail.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please enter both email and password.");
        }

        // Kiểm tra xem tài khoản có đang bị khóa tạm thời không
        LocalDateTime lockedUntil = lockUntilMap.get(mail);
        if (lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil)) {
            throw new IllegalStateException("Account is locked due to 5 failed attempts. Try again in 15 minutes.");
        }

        User user = userRepository.findByEmail(mail).orElse(null);

        // Kiểm tra email và mật khẩu đã băm BCrypt
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            int count = failedAttempts.getOrDefault(mail, 0) + 1;
            failedAttempts.put(mail, count);

            if (count >= 5) {
                lockUntilMap.put(mail, LocalDateTime.now().plusMinutes(15));
                throw new IllegalStateException("Too many failed attempts. Your account is locked for 15 minutes.");
            }

            int remaining = 5 - count;
            throw new IllegalArgumentException("Incorrect email or password. " + remaining + " attempt(s) left.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Your account is inactive.");
        }

        // Đăng nhập đúng thì reset số lần sai về 0
        failedAttempts.remove(mail);
        lockUntilMap.remove(mail);
        return user;
    }

    public String createPasswordResetToken(String email) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (!mail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        User user = userRepository.findByEmail(mail).orElse(null);
        if (user == null) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        resetTokenEmail.put(token, mail);
        resetTokenExpiry.put(token, LocalDateTime.now().plusMinutes(30));
        return token;
    }

    public boolean isValidResetToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        LocalDateTime expiry = resetTokenExpiry.get(token);
        if (expiry == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(expiry)) {
            resetTokenEmail.remove(token);
            resetTokenExpiry.remove(token);
            return false;
        }
        return true;
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!isValidResetToken(token)) {
            throw new IllegalArgumentException("Reset link is invalid or expired.");
        }
        if (newPassword == null || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException("New password must be at least 8 characters with letters and numbers.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match.");
        }

        String mail = resetTokenEmail.get(token);
        User user = userRepository.findByEmail(mail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa token sau khi đổi mật khẩu thành công
        resetTokenEmail.remove(token);
        resetTokenExpiry.remove(token);
        failedAttempts.remove(mail);
        lockUntilMap.remove(mail);
    }

    public User updateProfile(Long userId, String fullName, String phone, LocalDate dateOfBirth,
                              String gender, String address, String licenseNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String name = fullName == null ? "" : fullName.trim();
        String phoneNum = phone == null ? "" : phone.trim();

        if (name.isEmpty() || name.length() > 150) {
            throw new IllegalArgumentException("Full name is required (max 150 characters).");
        }
        if (!phoneNum.matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }

        user.setFullName(name);
        user.setPhone(phoneNum);
        user.setDateOfBirth(dateOfBirth);
        user.setGender((gender == null || gender.trim().isEmpty()) ? null : gender.trim());
        user.setAddress((address == null || address.trim().isEmpty()) ? null : address.trim());
        user.setLicenseNumber((licenseNumber == null || licenseNumber.trim().isEmpty()) ? null : licenseNumber.trim());

        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException("New password must be at least 8 characters with letters and numbers.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}