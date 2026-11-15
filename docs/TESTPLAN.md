# PulseLibre 0.1 test plan

## Automated
- JVM unit tests: hex payload formatting and empty payload.
- Android lint and debug APK build in GitHub Actions.

## Manual on real Android phone
1. Launch with Bluetooth off: scan prompts to enable Bluetooth.
2. Grant Nearby Devices (Android 12+) or Location (Android 11-): scan starts.
3. Deny permissions: app must not crash; retry prompts again.
4. Scan for 10 seconds: each BLE address listed once, RSSI shown.
5. Select a non-sensitive test BLE device: attempt GATT connection and enumerate services.
6. Test Garmin CIRQA while Garmin Connect is still paired: record whether scan visibility and GATT discovery work. **Do not unpair or reset yet.**
7. Switch app away / back; close app; no background data transmission.
8. Inspect merged Android manifest: INTERNET permission must be absent.

## Known limitations
- No Garmin proprietary decoding, no historical sync, no guaranteed live heart-rate.
- GATT notifications are displayed only if a peripheral sends them; subscribing to CCCD is future work.
- Android 12+ neverForLocation may filter some beacon results.
- Debug APK signing can change across CI runs. Stable release signing is future work.
- Device tests require real hardware; CI does not prove CIRQA compatibility.
