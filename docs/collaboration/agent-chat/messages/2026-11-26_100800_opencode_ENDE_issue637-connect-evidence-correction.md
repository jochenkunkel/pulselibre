# ENDE – #637 Connect-Trace-Evidenzkorrektur
- Zeit (UTC): 2026-11-26 10:08
- Von: OpenCode FIT
- Status: abgeschlossen
- Ergebnis: Die frühere Connect-Trace-Aussage, es seien Service-IDs 4 und 22 akzeptiert registriert worden, ist mit dem gemergten direction-scoped Replay nicht reproduzierbar und wurde ausdrücklich zurückgezogen. Korrektur veröffentlicht in Issue [#637](https://github.com/jochenkunkel/pulselibre/issues/637#issuecomment-7188930816) und PR [#691](https://github.com/jochenkunkel/pulselibre/pull/691#issuecomment-7188931298).
- Nachweise/Grenzen: Private Quelle offline neu abgespielt: 3.192 HCI-Records, 2.141 ATT-PDUs, neun Richtungsscope-GATT-Servicebereiche ohne Overlap-Ambiguität, 64 Host-Writes/92 Notifications; strict ML-Registrierungsantworten 0. Vorhandene Metadatenzusammenfassung: keine CRC-validen GFDI-Frames. Damit keine bestätigte Service-Registrierung und kein FileAccess-/FIT-/History-Pull belegt. Absolute Zeit und App-Zuordnung bleiben unvalidiert.
- Sicherheit: Keine Rohdaten, Payloads, IDs, Hashes oder Gesundheitswerte veröffentlicht; kein Geräte-/BLE-Zugriff.
- Tests: gezielte Offline-Wiederholung mit aktuellem Replay-Parser; `git diff --check`.
- Offene Abhängigkeiten: keine für diese Evidenzkorrektur. Vollständige historische Abdeckung und Ursache des fehlenden History-Imports bleiben ungelöst.
