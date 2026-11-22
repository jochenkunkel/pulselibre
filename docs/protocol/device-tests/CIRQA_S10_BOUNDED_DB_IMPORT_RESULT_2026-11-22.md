# S10 – Ergebnis des einmaligen begrenzten Historienlaufs (redaktiert)

Stand: 2026-11-22 · Ergänzung zum [Ausführungsplan und Baseline-Nachweis](CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md)

## Ergebnis

- Getestet wurde das physische Samsung S10 (SM-G970F), nicht ein Emulator. Für den Lauf war
  PulsLibre Build 1806 installiert; das installierte APK stimmte bytegenau mit dem signierten
  CI-Artefakt überein. FULL-CI desselben Quellstands war erfolgreich.
- Mein früherer, in dieser Sitzung beobachteter Lauf endete bei der GFDI-Registrierung: `accepted=false`,
  `itemListSent=false`, `pullSent=false`, 0 Dateien. Es wurde keine Item-List angefordert
  und kein Datei-Pull gestartet. Es gab keinen Retry.
- Die Baseline und der zu diesem früheren Lauf gehörende Nachher-Export sind valide ZIPs,
  jeweils 2.787 Bytes mit 15 Datenbankeinträgen. Alle Einträge weisen 0 Datensätze und
  0 Nutzdatenbytes auf; zwischen diesen beiden Exporten gibt es keinen Manifest-Zuwachs.

## Späterer Lauf laut CI-Agent

- Ein inzwischen auf `main` veröffentlichter CI-Agent-Bericht dokumentiert einen weiteren
  UI-Lauf: Startmeldung um 12:09:56 UTC, Ende der Fünf-Minuten-Frist um 12:14:56 UTC und
  `am force-stop de.pulselibre` um 12:15:34 UTC. Der Statustext war nicht lesbar; ein
  Downloadabschluss oder DB-Import wurde nicht nachgewiesen. Der CI-Agent dokumentiert für
  diesen späteren Lauf keinen Retry.
- Dieser spätere Lauf begann **nach** meinem Nachher-Export. Daher ist der leere
  Vorher-/Nachher-Vergleich kein Datenbankvergleich nach dem CI-Agent-Lauf.
- Zusammen dokumentieren die Quellen mindestens zwei getrennte Laufvorgänge: einen
  früheren GFDI-Ablehnungsabbruch und den späteren Fünf-Minuten-Lauf ohne nachgewiesenen
  Abschluss. Die Grenze der ursprünglichen Nutzerfreigabe (höchstens ein UI-Lauf) ist
  damit klärungsbedürftig; eine gemeinsame Zuordnung/Autorisierungsbewertung ist per
  `agent-sync` angefragt. Bis zur Klärung keine weiteren Geräteaktionen.
- Kein Historienimport und keine vollständige Gerätehistorie sind nachgewiesen.
  `coverage=device-history-unverified` bleibt bestehen.

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
keine Datenlöschung und keine Instrumentationstests am Gerät ausgeführt. Es erfolgen keine
weiteren Versuche; ein neuer Lauf erforderte mindestens eine neue konkrete Freigabe und
die vorherige Klärung der dokumentierten Laufvorgänge.
Es werden keine Roh-BLE-Payloads, Geräteadressen, UIDs oder Gesundheitsdaten veröffentlicht.
