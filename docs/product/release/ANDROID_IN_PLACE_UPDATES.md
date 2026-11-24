# Android in-place APK upgrades — diagnosis and setup

## Findings (2026-11-16)

- App package ID is stable: `de.pulselibre`.
- Previously `versionCode = 2` was fixed for every CI build.
- Workflow used `assembleDebug` on fresh GitHub runners. The default generated debug signing certificate may change between runners; Android rejects mismatched signatures.
- No device-side install failure log has been supplied; signature mismatch is a **strong hypothesis**, not a confirmed diagnosis.
- Neither an experimental app nor a debug build inherently prevents upgrades.
- App private SharedPreferences (profile) and SQLite readings survive a normal signed in-place upgrade, but are removed on uninstall (unless separately backed up; this app disables Android backup).

## Implemented

- `app/build.gradle.kts` accepts `-PpulselibreVersionCode=N`; CI sets it to `github.run_number`. Never lower the code relative to installed APK. If another distribution channel has higher codes, adjust strategy.
- CI restores one persistent private signing keystore from four GitHub Actions secrets. If absent, tests can run but **the APK artifact is not published**, preventing accidental distribution of an incompatible debug APK.
- CI prints non-secret package/version/certificate SHA-256 digest, for comparisons across runs.
- Do **not** commit signing keys, passwords, or exported private key material.

## One-time repository secrets (required for stable updateable CI APK)

Create and securely back up a dedicated Android test keystore *outside the repository*. On a trusted Linux/macOS machine, use `bash scripts/create-android-signing-key.sh` (the script refuses to overwrite an existing key). Alternatively:

```bash
keytool -genkeypair -v -keystore pulselibre-ci.keystore -alias pulselibre-ci -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 pulselibre-ci.keystore
```

In GitHub repository Settings → Secrets and variables → Actions, add:
- `PULSELIBRE_SIGNING_KEY_B64`: output of base64 command (private; never publish)
- `PULSELIBRE_SIGNING_STORE_PASSWORD`: keystore password
- `PULSELIBRE_SIGNING_KEY_ALIAS`: `pulselibre-ci`
- `PULSELIBRE_SIGNING_KEY_PASSWORD`: key password

Keep a safe offline backup of the keystore and passwords. Losing or changing the signing key breaks upgrades from prior installs. GitHub PRs from forks generally do not receive these secrets, so their APKs may use a different signature. The same key should not be reused for a future public production release.

**Migration limitation:** If the currently installed APK was signed with an earlier ephemeral debug certificate, a newly stably signed APK **cannot update it in place**. One last uninstall/reinstall may be necessary when switching keys. Export app data before doing so; uninstallation deletes local readings and profile.

## Determine the exact Android install error

Via a PC with Android platform-tools (ADB), with phone connected and USB debugging enabled:

```bash
adb install -r app-debug.apk
```

Typical failures:
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: certificate/signature mismatch
- `INSTALL_FAILED_VERSION_DOWNGRADE`: new versionCode lower than installed
- `INSTALL_FAILED_INVALID_APK`: packaging or APK issue
- `INSTALL_FAILED_INSUFFICIENT_STORAGE`: not enough free space

Use `apksigner verify --print-certs app-debug.apk` on both old/new APKs and compare signer SHA-256 fingerprints. `adb shell dumpsys package de.pulselibre` can show installed version metadata.

## Acceptance test

1. Configure secrets and confirm the CI log reports `Stable signing configured: yes`.
2. Install a freshly built APK, enter a dummy test profile value and collect a harmless local test event.
3. Trigger a subsequent CI build and install its APK **over** the first without uninstalling.
4. Confirm new version starts, profile persists, and database content remains.
5. If upgrade fails, collect the exact installer/ADB error before changing package ID or wiping the app.

Never commit personal health data to CI artifacts. 

## Artifact distribution safeguard

The workflow publishes `pulselibre-signed-update-apk` only when the stable signing secrets are present and the build succeeds, and never for pull requests. When secrets are missing, no APK artifact is offered as an update. A signing-key change still requires a migration decision: Android does not automatically trust a different certificate. The signing fingerprint printed in CI should remain identical between two consecutive update APKs. A true device upgrade test remains mandatory.
