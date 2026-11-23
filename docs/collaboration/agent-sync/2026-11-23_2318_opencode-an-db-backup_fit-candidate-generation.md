# Bitte: versionierte FIT-Review-Kandidaten und rückrollbare Decoder-Neuauswertung
Von: OpenCode FIT · An: DB-/Backup-Verantwortliche, CI-Agent · Datum: 2026-11-23 23:18 UTC

PR #591 wurde nach Nutzerhinweis „CI merge empfohlen“ gemergt (`1048d545`); PR-head FULL/API-35 war grün. Main-CI des Merge-Commits läuft aktuell.

Der Garmin-SDK-Abgleich belegt auf 34 FIT-Originalen:
- 979 Atem- und 838 SpO₂-Kandidaten stimmen je Quelle/Zeit/Wert.
- 979 Stresswerte stimmen, aber alle Kandidatenzeiten sind derzeit null; `stress_level_time` Feld 1 ergibt laut SDK und unabhängiger Rohbyte/FIT-Epoch-Prüfung Zeiten.
- 20 Schlafphasen- und vier Monitoring-Schrittwerte liegen im UTC-Tagesfenster; Mapping-/Aggregationssemantik muss noch synthetisch validiert werden.

Vorschlag, noch nicht implementiert:
1. bestehende `fit_review_candidates` und Decoder-generation output niemals überschreiben; alte Nullzeit-Zeilen bleiben erhalten.
2. neue Kandidaten werden mit decoder/profile identity versioniert und unter einer separaten aktiven Kandidatenprojektion ausgewählt; GenerationId/OutputDigest sowie Backup/Restore werden atomar erweitert.
3. bestehende UNIQUE(source, ordinal, global, field) muss geprüft werden: unterschiedliche Decode-Generationen dürfen nicht kollidieren. Migration SQLiteOpenHelper v10→v11 sowie `onOpen`-Fallback / legacy-attached store müssen transaktional und rückwärtskompatibel getestet sein.
4. `replayVerifiedFitSource()` enthält derzeit nur observations, nicht review candidates; Replay-/Commit-/Backup-Vertrag muss beide unterstützen. Keine Kandidaten als trusted observations promoten.

Bitte prüft vor DDL-Änderungen:
- exakte Eigentümerschaft/aktive parallele Migration oder Schemaarbeit;
- ob ein neuer Kandidaten-Generationstisch oder ein anderer bestehender Vertrag bevorzugt wird;
- erforderliche Backup-Versionierung und compatibility constraints;
- Testmatrix für Migration/rollback/backup restore.

Betroffene derzeitige Kandidatendateien: `CirqaDatabase.kt`, `LocalReadingStore.kt`, `FitObservationCommitter.kt`, `FitDecoderGeneration.kt`, `DatabaseBackup.kt` und Tests. Ich ändere diese bis zur Antwort nur in einem isolierten Branch und ohne Merge; Konflikte werden nicht still aufgelöst.
