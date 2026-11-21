package com.group01.service;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** M3 never trusts a user ID supplied by the browser. */
@Service
public class CurrentMemberService {
    private final JdbcTemplate jdbc;

    public CurrentMemberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long requireMemberId(Principal principal) {
        // A session attribute or a URL query parameter is NOT an authenticated Principal.
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        List<Long> ids = jdbc.query("""
                SELECT user_id FROM users
                WHERE LOWER(email) = LOWER(?) AND status = 'ACTIVE' AND role = 'MEMBER'
                """, (rs, rowNum) -> rs.getLong("user_id"), principal.getName());
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active member account required");
        }
        return ids.get(0);
    }
}
