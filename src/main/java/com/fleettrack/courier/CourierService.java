package com.fleettrack.courier;

import java.util.List;

import com.fleettrack.common.exception.ConflictException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.courier.dto.CourierResponse;
import com.fleettrack.courier.dto.CreateCourierRequest;
import com.fleettrack.courier.dto.NearbyCourierResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierService {

    private final CourierRepository courierRepository;

    public CourierService(CourierRepository courierRepository) {
        this.courierRepository = courierRepository;
    }

    @Transactional
    public CourierResponse create(CreateCourierRequest request) {
        // Friendly fast-path check. The UNIQUE constraint on phone is the real guarantee:
        // if two requests race past this line, the loser gets a DataIntegrityViolation -> 409.
        if (courierRepository.existsByPhone(request.phone())) {
            throw new ConflictException("A courier with phone " + request.phone() + " already exists");
        }
        Courier courier = new Courier(request.name().trim(), request.phone(), request.vehicleType());
        return CourierResponse.from(courierRepository.saveAndFlush(courier));
    }

    @Transactional(readOnly = true)
    public CourierResponse get(Long id) {
        return CourierResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> list(CourierStatus status) {
        List<Courier> couriers = status == null
                ? courierRepository.findAllByOrderByIdAsc()
                : courierRepository.findByStatusOrderByIdAsc(status);
        return couriers.stream().map(CourierResponse::from).toList();
    }

    @Transactional
    public CourierResponse changeAvailability(Long id, CourierStatus target) {
        Courier courier = findOrThrow(id);
        courier.changeAvailability(target);
        return CourierResponse.from(courierRepository.saveAndFlush(courier));
    }

    @Transactional(readOnly = true)
    public List<NearbyCourierResponse> findAvailableNearby(double latitude, double longitude,
                                                          double radiusMeters, int limit) {
        return courierRepository.findAvailableNearby(latitude, longitude, radiusMeters, limit).stream()
                .map(NearbyCourierResponse::from)
                .toList();
    }

    Courier findOrThrow(Long id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", id));
    }
}
