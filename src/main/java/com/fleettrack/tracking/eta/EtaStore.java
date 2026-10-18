package com.fleettrack.tracking.eta;

import java.time.Duration;
import java.util.Optional;

import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.messaging.EventJson;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * ETAs change on every ping and are worthless after a few minutes, so they live in Redis
 * with a TTL rather than in PostgreSQL. Writing them to the deliveries table would mean a
 * DB write per ping per delivery, plus optimistic-lock conflicts with dispatcher updates.
 */
@Component
public class EtaStore {

    private static final String KEY_PREFIX = "fleettrack:eta:delivery:";

    private final StringRedisTemplate redis;
    private final EventJson json;
    private final Duration ttl;

    public EtaStore(StringRedisTemplate redis, EventJson json, FleettrackProperties properties) {
        this.redis = redis;
        this.json = json;
        this.ttl = Duration.ofMinutes(properties.eta().ttlMinutes());
    }

    public void save(EtaEstimate estimate) {
        redis.opsForValue().set(KEY_PREFIX + estimate.deliveryId(), json.write(estimate), ttl);
    }

    public Optional<EtaEstimate> find(Long deliveryId) {
        String value = redis.opsForValue().get(KEY_PREFIX + deliveryId);
        return Optional.ofNullable(value).map(v -> json.read(v, EtaEstimate.class));
    }
}
