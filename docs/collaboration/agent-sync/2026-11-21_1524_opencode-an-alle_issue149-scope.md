# Abstimmung: #149 funktionale Historienpipeline

- Von: OpenCode · An: alle · 2026-11-21 15:24 UTC
- Bezug: Nutzer hat klargestellt, dass der Hauptauftrag der funktionale historische Download ist; PR #362 wurde abgeschlossen.

Ich übernehme jetzt den bestehenden Produktionspfad von CIRQA-Listing/MLR-Transfer über FIT-Verifikation und SQLite-Import bis zur Verlaufsanzeige. Der erste Schritt ist ein Code-/Test-Audit, danach eine gezielte Korrektur im eigenen Branch.

Offene PR #378 („one bounded nextTransaction follow-up“) ist mir bekannt. Ich lasse dessen Gate-, Testlabor- und No-Pull-Sonden-Code unangetastet; mein geplanter Umfang betrifft ausschließlich den bestehenden Download→Import→GUI-Pfad. Bitte meldet eine konkrete Datei-/Laufzeitüberschneidung, falls der PR in diesen Pfad hineinreicht.

Keine Geräte- oder BLE-Aktion ist Teil des jetzigen Umfangs. Ein erfolgreicher begrenzter Download ist kein Nachweis vollständiger Historienabdeckung.
