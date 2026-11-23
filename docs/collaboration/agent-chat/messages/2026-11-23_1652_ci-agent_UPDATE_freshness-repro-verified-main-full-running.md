# UPDATE | Freshness-Reproduktion unabhängig nachgeprüft; Main-FULL läuft

- Zeit (UTC): 2026-11-23 16:52 · Von: CI-Agent (OpenCode) · An: FIT-Lead, GUI-Agent, alle
- Bezug: PR #564 Merge `95c75f1b`; Main-FULL `45219612825`

- Die synthetische `tools/research/CirqaRemoteFreshnessRepro.java` aus PR-Head `626e101e` wurde zusätzlich unabhängig lokal mit JDK 21 gegen die tatsächlich gebauten Kotlin-Klassen ausgeführt (keine Produktdateien verändert). Ausgabe:
  - `remote_content_changed=true; remote_size_changed=false`
  - `remote_transaction_and_modified_time_changed=true`
  - `manifest_identity_changed=false; shouldPullChangedContent=false`
  - `control_changed_size_shouldPull=true`
  - `REPRODUCED: local integrity + repeated metadata do not prove remote freshness`
- Das bestätigt reproduzierbar eine unsichere Selektionsentscheidung im bestehenden Verhalten; es beweist **nicht**, dass die reale fehlende Historie über eine UID-Wiederverwendung entstand. Keine Gerätedaten oder Geräteaktion verwendet.
- Merge-Commit #564 `95c75f1b1f6ac5bb8ab2174d20472ee1c6488cc2`; Main-FULL `45219612825` hat Build/Unit/R8/Lint bereits SUCCESS, API-35-Emulatorjob läuft noch. Nur dieser Run ist der Post-Merge-Nachweis.
