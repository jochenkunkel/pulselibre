# Bitte: #707 unabhängigen Garmin-FIT-SDK-Vergleich ausführen
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 18:30 UTC
Bezug: Nutzerauftrag #707 zuerst, danach verbleibende P0s; Standardfeld-PR #708 ist bereits gemergt.

Gemäß Owner-Wunsch delegiere ich Testausführung an GUI. Bitte als **rein offline/read-only** Test — keine Installation und kein S10/S25-Zugriff — einen unabhängigen Vergleich mit einer festgehaltenen Version des offiziellen Garmin FIT SDK/FitCSVTool ausführen, soweit das SDK verfügbar ist.

### Umfang
- Nutze ausschließlich bereits lokal vorhandene und zugängliche Test-/FIT-Dateien; private Originale nicht umkopieren oder committen. In dieser Umgebung wurden in einem privaten Backup lokal 32 FIT-Dateien gefunden; falls du darauf keinen bereits autorisierten Zugriff hast, arbeite mit den synthetischen Fixtures aus den bestehenden Tests und melde die Originaldatei-Prüfung `NOT_AVAILABLE`.
- Vergleiche für die verfügbaren Originale dekodierte **Feldnamen, Basistypen, Werte-/Invalid-/Missing-Klassifikation und Zeitstempel** mindestens für Monitoring 55/24, 55/26, 55/27, SpO₂ 269/0–2, respiration 297/0 und 398/1–2 gegen PulsLibre `FitHistoricalObservations`.
- Proprietäre 233/2 und 397/1 separat als unbekannt/roh behandeln; kein Profilmapping erfinden.
- Synthetische Testvektoren aus #707 gegen die offizielle Referenz dekodieren, besonders `0xA8`, `0x46`, `0xFF`, timestamp16 `0xA6D4→0xA710`, HR `0/255/absent`, 297 signed values, 269 confidence `0` und Invalid `255`.

### Rückmeldung
Bitte angeben: SDK-Version/Hash oder genaue Release-ID; getestete Dateianzahl und Nachrichten-/Feldanzahlen nur aggregiert; Vergleichszähler (match/mismatch/not comparable), Grundklassen und tatsächlich ausgeführte Befehle/Tests. Keine privaten Werte, Zeitreihen, Hashes, Namen oder Payloads in GitHub/Issue kopieren. Falls das offizielle SDK nicht erhältlich ist, nicht durch Community-Code als Garmin-Primärreferenz ersetzen; sauber `BLOCKED/NOT_AVAILABLE` melden.

Diese Bitte ist nur für Validierung. Keine Produktdatei ändern, keine Geräteaktion. OpenCode prüft parallel statisch den gemergten Implementierungsumfang und konsolidiert die Forschungsrestpunkte in #707.
