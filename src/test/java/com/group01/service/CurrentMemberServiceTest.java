package com.group01.service;

import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CurrentMemberServiceTest {
    @Test
    void rejectAnonymousWithoutAnyDatabaseQuery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var service = new CurrentMemberService(jdbc);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.requireMemberId(null));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("null") // Mockito matchers may return null; JDBC mocks do not execute the query.
    void resolveMemberByAuthenticatedEmail() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq("member@example.com")))
                .thenReturn(List.of(42L));
        var service = new CurrentMemberService(jdbc);
        Principal authenticated = () -> "member@example.com";
        assertEquals(42L, service.requireMemberId(authenticated));
    }

    @Test
    @SuppressWarnings("null") // Mockito matchers may return null; JDBC mocks do not execute the query.
    void rejectIdentityNotPresentOrNotActiveMember() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString()))
                .thenReturn(List.of());
        var service = new CurrentMemberService(jdbc);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.requireMemberId(() -> "outsider@example.com"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
}