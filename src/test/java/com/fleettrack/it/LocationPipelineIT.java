package com.fleettrack.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import com.fleettrack.support.AbstractIntegrationTest;
import com.fleettrack.support.Api;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Location ping -> Kafka -> independent consumer groups (ETA, metrics). */
class LocationPipelineIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void pingFlowsThroughKafkaToEtaAndMetricsConsumers() throws Exception {
        Api api = new Api(mvc);
        double lat = 51.5 + Math.random();
        long courier = api.createAvailableCourier("Eve", uniquePhone());
        api.reportLocation(courier, lat + 0.005, -0.12, null);
        long delivery = api.createDelivery(lat, -0.12, lat + 0.02, -0.12);
        mvc.perform(post("/api/deliveries/" + delivery + "/assign")).andExpect(status().isOk());

        double before = meterRegistry.counter("fleettrack.location.updates").count();
        api.reportLocation(courier, lat + 0.004, -0.12, null);

        // ETA consumer computed and cached an ETA to the pickup (~445 m away)
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                mvc.perform(get("/api/deliveries/" + delivery + "/eta"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.target").value("PICKUP"))
                        .andExpect(jsonPath("$.courierId").value(courier)));

        // Metrics consumer, a separate consumer group, saw the same event independently
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(meterRegistry.counter("fleettrack.location.updates").count()).isGreaterThan(before));
    }
}
