package com.fleettrack.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests: a real PostGIS (same image as docker-compose), a real
 * Redis, and an embedded Kafka broker.
 *
 * <p>Using real PostgreSQL rather than H2 is the point: Flyway migrations, generated
 * geography columns, GiST indexes, CHECK constraints, the partial unique index and
 * FOR UPDATE SKIP LOCKED are all PostgreSQL/PostGIS features an in-memory DB can't run.
 * And with ddl-auto=validate, merely starting the context proves entities match the schema.
 *
 * <p>Containers are started once (singleton pattern) and shared by every IT class;
 * Testcontainers' Ryuk sidecar removes them when the JVM exits.
 */
@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public abstract class AbstractIntegrationTest {

    static final GenericContainer<?> POSTGIS = new GenericContainer<>(DockerImageName.parse("postgis/postgis:16-3.4"))
            .withEnv("POSTGRES_DB", "fleettrack")
            .withEnv("POSTGRES_USER", "fleettrack")
            .withEnv("POSTGRES_PASSWORD", "fleettrack")
            .withExposedPorts(5432)
            // The image starts a temporary server for init scripts first, so wait for the 2nd "ready".
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGIS.start();
        REDIS.start();
    }

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGIS.getHost() + ":"
                + POSTGIS.getMappedPort(5432) + "/fleettrack");
        registry.add("spring.datasource.username", () -> "fleettrack");
        registry.add("spring.datasource.password", () -> "fleettrack");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    /** Unique phone numbers so tests sharing one database never collide on the UNIQUE constraint. */
    protected static String uniquePhone() {
        return "+1555" + String.format("%07d", SEQUENCE.incrementAndGet());
    }
}
