package com.fleettrack.courier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourierRepository extends JpaRepository<Courier, Long> {

    boolean existsByPhone(String phone);

    List<Courier> findByStatusOrderByIdAsc(CourierStatus status);

    List<Courier> findAllByOrderByIdAsc();

    /** Row-locks one courier for the rest of the transaction (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Courier c where c.id = :id")
    Optional<Courier> findByIdForUpdate(@Param("id") Long id);

    /**
     * Writes the latest position without touching {@code version}: a JPQL bulk update is not
     * versioned. Location pings arrive every few seconds; if each one bumped the version,
     * they would constantly cause optimistic-lock failures for business updates (assignment,
     * availability) on the same row.
     *
     * <p>The {@code lastSeenAt < :recordedAt} guard drops out-of-order pings atomically, so a
     * delayed old ping can never move a courier backwards.
     *
     * @return 1 if the position was updated, 0 if the ping was older than the stored one
     */
    @Modifying
    @Query("""
            update Courier c
               set c.lastLatitude = :latitude,
                   c.lastLongitude = :longitude,
                   c.lastSeenAt = :recordedAt
             where c.id = :id
               and (c.lastSeenAt is null or c.lastSeenAt < :recordedAt)
            """)
    int updateLastPosition(@Param("id") Long id,
                           @Param("latitude") double latitude,
                           @Param("longitude") double longitude,
                           @Param("recordedAt") Instant recordedAt);

    /**
     * Available couriers within {@code radiusMeters} of a point, nearest first.
     * ST_DWithin on geography uses the GiST index on last_location; {@code <->} is the
     * index-assisted KNN distance operator.
     */
    @Query(value = """
            SELECT c.id             AS "id",
                   c.name           AS "name",
                   c.vehicle_type   AS "vehicleType",
                   c.last_latitude  AS "latitude",
                   c.last_longitude AS "longitude",
                   ST_Distance(c.last_location,
                               CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distanceMeters"
              FROM couriers c
             WHERE c.status = 'AVAILABLE'
               AND ST_DWithin(c.last_location,
                              CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                              :radiusMeters)
             ORDER BY c.last_location <-> CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
             LIMIT :maxResults
            """, nativeQuery = true)
    List<NearbyCourierView> findAvailableNearby(@Param("latitude") double latitude,
                                                @Param("longitude") double longitude,
                                                @Param("radiusMeters") double radiusMeters,
                                                @Param("maxResults") int maxResults);

    /**
     * Finds and row-locks the nearest available courier for auto-assignment.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED}: if two dispatchers auto-assign at the same moment,
     * the second transaction skips the courier the first has locked and takes the next
     * nearest, instead of blocking on it or double-booking it.
     *
     * <p>Orders by ST_Distance rather than the KNN operator {@code <->}: PostgreSQL raises
     * "attempted to lock invisible tuple" when a KNN-ordered GiST scan is combined with
     * FOR UPDATE. ST_DWithin still uses the GiST index to narrow candidates to the radius,
     * so sorting that small set is cheap.
     */
    @Query(value = """
            SELECT c.id, c.name, c.phone, c.vehicle_type, c.status, c.last_latitude, c.last_longitude,
                   c.last_seen_at, c.version, c.created_at, c.updated_at
              FROM couriers c
             WHERE c.status = 'AVAILABLE'
               AND ST_DWithin(c.last_location,
                              CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                              :radiusMeters)
             ORDER BY ST_Distance(c.last_location,
                                  CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography))
             LIMIT 1
               FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Courier> lockNearestAvailable(@Param("latitude") double latitude,
                                           @Param("longitude") double longitude,
                                           @Param("radiusMeters") double radiusMeters);
}
