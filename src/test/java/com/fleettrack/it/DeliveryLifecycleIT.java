package com.fleettrack.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fleettrack.support.AbstractIntegrationTest;
import com.fleettrack.support.Api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class DeliveryLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Api api;

    // Each test works in its own small area so couriers from other tests are never "nearest".
    private double baseLat;
    private final double baseLng = -74.0;

    @BeforeEach
    void setUp() {
        api = new Api(mvc);
        baseLat = -60 + Math.random() * 120;
    }

    @Test
    void nearbyReturnsAvailableCouriersNearestFirst() throws Exception {
        long far = api.createAvailableCourier("Far", uniquePhone());
        long near = api.createAvailableCourier("Near", uniquePhone());
        api.reportLocation(far, baseLat + 0.02, baseLng, null);    // ~2.2 km
        api.reportLocation(near, baseLat + 0.001, baseLng, null);  // ~110 m

        mvc.perform(get("/api/couriers/nearby")
                        .param("lat", String.valueOf(baseLat)).param("lng", String.valueOf(baseLng))
                        .param("radiusMeters", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(near))
                .andExpect(jsonPath("$[1].id").value(far));
    }

    @Test
    void fullLifecycleAutoAssignsNearestAndEnforcesTheStateMachine() throws Exception {
        long far = api.createAvailableCourier("Far", uniquePhone());
        long near = api.createAvailableCourier("Near", uniquePhone());
        api.reportLocation(far, baseLat + 0.03, baseLng, null);
        api.reportLocation(near, baseLat + 0.002, baseLng, null);
        long delivery = api.createDelivery(baseLat, baseLng, baseLat + 0.01, baseLng + 0.01);

        // auto-assign -> nearest courier, who becomes BUSY
        mvc.perform(post("/api/deliveries/" + delivery + "/assign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.courierId").value(near));
        mvc.perform(get("/api/couriers/" + near)).andExpect(jsonPath("$.status").value("BUSY"));

        // skipping states is rejected with 409 and changes nothing
        transition(delivery, "DELIVERED").andExpect(status().isConflict());
        mvc.perform(get("/api/deliveries/" + delivery)).andExpect(jsonPath("$.status").value("ASSIGNED"));

        transition(delivery, "PICKED_UP").andExpect(status().isOk());
        transition(delivery, "CANCELLED").andExpect(status().isConflict()); // too late to cancel
        transition(delivery, "IN_TRANSIT").andExpect(status().isOk());
        transition(delivery, "DELIVERED").andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTransitions").isEmpty());

        // courier released, audit trail complete
        mvc.perform(get("/api/couriers/" + near)).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/deliveries/" + delivery + "/history"))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].toStatus").value("CREATED"))
                .andExpect(jsonPath("$[4].toStatus").value("DELIVERED"));
    }

    @Test
    void noCourierInRangeIs409() throws Exception {
        long delivery = api.createDelivery(baseLat, baseLng, baseLat, baseLng + 0.01);

        mvc.perform(post("/api/deliveries/" + delivery + "/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"radiusMeters\": 50}"))
                .andExpect(status().isConflict());
    }

    @Test
    void outOfOrderPingIsStoredButDoesNotMoveTheCourier() throws Exception {
        long courier = api.createAvailableCourier("Bob", uniquePhone());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        api.reportLocation(courier, baseLat, baseLng, now);
        String late = api.reportLocation(courier, baseLat + 1, baseLng, now.minusSeconds(30));

        assertThat(late).contains("\"positionUpdated\":false");
        mvc.perform(get("/api/couriers/" + courier)).andExpect(jsonPath("$.lastLatitude").value(baseLat));
        mvc.perform(get("/api/couriers/" + courier + "/locations")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void databaseRejectsTwoActiveDeliveriesForOneCourier() throws Exception {
        long courier = api.createAvailableCourier("Bob", uniquePhone());
        String insert = """
                insert into deliveries (recipient_name, recipient_phone, pickup_latitude, pickup_longitude,
                                        dropoff_latitude, dropoff_longitude, status, courier_id)
                values ('X', '+15550100', 0, 0, 0, 0, ?, ?)
                """;
        jdbc.update(insert, "ASSIGNED", courier);
        jdbc.update(insert, "DELIVERED", courier); // finished ones don't count

        assertThatThrownBy(() -> jdbc.update(insert, "IN_TRANSIT", courier))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_deliveries_one_active_per_courier");
    }

    private org.springframework.test.web.servlet.ResultActions transition(long delivery, String status) throws Exception {
        return mvc.perform(post("/api/deliveries/" + delivery + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }
}
