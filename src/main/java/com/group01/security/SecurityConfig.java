package com.group01.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Three kinds of callers:
 * <ul>
 *   <li><b>Guest</b> (not logged in): health check and read-only catalogue (GET vehicles, categories, branches)</li>
 *   <li><b>Member</b>: everything under /api/member/** plus any other authenticated endpoint</li>
 *   <li><b>Admin</b>: everything under /api/admin/** and /api/database</li>
 * </ul>
 * The API is stateless: every request carries HTTP Basic credentials (e-mail + password). A token (JWT) can replace
 * this later without touching the rules below.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiAuthenticationEntryPoint entryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**", "/api/categories/**", "/api/branches/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**", "/api/database").hasRole("ADMIN")
                        .requestMatchers("/api/member/**").hasRole("MEMBER")
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
