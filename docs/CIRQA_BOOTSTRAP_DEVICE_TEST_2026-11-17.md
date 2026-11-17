# CIRQA Bootstrap – Gerätetest nach Build 48763f8 (2026-11-17)

Quelle: vom Tester geteilter Phase-1-Historien-Diagnosebericht; nur nicht-personenbezogene Metadaten.

- Bootstrap-ACK-Kandidat gesendet; GATT-Schreibvorgang bestätigt. **Keine Bestätigung auf GFDI-Anwendungsebene nachgewiesen.**
- GFDI-Registrierung bestätigt; 5 gültige GFDI-Frames, 0 ungültige.
- Empfangsreihenfolge: 5024 ×1, 5050 ×3, 5011 ×1.
- Die drei 5050-Nachrichten sind payload-identisch (19 Byte Body); Zeitabstände 4968 ms und 5039 ms; 5s-Retry-Kandidaten=2, Payloadwechsel=0.
- 5011: 21 Byte Body.
- Keine FileListResponse, keine Dateieinträge; Dateiliste im Test **nicht angefordert**; keine Dateibefehle.
- Beobachtungsfenster beendet; Handshake-Zustand POST_RETRY_5011_SEEN.

## Interpretation und nächste Entwicklung

Die Wiederholungen stützen die Timeout-/Retransmission-Hypothese, beweisen aber **weder** ein ACK-Format für 5050 **noch** die korrekte Semantik des zuvor gesendeten 5024-ACK-Kandidaten. GATT write success ist kein GFDI application ACK. Nächster Schritt: TX/RX-Korrelation und genaue Struktur der 5024-Antwort offline gegen verifizierte Referenz prüfen; danach ggf. nur mit ausdrücklich als experimentell gekennzeichnetem, begrenztem Gerätetest fortfahren. Keine blind konstruierten 5050-ACKs. Keine weiteren HCI-SysDump-Wiederholungen ohne neue Zugriffsmethode.
