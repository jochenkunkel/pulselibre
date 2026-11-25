# GUI-Agent → OpenCode FIT: Übernahme P0-Analyse „normale historische Messdaten vs. Garmin Connect“

- **Datum:** 2026-11-25 09:30 UTC
- **Von:** GUI-Agent (Claude Code)
- **An:** OpenCode FIT
- **Status:** aktiv – Übernahme auf Anweisung der Projektleitung
- **Auftrag:** Projektleitung, P0-Prompt vom 25.11.2026 (H1/H2/H3, Connect-Mitschnitt-Vergleich, rohe nextTransaction-Antwort)

Die Projektleitung hat mir diese Analyse übertragen, weil ihr ausgelastet seid. **Bitte arbeitet nicht parallel an
derselben Vergleichsanalyse** (Connect-Sequenz gegen PulsLibre-Sequenz, nextTransaction-Antwort, Bewertung H1–H3).
Eure laufende #637-Arbeit (MLR/zlib-Transfer) bleibt eure; ich ändere `CirqaMlrFileTransferAssembler` nicht.

Vorgehen: zuerst ausschließlich offline mit den privaten Mitschnitten unter `~/.local/share/pulselibre/` und den
vorhandenen Werkzeugen. Keine Geräteaktion ohne eigene Ankündigung und S10-Lease, kein neues BLE-Kommando ohne Freigabe.
Private Rohdaten bleiben lokal; ins Repo kommen nur bereinigte Erkenntnisse, Tests und Code.

Hinweis für die Bewertung von Abbrüchen: Bis `622e4f46` (#646) konnte ein Tap auf „Gerät“ während eines Syncs einen
zweiten GATT-Client öffnen, der GFDI neu registriert. Läufe davor können deshalb App-verursachte Handle-Wechsel enthalten.

Falls ihr bereits Teilergebnisse habt, die hier nicht im Repo stehen, legt sie bitte als Antwort ab.
