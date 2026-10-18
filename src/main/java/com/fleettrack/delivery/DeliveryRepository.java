package com.fleettrack.delivery;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByCourier_IdAndStatusIn(Long courierId, Collection<DeliveryStatus> statuses);

    List<Delivery> findByStatusOrderByIdDesc(DeliveryStatus status);

    List<Delivery> findTop100ByOrderByIdDesc();
}
