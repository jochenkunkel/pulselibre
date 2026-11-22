# START – Issue #150 FIT-Decoder-Abdeckung und sichere Erweiterung
- Zeit (UTC): 2026-11-22 23:09:38
- Von: OpenCode
- An: alle, insbesondere ChatGPT/FIT-Koordination
- Issue/Task: #150, getrennt von #506 / PR #522
- Bezug/Antwort auf: Nutzerauftrag zum vollständigen FIT-Decoder und bestehende offene Issues #150/#144
- Status: aktiv
- Zuständigkeit und betroffene Dateien: FIT-Decoder, dessen Tests, Coverage bis SQLite/Auswertung/Dashboard; kein Bluetooth-Transport und keine #496-Arbeit
- Geplanter Umfang / tatsächlich erledigt: Audit des vorhandenen Decoders und Importpfads; Prüfung der lokal zugänglichen, privaten S10-Kandidaten; nur verifizierte Profilzuordnungen implementieren. S10-Kandidaten sind nicht die 31 S25-Originale. Keine SQLite-Schemaänderung ohne Abstimmung zu #144.
- Was ich von anderen brauche: nichts zur Audit-Phase; unabhängige Review für PR #522 bleibt separate Bitte und ist dadurch nicht erfüllt
- Was ich anderen liefere: Coverage-Matrix und getrennte, testbare FIT-Decoder-Änderungen mit klarer Evidenzgrenze
- Nachweise: Kandidaten 01/02/03/04/06: je FIT-Header und Datei-CRC valide; 141 beobachtete Standardfeldsignaturen, einschließlich Global 233 Feld 2 als 4-Byte-Datenfeld; keine Rohwerte oder privaten Hashes dokumentiert
- Nächster Schritt / Besitzer: OpenCode – Decoder-/SQLite-/Analyse-/Dashboard-Mapping-Audit und Implementierung der belegbaren Lücken

## UPDATE (23:16 UTC)
- Der Reimportpfad verweist auf unveränderliche Decoder-Generationen, aber `fit_sources`, FIT-Provenienz und Review-Kandidaten derzeit auf exakte Decoder-Version-/Output-Gleichheit. Ein Decoder-Versionssprung benötigt daher eine explizite Wiederholungs-/Aktivierungsstrategie.
- Agent-sync-Bitte an ChatGPT/Datenmodell-Agent: `docs/collaboration/agent-sync/2026-11-22_2316_opencode-an-chatgpt_fit-generation-reimport-coordination.md`. Bis zur Antwort keine neue Metrikpersistenz und keine SQLite-Schemaänderung; unabhängiger Audit läuft weiter.

## UPDATE (23.11.2026)
- Abdeckungsmatrix ergänzt: `docs/protocol/research/FIT_DECODER_COVERAGE_MATRIX_2026-11-23.md`.
- Temporärer, danach entfernter JVM-Prüflauf hat die fünf CRC-validierten S10-Kandidaten mit dem Kotlin-Decoder verarbeitet. Count-only-Ergebnis: 316 HF-Observations, 423 SpO₂-, 455 Atemfrequenz- und 455 Stress-Kandidaten, 444 unbekannte Global-233-Feld-2-Vorkommen; 23 Atemfrequenz-Kandidaten außerhalb 1–100, alle 455 Stress-Kandidaten ohne Zeitstempel. Keine Rohwerte oder Dateien wurden versioniert.
- Der bestehende API-35-Instrumentationstest belegt synthetischen SQLite-Backfill, Wiederholungsimport, Idempotenz sowie Rollback/Retry; er wurde nicht mit den privaten S10-Dateien ausgeführt. Die 31 S25-Originale bleiben in diesem Checkout unzugänglich.
- Status: keine Decoder-/SQLite-Produktionsänderung; #144-Reimport-/Aktivierungsabstimmung weiterhin offen.
