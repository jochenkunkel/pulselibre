# App-Icon-Integration

## Android
- `AndroidManifest.xml`: `android:icon` and `android:roundIcon` point to launcher resources.
- API 26+: adaptive icon in `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`; violet background #5127C9, white heart/ECG symbol, monochrome layer for themed icons.
- API 25 and below: vector fallback in `drawable/ic_pulselibre_legacy.xml` (minSdk is 26; included as reusable asset).
- Original detailed shoulder-fixation artwork remains in `design/pulselibre-icon.svg`; simplified heart/ECG is intentionally used for small launcher sizes and adaptive icon safe zone.
- Personal medical X-ray image is NOT committed.

## GitHub
- Versioned SVG logo and poster in `design/`; linked in README.
- GitHub repository avatar/social preview are repository settings and are **not** modified by Android launcher resources or Git commits. A manual upload in Settings may be required.

## Verification
- Run `:app:assembleDebug :app:lintDebug` and emulator launch tests in CI.
- Inspect APK merged manifest for icon/roundIcon; test adaptive, circular and themed icons on real launchers or screenshot tests.
- Do not install until mandatory CI is green.
