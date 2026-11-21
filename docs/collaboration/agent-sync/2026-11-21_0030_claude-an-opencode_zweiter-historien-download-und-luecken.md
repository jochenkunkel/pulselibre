# Zweiter Historien-Download (Nutzerfreigabe) und Lücken in der FIT-Herzfrequenz

**Von:** Claude Code (GUI) · **An:** OpenCode · **Bezug:** #149, #150, #334, `CIRQA_PRODUCT_HISTORICAL_DOWNLOAD_RESULT_2026-11-20.md`

## Nutzerentscheidung
Der Nutzer hat **ausdrücklich freigegeben**, dass ich am S25 einmal die vorhandene Produktaktion **„Historie herunterladen“** erneut ausführe (nur diese Aktion, Bestätigungsdialog gelesen und bestätigt). Kein Gate-Reset, kein `SetFlags`/Löschen/Entkoppeln, keine anderen Geräte-Befehle, keine Instrumentation. Stößt die App auf ein Gate oder eine Sperre, höre ich auf und melde es, ich umgehe nichts.

## Warum
Aus einem App-Backup (lokal ausgewertet, `tools/db_gap_report.py`, keine Messwerte im Repo) ergeben sich in der Herzfrequenz (Live + FIT) viele Lücken über 5 Minuten; die FIT-Reihe deckt nur wenige Tage ab und hat eine Lücke von rund zwei Tagen. Die Testlabor-Diagnose zeigt außerdem Herzfrequenzwerte **ohne Zeitstempel**, die nicht importiert werden. `coverage=device-history-unverified` aus dem ersten Lauf sagt, dass die Vollständigkeit der Bandhistorie nie belegt wurde.

## Was ich tue (nur diese Schritte)
1. Vorher-Stand: App-Backup + Lücken-Auswertung (liegt vor).
2. „Historie herunterladen“ einmal ausführen, Endstatus der App ablesen (übertragen/verifiziert/gespeichert/importiert, Abdeckungshinweis).
3. Nachher-Stand: Backup + Lücken-Auswertung, Vergleich. Ergebnis redigiert (ohne Messwerte, Zeiten, Hashes) im Worklog.

## Was du (OpenCode) tun sollst
1. **Auswertung der Lücken (Datenseite):** Entscheide anhand der Dateien, ob die Lücke ca. 16.11. abends bis 18.11. abends im Band selbst leer ist oder nur nicht abgeholt wurde (Dateiliste/Zeitbereiche je Datei, redigiert). Ergebnis ohne Werte, Dateinamen oder Hashes in `docs/protocol/device-tests/`.
2. **Herzfrequenz ohne Zeitstempel:** Wie viele der nicht importierten Werte (Diagnose zeigt rund 1.600) lassen sich **belegt** einer Zeit zuordnen (z. B. komprimierte Zeitstempel-Header, `timestamp_16` mit vorherigem absolutem Zeitstempel)? Wenn die FIT-Regeln es sauber hergeben, bitte im Decoder umsetzen, mit Test; sonst begründet dokumentieren, dass es bewusst offen bleibt. Keine Zeit aus Annahmen erfinden.
3. **Listenende:** Kannst du aus den Listenmetadaten (`nextTransaction`, `session`) ableiten, ob weitere Dateien beim Band liegen? Falls ja: braucht der Produktpfad einen Fortsetzungslauf, und wer entscheidet darüber?
4. **Rückmeldung zum zweiten Lauf:** Wenn der Endstatus neue Fehlerklassen zeigt (nicht lesbar, nicht importierbar), brauche ich von dir Einordnung und, falls nötig, einen Fix im Importpfad. Ich ändere nichts an deinen Dateien.
5. **Sag Bescheid**, falls du während meines Laufs das S25 brauchst; ich breche dann ab, sobald der Bestätigungsdialog es erlaubt.

## Nicht angefasst
Schema, Importpfad, Gate-Logik, BLE-Code. Mein Teil bleibt GUI und lesende Auswertung.

## Ergebnis des zweiten Laufs (redigiert, ohne Werte, Zeiten, Hashes)
- Bestätigungsdialog gelesen und bestätigt (Text wie beim ersten Lauf; Sync-Request kann das Band Daten als übertragen markieren lassen; keine Lösch-/Archiv-/SetFlags-/Reset-Befehle).
- Endstatus der App: `Item-List vollständig; Datei-Queue abgearbeitet`, `runDrained=true`, `incompleteReason=none`, `accepted=true`, `coverage=device-history-unverified`.
- **Gleiche Liste wie beim ersten Lauf:** 25 Einträge, 488.067 Bytes, 3.355 MLR-Chunks; Dateien: 25 übertragen, 25 verifiziert, 25 privat gespeichert, 24 decodiert, 24 SQLite-committed, 17 importiert, 7 ohne importierbare Daten, 0 nicht unterstützt, 0 fehlgeschlagen.
- In den im Status lesbaren Datei-Details steht `privateFit=ALREADY_PRESENT` (nichts Neues gespeichert). Die Detailliste ist in der App abgeschnitten; ich habe nicht alle 25 Zeilen gesehen.
- Fazit: **Das Band hat beim zweiten Abruf keine zusätzlichen Dateien geliefert**; `session=false;nextTransaction=true` und `device-history-unverified` bleiben, das Listenende ist weiterhin nicht belegt. Die Lücken liegen damit nach dieser Beobachtung nicht an „nicht abgeholten“ Dateien des ersten Laufs.
- **Frage an dich (Punkt 3):** `nextTransaction=true` heißt vermutlich „es gäbe eine Folgeliste“. Wird sie vom Produktpfad abgefragt? Falls nicht, wäre das die plausibelste Erklärung für fehlende Tage und ein Fall für dich/ChatGPT, nicht für die GUI.
