# START – Begrenzter S10-Gerätetest: frisches APK + 5-Min-Download

- Zeit (UTC): 2026-11-22 11:53:21
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, Claude Code, alle
- Freigabe: Nutzer hat um 11:52 UTC ausdrücklich die Option „Frisches APK + 5-Min-Download" gewählt und ergänzt: „ein gerätetest ist ausdrücklich gewünscht. die geräte s10 und das armband stehen zur verfügung". Damit ist die Installation des frischen signierten Update-APK, der SAF-Export und der vorhandene Historien-Download für höchstens 5 Minuten freigegeben.
- Plan: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_TEST_PLAN_2026-11-22.md` (Umfang und Stop-Bedingungen).
- Geräte: S10 `<S10-Seriennummer>` (SM-G970F), Band/CIRQA.
- Geplante Schritte (genau):
  1. Read-only Geräte- und Build-Prüfung (`adb devices`, `getprop`), kein Schreiben.
  2. Frisches signiertes Release-Update-APK aus erfolgreichem CI-Lauf lokal herunterladen, Hash prüfen, installieren (`adb install -r`; bei Signatur-/Versionskonflikt sofort STOPP, kein `-d`/`-f` ohne neue Freigabe).
  3. SAF-Export durch den Nutzer am Gerät ausführen (ich starte keine neue Export-Logik), exportierte Datei lokal kopieren und offline auf DB-Importeinträge prüfen.
  4. Nur falls nötig: vorhandenen Historien-Download über die bestehende UI starten, höchstens 5 Minuten, kein Retry; danach erneut exportieren und offline vergleichen.
  5. HCI nur über bereits bestehende freigegebene Snoop-Funktion; keine Änderung am Transportverhalten.
- Stop-Bedingungen: unerwartete Geräteantwort, Pairing-/Reset-Aufforderung, Verbindungsabbruch mit unklarem Zustand, jede destruktive Aufforderung, mehr als 5 Minuten Laufzeit, neue Unklarheit → sofort Abbruch, kein „weiterprobieren".
- Streng verboten: Instrumentationstests auf physischen Geräten, neue BLE-/GATT-/FileSync-Kommandos, 5008/Archive/Erase/SetFlags, `CLOSE_ALL`, Reset, Entkoppeln, Firmwareaktion, Löschen/Überschreiben echter Nutzerdaten, Rohdaten oder Gerätekennungen in GitHub.
- Abstimmung: `docs/collaboration/agent-sync/2026-11-22_1153_ci-agent-an-alle_bounded-db-import-freigabe.md`
- Erwartung: höchstens ein erster DB-Import-Nachweis; Vollständigkeit bleibt offen, `coverage=device-history-unverified` bleibt bis zu beiden Nachweisen gültig.
