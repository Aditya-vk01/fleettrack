package com.fleettrack.location;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, Long> {

    /** Served by idx_courier_locations_courier_recorded (courier_id, recorded_at DESC). */
    List<CourierLocation> findByCourierIdOrderByRecordedAtDesc(Long courierId, Pageable pageable);
}
