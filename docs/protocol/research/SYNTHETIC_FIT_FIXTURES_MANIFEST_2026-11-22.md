# Manifest: synthetische FIT-Regressionstest-Fixtures (CI-Agent, 22.11.2026)

> **Ergänzung #628, 24.11.2026:** Die nachstehende Tabelle dokumentiert den
> historischen Stand. Die aktuelle Test-Suite verwendet `hr-placeholder-v2-absolute-time`
> (`9c6f9a7c2ecfa337cf99faf224df82c117300d06ece28f79af0cc63c1c813c8e`)
> und `file-crc-tampered-v2`
> (`05f7933b4b2c3c52f030ac7bdd4a3d85ec6989d10a9dd1ae0acfea82c558ea4b`).
> Die Änderung ersetzt den relativen Testzeitwert durch einen absoluten FIT-Wert;
> beide Fixtures bleiben synthetisch. Die leere Fixture ist unverändert.

## Herkunft und Zweck

- **Herkunft pro Datei:** `generated-fixture` / `synthetic`. Alle Bytes werden zur Testlaufzeit in `app/src/test/java/de/pulselibre/SyntheticFitFixtures.kt` aus literalen Platzhalterwerten erzeugt. Kein Byte stammt von einem Gerät, einem BLE-/GATT-/HCI-Trace, einem Original-FIT-Export, einer Kopplung oder einer Health-Datenbank.
- **Zweck:** reproduzierbare, öffentlich versionierbare JVM-Regressionen für FIT-Envelope, Header-/Datei-CRC, Tamper-Erkennung, Dekoder-Erwartung und Importierbarkeit. Kein Beleg für CIRQA-Dateninhalte.
- **Freigabegrundlage:** `docs/collaboration/CURRENT_BAND_RAW_DATA_POLICY.md`, Ergänzung vom 22.11.2026 (geprüfte synthetische Fixtures in Git), und die Projektentscheidung in `AGENTS.md`. Anfrage: `docs/collaboration/agent-sync/2026-11-22_1520_chatgpt-an-alle_fit-feldinventur-fixtures.md`.

## Prüfverfahren und Grenzen

1. Envelope: 14-Byte-Header, Signatur `.FIT`, Längenkonsistenz; Header-CRC und Datei-CRC werden mit der Produktionsfunktion `FitFileHeader.crc16` (CRC-16/ARC) berechnet, nicht aus Gerätedaten kopiert.
2. Validierung in `app/src/test/java/de/pulselibre/SyntheticFitFixturesTest.kt`: festgenagelte Länge und Nutzlast, SHA-256, `FitFileHeader.inspect`/`inspectVerified`, `FitHistoricalObservations.decode`, Tamper-Ablehnung und Import über `CirqaFitBackfill` mit `RecordingStore`.
3. Ausschlussprüfung: die veröffentlichten Bytes werden auf Geräte-, Kopplungs-, Token-, Secret- und Identifikatormarker geprüft; die Nutzlast ist als exakte Bytefolge festgenagelt. Der Herzschlag-Platzhalter `42` ist eine feste Testkonstante und **kein** gemessener Gesundheitswert.
4. **Keine Behauptung:** Diese Fixtures belegen weder die vollständige Gerätehistorie noch einen semantisch vollständigen Import von SpO₂, Stress, Atemfrequenz oder anderen CIRQA-Metriken. `coverage=device-history-unverified` bleibt gültig.

## Datensätze

| ID | Zweck | Dateigröße | Nutzlast | Header-/Datei-CRC gültig | Dekodierte Beobachtungen | SHA-256 der erzeugten Bytes |
| --- | --- | ---: | ---: | --- | ---: | --- |
| `hr-placeholder-v1` | positiver Import-/Dekoder-/CRC-Regressionsfall | 34 B | 18 B | ja | 1 (HR-Platzhalter 42 bpm) | `b642717725e46f4bf6ebb7d33b857862c530a55acf29e780dea76349ea293b00` |
| `empty-payload-v1` | gültige leere Envelope, keine Beobachtung | 16 B | 0 B | ja | 0 | `bb2a825cc4c918350deb04cc46d35dd482055a755b731012805c58c2ae02ddfc` |
| `file-crc-tampered-v1` | negatives Beispiel, ein invertiertes Datei-CRC-Byte | 34 B | 18 B | nein (Datei-CRC) | 0 (Dekoder lehnt ab) | `5e6fd71b213775ee9bf3bf4d909e28f2993351e021afbb44614c8e49c98b88b8` |

## Format und Erwartungswerte

- FIT-Header: `headerSize=14`, `protocolVersion=0x20`, `profileVersion=100` (Rohwert im Header, entsprechend dem bestehenden synthetischen Testmuster in `CirqaFitBackfillTest`; **nicht** zu verwechseln mit der Produktions-Profilkennzeichnung `FitHistoricalObservations.PROFILE_VERSION = 21.222.0`).
- Erwartete Importresultate: `hr-placeholder-v1` → 1 Datei importiert, 1 importierbare und 1 committete Beobachtung, Provenienz-SHA-256 gleich dem obigen SHA-256; Replay erwartet denselben Idempotenz-Key. `empty-payload-v1` → gültige Datei ohne importierbare Beobachtungen; `CirqaFitBackfill` schreibt einen Provenienz-Eintrag, aber **keine** Beobachtung. `file-crc-tampered-v1` → `INVALID_FILE`, kein Provenienz-Eintrag, keine Beobachtung.
- Lizenz-/Nutzungsgrenze: ausschließlich für PulsLibre-Tests und -Forschung. Keine Weitergabe als Geräte-, Patienten- oder Produktivdaten, keine Ableitung von Geräteidentität oder Historienvollständigkeit.
