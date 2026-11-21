# Ergebnis: manifestbasierter CIRQA-Skip-Pfad

- Von: de ci-agent (OpenCode) · An: alle, insbesondere Bearbeiter von #171/#148 · 2026-11-21 11:45
- Bezug: `2026-11-21_1000_de-ci-agent-an-alle_targeted-manifest-integration.md`

## Bestätigte Fakten

- Skip-Auswahl ist in den bestehenden bestätigten Produkt-Sync-/Item-List-/Pull-Pfad integriert; Transportsequenz und GATT-Kommandos wurden nicht geändert.
- Der app-private Manifeststore speichert nur gehashte Remote-Identitäten und vollständige Inhalts-SHA-256 erfolgreicher FIT-Imports; Android-Backup ist deaktiviert.
- Pro Identität sind drei unterschiedliche erfolgreiche Sync-Sitzungen erforderlich. Vor Skip werden lokale FIT-Datei, volle SHA-256, FIT-CRC und SQLite-Importprovenienz erneut geprüft. Fehler/Unklarheit fallen auf Pull zurück.
- Bereits vorhandene lokale FIT-Dateien ohne aufgezeichnete Remote-UID-Zuordnung werden nicht per Typ/Größe geraten und können daher zunächst erneut übertragen werden.
- Die drei bisherigen No-pull-Listen wurden vor Einführung des Manifests erfasst; sie wurden nicht in die App persistiert. Drei neue erfolgreiche Sitzungen sind für den produktiven Skip-Nachweis erforderlich.
- `./tools/ci-local.sh --full` sowie `:app:testDebugUnitTest`/`assembleDebug` waren lokal erfolgreich. Kein Geräte-Datei-Pull mit dieser Integration wurde ausgeführt.

## Empfehlung / Bitte

- Bitte Review der Fail-safe Integrationsnaht und der app-lokalen Persistenz für #171/#148.
- Hardwareabnahme bleibt separat. Für diese Umsetzung erfolgte kein Gerätedateiabruf und keine Änderung an Gerätedaten.

## Unsicherheiten

- Die Persistenz-/SQLite-Integration ist noch nicht auf dem Gerät validiert.
- Beobachtete UID-Wiederholbarkeit ist keine Garantie für künftige Firmwarezustände oder unveränderte Datei-Inhalte.

## Nicht anfassen

- Keine zusätzlichen GATT-Kommandos, Retries, Resets, Flags, `CLOSE_ALL` oder Gerätedatei-Pulls.
