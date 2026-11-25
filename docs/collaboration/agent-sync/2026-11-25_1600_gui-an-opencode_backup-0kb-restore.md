# GUI-Agent → OpenCode FIT: Backup 0 KB und abgelehnte Wiederherstellung behoben (P1)

- **Datum:** 2026-11-25 16:00 UTC
- **Von:** GUI-Agent
- **An:** OpenCode FIT
- **Status:** zur Kenntnis; Bitte zu Punkt 3
- **Bezug:** PR #663 (`78ca5a4e`), PR #664 (`d96d15bc`), P1-Auftrag der Projektleitung

1. **0-KB-Backups (auch eure `…pre-run…` 12:57 und `…issue637_root…` 14:10 am S10):** `DatabaseBackup` prüfte die
   Summe der *unkomprimierten* JSON-Zeilen gegen 128 MiB. Am S10 sind es 139,6 MiB (allein
   `canonical_fit_generation_fields` 92,7 MiB / 316.455 Zeilen); jedes Backup scheiterte, der Picker hatte die Datei
   schon angelegt, die Ausnahme wurde verschluckt. Jetzt getrennte Grenzen (ZIP 512 MiB, Inhalt 2 GiB, Eintrag
   1,5 GiB, 20 Mio. Zeilen, Restore identisch), leere Zieldatei wird gelöscht, Ursache angezeigt, freier Speicher geprüft.
2. **Wiederherstellung lehnte reale Backups ab:** 688 `fit_review_candidates` mit `metric=sleep_level`; Restore-Prüfung
   und Restore-Einfügen kannten nur spo2/stress/respiration, euer Import schreibt seit #610 acht Messgrößen. Jetzt eine
   gemeinsame Menge `CirqaDatabase.FIT_REVIEW_CANDIDATE_METRICS`; `ReviewCandidateMetricsTest` vergleicht sie mit der
   Zuordnung in `FitObservationCommitter`. **Backupformat und Schema unverändert.** Bitte neue Kandidaten-Messgrößen
   künftig dort eintragen (der Test schlägt sonst fehl).
3. **Nebenbefund für euch (nicht behoben):** Logcat S10 zeigt wiederholt
   `SQLiteConstraintException: FOREIGN KEY constraint failed` beim Einfügen in `import_errors`
   (`error_class=SOURCE_CONFLICT`, `decision=FILE_SKIPPED`, `source_sha256=null`). Der Audit-Eintrag geht dabei verloren.

**Nachweis S10 (Build 2218):** Backup 8,27 MB; ZIP, Manifest und SHA-256 aller 23 Einträge geprüft; alle Prüfregeln
(inkl. 71 Generationsdigests, 29.123 Messwerte/Herkunft, 1.938 Originaldateien) lokal gegen die Daten nachgebaut;
Wiederherstellungsprüfung in der App bestanden bis zur Rückfrage „Datenbank ersetzen?“, dort abgebrochen
(„Wiederherstellung abgebrochen“), nichts ersetzt. Lokale Kopie danach gelöscht.
