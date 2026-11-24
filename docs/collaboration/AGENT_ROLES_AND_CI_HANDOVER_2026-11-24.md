# Verbindliche Agentenrollen und CI→FIT-Übergabe (Owner-Entscheidung 24.11.2026)

**Status: neue verbindliche Rollenverteilung; Übergabe muss noch praktisch bestätigt werden.** Dieses Dokument hat bei Widersprüchen Vorrang vor älteren Rollenbeschreibungen in AGENTS.md, Projektstatus, Onboarding und historischen Agentenmeldungen. Historische Belege nicht umschreiben.

## Aktive Rollen
1. **FIT/OpenCode – technischer Entwicklungs-Owner:** BLE/GATT/GFDI/MLR, Protokollforschung, Full-Pull und Pagination, FIT/Decoder, SQLite-Schema und Migrationen, Import/Backfill, Backup/Restore, technische Tests, Build-/Workflow-Reparaturen und bisherige CI-Implementierungsaufgaben. Eigentümer für offene technische Issues einschließlich #599 und #601. FIT darf CI-Arbeiten nach Übergabe eigenständig fortführen.
2. **GUI – zweiter und einziger weiterer Entwicklungsagent:** Android-GUI, Dashboard, Visualisierung, Analysen, i18n und UI-Tests. Abstimmung über Schnittstellen mit FIT; keine konkurrierenden Änderungen an FIT-owned Dateien ohne Übergabe.
3. **CI – bleibt aktiv, aber nur als Monitor und Berater:** GitHub-Actions-Läufe, Workflow-Ausfälle, PR-/main-Gates, Dependency-/Sicherheitsmeldungen und Trends beobachten; der Projektleitung Befund, Ursache, Dringlichkeit und konkrete Handlungsempfehlung melden. **Keine eigenen Code-/Dokumentations-/Workflow-Implementierungen, keine eigenständigen PRs, Commits, Merges, Reviews als obligatorischer CI-Agenten-Schritt oder Gerätezugriffe.** CI kann evidenzbasierte Review-Hinweise liefern; erforderliche formale unabhängige Reviews werden separat durch geeignete, nicht selbst ändernde Reviewer organisiert. Bestehende Qualitätsgates bleiben bestehen.
4. **Watchdog – Rückbau:** Der Owner hat den Rückbau separat beauftragt. Keine neue Watchdog-Funktion aufbauen oder voraussetzen. Vorhandene Automatisierungen/Services nur durch den zuständigen Rückbauprozess und mit dokumentierter Übergabe abschalten.

## Verbindliche CI→FIT-Übergabe
- CI stoppt **neue Implementierungsarbeiten sofort**. Laufende nicht abgeschlossene Arbeiten, lokale Branches/Worktrees, uncommitted Änderungen, Patches, Skripte, synthetische Fixtures, Testberichte, private Datenarchive, lokale SQLite-/Backup-Dateien und sonstige relevante Artefakte **inventarisieren**, inklusive Pfad, Größe, Stand/Commit, Eigentümer, Vertraulichkeit und benötigtem Zugriff. Keine Geheimnisse oder Gesundheitsrohwerte in Git-Logs.
- Alles, was rechtmäßig und technisch zugänglich ist, FIT übergeben: Code/Tests über einen eigenen Übergabe-Branch oder Patch mit dokumentiertem Commit; private Daten ausschließlich über bereits freigegebene lokale, zugriffsbeschränkte Pfade bzw. kontrollierte Kopien, **nicht** in öffentliche Git-PRs, Agenten-Chat oder CI-Artefakte. Nicht verfügbare lokale Daten als solche kennzeichnen; keine Verfügbarkeit behaupten.
- **Nichts löschen, bereinigen, überschreiben oder Dienste abschalten**, solange FIT nicht die Vollständigkeit, Lesbarkeit und Integrität des übernommenen Bestands bestätigt hat. Vor Kopieren und Öffnen vertraulicher Daten Zugriffsberechtigungen beachten. FIT prüft Hash/Byteanzahl privat, dokumentiert nur datensparsame Übergabenachweise.
- CI veröffentlicht einmalig ein Übergabemanifest mit allen offenen Issues, Branches, PRs, Workflow-Läufen, lokalen Artefaktgruppen, Risiken, Testbefunden, Zugangsvoraussetzungen und konkreten nächsten Schritten; FIT bestätigt Übernahme, benennt fehlende Dateien und legt eine Prioritätenliste an.
- Nach FIT-Abnahme arbeitet CI ausschließlich im **Monitor-Modus**. Monitoring darf fortlaufend erfolgen, aber keine eigenständigen Änderungen oder ungefragten Geräteaktionen auslösen. Empfehlungen gehen an den Owner, technische Befunde an FIT/GUI.
- Offene Tickets/PRs werden nicht allein wegen Rollenwechsel geschlossen; Zuständigkeiten umhängen, bisherige Evidenz behalten. GitHub bleibt Source of Truth, aber lokale private Daten bleiben lokal.

## Reviews, CI und Gerätesicherheit
- FULL/API-35, Dependency Review, notwendige unabhängige Codeprüfung und Post-Merge-Prüfung bleiben Qualitätsanforderungen. **CI-Agent als Person/Prozess ist dafür nicht mehr verpflichtend.** FIT/GUI organisieren eine tatsächlich unabhängige geeignete Prüfung; niemand genehmigt seine eigenen Änderungen. Bei fehlendem Reviewer eskalieren, statt Gates stillschweigend zu umgehen.
- S10/S25-Leases, bestehende explizite Hardwarefreigaben, nicht destruktive Befehle, Datenschutz und Testdaten-Veröffentlichungsregeln bleiben unverändert. Eine Rollenübergabe erteilt keine neue Hardwarefreigabe.
- P0-Ziel unverändert: alle technisch zugänglichen Banddaten lesen, verlustfrei sichern und SQLite-seitig nachvollziehbar speichern; unbekannte Dateiidentität verhindert keinen Full-Pull.

## Abnahme und Meldungen
- **CI:** Übergabemanifest erstellt, FIT hat Artefakte bestätigt, Implementierung gestoppt; danach nur Monitoring und Empfehlungen.
- **FIT:** Übernahmeprotokoll mit übernommenen/fehlenden Artefakten, offenen Tickets und nächstem technischen Schritt.
- **GUI:** UI-Aufgaben bestätigt, Schnittstellen zu FIT benannt, keine CI-Implementierung vorausgesetzt.
- **Projektleitung:** wird über echte Blockaden und wichtige CI-Ereignisse informiert; keine unnötigen Zwischenfreigaben.
