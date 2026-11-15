# PulseLibre

**Verbindliche Zielgeräte:** [Garmin CIRQA Smartband und Samsung Galaxy S25 – Kontext, Sicherheitsregeln und Teststatus](docs/TARGET_DEVICES.md)

**Projektleitbild und vollständige Anforderungen:** [Warum, was und wie wir entwickeln](docs/REQUIREMENTS_AND_PRINCIPLES.md)

![PulseLibre project logo](design/pulselibre-icon.svg)

[Project poster](design/pulselibre-poster.svg) · [Brand guide](docs/BRANDING.md) · [Icon integration](docs/ICON_INTEGRATION.md)

Privacy-first Android Bluetooth LE explorer for Garmin CIRQA research.

## Goal

An offline-first Android prototype that scans for nearby Bluetooth LE devices, connects on demand, discovers GATT services/characteristics, and displays received notifications and characteristic reads. This is a diagnostic prototype, **not yet Garmin CIRQA protocol support**. Garmin Connect pairing is not modified.

## Privacy

No INTERNET permission, analytics, account, or cloud upload. Do not commit personal sensor logs. Bluetooth MAC addresses and raw payloads can be sensitive.

## Build

JDK 17, Android SDK 35, Gradle 8.13. Run `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (or `./gradlew` once the wrapper has been generated and committed). Android 12+ requires Nearby Devices permissions; Android 11 and earlier require location permission for BLE scanning.

## Roadmap

1. BLE scanner, connect/disconnect, GATT service discovery, read/notify diagnostic view.
2. Investigate Garmin CIRQA protocol with legally usable references and verify compatibility on a real device.
3. Offline Room history and charts.
4. Background synchronization and reliable signed releases.

**Caution:** Scanning can work while Garmin Connect remains paired, but connecting to a proprietary device may require unpairing or exclusive access. Do not reset/unpair before saving existing Garmin data.
