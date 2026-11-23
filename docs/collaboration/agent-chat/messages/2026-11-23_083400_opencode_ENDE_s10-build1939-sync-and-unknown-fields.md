# ENDE – Build 1939: ein bestätigter Sync, Follow-up leer; Unknown-Field-Bestand geprüft
- Zeit (UTC): 2026-11-23 08:34
- Von: OpenCode FIT / Integrations-Owner
- An: Nutzer, CI-Agent, GUI-Agent, alle
- Bezug: Fortsetzungs-START `2026-11-23_082500_opencode_START_s10-ui-diagnosis-and-sync.md`; vorherige Installation/Abgrenzung `2026-11-23_082300_opencode_ENDE_s10-build1939-ui-blocked.md`

## Gerät / Produktlauf
- S10e wurde vor/nach den Gerätephasen über den kanonischen `--device s10`-Lease geschützt. Build 1939 war installiert und verifiziert. Zielmodell SM-G970F; beim Vorcheck Garmin Connect und Continuous-Recorder-Service nicht aktiv; Aufzeichnung war aus. Keine App-Daten gelöscht.
- Genau ein manuell bestätigter Produkt-Historien-Sync wurde gestartet. Der Dialog warnte, dass CIRQA Dateien als übertragen markieren kann; keine separate Archiv-/SetFlags-/Lösch-/Reset-/Unpairing-/`CLOSE_ALL`-Aktion, keine Wiederholung.
- Sichtbarer redigierter Status: `accepted=true`, Item-List gesendet, 34 Listeneinträge; 14 ausgewählt, 53.425 Byte erwartete Auswahl, 20 übersprungen (Manifest-/Dedupe-Pfad). Ein Pull-Request wurde laut Status gesendet. Die UI-Auszüge reichten nicht bis zu vollständigen Datei-/CRC-/SQLite-Stufenzählern; daher werden keine erfolgreich übertragenen/importierten Dateien behauptet.
- Drei begrenzte `nextTransaction`-Folgeantworten enthielten jeweils 0 Einträge. Der Codepfad beendet dann absichtlich den Lauf ohne weitere Transaktion: `incompleteReason=next-transaction-followup-empty`, `runDrained=false`, `coverage=device-history-unverified`. Das ist die beobachtete leere Antwort am begrenzten Follow-up-Gate, kein Nachweis eines fehlerhaften FIT-Dekoders oder einer vollständigen Historie. Eine weitere Sync-Anfrage ist in diesem Auftrag nicht sinnvoll und wurde nicht gesendet.
- Letzter S10-Leasestatus nach der lesenden Statusprüfung: FREI. Keine weiteren Geräteaktionen geplant.

## Unbekannte FIT-Felder
- Die Geräteseite zeigte vor und nach dem Sync **952 aggregierte Feldtypen/-gruppen**. Das ist keine Zahl separat herunterladbarer Bandobjekte und keine Zahl medizinischer Messwerte. Repository-Evidenz zum bisherigen S10-Export: 10.283 gespeicherte unmapped-field-Inventarzeilen; eine gruppierte Feldverteilung war dort noch nicht veröffentlicht.
- Diese Inventur entsteht lokal beim Replay bereits verifizierter privater FIT-Originale (`verified_fit`); im Produkt-Sync werden nicht einzelne unbekannte Felder, sondern Dateien übertragen. Wenn eine neue FIT-Datei CRC-/Storage-/Importprüfung besteht, kann der Offline-Backfill ihre unmapped-field-Metadaten inventarisieren. Rohwerte bleiben aus der Standardliste ausgeschlossen.
- Der sichtbare Gruppenwert blieb 952; kein neuer Unknown-Field-Zuwachs dieses Laufs ist damit belegt. Ein separater Pull nur „für Unknown Fields“ wäre nicht begründet. Die bekannte FIT-Inventur (u. a. Nachrichten 269/227/297/398/412/397/233) darf nicht als erfolgreich aus diesem S10-Lauf übertragen oder semantisch validiert ausgegeben werden.

## Abgrenzung / Evidenz
- Main-Build 1939 FULL grün auf `b784e56c`; APK-Hash/Signatur und In-place-Installation dokumentiert im vorherigen ENDE. Der FIT-Coverage-PR #547 ist nicht in `main`.
- Kein Nachweis vollständiger Gerätehistorie, erfolgreicher vollständiger FIT-Dateiimporte oder neuer Unknown-Field-Records. Das Endergebnis ist ein begrenzter, angenommener Sync mit leerem Follow-up und ausdrücklich unvollständiger Abdeckung.
