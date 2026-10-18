#!/usr/bin/env python3
"""
FleetTrack courier simulator - standard library only.

Creates couriers around a city centre, makes them available, then every tick:
  * moves every courier a little and reports its position (POST /locations),
  * occasionally creates a delivery and auto-assigns the nearest courier,
    retrying deliveries that found no free courier on later ticks,
  * advances assigned deliveries through PICKED_UP -> IN_TRANSIT -> DELIVERED,
    steering the courier toward the pickup / dropoff point.

Usage:
  python scripts/simulate.py                       # 10 couriers around New York
  python scripts/simulate.py --couriers 25 --tick 0.5
  python scripts/simulate.py --base-url http://localhost:8080 --lat 51.5074 --lng -0.1278
Open http://localhost:8080 to watch the live map.
"""
import argparse
import json
import math
import random
import time
import urllib.error
import urllib.request


def call(base, method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except ValueError:
            return e.code, raw.decode(errors="replace")


def jitter(lat, lng, meters):
    dlat = (random.uniform(-1, 1) * meters) / 111_320
    dlng = (random.uniform(-1, 1) * meters) / (111_320 * math.cos(math.radians(lat)))
    return lat + dlat, lng + dlng


def step_toward(lat, lng, tlat, tlng, meters):
    dy = (tlat - lat) * 111_320
    dx = (tlng - lng) * 111_320 * math.cos(math.radians(lat))
    dist = math.hypot(dx, dy)
    if dist <= meters:
        return tlat, tlng, True
    f = meters / dist
    return lat + (tlat - lat) * f, lng + (tlng - lng) * f, False


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base-url", default="http://localhost:8080")
    p.add_argument("--couriers", type=int, default=10)
    p.add_argument("--lat", type=float, default=40.7128)
    p.add_argument("--lng", type=float, default=-74.0060)
    p.add_argument("--tick", type=float, default=1.0, help="seconds between position reports")
    p.add_argument("--speed-kmh", type=float, default=30.0)
    p.add_argument("--max-waiting", type=int, default=3, help="stop creating orders while this many wait for a courier")
    args = p.parse_args()
    base = args.base_url.rstrip("/")
    step_m = args.speed_kmh * 1000 / 3600 * args.tick

    run_id = random.randint(100, 999)
    couriers = {}
    for i in range(args.couriers):
        status, c = call(base, "POST", "/api/couriers", {
            "name": f"Sim Courier {run_id}-{i}",
            "phone": f"+1{run_id}{i:07d}",
            "vehicleType": random.choice(["BICYCLE", "MOTORBIKE", "CAR"]),
        })
        if status != 201:
            raise SystemExit(f"Could not create courier: {status} {c}")
        call(base, "PATCH", f"/api/couriers/{c['id']}/availability", {"status": "AVAILABLE"})
        lat, lng = jitter(args.lat, args.lng, 3000)
        couriers[c["id"]] = {"lat": lat, "lng": lng, "delivery": None}
    print(f"Created {len(couriers)} couriers. Ctrl+C to stop.")

    deliveries = {}  # delivery id -> dict(status, pickup, dropoff, courier)
    waiting = []     # created but not yet assigned, oldest first
    tick = 0
    while True:
        tick += 1
        # A new order arrives every ~5 ticks, but only while the backlog is small.
        if tick % 5 == 0 and len(waiting) < args.max_waiting:
            plat, plng = jitter(args.lat, args.lng, 2500)
            dlat, dlng = jitter(plat, plng, 2000)
            s, d = call(base, "POST", "/api/deliveries", {
                "recipientName": "Sim Customer", "recipientPhone": "+15550100",
                "pickupLatitude": plat, "pickupLongitude": plng,
                "dropoffLatitude": dlat, "dropoffLongitude": dlng})
            if s == 201:
                waiting.append({"id": d["id"], "pickup": (plat, plng), "dropoff": (dlat, dlng)})

        # Like a real dispatcher: retry unassigned deliveries, oldest first, until one finds no courier.
        while waiting:
            order = waiting[0]
            s, a = call(base, "POST", f"/api/deliveries/{order['id']}/assign")
            if s != 200:
                break  # 409: every courier in range is busy; try again next tick
            waiting.pop(0)
            cid = a["courierId"]
            deliveries[order["id"]] = {"status": "ASSIGNED", "pickup": order["pickup"],
                                       "dropoff": order["dropoff"], "courier": cid}
            couriers[cid]["delivery"] = order["id"]
            print(f"Delivery {order['id']} assigned to courier {cid}")

        for cid, c in couriers.items():
            did = c["delivery"]
            if did is None:
                c["lat"], c["lng"] = jitter(c["lat"], c["lng"], step_m)
                speed = random.uniform(0, args.speed_kmh / 2)
            else:
                d = deliveries[did]
                target = d["pickup"] if d["status"] == "ASSIGNED" else d["dropoff"]
                c["lat"], c["lng"], arrived = step_toward(c["lat"], c["lng"], *target, step_m)
                speed = args.speed_kmh
                if arrived and d["status"] == "ASSIGNED":
                    for nxt in ("PICKED_UP", "IN_TRANSIT"):
                        call(base, "POST", f"/api/deliveries/{did}/status", {"status": nxt})
                    d["status"] = "IN_TRANSIT"
                    print(f"Delivery {did} picked up")
                elif arrived and d["status"] == "IN_TRANSIT":
                    call(base, "POST", f"/api/deliveries/{did}/status", {"status": "DELIVERED"})
                    print(f"Delivery {did} delivered")
                    del deliveries[did]
                    c["delivery"] = None
            call(base, "POST", f"/api/couriers/{cid}/locations",
                 {"latitude": c["lat"], "longitude": c["lng"], "speedKmh": round(speed, 1)})
        time.sleep(args.tick)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped.")
