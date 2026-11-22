# S10 – Ergebnis des einmaligen begrenzten Historienlaufs (redaktiert)

Stand: 2026-11-22 · Ergänzung zum [Ausführungsplan und Baseline-Nachweis](CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md)

## Ergebnis

- Getestet wurde das physische Samsung S10 (SM-G970F), nicht ein Emulator. Für den Lauf war
  PulsLibre Build 1806 installiert; das installierte APK stimmte bytegenau mit dem signierten
  CI-Artefakt überein. FULL-CI desselben Quellstands war erfolgreich.
- Der UI-Einmalversuch endete bei der GFDI-Registrierung: `accepted=false`,
  `itemListSent=false`, `pullSent=false`, 0 Dateien. Es wurde keine Item-List angefordert
  und kein Datei-Pull gestartet. Es gab keinen Retry.
- Die SAF-Datenbank-Backups vor und nach dem Lauf sind valide ZIPs, jeweils 2.787 Bytes mit
  15 Datenbankeinträgen. Alle Einträge weisen 0 Datensätze und 0 Nutzdatenbytes auf; im
  Manifest gibt es keinen Zuwachs gegenüber der Baseline.
- Der Versuch belegt somit weder einen Historienimport noch eine vollständige
  Gerätehistorie. `coverage=device-history-unverified` bleibt bestehen.

## Korrektur der UI-Zwischenbefunde

Eine veraltete UI-Hierarchie und ein Tap außerhalb der App-Fläche führten zwischenzeitlich
zur falschen Diagnose, das S10 zeige den Samsung-Keyguard. Die erneute Prüfung bestätigte
das physische Gerät und PulsLibre im Vordergrund; der Keyguard-Befund war falsch. Ein
Instrumentations- oder Root-Umweg wurde nicht verwendet.

## Offline-Einordnung

Der Produktpfad akzeptiert eine GFDI-Registerantwort nur bei Status `0` und dynamischem
Handle `1..127`. Bei Ablehnung wird vor der FileAccess-Item-List gestoppt. Die redigierte
Laufdiagnose enthält nicht den konkreten Status-/Handle-Grund; eine genauere Ursache wird
nicht behauptet. Dieser Codeabgleich ist offline und löst keine weitere Geräteaktion aus.

## Grenzen und Sicherheit

Es wurden keine Band-Schreibaktionen, kein Reset, keine Entkopplung, kein `CLOSE_ALL`,
keine Datenlöschung und keine Instrumentationstests am Gerät ausgeführt. Der genehmigte
Einmalversuch ist ausgeschöpft; ein weiterer Versuch benötigt eine neue konkrete Freigabe.
Es werden keine Roh-BLE-Payloads, Geräteadressen, UIDs oder Gesundheitsdaten veröffentlicht.
