# START | CI-Agent: gerätebezogene Lease und Konkurrenztests

- Zeit (UTC): 2026-11-23 07:03
- Von: CI-Agent (OpenCode)
- An: OpenCode FIT-Integrationsverantwortlicher, GUI-Agent; zur Kenntnis Nutzer
- Bezug: verbindliche Device-Lease-Entscheidung 07:50 und S10/CIRQA-Testfreigabe 08:00
- Auftrag: S10/S25-Lock technisch gerätebezogen erweitern; Konkurrenz-, Überlauf-/ETA-, Cleanup- und Kompatibilitätstests ergänzen; Hardwarezugriffsstellen auf Wrapper prüfen.
- Basis: beim Task-Start beobachtet `cf5fc978`; während der Worktree-Erstellung wurde #542 gemergt und `main` rückte auf `1fdff153` vor. Isolierter Branch basiert auf diesem Stand (enthält `cf5fc978`).
- Koordination: S10/S25-Lease zeigte `BELEGT` durch GUI-Agent. Ich führte zunächst keinen Gerätezugriff aus und arbeitete offline.
- Betroffene Dateien geplant: `tools/s25_lease.py`, Python-Unit-Tests, Lease-Dokumentation und Agenten-Übergabe; keine App-/FIT-Änderung.
- Sicherheit: atomarer `flock` bleibt verbindlich; ETA ist kein TTL; keine Präemption; keine Seriennummern in Git. 30-Minuten-Test muss Test+Vor-/Nachlauf komplett umfassen.
- Nächster Schritt: gerätebezogene CLI und Offline-Regressionen implementieren; vor dem GUI-Gerätecheck Status/Freigabe abgleichen.
