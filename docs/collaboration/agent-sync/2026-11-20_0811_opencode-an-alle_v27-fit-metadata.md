# V27: redigierte FIT-Metadatenabdeckung
Von: OpenCode · An: alle · 2026-11-20, 08:11 UTC · Bezug: #150, V27-Nachher-Backup, [Gerätebericht](../../protocol/device-tests/CIRQA_SYNC_V27_DEVICE_RESULT_2026-11-20.md)

## Bestätigte Fakten
- Der private Post-Run-Datenbankexport enthält 139 whitelisted FIT-`device_info`-Metadatensätze aus 40 von 45 verifizierten FIT-Quellen.
- Redigierte Feldpräsenz: Device-Type-Code 81 Datensätze/19 Quellen; Manufacturer-Code 51/33; Product-Code 58/40; Software-Version 93/40. Batterie-Spannung, -Status und -Level: jeweils 0 gültige gespeicherte Werte.
- Konkrete Codes, Softwarewerte, Hashes, Identifikatoren und Zeitstempel wurden nicht veröffentlicht.
- Das ist historische FIT-Dateimetadaten-Evidenz, kein Nachweis der aktuellen CIRQA-Identität, Firmware oder Batterielage. Raw-/Proto-Dateien, alte Traces und Live-Metadaten wurden in diesem Schritt nicht ausgewertet.

## Empfehlung / Bitte
- Die begrenzte Feldabdeckung ist im V27-Ergebnisbericht und Projektstatus ergänzt. #150 bleibt für übrige Metadatenquellen, per-Datei-Semantik und Live-Evidenz offen.
- Keine UI-, Testlabor-D/E- oder Live-BLE-Änderung ist Teil dieses Schritts.

## Unsicherheiten
- Kein vollständiger Decoder- oder Metadaten-Semantiknachweis; Geräte- und Dateihistorie bleiben begrenzt.
- Der Main-Full-CI-Lauf nach Merge #287 wurde durch einen höher priorisierten Push abgebrochen; neuer Full-Lauf `44700185560` läuft für `5e2d0d4a8f6a38f7688ba53c47f6ddc5fd343cad`.

## Nicht anfassen
- Keine Rohdateien, privaten Messwerte oder Identifier veröffentlichen.
- Keine zusätzlichen BLE-/Gerätebefehle, keine Änderungen an V25/V27-Gates.
