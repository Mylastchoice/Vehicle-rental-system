package com.group01;

import com.group01.entity.User;
import com.group01.repository.UserRepository;
import com.group01.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setEmail("member@example.com");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setPhone("0912345678");
        sampleUser.setPasswordHash(encoder.encode("Password123"));
        sampleUser.setRole("MEMBER");
        sampleUser.setStatus("ACTIVE");
    }

    @Test
    void register_success_hashesPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("Tran B", "0987654321", "new@example.com", "Secret123", "Secret123");

        assertNotNull(created);
        assertEquals("new@example.com", created.getEmail());
        assertNotEquals("Secret123", created.getPasswordHash());
        assertTrue(encoder.matches("Secret123", created.getPasswordHash()));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("member@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Nguyen Van A", "0912345678", "member@example.com", "Password123", "Password123")
        );
    }

    @Test
    void login_fiveFailedAttempts_locksFor15Minutes() {
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(sampleUser));

        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalArgumentException.class, () ->
                    authService.login("member@example.com", "WrongPass1")
            );
        }

        assertThrows(IllegalStateException.class, () ->
                authService.login("member@example.com", "WrongPass1")
        );

        assertThrows(IllegalStateException.class, () ->
                authService.login("member@example.com", "Password123")
        );
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndInvalidatesToken() {
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.createPasswordResetToken("member@example.com");
        assertNotNull(token);
        assertTrue(authService.isValidResetToken(token));

        authService.resetPassword(token, "NewPass123", "NewPass123");

        assertFalse(authService.isValidResetToken(token));
        assertTrue(encoder.matches("NewPass123", sampleUser.getPasswordHash()));
    }

    @Test
    void updateProfile_updatesAllowedFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = authService.updateProfile(
                1L, "Nguyen Van A Updated", "0901112223",
                LocalDate.of(2000, 1, 15), "Male", "District 1, HCMC", "B2-123456"
        );

        assertEquals("Nguyen Van A Updated", updated.getFullName());
        assertEquals("0901112223", updated.getPhone());
        assertEquals("B2-123456", updated.getLicenseNumber());
        assertEquals("member@example.com", updated.getEmail());
    }

    @Test
    void changePassword_validCurrentPassword_updatesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword(1L, "Password123", "NewPass456", "NewPass456");

        assertTrue(encoder.matches("NewPass456", sampleUser.getPasswordHash()));
    }
}