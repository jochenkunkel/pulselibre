# Garmin Multi-Link experimental service discovery

## Implementation
- `GarminServiceDiscovery.CANDIDATES = [16]` is the reviewed allowlist.
- `CirqaMlHandshakeProbe.startMotionDiscovery()` performs an explicitly user-triggered, 20-second GATT probe; uses the existing Garmin GFDI handshake without CLOSE_ALL, registers only candidate service 16, and counts frames routed to the dynamically assigned handle.
- The device screen exposes a dedicated button and reports accepted/rejected/unanswered status and packet count.
- No motion packet content is stored, no decoder assumes a payload format, no nightly collector is changed.
- Other service IDs are not blindly enumerated. Review each candidate's semantics and external evidence before allowlisting.

## Interpretation
A successful registration means the device acknowledged the service. A nonzero packet count means frames were routed to its registered handle. Neither establishes that they are accelerometer data, that axes/units are known, or that the stream is suitable for actigraphy. A rejection or timeout is not proof that the hardware lacks an accelerometer.

## Test protocol
1. Stop the overnight collector to avoid concurrent BLE connections.
2. Open device tab and run **CIRQA: Bewegungssensor (Service 16) prüfen** once.
3. Record the exact summary (accepted/rejected/timeout, packet count) and app build.
4. If accepted with frames, implement a separate bounded, consented diagnostic with payload length and redacted hex samples for offline decoder validation; never enable high-frequency collection by default.
5. Validate accelerometer axis encoding, sample rate and stationary/movement behavior against a reference before calculating motion-based sleep measures.

No unpair/reset, CLOSE_ALL, internet access, or background accelerometer recording.

## First CIRQA device test and fix (2026-11-16)

The first on-device test reported `Service 16 ohne bestätigte Antwort`, while the standard nine-service test successfully decoded 52 frames. Code review found a routing bug: after GFDI registration the shared handshake unconditionally sent the heart-rate registration request (service 6) even in exploratory mode, while `pendingService` was 16. Thus the first result is **inconclusive** and does not demonstrate service-16 rejection or absence. Fixed in commit `ffa17a33` by selecting `servicePlan.first()` for expanded/discovery mode. Re-test using a build containing the fix. The test still does not decode motion payloads.
