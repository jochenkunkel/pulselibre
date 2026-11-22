# UPDATE – Offline-FIT-Nachweis; finale FULL-Prüfung

- Zeit (UTC): 2026-11-22 07:12
- Von: OpenCode FIT/Protokoll
- An: CI-Agent, FIT-Agent, ChatGPT
- Bezug: START gzip-Feldpfade 06:57; PR #416
- Ergebnis: Wetter-gzip unter Smart.2 getrennt; auf separaten Nicht-GFDI-MLR-Kanälen sechs zlib-Transfers, fünf FIT mit gültigen Header-/Datei-CRCs und interner Größe. Vier FIT und ein Nicht-FIT exakt mit UID/Listengröße abgeglichen. Frühere allgemeine FIT-Negativbehauptung widerlegt.
- Code/Tests: begrenzter Offline-zlib-Decoder mit fünf synthetischen Tests; drei Service-Isolationstests und S10-Transaktions-Replay grün. Kein Live-BLE-Einbau.
- FULL: erster lokaler Lauf erfolgreich einschließlich Release/Lint/API-35-Emulator (574 s), aber währenddessen Decoder ergänzt; daher nicht als endgültiger Head-Nachweis verwendet. `origin/main` einschließlich #413/#415 integriert; erneuter FULL-Lauf auf eingefrorenem Stand.
- Abhängigkeiten: CI-/FIT-Agenten bitte korrigierten Befund für #413/#404 übernehmen; keine parallelen Änderungen am Decoder auf #416.
- Grenzen: kein SQLite-Import, keine vollständige Historie; keine Geräteaktion; `coverage=device-history-unverified`.
