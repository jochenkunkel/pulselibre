# START – #637 SQLite BUSY/LOCKED Retry-Lücke
- Zeit (UTC): 2026-11-26 06:07
- Von: OpenCode FIT
- An: Projektleitung, FIT/GUI
- Issue/Task: #637, gezielter Follow-up zum Offline-Audit
- Bezug/Antwort auf: PR #682 / Issue-Auditbefund vom 26.11.
- Status: aktiv
- Zuständigkeit und betroffene Dateien: `CirqaSqliteBusyRetry.kt`, fokussierte Retry-Unit-Tests
- Geplanter Umfang: Beide tatsächlichen Android-Ausnahmen für SQLITE_BUSY/LOCKED (`SQLiteDatabaseLockedException`, `SQLiteTableLockedException`) sollen vom bestehenden begrenzten Retry-Default behandelt und getestet werden. Retry-Zeiten/-Anzahl bleiben unverändert.
- Sicherheitsgrenzen: Offline-Code/Tests; kein ADB, kein S10/S25, keine BLE-/Bandaktion; keine Laufbudgetänderung.
- Was ich von anderen brauche: nichts
- Was ich anderen liefere: Codekorrektur und Testnachweis im separaten PR
- Nachweise: Aktueller Main `229c8af9`; eigener Branch basiert auf aktuellem `origin/main`.
- Nächster Schritt / Besitzer: OpenCode – Tests ergänzen, fokussiert ausführen und PR-Gates dokumentieren.

## UPDATE (2026-11-26 07:25 UTC)
- Korrekturklassifikation bestätigt: Android JNI mappt SQLITE_BUSY → `SQLiteDatabaseLockedException`, SQLITE_LOCKED → `SQLiteTableLockedException`; keine Android-Klasse `SQLiteBusyException`.
- Implementation/test fertig: beide realen Klassen werden durch Default-Policy begrenzt wiederholt; generisches `SQLiteException` nicht. Delays/Call-Sites unverändert.
- Nachweise: fokussierte Unit-Tests grün; lokaler QUICK grün, danach finaler Negativtest hinzugefügt und erneut fokussiert grün. PR #683 `f054f8ac`: erforderlicher Build/Unit/Lint/APK sowie Dependency Review/Graph grün; FULL-Emulator im QUICK übersprungen.
- Self-Review: `git diff --check` sauber, Änderungen eng auf Utility/Test beschränkt; keine Geräteaktion. GitHub-PR meldete `CLEAN`/`MERGEABLE`.
- Nächster Schritt: abschließenden ENDE-Eintrag hinzufügen und Required-QUICK auf dann finalem Head abwarten, danach regulär mergen.
