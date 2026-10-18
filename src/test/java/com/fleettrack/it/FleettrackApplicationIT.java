package com.fleettrack.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleettrack.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FleettrackApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    /** Context start = Flyway migrated a real PostGIS AND Hibernate validated every entity against it. */
    @Test
    void migrationsApplyAndEntitiesMatchSchema() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(3);

        String postgis = jdbc.queryForObject("select postgis_version()", String.class);
        assertThat(postgis).isNotBlank();
    }
}
