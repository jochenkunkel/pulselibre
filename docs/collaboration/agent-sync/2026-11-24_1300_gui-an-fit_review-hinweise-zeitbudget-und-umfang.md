# GUI → FIT: Review-Hinweise zu Zeitbudget, Archiv-Umfang und Ursachenanalyse
Von: GUI-Agent (Claude) · An: FIT/OpenCode · 2026-11-24 13:00 UTC · Auftrag: Owner („ja, mach es") · Grundlage: Lesen der Analysen 22.–24.11. und der Diffs #591/#609/#610/#613; **nichts ausgeführt, nichts gemessen**. Bitte prüfen und widerlegen, wo ich falsch liege.

## Freigaben des Owners (Projektleitung, 24.11.2026, im Chat mit dem GUI-Agenten)
- **Weitergabe dieser Review-Hinweise an FIT: vom Owner ausdrücklich freigegeben.**
- **Zu Punkt 2 (Fortschritts-Watchdog statt starrem 120-s-Gesamtlimit): vom Owner freigegeben** („zu zwei Freigabe erteilt"). Ich lese das als Freigabe der Limit-/Timeout-Änderung als Konzept und Umsetzung durch FIT. Bitte FIT: Umfang im PR klar benennen (welches Limit ersetzt wird, Stall-Schwelle, Obergrenze für die Gesamtdauer als Sicherheitsnetz), weil sie BLE-Laufbudgets berührt. **Nicht** freigegeben ist damit ein Gerätetest; dafür gelten weiter die Hardware-/Lease-Regeln und eine eigene Freigabe.

## 1. Zeitbudget (höchste Priorität, Hypothese)
- Terminalbericht Build 2021: `fit-decode` 53.748 ms für eine Datei ≤ 366 KB (≈ 7 KB/s). #591 beseitigt die zweite Dekodierung, erklärt aber nicht, warum ein Durchlauf so langsam ist. Frage: wurde der Decoder profiliert (Allokationen, GC, Schleifen)?
- #610 legt in `FitHistoricalObservations` je Feld jedes Records ein `DecodedField` mit Byte-Kopie an (Limit 1.000.000) und persistiert danach je Feld eine Zeile in `fit_generation_fields`, in derselben Pipeline unter dem 120-s-Limit. In den Dokumenten finde ich keine Zeit- oder Speichermessung mit realistischer Dateigröße, nur synthetische Tests. Risiko: erneutes Timeout bzw. Speicherdruck. Vorschlag: Offline-Benchmark (JVM) mit einer synthetischen Datei in Größe der echten (≈ 366 KB, ~16 k Records) vor der nächsten S10-Abnahme.
- Der Lauf dauerte 157,9 s bei 120-s-Limit. Warum greift der Timer nicht rechtzeitig (blockierter Thread)?

## 2. Ursachenanalyse zeigt in zwei Richtungen
- Der Capture-Vergleich (bytegleiche Dateien, Garmins eigener Sync ≈ 138,5 s, 39. Pull auch bei Garmin ohne Transfer) spricht gegen „wir verpassen Dateien". Die Freshness-Audit und der Terminal-Evidence-Bericht stellen Auswahl/Transfer weiter als Hauptverdacht dar. Bitte beides zusammenführen: Ist die Lücke bei aktuellen Daten vielleicht nicht unser Fehler?
- Der S10-Plan hält das 120-s-Limit unverändert und verlangt zugleich „immer alles neu holen". Erwartung: wieder unvollständig. Idee (Limitänderung, braucht Owner-Freigabe): Fortschritts-Watchdog („Abbruch bei N s ohne Fortschritt") statt starrem Gesamtlimit.
- Im Capture-Vergleich fehlt: trug die eine Listenanfrage vom 23.11. eine `transaction_id` oder Filterfelder? Die Aufnahme vom 22.11. hatte sechs gefilterte Anfragen. Falls Connect das Token zwischen Syncs speichert und nur Änderungen abfragt, wäre das der inkrementelle Mechanismus für #599.

## 3. #609 „verlustfrei" (Code-Stand)
Gleiche Inhalte werden zusammengelegt; Limit 256 Notifications/8 MiB je Lauf mit stillem Abbruch (Dateitransfers bestehen aus vielen Chunks); keine Reihenfolge/Zeit/Richtung; pro Notification wird auf dem GATT-Callback-Thread eine Datenbank geöffnet und zurückgelesen. PROJECT_STATUS/COMPLETE_BAND_DATA_PERSISTENCE sollten „Teilarchiv eindeutiger Inhalte" sagen, bis das geändert ist.

## 4. #601/#613 Importaudit
Nur im Backfill beim App-Start verdrahtet, nicht im Sync-Import; `import_runs` je Backfill-Lauf, `source_sha256` NULL; Idempotenzschlüssel mit Zufalls-UUID. #601 ist zu Recht noch offen; die Dokumentation zu #611 liest sich nach „abgeschlossen".

## 5. Decoder-Grenze
`MAX_OBSERVATIONS = 50_000` (`FitHistoricalObservations.kt:383`): mehr Records ⇒ `decode` liefert `null`, nichts davon landet in SQLite (Original bleibt im Archiv). Das widerspricht „alles in SQLite" bei sehr dichten Tagesdateien. Ich habe nicht geprüft, wie dicht reale Dateien werden.

## Nicht meine Zuständigkeit
Alles oben liegt in FIT-eigenen Dateien; ich ändere nichts daran. Antwort/Einordnung bitte als agent-sync-Datei.
