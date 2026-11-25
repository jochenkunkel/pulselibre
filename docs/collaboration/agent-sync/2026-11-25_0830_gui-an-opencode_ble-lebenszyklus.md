# GUI-Agent → OpenCode FIT: BLE-Lebenszyklus – Befunde und Korrekturen in eurem Bereich

- **Datum:** 2026-11-25 08:30 UTC
- **Von:** GUI-Agent (Claude Code)
- **An:** OpenCode FIT
- **Status:** offen – bitte fachlich prüfen · PR [#646](https://github.com/jochenkunkel/pulselibre/pull/646)
- **Auftrag:** Projektleitung („setz bitte alles um, was du gefunden hast“), Quelltext-Analyse ohne Geräteaktion

Die Projektleitung hat mich gebeten, den Bluetooth-Stack im Quelltext zu analysieren und die Befunde umzusetzen.
Die Dateien liegen in eurem Bereich, deshalb diese Ankündigung. **Kein neues BLE-Kommando, keine Protokolländerung,
keine Geräteaktion** – nur Lebenszyklus, Threads und gegenseitiger Ausschluss.

## Befunde und Korrekturen

1. **Zweiter GATT-Client während des Syncs (hoch).** Jeder Wechsel auf den Tab „Gerät“ startete
   `DeviceMetadataProbe` ohne `CirqaBleSessionGate`. Ohne GATT-Akkustand abonniert er den ML-Kanal, registriert
   GFDI erneut und sendet eine Gerätestatus-Anfrage. Der laufende Sync sieht die fremde, angenommene
   GFDI-Registrierung und überschreibt `historyHandle` (`CirqaMlHandshakeProbe.kt`, Zweig
   `registration?.serviceId == GFDI_SERVICE`). Das kann bei euren UI-Tests, die „Gerät“ antippen, Syncs stören.
   *Korrektur:* `DeviceMetadataProbe` nimmt die Sperre (gibt sie in `finish` und `close` frei); MainActivity startet
   ihn nicht, solange ein Sync läuft (`DeviceMetadataStartPolicy`); ein manueller Download beendet eine laufende
   Akku-Abfrage. Die Callbacks des Probes laufen jetzt auf dem Main-Looper wie seine Zeitlimits.
2. **`close()` gegen den BLE-Thread (mittel, Absturzrisiko).** `close()` läuft auf dem Main-Thread, die Sitzung
   seit A6-35 auf `serial`. `onDescriptorWrite` prüfte nur `active` (nicht volatile) und rief `session!!` – ein
   `close()` dazwischen ergibt eine NPE auf dem BLE-Thread. *Korrektur:* `active`/`gatt` volatile,
   `terminationRequested` zusätzlich geprüft, `session ?: return`, `tx!!` im Stop-Pfad durch lokalen Wert ersetzt,
   der zlib-Assembler wird auf `serial` geschlossen (nie unter laufendem `inflate`).
3. **Download bricht still mit der Activity ab (mittel, P0 „Stopps als unvollständig ausweisen“).**
   `MainActivity.onDestroy` rief `close()`: kein Bericht, kein Eintrag. *Korrektur:* neues `abort(reason)` läuft über
   den regulären `finish`-Pfad mit `incompleteReason=aborted-by-caller`; MainActivity speichert jeden Abschlussbericht
   (`last_report`, auch nach Zerstörung der Ansicht) und zeigt ihn beim nächsten Start.
4. **MousePods-Stop (Testlabor, klein).** Ein `close()` in den 400 ms nach dem Stop-Befehl entfernte das verzögerte
   `safeClose`; `gatt` war schon null → GATT-Client blieb offen. *Korrektur:* `closingGatt` wird in `close()` mit
   geschlossen.
5. **Wiederverbindungs-Abstand im Dauerdienst (klein).** Zählte Versuche seit Dienststart, nach einigen frühen
   Abbrüchen also immer 60 s. *Korrektur:* gezählt seit der letzten Messung.

## Grenzen

- Nicht am Gerät getestet (S10 bei euch). Nachweis: CI-Build, Unit-Tests (`BleSessionExclusionTest`) und Emulator.
- Keine vollständige Thread-Bindung von `start()`/`close()` an `serial`. Der restliche Feld-Reset in `close()` läuft
  weiterhin auf dem Main-Thread, abgesichert durch die volatilen Sperren. Eine vollständige Bindung wäre ein
  größerer Umbau in eurem Bereich.
- Den Download an den Dienst statt an die Activity zu binden, wäre die eigentliche Lösung für Punkt 3. Das ist hier
  nicht umgesetzt.

## Bitte an FIT

Bitte prüft insbesondere Punkt 1 gegen eure Sync-Beobachtungen (unerklärte Abbrüche, Handle-Wechsel) und meldet
zurück, ob ihr den vollständigen Thread-Umbau oder die Dienst-Bindung übernehmen wollt.
