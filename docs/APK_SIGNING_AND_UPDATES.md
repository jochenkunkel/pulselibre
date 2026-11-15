# Android APK signing and in-place updates

## Observed on Samsung Galaxy S25 (2026-11-15)

The user reported that installing the CI debug APK over an existing PulseLibre installation failed with Android's generic "App nicht installiert" message. Uninstalling PulseLibre and installing the new APK succeeded. A signing-certificate mismatch is plausible, **not confirmed**; Android's installer message alone cannot distinguish this from all other installation failures.

## Cause to investigate

GitHub Actions currently builds `assembleDebug` on ephemeral Ubuntu runners. The Android debug signing key is generated in the runner's local Android configuration and is not intentionally persisted between CI runs. Different certificates prevent Android from installing an APK over the same application ID, even if `versionCode` increases.

## Safe production-ready strategy

1. Create a dedicated PulseLibre signing key **locally**, outside Git, and store its encrypted backup securely. Never commit the keystore, passwords, private keys or signing credentials.
2. Configure a **separate** signed distribution build with GitHub Actions encrypted secrets and an explicitly configured Gradle signingConfig; do not embed any secrets in workflow logs or artifacts.
3. Verify the signing certificate SHA-256 fingerprint on consecutive artifacts using `apksigner verify --print-certs`, and verify that `versionCode` increases.
4. Because existing debug APKs may use different certificates, the **first** switch to a stable certificate may still require one uninstall, with explicit warning that app-local data is erased. Subsequent releases signed with the same key should permit in-place updates.
5. Keep debug CI build/emulator tests working independently of release-signing secrets. Do not publish an unsigned or unverified release artifact as an update.

**Status:** signing-key provisioning and encrypted GitHub secret setup have **not** been performed. No claim of stable update compatibility until verified on the S25.
