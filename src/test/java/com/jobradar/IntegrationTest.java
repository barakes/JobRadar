package com.jobradar;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Base class for tests that need a real database.
// One PostgreSQL container is started once and shared by all integration tests.
@SpringBootTest(properties = "jobradar.sync.enabled=false")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }
}