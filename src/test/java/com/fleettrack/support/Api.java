package com.fleettrack.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Small helpers so integration tests read like a dispatcher's workflow. */
public final class Api {

    private final MockMvc mvc;

    public Api(MockMvc mvc) {
        this.mvc = mvc;
    }

    public long createAvailableCourier(String name, String phone) throws Exception {
        String body = mvc.perform(post("/api/couriers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"phone\":\"%s\",\"vehicleType\":\"BICYCLE\"}".formatted(name, phone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(patch("/api/couriers/" + id + "/availability").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());
        return id;
    }

    public String reportLocation(long courierId, double lat, double lng, Instant recordedAt) throws Exception {
        String recorded = recordedAt == null ? "" : ",\"recordedAt\":\"" + recordedAt + "\"";
        return mvc.perform(post("/api/couriers/" + courierId + "/locations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":%s,\"longitude\":%s%s}".formatted(lat, lng, recorded)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
    }

    public long createDelivery(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) throws Exception {
        String body = mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientName":"Ada","recipientPhone":"+15550100",
                                 "pickupLatitude":%s,"pickupLongitude":%s,
                                 "dropoffLatitude":%s,"dropoffLongitude":%s}
                                """.formatted(pickupLat, pickupLng, dropoffLat, dropoffLng)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
