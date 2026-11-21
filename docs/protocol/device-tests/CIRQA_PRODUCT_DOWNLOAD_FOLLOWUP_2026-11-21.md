# CIRQA Produktdownload – redigierter Follow-up-Befund (2026-11-21)

## Umfang und Sicherheitsgrenzen

- Nutzerfreigabe: genau ein erneuter Aufruf der vorhandenen Produktaktion „Historie herunterladen“.
- Exklusive S25-Sperre wurde für die Gerätezugriffe erworben und nach jedem Zugriff freigegeben.
- Vor Bestätigung wurde der Warntext gelesen: Der Sync kann das Band veranlassen, Daten als übertragen zu markieren. Keine Lösch-, Archiv-, SetFlags- oder Reset-Befehle werden von der App gesendet.
- Der Bestätigungsdialog wurde genau einmal bestätigt. Kein Gate-Reset, keine Wiederholung, keine Instrumentation und keine App-Installation.
- Installiert war Build 1667. Die Signatur stimmte mit dem signierten Main-CI-Artefakt Build 1675 überein; dieses neuere Artefakt wurde nicht installiert.
- Full CI für Main `e70aad43` (Run [44919422926](https://github.com/jochenkunkel/pulselibre/actions/runs/44919422926)) wurde während der Auswertung vollständig erfolgreich.

## Beobachtung und Evidenzgrenze

Unmittelbar **vor** dem Bestätigen zeigte die Geräteseite bereits einen beendeten Sync-Status mit `runDrained=false`, `incompleteReason=next-transaction-pending` und `coverage=device-history-unverified`. Der sichtbare Status nannte 35 Listeneinträge, 18 erfolgreiche Dateiübertragungen und 17 lokal verifizierte Manifest-Skips. Der Status **nach** dem einmaligen Bestätigen zeigte dieselben Angaben.

Da die Oberfläche für diesen Status keinen Zeitstempel oder Laufzähler anzeigt und der Text bereits vor dem Bestätigen vorhanden war, lässt sich nicht belastbar unterscheiden, ob der Nachher-Text den neuen Lauf oder den vorherigen Lauf repräsentiert. Daher wird dieser Bericht **nicht** als Nachweis eines neuen vollständigen Downloads gewertet. Die Aktion wurde nicht erneut betätigt.

## Ergebnis

- Eine vollständige Historie ist weiterhin nicht nachgewiesen.
- Der sichtbare Status weist auf eine nicht abgeschlossene Item-List-Sitzung mit angekündigter separater Transaktion hin; der Produktionspfad meldet entsprechend `next-transaction-pending`.
- Keine Behauptung, dass alle übertragenen Dateien neu importiert wurden oder dass sich die Datenbank durch diesen Follow-up geändert hat.
- Ergänzende Protokollevidenz: Zwei separat begrenzte No-Pull-Proben, zuletzt [PR #384](https://github.com/jochenkunkel/pulselibre/pull/384), erhielten jeweils nach einer einzelnen `transaction_id`-Anfrage `status=0` mit 0 Einträgen. Das erklärt, warum eine begrenzte Produkt-Folgeanfrage sinnvoll ist; es beweist weder weitere Dateien noch vollständige Gerätehistorie.
- Nächster erforderlicher Schritt im Code: genau eine separate Transaktions-Folgeanfrage pro Produktlauf zulassen, mögliche Listeneinträge über den bestehenden Pull-/FIT-/SQLite-Pfad verarbeiten und danach fail-closed stoppen. Der Laufstatus bekommt eine eindeutige lokale ID. Keine weitere Gerätekette.
