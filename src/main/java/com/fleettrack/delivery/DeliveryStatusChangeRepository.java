package com.fleettrack.delivery;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryStatusChangeRepository extends JpaRepository<DeliveryStatusChange, Long> {

    List<DeliveryStatusChange> findByDeliveryIdOrderByChangedAtAscIdAsc(Long deliveryId);
}
