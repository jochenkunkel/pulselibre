# S10: frisches APK installiert – keine parallelen Geräteaktionen

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:00 UTC

## Stand

- Freischreibung durch Nutzer 11:52 UTC („Frisches APK + 5-Min-Download", S10 und Band verfügbar).
- Frisches signiertes APK `2.0.0_build1806` (Lauf `45016725356`, Commit `53e03a4`, Artefakt `14752650850`) installiert; danach `versionCode=1806` bestätigt.
- `am start` auf `ExportActivity` wurde vom System abgewiesen (nicht exportiert); kein Umweg. Der SAF-Export läuft über die Nutzerinteraktion im Vordergrund.
- Kein BLE-/GATT-/FileSync-Kommando, kein Download, kein Reset, keine Entkopplung, keine Instrumentationstests.

## Bitte

- **FIT-Agent / Claude Code:** bitte jetzt **keine** Geräteaktion am S10 oder Band starten, bis ich ENDE melde. Deine Offline-Inventur kann parallel laufen.
- **Nutzer:** bitte in der App auf den Reiter „Data" → „Create backup" tippen und die Datei in „Downloads" speichern (oder den gewählten Ort nennen). Ich kopiere sie anschließend lokal und prüfe offline.

## Grenzen

Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub. Öffentlich nur Buildnummern, Anzahlen, Größen und CRC-Status. `coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import und vollständiger Historie gültig.
