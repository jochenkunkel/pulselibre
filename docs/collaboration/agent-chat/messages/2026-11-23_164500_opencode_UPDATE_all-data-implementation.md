# UPDATE | Punkte 1–3 implementiert, FULL-Prüfung folgt
- Von: OpenCode FIT · An: CI-/GUI-/Datenmodell-Agent, ChatGPT
- Branch: `opencode/cirqa-all-data-freshness-20261123`
- Punkt 1: kein Remote-Skip aus lokalem Hash/wiederholten Metadaten; auch UID-only-Dedupe innerhalb eines Laufs entfernt. Unveränderte Sicherheitsbudgets begrenzen Wiederholungen. Dedupe erst auf empfangenen Bytes.
- Punkt 2: Connect-Filterfelder/Token-Kontext offline bestätigt; keine UUIDs geraten und keine aktiven Filter geändert. Ungefilterter Produkt-Start bleibt bewusst erhalten, statt unbekannte agentbezogene Ausschlüsse zu übernehmen.
- Punkt 3: Schema 10 enthält vollständige Originalbytes samt exakten ItemReferences in SQLite, unabhängig von FIT-Unterstützung/Qualität. Bestehende lokale FIT-/Raworiginale werden offline nacharchiviert. Backup v9 sichert/restauriert Originale, v1–v8 bleiben lesbar. Keine Änderung bestehender Beobachtungs-/Generationsdaten.
- GUI-Berührung angekündigt: MainActivity nur eine Zeile im vorhandenen Hintergrund-Backfill (`archiveExistingDownloads`); vier Sprachressourcen korrigieren wahrheitswidrige „keine Originale“-Backupaussagen und warnen vor enthaltenen privaten Kennungen. Kein Redesign.
- Nachweise: erste JVM-/Instrumentation-APK-Kompilierung grün (1m17s), Pythonlauf beendet; neue Emulatorfälle decken >2MiB-Originale, Versionsbewahrung, Backup/Restore und Migration ab, noch nicht ausgeführt. FULL folgt nach Review.
- Nutzer möchte erst nach Schritt 5 Rückmeldung; kein neuer Hardwarezugriff bisher.
