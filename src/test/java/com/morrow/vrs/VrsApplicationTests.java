package com.morrow.vrs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestIntegrationStubConfig.class)
class VrsApplicationTests {

    @Test
    void contextLoads() {
    }
}