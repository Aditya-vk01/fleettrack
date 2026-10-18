package com.fleettrack.delivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.fleettrack.common.exception.InvalidStateTransitionException;
import com.fleettrack.tracking.eta.EtaStore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @MockitoBean
    private EtaStore etaStore;

    @Test
    void illegalTransitionIs409() throws Exception {
        when(deliveryService.transition(1L, DeliveryStatus.DELIVERED))
                .thenThrow(new InvalidStateTransitionException(DeliveryStatus.CREATED, DeliveryStatus.DELIVERED));

        mvc.perform(post("/api/deliveries/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Illegal state transition CREATED -> DELIVERED"));
    }

    @Test
    void concurrentModificationIs409() throws Exception {
        when(deliveryService.transition(eq(1L), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Delivery.class, 1L));

        mvc.perform(post("/api/deliveries/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PICKED_UP\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/deliveries/1/status").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void createValidatesCoordinates() throws Exception {
        mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientName": "Ada", "recipientPhone": "+15550100",
                                 "pickupLatitude": 95, "pickupLongitude": -74,
                                 "dropoffLatitude": 40.7, "dropoffLongitude": -74}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pickupLatitude").exists());
    }

    @Test
    void etaNotYetComputedIs404() throws Exception {
        when(etaStore.find(5L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/deliveries/5/eta"))
                .andExpect(status().isNotFound());
    }
}
