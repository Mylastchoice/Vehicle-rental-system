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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

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
            .logout(logout -> logout.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor(
                    entryPoint,
                    request -> request.getRequestURI().startsWith("/api/")
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    request -> true
                )
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/*.html", "/rental/**", "/error", "/api/health",
                    "/login", "/register", "/forgot-password", "/reset-password", "/logout",
                    "/api/payment-options", "/api/payments/stripe/webhook",
                    "/css/**", "/js/**", "/images/**", "/assets/**", "/favicon.ico"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehicles/**", "/api/categories/**", "/api/branches/**").permitAll()
                .requestMatchers("/api/admin/**", "/api/database").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/member/**", "/profile", "/profile/**", "/account/profile").hasRole("MEMBER")
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
