package com.fleettrack.courier;

import java.net.URI;
import java.util.List;

import com.fleettrack.courier.dto.CourierResponse;
import com.fleettrack.courier.dto.CreateCourierRequest;
import com.fleettrack.courier.dto.NearbyCourierResponse;
import com.fleettrack.courier.dto.UpdateAvailabilityRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/couriers")
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @PostMapping
    public ResponseEntity<CourierResponse> create(@Valid @RequestBody CreateCourierRequest request) {
        CourierResponse created = courierService.create(request);
        return ResponseEntity.created(URI.create("/api/couriers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public CourierResponse get(@PathVariable Long id) {
        return courierService.get(id);
    }

    @GetMapping
    public List<CourierResponse> list(@RequestParam(required = false) CourierStatus status) {
        return courierService.list(status);
    }

    @PatchMapping("/{id}/availability")
    public CourierResponse changeAvailability(@PathVariable Long id,
                                              @Valid @RequestBody UpdateAvailabilityRequest request) {
        return courierService.changeAvailability(id, request.status());
    }

    /** Example: GET /api/couriers/nearby?lat=40.7128&lng=-74.0060&radiusMeters=3000 */
    @GetMapping("/nearby")
    public List<NearbyCourierResponse> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "5000") @Positive @DecimalMax("100000") double radiusMeters,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return courierService.findAvailableNearby(lat, lng, radiusMeters, limit);
    }
}
