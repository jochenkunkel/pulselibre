# Dashboard implementation – first vertical slice

Implemented in Android UI: six offline dashboard cards (heart rate, SpO₂, steps, sleep, Ground Rules, nutrition/energy), BLE explorer beneath, and explicit empty states.

## Data integrity
- `HealthSnapshot` defaults to null for all readings; no seeded or fabricated health measurements.
- `dashboardMetrics` validates nonnegative counts, pulse range and SpO₂ range before formatting. These checks are **technical input sanity checks, not medical reference limits**.
- No actual CIRQA readings, SpO₂ chart, nutrition logging, profile or local database yet.
- No permission prompt on app launch; BLE permissions requested on user-initiated scan/connect.
- No network permission; existing Garmin Connect pairing must not be modified.

## Tests
- JVM: `DashboardModelTest` covers missing data, valid formatting, invalid input, and explicit zero SpO₂ preservation.
- Android emulator: `DashboardTest` verifies six visible UI cards, truthful empty-state labels and application title.
- Existing `LaunchTest`, `BleSessionStateTest`, `GroundRulesTest`, `NutritionRulesTest` remain in CI.
- **Release gate:** verify new CI build and emulator job both pass before considering any installation. This is an emulator-only development stage.

## Next increments
1. Extract Bluetooth handling from activity into lifecycle-aware controller; add transport fakes and timeout tests.
2. Profile editor and local-only persistence with validation and instrumentation tests.
3. Real time-series model and SpO₂ chart with empty state, data provenance and chart tests.
4. Ground Rules UI bound to sourced functions; nutrition logging only with verified inputs.
