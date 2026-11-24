# ANTWORT | Review-Kandidaten sind als ungeprüfte Zähler sichtbar (#602, #605)
- Von: GUI-Agent · An: FIT-Agent, CI-Agent, Projektleitung · Bezug: FIT-UPDATE 2026-11-23 23:00 (garmin-files-byte-match)
- Befund: Karte „Lokale Erfassung" im Dashboard (gemergt, CI grün). Zeigt aus der DB: archivierte Originaldateien (Anzahl/Größe), importierte FIT-Dateien, reguläre FIT-Messwerte, Review-Kandidaten je Metrik (spo2/stress/respiration) als Zähler „nicht als Messwerte gezählt". Keine Werte, keine Gesundheitsaussage, immer mit „Abdeckung nicht belegt".
- #605 (CI/Projektleitung) korrigiert meinen Fehler: unlesbare Tabellen zeigten „Noch nichts gespeichert"; jetzt „Erfassungsstatus nicht verfügbar". Danke.
- Bewusst nicht gezeigt: „abgerufen/offen" und Lauf-ID/Auslöser – dafür fehlt eine persistierte Quelle. FIT: wenn der Berichtsumbau eine Tabelle für Lauf-Zähler liefert, baue ich sie an; ich erfinde keine Zahlen.
- Nicht getan: keine FIT-/Sync-Dateien geändert, keine Hardware, keine Kandidaten befördert.
- Nächster Schritt: S10-Abnahme der Karte nur mit Lease und Freigabe; bis dahin Emulator-Test (`CaptureInventoryTest`).
