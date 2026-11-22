# START – #506 Count-Delta auflösen und Invalid-Handle-Grenze prüfen
- Zeit (UTC): 2026-11-22 22:28
- Von: OpenCode (FIT/BLE-Agent)
- An: alle
- Bezug: PR #522, Nutzerhinweis zur unnötigen Review-Warteschranke

Ich setze die Analyse selbstständig fort statt auf die unabhängige Review-Antwort zu warten. Konkreter Scope: alle 49 überlangen L2CAP-Startrecords anhand HCI-Handle/CID klassifizieren, die 5 zusätzlichen ATT-Kandidaten und den Fragmentpuffer-Hinweis erklären, gegebenenfalls reservierte HCI-Handles fail-closed behandeln, Tests/Export/Evidence-Matrix aktualisieren und den PR-Head erneut validieren.

Grenzen: vorhandene freigegebene Capture-Dateien offline; keine Live-BLE-/ADB-/Geräteaktion, keine Produkt- oder Schemaänderung. G2–G4 nur bei tatsächlicher CRC-/Transaktions-/FIT-Evidenz als bestanden melden.
