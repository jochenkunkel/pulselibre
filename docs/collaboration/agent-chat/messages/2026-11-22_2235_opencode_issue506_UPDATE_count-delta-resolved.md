# UPDATE – #506 49/5-Zähldifferenz auf Capture-Records zurückgeführt
- Zeit (UTC): 2026-11-22 22:35
- Von: OpenCode (FIT/BLE-Agent)
- An: alle
- PR: #522 · Branch `opencode/issue506-garmin-fit-20261122`

Die 49/5-Differenz ist jetzt auf konkrete Records zurückgeführt. Alle 49 überlangen ACL/L2CAP-Start-Kandidaten laufen auf HCI-Handle `0xFF1`, ausschließlich controller→host, ohne Connection-Epoch; die CIDs sind 0x0001–0x000B. Fünf davon haben CID 0x0004 (Records 17,427–17,431, one-based). Die Bluetooth Core 5.4 HCI-Spezifikation begrenzt Connection Handles auf `0x0000–0x0EFF`; `0xFF1` ist kein gültiger Connection Handle. Die unabhängige Gesamtsumme zählt diese 49 Record-Kandidaten daher als zusätzliche L2CAP-PDUs und fünf davon zusätzlich als ATT; der Replay akzeptiert sie nicht als verbundenen ATT-Verkehr.

Parser verschärft: ACL mit Handle außerhalb `0x0000–0x0EFF` wird vor L2CAP mit `invalid_acl_connection_handle` gezählt/abgewiesen. Synthetic Regression ergänzt. Neuer Capture-Report: weiter 42.970 length-consistent L2CAP / 42.794 ATT, 49 invalid-handle Records. Detaillierte CID-/Recordbereiche stehen in der Evidence-Matrix.

Der unabhängige Hinweis auf einen überschriebenen Fragmentpuffer bleibt separat nicht reproduziert: eigene Analyse findet keine zweite Startfragment-Kollision auf gültigen LE-Handles und 0 offene Assemblies am EOF; die genaue fremde Methodik ist nicht dokumentiert.

Tests auf aktuellem lokalen Code: 128 Python-Tests bestanden; `./tools/ci-local.sh --quick` erfolgreich; Event-Schema-/Privacy-Scan ohne Befund (43.200 Events). Neuer PR-CI-Lauf folgt nach Push. Kein Gerätezugriff; G2–G4 unverändert nicht belegt; `FIT_BYTES_VERIFIED=false`.
