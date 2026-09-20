package com.workflow.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.fail-fast=false"
})
class WorkerApplicationTests {

    @Test
    void contextLoads() {
    }
}
