package com.fleettrack.courier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fleettrack.common.exception.ConflictException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.courier.dto.CourierResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer slice: routing, Bean Validation and the exception -> HTTP status mapping. */
@WebMvcTest(CourierController.class)
class CourierControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourierService courierService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(courierService.create(any())).thenReturn(new CourierResponse(42L, "Bob", "+15550200",
                VehicleType.BICYCLE, CourierStatus.OFFLINE, null, null, null, Instant.now()));

        mvc.perform(post("/api/couriers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bob", "phone": "+15550200", "vehicleType": "BICYCLE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/couriers/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrorsAndNeverReachesTheService() throws Exception {
        mvc.perform(post("/api/couriers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "phone": "call-me"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.vehicleType").exists());
        verifyNoInteractions(courierService);
    }

    @Test
    void unknownEnumValueIs400() throws Exception {
        mvc.perform(post("/api/couriers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bob", "phone": "+15550200", "vehicleType": "HELICOPTER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingCourierIs404() throws Exception {
        when(courierService.get(99L)).thenThrow(new ResourceNotFoundException("Courier", 99L));

        mvc.perform(get("/api/couriers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Courier 99 not found"));
    }

    @Test
    void duplicatePhoneIs409() throws Exception {
        when(courierService.create(any())).thenThrow(new ConflictException("A courier with phone +15550200 already exists"));

        mvc.perform(post("/api/couriers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bob", "phone": "+15550200", "vehicleType": "CAR"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void nearbyRejectsOutOfRangeCoordinates() throws Exception {
        mvc.perform(get("/api/couriers/nearby").param("lat", "200").param("lng", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(courierService);
    }
}
