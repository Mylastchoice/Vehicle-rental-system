package com.group01.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CheckoutPhoneValidationTest {
    @Test
    void rejectShortPhone() {
        assertThrows(ResponseStatusException.class,
                () -> CheckoutService.validVietnamesePhone("12345"));
    }

    @Test
    void acceptValidVietnameseMobile() {
        assertEquals("0901234567", CheckoutService.validVietnamesePhone("0901234567"));
        assertEquals("+84901234567", CheckoutService.validVietnamesePhone("+84901234567"));
    }

    @Test
    void rejectInvalidAndNullNumbers() {
        assertThrows(ResponseStatusException.class,
                () -> CheckoutService.validVietnamesePhone(null));
        assertThrows(ResponseStatusException.class,
                () -> CheckoutService.validVietnamesePhone("0123456789"));
    }
}
