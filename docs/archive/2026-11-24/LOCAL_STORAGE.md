# Local storage – first implementation

PulseLibre uses Android's private SQLite database `pulselibre_readings.db` without a network permission. `LocalReadingStore` stores original measurement timestamps, receipt timestamps, device IDs, metric names, values, units and optional sample IDs. Its `commitBatch` method inserts an entire validated batch and advances the device checkpoint **in the same SQLite transaction**.

## Guarantees and limits

- Identical readings are ignored on replay using a composite primary key; existing data is not overwritten.
- The checkpoint never decreases, and an invalid batch fails validation before the transaction starts.
- `readWindow` filters by **measurement** timestamp using inclusive start and exclusive end; receipt time is preserved separately.
- No inferred or synthetic sensor data is stored. No connection to a Garmin device is attempted by this component.
- The app has **not yet wired live BLE notifications to this store**, because the CIRQA data protocol has not been verified. The dashboard is not yet populated from this database.
- Export UI, date selection, streaming large exports, database migration from future schema versions, backups and optional encryption remain future tasks.
- A timestamp-based cursor is a local persistence checkpoint, **not proof that the device supports historical sync**. A real device protocol may require a different cursor.
- Instrumentation tests verify persistence across reopening, replay idempotence, cursor monotonicity, window boundaries, and rejection of mixed-device batches. The CI emulator must pass before this work is considered verified.

No change to Bluetooth pairing, Garmin Connect, or the phone is required.
