package com.group01.security;

import com.group01.enums.UserRole;
import com.group01.enums.UserStatus;
import com.group01.repository.AdministratorRepository;
import com.group01.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads accounts from PostgreSQL. The login name is the e-mail address.
 * <ul>
 *   <li>{@code users} with role MEMBER  -> ROLE_MEMBER</li>
 *   <li>{@code users} with role ADMIN and every row of {@code administrators} -> ROLE_ADMIN</li>
 * </ul>
 * Accounts with status INACTIVE cannot log in. Passwords in the database are BCrypt hashes.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdministratorRepository administratorRepository;

    public DatabaseUserDetailsService(UserRepository userRepository, AdministratorRepository administratorRepository) {
        this.userRepository = userRepository;
        this.administratorRepository = administratorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmailIgnoreCase(email);
        if (user.isPresent()) {
            var u = user.get();
            String role = u.getRole() == UserRole.ADMIN ? "ADMIN" : "MEMBER";
            return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                    .password(u.getPasswordHash())
                    .roles(role)
                    .disabled(u.getStatus() != UserStatus.ACTIVE)
                    .build();
        }
        var admin = administratorRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
        return org.springframework.security.core.userdetails.User.withUsername(admin.getEmail())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .disabled(admin.getStatus() != UserStatus.ACTIVE)
                .build();
    }
}
