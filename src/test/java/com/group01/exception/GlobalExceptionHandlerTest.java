package com.group01.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Pure web-layer test (no Spring context, no database): every error must come back as the same ApiError JSON. */
class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/t/not-found")
        String notFound() {
            throw ResourceNotFoundException.of("Vehicle", "X1");
        }

        @GetMapping("/t/bad")
        String bad() {
            throw new BadRequestException("Return date must be later than the pickup date.");
        }

        @GetMapping("/t/conflict")
        String conflict() {
            throw new ConflictException("Vehicle is already booked for these dates.");
        }

        @GetMapping("/t/status-conflict")
        String statusConflict() {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "This booking cannot be cancelled");
        }

        @GetMapping("/t/integrity")
        String integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_email_key\"");
        }

        @GetMapping("/t/denied")
        String denied() {
            throw new AccessDeniedException("secret internal reason");
        }

        @GetMapping("/t/boom")
        String boom() {
            throw new IllegalStateException("password=hunter2 jdbc:postgresql://secret-host");
        }

        @GetMapping("/t/number/{id}")
        String number(@PathVariable Long id) {
            return "ok " + id;
        }

        @PostMapping("/t/body")
        String body(@RequestBody java.util.Map<String, String> body) {
            return "ok";
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundIs404() throws Exception {
        mvc.perform(get("/t/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vehicle not found: X1"))
                .andExpect(jsonPath("$.path").value("/t/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void badRequestIs400() throws Exception {
        mvc.perform(get("/t/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Return date must be later than the pickup date."));
    }

    @Test
    void conflictIs409() throws Exception {
        mvc.perform(get("/t/conflict")).andExpect(status().isConflict());
    }

    @Test
    void responseStatusExceptionPreservesStatusAndReason() throws Exception {
        mvc.perform(get("/t/status-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This booking cannot be cancelled"));
    }

    @Test
    void databaseConstraintErrorIs409AndHidesSql() throws Exception {
        mvc.perform(get("/t/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("users_email_key"))));
    }

    @Test
    void accessDeniedIs403() throws Exception {
        mvc.perform(get("/t/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void unexpectedErrorIs500AndHidesDetails() throws Exception {
        mvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error. Please try again later."));
    }

    @Test
    void wrongParameterTypeIs400() throws Exception {
        mvc.perform(get("/t/number/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/t/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
    }

    @Test
    void wrongHttpMethodIs405() throws Exception {
        mvc.perform(post("/t/not-found")).andExpect(status().isMethodNotAllowed());
    }
}
